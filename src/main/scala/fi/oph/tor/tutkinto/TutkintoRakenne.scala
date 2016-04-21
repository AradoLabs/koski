package fi.oph.tor.tutkinto

import fi.oph.tor.arvosana.Arviointiasteikko
import fi.oph.tor.koodisto.KoodistoViite
import fi.oph.tor.schema.{Suoritustapa, Koodistokoodiviite}

case class TutkintoRakenne(diaarinumero: String, suoritustavat: List[SuoritustapaJaRakenne], osaamisalat: List[Koodistokoodiviite], arviointiAsteikot: List[Arviointiasteikko]) {
  def findSuoritustapaJaRakenne(suoritustapa: Suoritustapa): Option[SuoritustapaJaRakenne] = {
    suoritustavat.find(_.suoritustapa == suoritustapa.tunniste)
  }
}

case class SuoritustapaJaRakenne(suoritustapa: Koodistokoodiviite, rakenne: RakenneOsa, laajuusYksikkö: Option[Koodistokoodiviite])

sealed trait RakenneOsa

case class RakenneModuuli(nimi: String, osat: List[RakenneOsa], osaamisalaKoodi: Option[String]) extends RakenneOsa {
  def tutkinnonOsat: List[TutkinnonOsa] = osat flatMap {
    case m: RakenneModuuli => m.tutkinnonOsat
    case o: TutkinnonOsa => List(o)
  }
}
case class TutkinnonOsa(tunniste: Koodistokoodiviite, nimi: String, arviointiAsteikko: Option[KoodistoViite], laajuus: Option[Float], pakollinen: Boolean) extends RakenneOsa