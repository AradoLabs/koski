package fi.oph.koski.tiedonsiirto

import java.sql.Timestamp
import java.time.LocalDate

import fi.oph.koski.elasticsearch.ElasticSearch
import fi.oph.koski.henkilo.HenkilöRepository
import fi.oph.koski.http.Http._
import fi.oph.koski.http._
import fi.oph.koski.json.Json.{fromJValue, toJValue}
import fi.oph.koski.json._
import fi.oph.koski.koodisto.KoodistoViitePalvelu
import fi.oph.koski.koskiuser.{AccessType, KoskiSession, KoskiUserInfo, KoskiUserRepository}
import fi.oph.koski.localization.LocalizedString
import fi.oph.koski.log.KoskiMessageField._
import fi.oph.koski.log.KoskiOperation._
import fi.oph.koski.log.{AuditLog, AuditLogMessage, Logging}
import fi.oph.koski.organisaatio.OrganisaatioRepository
import fi.oph.koski.perustiedot.KoskiElasticSearchIndex
import fi.oph.koski.schema.JsonSerializer.extract
import fi.oph.koski.schema._
import fi.oph.koski.util._
import io.prometheus.client.Counter
import org.json4s.JsonAST.{JArray, JString}
import org.json4s.jackson.JsonMethods
import org.json4s.{JValue, _}


class TiedonsiirtoService(index: KoskiElasticSearchIndex, mailer: TiedonsiirtoFailureMailer, organisaatioRepository: OrganisaatioRepository, henkilöRepository: HenkilöRepository, koodistoviitePalvelu: KoodistoViitePalvelu, userRepository: KoskiUserRepository) extends Logging with Timing {
  private val tiedonSiirtoVirheet = Counter.build().name("fi_oph_koski_tiedonsiirto_TiedonsiirtoService_virheet").help("Koski tiedonsiirto virheet").register()

  def deleteAll: Unit = {
    val doc = toJValue(Map("query" -> Map("match_all" -> Map())))

    val deleted = Http.runTask(index.http
      .post(uri"/koski/tiedonsiirto/_delete_by_query", doc)(Json4sHttp4s.json4sEncoderOf[JValue]) {
        case (200, text, request) => extract[Int](Json.parse(text) \ "deleted")
        case (status, text, request) if List(404, 409).contains(status) => 0
        case (status, text, request) => throw HttpStatusException(status, text, request)
      })

    logger.info(s"Tyhjennetty tiedonsiirrot ($deleted)")
  }

  def haeTiedonsiirrot(query: TiedonsiirtoQuery)(implicit koskiSession: KoskiSession): Either[HttpStatus, PaginatedResponse[Tiedonsiirrot]] = {
    haeTiedonsiirrot(filtersFrom(query), query.oppilaitos, query.paginationSettings)
  }

  def virheelliset(query: TiedonsiirtoQuery)(implicit koskiSession: KoskiSession): Either[HttpStatus, PaginatedResponse[Tiedonsiirrot]] = {
    haeTiedonsiirrot(Map("exists" -> Map("field" -> "virheet.key")) :: filtersFrom(query), query.oppilaitos, query.paginationSettings)
  }

  private def filtersFrom(query: TiedonsiirtoQuery)(implicit session: KoskiSession): List[Map[String, Any]] = {
    query.oppilaitos.toList.map(oppilaitos => Map("term" -> Map("oppilaitokset.oid" -> oppilaitos))) ++ tallentajaOrganisaatioFilters
  }

  private def tallentajaOrganisaatioFilters(implicit session: KoskiSession): List[Map[String, Any]] = tallentajaOrganisaatioFilter.toList

  private def tallentajaOrganisaatioFilter(implicit session: KoskiSession): Option[Map[String, Any]] =
    if (session.hasGlobalReadAccess) {
      None
    } else {
      Some(ElasticSearch.anyFilter(List(
        Map("terms" -> Map("tallentajaOrganisaatioOid" -> session.organisationOids(AccessType.read))),
        Map("terms" -> Map("oppilaitokset.oid" -> session.organisationOids(AccessType.read)))
      )))
    }

