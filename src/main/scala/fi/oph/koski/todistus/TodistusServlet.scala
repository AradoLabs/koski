package fi.oph.koski.todistus

import fi.oph.koski.config.KoskiApplication
import fi.oph.koski.http.KoskiErrorCategory
import fi.oph.koski.koskiuser.RequiresAuthentication
import fi.oph.koski.schema._
import fi.oph.koski.servlet.HtmlServlet
import fi.oph.koski.suoritusote.OpiskeluoikeusFinder

class TodistusServlet(implicit val application: KoskiApplication) extends HtmlServlet with RequiresAuthentication {
  get("/:oppijaOid") {
    val oppijaOid = params("oppijaOid")
    implicit val localizations = application.localizationRepository

    val filters: List[(Suoritus => Boolean)] = params.toList.flatMap {
      case ("koulutusmoduuli", koulutusmoduuli: String) => Some({ s: Suoritus => s.koulutusmoduuli.tunniste.toString == koulutusmoduuli })
      case ("suoritustyyppi", suoritustyyppi: String) => Some({ s: Suoritus => s.tyyppi.koodiarvo == suoritustyyppi })
      case (_, _) => None
    }

    renderEither(OpiskeluoikeusFinder(application.oppijaFacade).opiskeluoikeudet(oppijaOid, params).right.flatMap {
      case Oppija(henkilötiedot: TäydellisetHenkilötiedot, opiskeluoikeudet) =>
        val suoritukset: Seq[(Opiskeluoikeus, Suoritus)] = opiskeluoikeudet.flatMap {
          opiskeluoikeus => opiskeluoikeus.suoritukset.filter(suoritus => suoritus.valmis && filters.forall(f => f(suoritus)))
            .map (suoritus => (opiskeluoikeus, suoritus))
        }

        suoritukset match {
          case ((opiskeluoikeus, suoritus) :: Nil) =>
            suoritus match {
              case t: PerusopetukseenValmistavanOpetuksenSuoritus =>
                Right((new PerusopetukseenValmistavanOpetuksenTodistusHtml(opiskeluoikeus.koulutustoimija, opiskeluoikeus.getOppilaitos, henkilötiedot, t)).todistusHtml)
              case t: PerusopetuksenOppimääränSuoritus =>
                Right((new PerusopetuksenPaattotodistusHtml(opiskeluoikeus.koulutustoimija, opiskeluoikeus.getOppilaitos, henkilötiedot, t)).todistusHtml)
              case t: PerusopetuksenOppiaineenOppimääränSuoritus =>
                Right((new PerusopetuksenOppiaineenOppimaaranTodistusHtml(opiskeluoikeus.koulutustoimija, opiskeluoikeus.getOppilaitos, henkilötiedot, t)).todistusHtml)
              case t: PerusopetuksenVuosiluokanSuoritus =>
                Right((new PerusopetuksenLukuvuositodistusHtml(opiskeluoikeus.koulutustoimija, opiskeluoikeus.getOppilaitos, henkilötiedot, t)).todistusHtml)
              case t: PerusopetuksenLisäopetuksenSuoritus =>
                Right((new PerusopetuksenLisaopetuksenTodistusHtml(opiskeluoikeus.koulutustoimija, opiskeluoikeus.getOppilaitos, henkilötiedot, t)).todistusHtml)
              case t: AmmatillisenTutkinnonSuoritus =>
                Right((new AmmatillisenPerustutkinnonPaattotodistusHtml).render(opiskeluoikeus.koulutustoimija, opiskeluoikeus.getOppilaitos, henkilötiedot, t))
              case t: NäyttötutkintoonValmistavanKoulutuksenSuoritus =>
                Right(new NäyttötutkintoonValmentavanKoulutuksenTodistusHtml(opiskeluoikeus.koulutustoimija, opiskeluoikeus.getOppilaitos, henkilötiedot, t).todistusHtml)
              case t: ValmaKoulutuksenSuoritus =>
                Right(new ValmaTodistusHtml(opiskeluoikeus.koulutustoimija, opiskeluoikeus.getOppilaitos, henkilötiedot, t).todistusHtml)
              case t: TelmaKoulutuksenSuoritus =>
                Right(new TelmaTodistusHtml(opiskeluoikeus.koulutustoimija, opiskeluoikeus.getOppilaitos, henkilötiedot, t).todistusHtml)
              case t: LukionOppimääränSuoritus =>
                Right((new LukionPaattoTodistusHtml).render(opiskeluoikeus.koulutustoimija, opiskeluoikeus.getOppilaitos, henkilötiedot, t))
              case t: IBTutkinnonSuoritus =>
                Right((new IBPaattoTodistusHtml).render(opiskeluoikeus.koulutustoimija, opiskeluoikeus.getOppilaitos, henkilötiedot, t))
              case t: YlioppilastutkinnonSuoritus =>
                Right((new YlioppilastutkintotodistusHtml).render(opiskeluoikeus.koulutustoimija, opiskeluoikeus.getOppilaitos, henkilötiedot, t))
              case t: LukioonValmistavanKoulutuksenSuoritus =>
                Right((new LuvaTodistusHtml).render(opiskeluoikeus.koulutustoimija, opiskeluoikeus.getOppilaitos, henkilötiedot, t))
              case _ =>
                Left(KoskiErrorCategory.notFound.todistustaEiLöydy())
          }

          case _ =>
            Left(KoskiErrorCategory.notFound.todistustaEiLöydy())
        }
      case _ => throw new RuntimeException("Unreachable match arm: OpiskeluoikeusFinder must return exisiting Oppija on success")
    })
  }
}