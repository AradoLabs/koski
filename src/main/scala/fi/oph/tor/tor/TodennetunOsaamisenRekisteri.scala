package fi.oph.tor.tor

import fi.oph.tor.arvosana.ArviointiasteikkoRepository
import fi.oph.tor.http.HttpStatus
import fi.oph.tor.json.Json
import fi.oph.tor.opintooikeus._
import fi.oph.tor.oppija._
import fi.oph.tor.oppilaitos.OppilaitosRepository
import fi.oph.tor.tutkinto.{TutkintoRakenne, Suoritustapa, TutkintoRepository}
import fi.oph.tor.user.UserContext

class TodennetunOsaamisenRekisteri(oppijaRepository: OppijaRepository,
                                   opintoOikeusRepository: OpintoOikeusRepository,
                                   tutkintoRepository: TutkintoRepository,
                                   oppilaitosRepository: OppilaitosRepository,
                                   arviointiAsteikot: ArviointiasteikkoRepository) {

  def findOppijat(query: String)(implicit userContext: UserContext): Seq[Oppija] = {
    val oppijat: List[Oppija] = oppijaRepository.findOppijat(query)
    val filtered = opintoOikeusRepository.filterOppijat(oppijat)
    filtered
  }

  def createOrUpdate(oppija: TorOppija)(implicit userContext: UserContext): Either[HttpStatus, Oppija.Id] = {
    if (oppija.opintoOikeudet.length == 0) {
      Left(HttpStatus.badRequest("At least one OpintoOikeus required"))
    }
    else {
      HttpStatus.fold(oppija.opintoOikeudet.map(validateOpintoOikeus)) match {
        case error if error.isError => Left(error)
        case _ =>
          val result = oppijaRepository.findOrCreate(oppija)
          result.right.flatMap { oppijaOid: String =>
            val opintoOikeusCreationResults = oppija.opintoOikeudet.map { opintoOikeus =>
              opintoOikeusRepository.createOrUpdate(oppijaOid, opintoOikeus)
            }
            opintoOikeusCreationResults.find(_.isLeft) match {
              case Some(Left(error)) => Left(error)
              case _ => Right(oppijaOid)
            }
          }
      }
    }
  }

  def validateOpintoOikeus(opintoOikeus: OpintoOikeus)(implicit userContext: UserContext): HttpStatus = {
    tutkintoRepository.findPerusteRakenne(opintoOikeus.tutkinto.ePerusteetDiaarinumero)(arviointiAsteikot) match {
      case None =>
        HttpStatus.badRequest("Invalid ePeruste: " + opintoOikeus.tutkinto.ePerusteetDiaarinumero)
      case Some(rakenne) =>
        HttpStatus.ok.andThenIf(!userContext.hasReadAccess(opintoOikeus.oppilaitosOrganisaatio)) { HttpStatus.forbidden("Forbidden") }
          .andThenIf(opintoOikeus.suoritustapa.filter(!Suoritustapa.apply(_).isDefined)) { suoritustapa => HttpStatus.badRequest("Invalid suoritustapa: " + suoritustapa)}
          .andThenIf(opintoOikeus.osaamisala.filter(osaamisala => !TutkintoRakenne.findOsaamisala(rakenne, osaamisala).isDefined)) { osaamisala => HttpStatus.badRequest("Invalid osaamisala: " + osaamisala) }
          .andThen(HttpStatus.fold(opintoOikeus.suoritukset.map(validateSuoritus(_, rakenne))))
    }
  }

  def validateSuoritus(suoritus: Suoritus, rakenne: TutkintoRakenne): HttpStatus = {
    TutkintoRakenne.findTutkinnonOsa(rakenne, suoritus.koulutusModuuli) match {
      case None => HttpStatus.badRequest("Tuntematon tutkinnon osa: " + suoritus.koulutusModuuli)
      case Some(tutkinnonOsa) =>
        HttpStatus.andThenIf(suoritus.arviointi) { arviointi =>
          HttpStatus.ok
            .andThenIf(arviointi.asteikko != tutkinnonOsa.arviointiAsteikko) { HttpStatus.badRequest("Perusteiden vastainen arviointiasteikko: " + arviointi.asteikko)}
            .ifOkThen {
              rakenne.arviointiAsteikot.find(_.koodisto == arviointi.asteikko) match {
                case Some(asteikko) if (!asteikko.arvosanat.contains(arviointi.arvosana)) =>
                  HttpStatus.badRequest("Arvosana " + Json.write(arviointi.arvosana) + " ei kuulu asteikkoon " + Json.write(asteikko))
                case None =>
                  HttpStatus.internalError("Asteikkoa " + arviointi.asteikko + " ei löydy tutkintorakenteesta")
                case _ =>
                  HttpStatus.ok
              }
            }
          }
      }
    }

  def userView(oid: String)(implicit userContext: UserContext): Either[HttpStatus, TorOppija] = {
    oppijaRepository.findByOid(oid) match {
      case Some(oppija) =>
        opintoOikeudetForOppija(oppija) match {
          case Nil => notFound(oid)
          case opintoOikeudet => Right(TorOppija(oppija, opintoOikeudet))
        }
      case None => notFound(oid)
    }
  }

  def notFound(oid: String): Left[HttpStatus, Nothing] = {
    Left(HttpStatus.notFound(s"Oppija with oid: $oid not found"))
  }

  private def opintoOikeudetForOppija(oppija: Oppija)(implicit userContext: UserContext): Seq[OpintoOikeus] = {
    for {
      opintoOikeus   <- opintoOikeusRepository.findByOppijaOid(oppija.oid.get)
      tutkinto   <- tutkintoRepository.findByEPerusteDiaarinumero(opintoOikeus.tutkinto.ePerusteetDiaarinumero)
      oppilaitos <- oppilaitosRepository.findById(opintoOikeus.oppilaitosOrganisaatio.oid)
    } yield {
      opintoOikeus.copy(
        tutkinto = tutkinto.copy(rakenne = tutkintoRepository.findPerusteRakenne(tutkinto.ePerusteetDiaarinumero)(arviointiAsteikot)),
        oppilaitosOrganisaatio = oppilaitos
      )
    }
  }
}