  private def haeTiedonsiirrot(filters: List[Map[String, Any]], oppilaitosOid: Option[String], paginationSettings: Option[PaginationSettings])(implicit koskiSession: KoskiSession): Either[HttpStatus, PaginatedResponse[Tiedonsiirrot]] = {
    AuditLog.log(AuditLogMessage(TIEDONSIIRTO_KATSOMINEN, koskiSession, Map(juuriOrganisaatio -> koskiSession.juuriOrganisaatio.map(_.oid).getOrElse("ei juuriorganisaatiota"))))

    val doc = Json.toJValue(ElasticSearch.applyPagination(paginationSettings, Map(
      "query" -> ElasticSearch.allFilter(filters),
      "sort" -> List(Map("aikaleima" -> "desc"), Map("oppija.sukunimi.keyword" -> "asc"), Map("oppija.etunimet.keyword" -> "asc"))
    )))

    val rows: Seq[TiedonsiirtoDocument] = runSearch(doc)
      .map(response => extract[List[JValue]](response \ "hits" \ "hits").map(j => extract[TiedonsiirtoDocument](j \ "_source", validating = false)))
      .getOrElse(Nil)

    val oppilaitosResult: Either[HttpStatus, Option[Oppilaitos]] = oppilaitosOid match {
      case Some(oppilaitosOid) =>
        val oppilaitos: Option[Oppilaitos] = organisaatioRepository.getOrganisaatioHierarkia(oppilaitosOid).flatMap(_.toOppilaitos)
        oppilaitos match {
          case Some(oppilaitos) => Right(Some(oppilaitos))
          case None => Left(KoskiErrorCategory.notFound.oppilaitostaEiLöydy(s"Oppilaitosta $oppilaitosOid ei löydy"))
        }
      case None =>
        Right(None)
    }

    oppilaitosResult.right.map { oppilaitos =>
      val converted: Tiedonsiirrot = Tiedonsiirrot(toHenkilönTiedonsiirrot(rows), oppilaitos = oppilaitos.map(_.toOidOrganisaatio))
      PaginatedResponse(paginationSettings, converted, rows.length)
    }
  }

  private def runSearch(doc: JValue) = {
    try {
      val response = Http.runTask(index.http.post(uri"/koski/tiedonsiirto/_search", doc)(Json4sHttp4s.json4sEncoderOf[JValue])(Http.parseJson[JValue]))
      Some(response)
    } catch {
      case e: HttpStatusException if e.status == 400 =>
        logger.warn(e.getMessage)
        None
    }
  }

  def storeTiedonsiirtoResult(implicit koskiSession: KoskiSession, oppijaOid: Option[OidHenkilö], validatedOppija: Option[Oppija], data: Option[JValue], error: Option[TiedonsiirtoError]) {
    if (!koskiSession.isPalvelukäyttäjä && !koskiSession.isRoot) {
      return
    }

    val henkilö = data.flatMap(extractHenkilö(_, oppijaOid))
    val lahdejarjestelma: Option[String] = data.flatMap(extractLahdejarjestelma)
    val oppilaitokset: Option[List[OidOrganisaatio]] = data.map(_ \ "opiskeluoikeudet" \ "oppilaitos" \ "oid").map(jsonStringList).map(_.flatMap(organisaatioRepository.getOrganisaatio).map(_.toOidOrganisaatio))
    val koulutustoimija: Option[OidOrganisaatio] = validatedOppija.flatMap(_.opiskeluoikeudet.headOption.flatMap(_.koulutustoimija.map(_.toOidOrganisaatio)))

    val juuriOrganisaatio = if (koskiSession.isRoot) koulutustoimija else koskiSession.juuriOrganisaatio

    juuriOrganisaatio.foreach((org: OrganisaatioWithOid) => {
      val (data: Option[JValue], virheet: Option[List[ErrorDetail]]) = error.map(e => (Some(e.data), Some(e.virheet))).getOrElse((None, None))

      storeToElasticSearch(henkilö, org, oppilaitokset, data, virheet, lahdejarjestelma, koskiSession.oid, new Timestamp(System.currentTimeMillis))

      if (error.isDefined) {
        tiedonSiirtoVirheet.inc
        mailer.sendMail(org.oid)
      }
    })
  }

