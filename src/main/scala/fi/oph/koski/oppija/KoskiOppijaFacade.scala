package fi.oph.koski.oppija

import com.typesafe.config.Config
import fi.oph.koski.db.GlobalExecutionContext
import fi.oph.koski.henkilo._
import fi.oph.koski.history.OpiskeluoikeusHistoryRepository
import fi.oph.koski.http.{HttpStatus, KoskiErrorCategory}
import fi.oph.koski.koskiuser.KoskiSession
import fi.oph.koski.log.KoskiMessageField.{opiskeluoikeusId, opiskeluoikeusVersio, oppijaHenkiloOid}
import fi.oph.koski.log.KoskiOperation._
import fi.oph.koski.log.{AuditLog, _}
import fi.oph.koski.opiskeluoikeus._
import fi.oph.koski.perustiedot.{OpiskeluoikeudenPerustiedot, OpiskeluoikeudenPerustiedotIndexer}
import fi.oph.koski.schema._
import fi.oph.koski.util.Timing

class KoskiOppijaFacade(henkilöRepository: HenkilöRepository, OpiskeluoikeusRepository: OpiskeluoikeusRepository, historyRepository: OpiskeluoikeusHistoryRepository, perustiedotIndexer: OpiskeluoikeudenPerustiedotIndexer, config: Config) extends Logging with Timing with GlobalExecutionContext {
  private lazy val mockOids = config.hasPath("authentication-service.mockOid") && config.getBoolean("authentication-service.mockOid")
  def findOppija(oid: String)(implicit user: KoskiSession): Either[HttpStatus, Oppija] = toOppija(oid, OpiskeluoikeusRepository.findByOppijaOid(oid))

  def findVersion(oid: String, opiskeluoikeusOid: String, versionumero: Int)(implicit user: KoskiSession): Either[HttpStatus, Oppija] = {
    // TODO: tarkista, että opiskeluoikeus kuuluu tälle oppijalle
    historyRepository.findVersion(opiskeluoikeusOid, versionumero).right.flatMap { history =>
      toOppija(oid, List(history))
    }
  }

  def findUserOppija(implicit user: KoskiSession): Either[HttpStatus, Oppija] = toOppija(user.oid, OpiskeluoikeusRepository.findByUserOid(user.oid))

  def createOrUpdate(oppija: Oppija, allowUpdate: Boolean)(implicit user: KoskiSession): Either[HttpStatus, HenkilönOpiskeluoikeusVersiot] = {
    val oppijaOid: Either[HttpStatus, PossiblyUnverifiedHenkilöOid] = oppija.henkilö match {
      case h:UusiHenkilö if h.hetu.isDefined =>
        Hetu.validate(h.hetu.get, acceptSynthetic = false).right.flatMap { hetu =>
          henkilöRepository.findOrCreate(h).right.map(VerifiedHenkilöOid(_))
        }
      case h:TäydellisetHenkilötiedot if mockOids =>
        Right(VerifiedHenkilöOid(h))
      case h:HenkilöWithOid =>
        Right(UnverifiedHenkilöOid(h.oid, henkilöRepository))
    }

    timed("createOrUpdate") {
      val opiskeluoikeudet: Seq[KoskeenTallennettavaOpiskeluoikeus] = oppija.tallennettavatOpiskeluoikeudet

      oppijaOid.right.flatMap { oppijaOid: PossiblyUnverifiedHenkilöOid =>
        if (oppijaOid.oppijaOid == user.oid) {
          Left(KoskiErrorCategory.forbidden.omienTietojenMuokkaus())
        } else {
          val opiskeluoikeusCreationResults: Seq[Either[HttpStatus, OpiskeluoikeusVersio]] = opiskeluoikeudet.map { opiskeluoikeus =>
            createOrUpdateOpiskeluoikeus(oppijaOid, opiskeluoikeus, allowUpdate)
          }

          opiskeluoikeusCreationResults.find(_.isLeft) match {
            case Some(Left(error)) => Left(error)
            case _ => Right(HenkilönOpiskeluoikeusVersiot(OidHenkilö(oppijaOid.oppijaOid), opiskeluoikeusCreationResults.toList.map {
              case Right(r) => r
              case Left(_) => throw new RuntimeException("Unreachable match arm: Left")
            }))
          }
        }
      }
    }
  }

