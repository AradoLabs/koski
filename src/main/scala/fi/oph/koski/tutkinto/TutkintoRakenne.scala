package fi.oph.koski.tutkinto

import fi.oph.koski.eperusteet.ELaajuus
import fi.oph.koski.localization.LocalizedString
import fi.oph.koski.schema.Koodistokoodiviite
import fi.oph.koski.tutkinto.Koulutustyyppi.Koulutustyyppi

case class TutkintoRakenne(diaarinumero: String, koulutustyyppi: Koulutustyyppi, suoritustavat: List[SuoritustapaJaRakenne], osaamisalat: List[Koodistokoodiviite]) {
  def findSuoritustapaJaRakenne(suoritustapa: Koodistokoodiviite): Option[SuoritustapaJaRakenne] = {
    suoritustavat.find(_.suoritustapa == suoritustapa)
  }
}

case class SuoritustapaJaRakenne(suoritustapa: Koodistokoodiviite, rakenne: Option[RakenneOsa])
case class TutkinnonOsanLaajuus(min: Option[Long], max: Option[Long])

sealed trait RakenneOsa

case class RakenneModuuli(nimi: LocalizedString, osat: List[RakenneOsa], määrittelemätön: Boolean, laajuus: Option[TutkinnonOsanLaajuus]) extends RakenneOsa {
  def tutkinnonOsat: List[TutkinnonOsa] = osat flatMap {
    case m: RakenneModuuli => m.tutkinnonOsat
    case o: TutkinnonOsa => List(o)
  }
  def tutkinnonRakenneLaajuus: TutkinnonOsanLaajuus = {
    this.laajuus.getOrElse(TutkinnonOsanLaajuus(None, None))
  }
}
case class TutkinnonOsa(tunniste: Koodistokoodiviite, nimi: LocalizedString) extends RakenneOsa