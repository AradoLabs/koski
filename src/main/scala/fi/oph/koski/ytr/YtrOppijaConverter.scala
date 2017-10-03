package fi.oph.koski.ytr

import fi.oph.koski.koodisto.KoodistoViitePalvelu
import fi.oph.koski.localization.Finnish
import fi.oph.koski.log.Logging
import fi.oph.koski.oppilaitos.OppilaitosRepository
import fi.oph.koski.organisaatio.OrganisaatioRepository
import fi.oph.koski.schema._

case class YtrOppijaConverter(oppilaitosRepository: OppilaitosRepository, koodistoViitePalvelu: KoodistoViitePalvelu, organisaatioRepository: OrganisaatioRepository) extends Logging {
  def convert(ytrOppija: YtrOppija): Option[YlioppilastutkinnonOpiskeluoikeus] = {
    ytrOppija.graduationSchoolOphOid match {
      case None =>
        logger.warn("YTR-tiedosta puuttuu oppilaitoksen tunniste")
        None
      case Some(oid) =>
        oppilaitosRepository.findByOid(oid) match  {
          case None =>
            logger.error("Oppilaitosta " + oid + " ei löydy")
            None
          case Some(oppilaitos) =>
            val vahvistus = ytrOppija.graduationDate match {
              case Some(graduationDate) =>
                val helsinki: Koodistokoodiviite = koodistoViitePalvelu.getKoodistoKoodiViite("kunta", "091").getOrElse(throw new IllegalStateException("Helsingin kaupunkia ei löytynyt koodistopalvelusta"))
                val ytl = organisaatioRepository.getOrganisaatio("1.2.246.562.10.43628088406").getOrElse(throw new IllegalStateException(("Ylioppilastutkintolautakuntaorganisaatiota ei löytynyt organisaatiopalvelusta")))
                Some(Organisaatiovahvistus(graduationDate, helsinki, oppilaitos))
              case None =>
                None
            }
            Some(YlioppilastutkinnonOpiskeluoikeus(
                lähdejärjestelmänId = Some(LähdejärjestelmäId(None, requiredKoodi("lahdejarjestelma", "ytr"))),
                oppilaitos = Some(oppilaitos),
                koulutustoimija = None,
                tila = YlioppilastutkinnonOpiskeluoikeudenTila(Nil),
                tyyppi = requiredKoodi("opiskeluoikeudentyyppi", "ylioppilastutkinto"),
                suoritukset = List(YlioppilastutkinnonSuoritus(
                  tyyppi = requiredKoodi("suorituksentyyppi", "ylioppilastutkinto"),
                  vahvistus = vahvistus,
                  toimipiste = oppilaitos,
                  koulutusmoduuli = Ylioppilastutkinto(requiredKoodi("koulutus", "301000"), None),
                  osasuoritukset = Some(ytrOppija.exams.map(convertExam)))
                )
            ))
        }
    }
  }
  private def convertExam(exam: YtrExam) = YlioppilastutkinnonKokeenSuoritus(
    tyyppi = requiredKoodi("suorituksentyyppi", "ylioppilastutkinnonkoe"),
    arviointi = Some(List(YlioppilaskokeenArviointi(requiredKoodi("koskiyoarvosanat", exam.grade)))),
    koulutusmoduuli = YlioppilasTutkinnonKoe(PaikallinenKoodi(exam.examId, Finnish(exam.examNameFi.getOrElse(exam.examId), exam.examNameSv, exam.examNameEn), Some("ytr/koetunnukset")))
  )

  private def requiredKoodi(uri: String, koodi: String) = {
    koodistoViitePalvelu.validateRequired(uri, koodi)
  }

  private def tilaValmis = requiredKoodi("suorituksentila", "VALMIS")
  private def tilaKesken = requiredKoodi("suorituksentila", "KESKEN")
}
