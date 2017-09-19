package fi.oph.koski.api

import fi.oph.koski.json.{Json, JsonSerializer}
import fi.oph.koski.schema._
import fi.oph.koski.util.XML.texts

import scala.collection.immutable.Seq
import scala.xml.{Node, XML}

trait TodistusTestMethods extends SearchTestMethods with OpiskeluoikeusTestMethods {
  def todistus(oppijaOid: String, tyyppi: String, koulutusmoduuli: Option[String] = None): String = {
    var path: String = s"todistus/${oppijaOid}?suoritustyyppi=${tyyppi}"
    koulutusmoduuli foreach { koulutusmoduuli => path = path + s"&koulutusmoduuli=${koulutusmoduuli}" }
    authGet(path) {
      verifyResponseStatus(200)
      val lines: Seq[String] = XML.loadString(response.body)
        .flatMap(_.descendant_or_self).flatMap {
        case tr: Node if tr.label == "tr" && (tr \ "@class").text != "header" => Some(texts(tr \ "td"))
        case node: Node if List("h1", "h2", "h3", "h4", "p").contains(node.label) => Some(texts(node))
        case _ => None
      }
      lines.mkString("\n").trim
    }
  }

  private def opiskeluoikeus(searchTerm: String): Option[Opiskeluoikeus] = {
    searchForHenkilötiedot(searchTerm).flatMap { henkilötiedot =>
      val oppija: Oppija = authGet("api/oppija/" + henkilötiedot.oid) {
        verifyResponseStatus(200)
        JsonSerializer.parse[Oppija](body)
      }
      oppija.opiskeluoikeudet.headOption
    }.headOption
  }
}