  def storeToElasticSearch(henkilö: Option[TiedonsiirtoOppija], org: OrganisaatioWithOid,
                                   oppilaitokset: Option[List[OidOrganisaatio]], data: Option[JValue],
                                   virheet: Option[List[ErrorDetail]], lahdejarjestelma: Option[String],
                                    userOid: String, aikaleima: Timestamp) = {

    val idValue: String = henkilö.flatMap(h => h.hetu.orElse(h.oid)).getOrElse("")

    val document = TiedonsiirtoDocument(userOid, org.oid, henkilö, oppilaitokset, data, virheet.toList.flatten.isEmpty, virheet.getOrElse(Nil), lahdejarjestelma, aikaleima)

    val documentId = org.oid + "_" + idValue
    val docJson = JsonSerializer.serializeWithRoot(document).merge(JObject(if (data.isDefined) Nil else List("data" -> JNull))) // TODO: should replace all missing Options with JNull
    val json: JValue = JObject("doc_as_upsert" -> JBool(true), "doc" -> docJson)

    val response = Http.runTask(index.http.post(uri"/koski/tiedonsiirto/${documentId}/_update?retry_on_conflict=5", json)(Json4sHttp4s.json4sEncoderOf[JValue])(Http.parseJson[JValue]))

    val result = extract[String](response \ "result")
    if (!(List("created", "updated", "noop").contains(result))) {
      val msg = s"Elasticsearch indexing failed: ${JsonMethods.pretty(response)}"
      logger.error(msg)
      Left(KoskiErrorCategory.internalError(msg))
    } else {
      val itemResults = extract[Option[List[JValue]]](response \ "items").toList.flatten.map(_ \ "update" \ "_shards" \ "successful").map(extract[Int](_))
      Right(itemResults.sum)
    }
  }