  private def createOrUpdateOpiskeluoikeus(oppijaOid: PossiblyUnverifiedHenkilöOid, opiskeluoikeus: KoskeenTallennettavaOpiskeluoikeus, allowUpdate: Boolean)(implicit user: KoskiSession): Either[HttpStatus, OpiskeluoikeusVersio] = {
    def applicationLog(oppijaOid: PossiblyUnverifiedHenkilöOid, opiskeluoikeus: Opiskeluoikeus, result: CreateOrUpdateResult): Unit = {
      val verb = result match {
        case updated: Updated =>
          val tila = updated.old.tila.opiskeluoikeusjaksot.last
          if (tila.opiskeluoikeusPäättynyt) {
            s"Päivitetty päättynyt (${tila.tila.koodiarvo})"
          } else {
            "Päivitetty"
          }
        case _: Created => "Luotu"
        case _: NotChanged => "Päivitetty (ei muutoksia)"
      }
      val tutkinto = opiskeluoikeus.suoritukset.map(_.koulutusmoduuli.tunniste).mkString(",")
      val oppilaitos = opiskeluoikeus.getOppilaitos.oid
      logger(user).info(s"${verb} opiskeluoikeus ${result.id} (versio ${result.versionumero}) oppijalle ${oppijaOid} tutkintoon ${tutkinto} oppilaitoksessa ${oppilaitos}")
    }

    def auditLog(oppijaOid: PossiblyUnverifiedHenkilöOid, result: CreateOrUpdateResult): Unit = {
      (result match {
        case _: Updated => Some(OPISKELUOIKEUS_MUUTOS)
        case _: Created => Some(OPISKELUOIKEUS_LISAYS)
        case _ => None
      }).foreach { operaatio =>
        AuditLog.log(AuditLogMessage(operaatio, user,
          Map(oppijaHenkiloOid -> oppijaOid.oppijaOid, opiskeluoikeusId -> result.id.toString, opiskeluoikeusVersio -> result.versionumero.toString))
        )
      }
    }

    if (oppijaOid.oppijaOid == user.oid) {
      Left(KoskiErrorCategory.forbidden.omienTietojenMuokkaus())
    } else {
      val result = OpiskeluoikeusRepository.createOrUpdate(oppijaOid, opiskeluoikeus, allowUpdate)
      result.right.map { (result: CreateOrUpdateResult) =>
        applicationLog(oppijaOid, opiskeluoikeus, result)
        auditLog(oppijaOid, result)

        val nimitiedotJaOid = oppijaOid.verified.getOrElse(throw new RuntimeException(s"Oppijaa {${oppijaOid.oppijaOid}} ei löydy")) // TODO: päivitystapauksessa ei haeta henkilöä, päivitetään muut tiedon elasticsearchiin
        val oo = opiskeluoikeus

        if (result.changed) {
          val perustiedot = OpiskeluoikeudenPerustiedot.makePerustiedot(result.id, result.data, oo.luokka.orElse(oo.ryhmä), nimitiedotJaOid)
          perustiedotIndexer.update(perustiedot)
        }

        OpiskeluoikeusVersio(result.oid, result.versionumero)
      }
    }
  }

  // Hakee oppijan oppijanumerorekisteristä ja liittää siihen opiskeluoikeudet. Opiskeluoikeudet haetaan vain, jos oppija löytyy.
  private def toOppija(oid: Henkilö.Oid, opiskeluoikeudet: => Seq[Opiskeluoikeus])(implicit user: KoskiSession) = {
    def notFound = Left(KoskiErrorCategory.notFound.oppijaaEiLöydyTaiEiOikeuksia("Oppijaa " + oid + " ei löydy tai käyttäjällä ei ole oikeuksia tietojen katseluun."))
    val result = henkilöRepository.findByOid(oid) match {
      case Some(oppija) =>
        opiskeluoikeudet match {
          case Nil => notFound
          case opiskeluoikeudet: Seq[Opiskeluoikeus] => Right(Oppija(oppija, opiskeluoikeudet))
        }
      case None => notFound
    }

    result.right.foreach((oppija: Oppija) => writeViewingEventToAuditLog(user, oid))
    result
  }

  private def writeViewingEventToAuditLog(user: KoskiSession, oid: Henkilö.Oid): Unit = {
    if (user != KoskiSession.systemUser) { // To prevent health checks from polluting the audit log
      AuditLog.log(AuditLogMessage(OPISKELUOIKEUS_KATSOMINEN, user, Map(oppijaHenkiloOid -> oid)))
    }
  }
}

case class HenkilönOpiskeluoikeusVersiot(henkilö: OidHenkilö, opiskeluoikeudet: List[OpiskeluoikeusVersio])
case class OpiskeluoikeusVersio(oid: Opiskeluoikeus.Oid, versionumero: Int)