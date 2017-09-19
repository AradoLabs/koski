package fi.oph.koski.koodisto

import java.time.LocalDate

import com.typesafe.config.Config
import fi.oph.koski.json.Json
import fi.oph.koski.log.Logging
import fi.oph.koski.schema.JsonSerializer
import org.json4s.jackson.JsonMethods

object KoodistoCreator extends Logging {
  def createKoodistotFromMockData(koodistot: List[String], config: Config, updateExisting: Boolean = false): Unit = {
    val kp = KoodistoPalvelu.withoutCache(config)
    val kmp = KoodistoMuokkausPalvelu(config)

    val luotavatKoodistot = koodistot.par.filter(kp.getLatestVersion(_).isEmpty).toList

    luotavatKoodistot.foreach { koodistoUri =>
      MockKoodistoPalvelu().getKoodisto(KoodistoViite(koodistoUri, 1)) match {
        case None =>
          throw new IllegalStateException("Mock not found: " + koodistoUri)
        case Some(koodisto) =>
          kmp.createKoodisto(koodisto)
      }
    }

    if (updateExisting) {
      val olemassaOlevatKoodistot = koodistot.par.filter(!kp.getLatestVersion(_).isEmpty).toList
      val päivitettävätKoodistot = olemassaOlevatKoodistot.flatMap { koodistoUri =>
        val existing: Koodisto = kp.getLatestVersion(koodistoUri).flatMap(kp.getKoodisto).get
        val mock: Koodisto = MockKoodistoPalvelu().getKoodisto(KoodistoViite(koodistoUri, 1)).get.copy(version = existing.version)

        if (existing.withinCodes != mock.withinCodes) {
          logger.info("Päivitetään koodisto " + existing.koodistoUri + " diff " + objectDiff(existing, mock) + " original " + JsonSerializer.writeWithRoot(existing))
          Some(mock)
        } else {
          None
        }
      }
      päivitettävätKoodistot.foreach { koodisto =>
        kmp.updateKoodisto(koodisto)
      }
    }

    koodistot.par.foreach { koodistoUri =>
      def sortMetadata(k: KoodistoKoodi) = k.copy(metadata = k.metadata.sortBy(_.kieli))
      val koodistoViite: KoodistoViite = kp.getLatestVersion(koodistoUri).getOrElse(throw new Exception("Koodistoa ei löydy: " + koodistoUri))
      val olemassaOlevatKoodit: List[KoodistoKoodi] = kp.getKoodistoKoodit(koodistoViite).toList.flatten.map(sortMetadata)
      val mockKoodit: List[KoodistoKoodi] = MockKoodistoPalvelu().getKoodistoKoodit(koodistoViite).toList.flatten.map(sortMetadata)
      val luotavatKoodit: List[KoodistoKoodi] = mockKoodit.filter { koodi: KoodistoKoodi => !olemassaOlevatKoodit.find(_.koodiArvo == koodi.koodiArvo).isDefined }
      luotavatKoodit.zipWithIndex.foreach { case (koodi, index) =>
        logger.info("Luodaan koodi (" + (index + 1) + "/" + (luotavatKoodit.length) + ") " + koodi.koodiUri)
        kmp.createKoodi(koodistoUri, koodi.copy(voimassaAlkuPvm = Some(LocalDate.now)))
      }
      if (updateExisting) {
        val päivitettävätKoodit = olemassaOlevatKoodit.flatMap { vanhaKoodi =>
          mockKoodit.find(_.koodiArvo == vanhaKoodi.koodiArvo).flatMap { uusiKoodi =>
            val uusiKoodiSamallaKoodiUrilla = uusiKoodi.copy(
              koodiUri = vanhaKoodi.koodiUri
            )

            if (uusiKoodiSamallaKoodiUrilla != vanhaKoodi) {
              Some(vanhaKoodi, uusiKoodi)
            } else {
              None
            }
          }
        }

        päivitettävätKoodit.zipWithIndex.foreach { case ((vanhaKoodi, uusiKoodi), index) =>
          logger.info("Päivitetään koodi (" + (index + 1) + "/" + (päivitettävätKoodit.length) + ") " + uusiKoodi.koodiUri + " diff " + objectDiff(vanhaKoodi, uusiKoodi) + " original " + JsonSerializer.writeWithRoot(vanhaKoodi))
          kmp.updateKoodi(koodistoUri, uusiKoodi.copy(
            voimassaAlkuPvm = Some(LocalDate.now),
            tila = uusiKoodi.tila.orElse(vanhaKoodi.tila),
            version = uusiKoodi.version.orElse(vanhaKoodi.version)
          ))
        }
      }
    }
  }

  def objectDiff(a: AnyRef, b: AnyRef) = {
    JsonMethods.compact(Json.jsonDiff(JsonSerializer.serializeWithRoot(a), JsonSerializer.serializeWithRoot(b)))
  }
}