  def yhteenveto(implicit koskiSession: KoskiSession, sorting: SortOrder): Seq[TiedonsiirtoYhteenveto] = {
    var ordering = sorting.field match {
      case "aika" => Ordering.by{x: TiedonsiirtoYhteenveto => x.viimeisin.getTime}
      case "oppilaitos" => Ordering.by{x: TiedonsiirtoYhteenveto => x.oppilaitos.description.get(koskiSession.lang)}
    }
    if (sorting.descending) ordering = ordering.reverse

    val query = Json.toJValue(Map(
      "size" -> 0,
      "aggs" ->
        Map(
          "organisaatio"-> Map(
            "terms"-> Map( "field"-> "tallentajaOrganisaatioOid.keyword", "size" -> 20000 ),
            "aggs"-> Map(
              "oppilaitos"-> Map(
                "terms"-> Map( "field"-> "oppilaitokset.oid.keyword", "size" -> 20000 ),
                "aggs"-> Map(
                  "käyttäjä"-> Map(
                    "terms"-> Map( "field"-> "tallentajaKäyttäjäOid.keyword", "size" -> 20000 ),
                    "aggs"-> Map(
                      "lähdejärjestelmä"-> Map(
                        "terms"-> Map( "field"-> "lähdejärjestelmä.keyword", "size" -> 20000, "missing"-> "-" ),
                        "aggs"-> Map(
                          "viimeisin" -> Map( "max" -> Map( "field" -> "aikaleima" ) ),
                          "fail"-> Map(
                            "filter"-> Map( "term"-> Map( "success"-> false )))
                        )
                      )
                    )
                  )
                )
              )
            )
          )
        )
    ) ++ tallentajaOrganisaatioFilter.map(filter => Map("query" -> filter)).getOrElse(Map()))

    runSearch(query).map { response =>
      for {
        orgResults <- extract[List[JValue]](response \ "aggregations" \ "organisaatio" \ "buckets")
        tallentajaOrganisaatio = getOrganisaatio(extract[String](orgResults \ "key"))
        oppilaitosResults <- extract[List[JValue]](orgResults \ "oppilaitos" \ "buckets")
        oppilaitos = getOrganisaatio(extract[String](oppilaitosResults \ "key"))
        userResults <- extract[List[JValue]](oppilaitosResults \ "käyttäjä" \ "buckets")
        userOid = extract[String](userResults \ "key")
        käyttäjä = userRepository.findByOid(userOid) getOrElse {
          logger.warn(s"Käyttäjää ${userOid} ei löydy henkilöpalvelusta")
          KoskiUserInfo(userOid, None, None)
        }
        lähdejärjestelmäResults <- extract[List[JValue]](userResults \ "lähdejärjestelmä" \ "buckets")
        lähdejärjestelmäId = extract[String](lähdejärjestelmäResults \ "key")
        lähdejärjestelmä = koodistoviitePalvelu.getKoodistoKoodiViite("lahdejarjestelma", lähdejärjestelmäId)
        siirretyt = extract[Int](lähdejärjestelmäResults \ "doc_count")
        epäonnistuneet = extract[Int](lähdejärjestelmäResults \ "fail" \ "doc_count")
        onnistuneet = siirretyt - epäonnistuneet
        viimeisin = new Timestamp(extract[Long](lähdejärjestelmäResults \ "viimeisin" \ "value"))
      } yield {
        TiedonsiirtoYhteenveto(tallentajaOrganisaatio, oppilaitos, käyttäjä, viimeisin, siirretyt, epäonnistuneet, onnistuneet, lähdejärjestelmä)
      }
    }.getOrElse(Nil).sorted(ordering)
  }

  private def getOrganisaatio(oid: String) = organisaatioRepository.getOrganisaatio(oid).map(_.toOidOrganisaatio).getOrElse(OidOrganisaatio(oid, Some(LocalizedString.unlocalized(oid))))

  private def jsonStringList(value: JValue) = value match {
    case JArray(xs) => xs.collect { case JString(x) => x }
    case JString(x) => List(x)
    case JNothing => Nil
    case JNull => Nil
    case _ => throw new RuntimeException("Unreachable match arm" )
  }

  private def extractLahdejarjestelma(data: JValue): Option[String] = {
    data \ "opiskeluoikeudet" match {
      case JArray(opiskeluoikeudet) =>
        val lähdejärjestelmä: List[String] = opiskeluoikeudet.flatMap { opiskeluoikeus: JValue =>
          opiskeluoikeus \ "lähdejärjestelmänId" \ "lähdejärjestelmä" \ "koodiarvo" match {
            case JString(lähdejärjestelmä) => Some(lähdejärjestelmä)
            case _ => None
          }
        }
        lähdejärjestelmä.headOption
      case _ => None
    }
  }

