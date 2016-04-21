package fi.oph.tor.documentation

import java.time.LocalDate.{of => date}

import fi.oph.tor.documentation.ExampleData._
import fi.oph.tor.documentation.YleissivistavakoulutusExampleData._
import fi.oph.tor.documentation.PerusopetusExampleData._
import fi.oph.tor.oppija.MockOppijat
import fi.oph.tor.schema._
import fi.oph.tor.localization.LocalizedStringImplicits._

object PerusopetusExampleData {
  def suoritus(aine: PerusopetuksenOppiaine) = PerusopetuksenOppiaineenSuoritus(
    koulutusmoduuli = aine,
    paikallinenId = None,
    suorituskieli = None,
    tila = tilaValmis,
    arviointi = None
  )

  def vuosiviikkotuntia(määrä: Double): Some[Laajuus] = Some(Laajuus(määrä.toFloat, Koodistokoodiviite("3", Some("Vuosiviikkotuntia"), "opintojenlaajuusyksikko")))

  val exampleHenkilö = MockOppijat.koululainen.vainHenkilötiedot

  val perusopetus = Perusopetus(Some("104/011/2014"))
}

object ExamplesPerusopetus {
  val uusi = Oppija(
    exampleHenkilö,
    List(PerusopetuksenOpiskeluoikeus(
      id = None,
      versionumero = None,
      lähdejärjestelmänId = None,
      alkamispäivä = Some(date(2016, 9, 1)),
      arvioituPäättymispäivä = Some(date(2020, 5, 1)),
      päättymispäivä = None,
      oppilaitos = jyväskylänNormaalikoulu, None,
      suoritukset = Nil,
      tila = Some(YleissivistäväOpiskeluoikeudenTila(
        List(
          YleissivistäväOpiskeluoikeusjakso(date(2012, 9, 1), Some(date(2016, 1, 9)), opiskeluoikeusAktiivinen),
          YleissivistäväOpiskeluoikeusjakso(date(2016, 1, 10), None, opiskeluoikeusPäättynyt)
        )
      )),
      läsnäolotiedot = None
    ))
  )
  private val vahvistus: Some[Vahvistus] = Some(Vahvistus(päivä = date(2016, 6, 4), jyväskylä, myöntäjäOrganisaatio = jyväskylänNormaalikoulu, myöntäjäHenkilöt = List(OrganisaatioHenkilö("Reijo Reksi", "rehtori", jyväskylänNormaalikoulu))))

  val päättötodistus = Oppija(
    exampleHenkilö,
    List(PerusopetuksenOpiskeluoikeus(
      id = None,
      versionumero = None,
      lähdejärjestelmänId = None,
      alkamispäivä = Some(date(2007, 8, 15)),
      arvioituPäättymispäivä = Some(date(2016, 6, 4)),
      päättymispäivä = Some(date(2016, 6, 4)),
      oppilaitos = jyväskylänNormaalikoulu, None,
      suoritukset = List(
        PerusopetuksenOppimääränSuoritus(
          koulutusmoduuli = perusopetus,
          paikallinenId = None,
          suorituskieli = None,
          tila = tilaValmis,
          toimipiste = jyväskylänNormaalikoulu,
          vahvistus = vahvistus,
          osasuoritukset = Some(
            List(
              suoritus(äidinkieli("AI1")).copy(arviointi = arviointi(9)),
              suoritus(kieli("B1", "SV")).copy(arviointi = arviointi(8)),
              suoritus(kieli("B1", "SV").copy(pakollinen = false, laajuus = vuosiviikkotuntia(1))).copy(arviointi = hyväksytty),
              suoritus(kieli("A1", "EN")).copy(arviointi = arviointi(8)),
              suoritus(uskonto("KT1")).copy(arviointi = arviointi(10)),
              suoritus(oppiaine("HI")).copy(arviointi = arviointi(8)),
              suoritus(oppiaine("YH")).copy(arviointi = arviointi(10)),
              suoritus(oppiaine("MA")).copy(arviointi = arviointi(9)),
              suoritus(oppiaine("KE")).copy(arviointi = arviointi(7)),
              suoritus(oppiaine("FY")).copy(arviointi = arviointi(9)),
              suoritus(oppiaine("BI")).copy(arviointi = arviointi(9)),
              suoritus(oppiaine("GE")).copy(arviointi = arviointi(9)),
              suoritus(oppiaine("MU")).copy(arviointi = arviointi(7)),
              suoritus(oppiaine("KU")).copy(arviointi = arviointi(8)),
              suoritus(oppiaine("KO")).copy(arviointi = arviointi(8)),
              suoritus(oppiaine("KO").copy(pakollinen = false, laajuus = vuosiviikkotuntia(1))).copy(arviointi = hyväksytty),
              suoritus(oppiaine("TE")).copy(arviointi = arviointi(8)),
              suoritus(oppiaine("KS")).copy(arviointi = arviointi(9)),
              suoritus(oppiaine("LI")).copy(arviointi = arviointi(9)),
              suoritus(oppiaine("LI").copy(pakollinen = false, laajuus = vuosiviikkotuntia(0.5))).copy(arviointi = hyväksytty),
              suoritus(kieli("B2", "DE").copy(pakollinen = false, laajuus = vuosiviikkotuntia(4))).copy(arviointi = arviointi(9))
            ))
        )),
      tila = Some(YleissivistäväOpiskeluoikeudenTila(
        List(
          YleissivistäväOpiskeluoikeusjakso(date(2007, 8, 15), Some(date(2016, 6, 3)), opiskeluoikeusAktiivinen),
          YleissivistäväOpiskeluoikeusjakso(date(2016, 6, 4), None, opiskeluoikeusPäättynyt)
        )
      )),
      läsnäolotiedot = None
    ))
  )

  val examples = List(
    Example("perusopetuksen oppimäärä - uusi", "Uusi oppija lisätään suorittamaan perusopetuksen oppimäärää", uusi),
    Example("perusopetuksen oppimäärä - päättötodistus", "Oppija on saanut perusopetuksen päättötodistuksen", päättötodistus)
  )
}
