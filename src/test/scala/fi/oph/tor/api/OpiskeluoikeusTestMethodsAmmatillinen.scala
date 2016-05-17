package fi.oph.tor.api

import fi.oph.tor.documentation.AmmatillinenExampleData
import fi.oph.tor.organisaatio.MockOrganisaatiot
import fi.oph.tor.schema._

trait OpiskeluoikeusTestMethodsAmmatillinen extends OpiskeluOikeusTestMethods[AmmatillinenOpiskeluoikeus] {
  override def defaultOpiskeluoikeus = opiskeluoikeus()

  val autoalanPerustutkinto: AmmatillinenTutkintoKoulutus = AmmatillinenTutkintoKoulutus(Koodistokoodiviite("351301", "koulutus"), Some("39/011/2014"))

  lazy val tutkintoSuoritus: AmmatillisenTutkinnonSuoritus = AmmatillisenTutkinnonSuoritus(
    koulutusmoduuli = autoalanPerustutkinto,
    tutkintonimike = None,
    osaamisala = None,
    suoritustapa = None,
    järjestämismuoto = None,
    paikallinenId = None,
    suorituskieli = None,
    tila = tilaKesken,
    alkamispäivä = None,
    toimipiste = OidOrganisaatio(MockOrganisaatiot.lehtikuusentienToimipiste),
    arviointi = None,
    vahvistus = None,
    osasuoritukset = None
  )

  def opiskeluoikeus(suoritus: AmmatillisenTutkinnonSuoritus = tutkintoSuoritus) = AmmatillinenOpiskeluoikeus(None, None, None, None, None, None,
    oppilaitos = Oppilaitos(MockOrganisaatiot.stadinAmmattiopisto), None,
    suoritukset = List(suoritus),
    None, AmmatillinenExampleData.tavoiteTutkinto, None, None
  )
}
