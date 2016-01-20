package fi.oph.tor.koodisto

import fi.oph.tor.schema.KoodistoKoodiViite
import fi.vm.sade.utils.slf4j.Logging

case class KoodistoViitePalvelu(koodistoPalvelu: KoodistoPalvelu) extends Logging {
  def getKoodistoKoodiViitteet(koodisto: KoodistoViite): Option[List[KoodistoKoodiViite]] = {
    koodistoPalvelu.getKoodistoKoodit(koodisto).map { _.map { koodi => KoodistoKoodiViite(koodi.koodiArvo, koodi.nimi("fi"), koodisto.koodistoUri, Some(koodisto.versio))} }
  }
  def getLatestVersion(koodistoUri: String): Option[KoodistoViite] = koodistoPalvelu.getLatestVersion(koodistoUri)

  def getKoodistoKoodiViite(koodistoUri: String, koodiArvo: String): Option[KoodistoKoodiViite] = getLatestVersion(koodistoUri).flatMap(koodisto => getKoodistoKoodiViitteet(koodisto).toList.flatten.find(_.koodiarvo == koodiArvo))

  def validate(input: KoodistoKoodiViite):Option[KoodistoKoodiViite] = {
    val koodistoViite = input.koodistoViite.orElse(getLatestVersion(input.koodistoUri))

    val viite = koodistoViite.flatMap(getKoodistoKoodiViitteet).toList.flatten.find(_.koodiarvo == input.koodiarvo)

    if (!viite.isDefined) {
      logger.warn("Koodia " + input.koodiarvo + " ei löydy koodistosta " + input.koodistoUri)
    }
    viite
  }
}