  private def extractHenkilö(data: JValue, oidHenkilö: Option[OidHenkilö])(implicit user: KoskiSession): Option[TiedonsiirtoOppija] = {
    val annetutHenkilötiedot: JValue = data \ "henkilö"
    val annettuTunniste: HetuTaiOid = fromJValue[HetuTaiOid](annetutHenkilötiedot)
    val oid: Option[String] = oidHenkilö.map(_.oid).orElse(annettuTunniste.oid)

    val haetutTiedot: Option[TiedonsiirtoOppija] = (oid, annettuTunniste.hetu) match {
      case (Some(oid), None) => henkilöRepository.findByOid(oid).map { h =>
        TiedonsiirtoOppija(Some(h.oid), h.hetu, h.syntymäaika, Some(h.etunimet), Some(h.kutsumanimi), Some(h.sukunimi), h.äidinkieli)
      }
      case (None, Some(hetu)) => henkilöRepository.findOppijat(hetu).headOption.map { h =>
        TiedonsiirtoOppija(Some(h.oid), h.hetu, syntymäaika = None, Some(h.etunimet), Some(h.kutsumanimi), Some(h.sukunimi), äidinkieli = None)
      }
      case _ => None
    }

    haetutTiedot.orElse(oidHenkilö match {
      case Some(oidHenkilö) => Some(extract[TiedonsiirtoOppija](annetutHenkilötiedot.merge(toJValue(oidHenkilö)), validating = false))
      case None => annetutHenkilötiedot.toOption.map(extract[TiedonsiirtoOppija](_, validating = false))
    })
  }

  private def toHenkilönTiedonsiirrot(tiedonsiirrot: Seq[TiedonsiirtoDocument]): List[HenkilönTiedonsiirrot] = {
    tiedonsiirrot.map { row =>
      val rivi = TiedonsiirtoRivi(Math.random().toInt /*TODO tarvitaanko id?*/, row.aikaleima, row.oppija, row.oppilaitokset.getOrElse(Nil), row.virheet, row.data, row.lähdejärjestelmä)
      HenkilönTiedonsiirrot(row.oppija, List(rivi))
    }.toList
  }

  lazy val init = {
    index.init

    val mappings = Json.toJValue(Map("properties" -> Map(
      "virheet" -> Map(
        "properties" -> Map(
          "key" -> Map(
            "type" -> "text"
          )
        ),
        "dynamic" -> false
      ),
      "data" -> Map(
        "properties" -> Map(
        ),
        "dynamic" -> false
      )
    )))

    Http.runTask(index.http.put(uri"/koski-index/_mapping/tiedonsiirto", Json.toJValue(mappings))(Json4sHttp4s.json4sEncoderOf)(Http.parseJson[JValue]))
  }
}

case class Tiedonsiirrot(henkilöt: List[HenkilönTiedonsiirrot], oppilaitos: Option[OidOrganisaatio])
case class HenkilönTiedonsiirrot(oppija: Option[TiedonsiirtoOppija], rivit: Seq[TiedonsiirtoRivi])
case class TiedonsiirtoRivi(id: Int, aika: Timestamp, oppija: Option[TiedonsiirtoOppija], oppilaitos: List[OidOrganisaatio], virhe: List[ErrorDetail], inputData: Option[JValue], lähdejärjestelmä: Option[String])
case class TiedonsiirtoOppija(oid: Option[String], hetu: Option[String], syntymäaika: Option[LocalDate], etunimet: Option[String], kutsumanimi: Option[String], sukunimi: Option[String], äidinkieli: Option[Koodistokoodiviite])
case class HetuTaiOid(oid: Option[String], hetu: Option[String])
case class TiedonsiirtoYhteenveto(tallentajaOrganisaatio: OidOrganisaatio, oppilaitos: OidOrganisaatio, käyttäjä: KoskiUserInfo, viimeisin: Timestamp, siirretyt: Int, virheelliset: Int, onnistuneet: Int, lähdejärjestelmä: Option[Koodistokoodiviite])
case class TiedonsiirtoQuery(oppilaitos: Option[String], paginationSettings: Option[PaginationSettings])
case class TiedonsiirtoKäyttäjä(oid: String, nimi: Option[String])
case class TiedonsiirtoError(data: JValue, virheet: List[ErrorDetail])

case class TiedonsiirtoDocument(tallentajaKäyttäjäOid: String, tallentajaOrganisaatioOid: String, oppija: Option[TiedonsiirtoOppija], oppilaitokset: Option[List[OidOrganisaatio]], data: Option[JValue], success: Boolean, virheet: List[ErrorDetail], lähdejärjestelmä: Option[String], aikaleima: Timestamp)
