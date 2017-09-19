package fi.oph.koski.koodisto
import fi.oph.koski.cache.{Cached, GlobalCacheManager}
import fi.oph.koski.json.{Json, JsonSerializer}
import fi.oph.koski.koodisto.MockKoodistoPalvelu._

private class MockKoodistoPalvelu extends KoodistoPalvelu {
  def getKoodistoKoodit(koodisto: KoodistoViite): Option[List[KoodistoKoodi]] = {
    koodistoKooditResourceName(koodisto.koodistoUri).flatMap(Json.readResourceIfExists(_)).map(JsonSerializer.extract[List[KoodistoKoodi]](_, ignoreExtras = true, validating = false))
  }

  def getKoodisto(koodisto: KoodistoViite): Option[Koodisto] = {
    getKoodisto(koodisto.koodistoUri)
  }

  def getKoodisto(koodistoUri: String): Option[Koodisto] = {
    koodistoResourceName(koodistoUri).flatMap(Json.readResourceIfExists(_)).map(JsonSerializer.extract[Koodisto](_, ignoreExtras = true, validating = false))
  }

  def getLatestVersion(koodistoUri: String): Option[KoodistoViite] = getKoodisto(koodistoUri).map { _.koodistoViite }
}



object MockKoodistoPalvelu {
  // this is done to ensure that the cached instance is used everywhere (performance penalties are huge)
  private lazy val palvelu = KoodistoPalvelu.cached(new MockKoodistoPalvelu)(GlobalCacheManager)
  def apply(): KoodistoPalvelu with Cached = palvelu
  protected[koodisto] def koodistoKooditResourceName(koodistoUri: String): Option[String] = Koodistot.koodistot.find(_ == koodistoUri).map(uri => "/mockdata/koodisto/koodit/" + uri + ".json")
  protected[koodisto] def koodistoResourceName(koodistoUri: String): Option[String] = {
    val found: Option[String] = Koodistot.koodistot.find(_ == koodistoUri)
    found.map(uri => "/mockdata/koodisto/koodistot/" + uri + ".json")
  }

  def koodistoKooditFileName(koodistoUri: String): String = "src/main/resources" + koodistoKooditResourceName(koodistoUri).get
  def koodistoFileName(koodistoUri: String): String = "src/main/resources" + koodistoResourceName(koodistoUri).get
}