package fi.oph.tor.koodisto

import fi.oph.tor.http.{Http, VirkailijaHttpClient}
import fi.oph.tor.json.Json
import fi.vm.sade.utils.slf4j.Logging

class RemoteKoodistoPalvelu(username: String, password: String, virkailijaUrl: String) extends LowLevelKoodistoPalvelu with Logging {
  val virkalijaClient = new VirkailijaHttpClient(username, password, virkailijaUrl, "/koodisto-service")
  val http = virkalijaClient.httpClient

  def getKoodistoKoodit(koodisto: KoodistoViite): Option[List[KoodistoKoodi]] = {
    http(virkalijaClient.virkailijaUriFromString("/koodisto-service/rest/codeelement/codes/" + koodisto + noCache)) {
      case (404, _) => None
      case (500, "error.codes.not.found") => None // If codes are not found, the service actually returns 500 with this error text.
      case (200, text) => Some(Json.read[List[KoodistoKoodi]](text))
      case (status, text) => throw new RuntimeException(status + ": " + text)
    }
  }

  def getKoodisto(koodisto: KoodistoViite): Option[Koodisto] = {
    http(virkalijaClient.virkailijaUriFromString("/koodisto-service/rest/codes/" + koodisto + noCache))(Http.parseJsonOptional[Koodisto])
  }

  def getLatestVersion(koodisto: String): Option[KoodistoViite] = {
    val latestKoodisto: Option[KoodistoWithLatestVersion] = http(virkalijaClient.virkailijaUriFromString("/koodisto-service/rest/codes/" + koodisto + noCache))(Http.parseJsonIgnoreError[KoodistoWithLatestVersion])
    latestKoodisto.flatMap { latest => Option(latest.latestKoodistoVersio).map(v => KoodistoViite(koodisto, v.versio)) }
  }

  private def noCache = "?noCache=" + System.currentTimeMillis()

  def createKoodisto(koodisto: Koodisto): Unit = {
    http.post(virkalijaClient.virkailijaUriFromString("/koodisto-service/rest/codes"), koodisto)
  }


  def createKoodi(koodistoUri: String, koodi: KoodistoKoodi) = {
    http.post(virkalijaClient.virkailijaUriFromString("/koodisto-service/rest/codeelement/" + koodistoUri), koodi)
  }

  def createKoodistoRyhmä(ryhmä: KoodistoRyhmä) = {
    http.post(virkalijaClient.virkailijaUriFromString("/koodisto-service/rest/codesgroup"), ryhmä)
  }
}

case class KoodistoWithLatestVersion(latestKoodistoVersio: LatestVersion)
case class LatestVersion(versio: Int)

