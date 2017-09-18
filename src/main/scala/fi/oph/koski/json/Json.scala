package fi.oph.koski.json

import java.io.InputStream
import java.time.{Instant, LocalDate, LocalDateTime, ZoneId}

import com.github.fge.jsonpatch.diff.JsonDiff
import fi.oph.koski.editor.EditorModelSerializer
import fi.oph.koski.http.{HttpStatus, KoskiErrorCategory}
import fi.oph.koski.log.Logging
import fi.oph.koski.schema._
import fi.oph.koski.servlet.InvalidRequestException
import fi.oph.koski.util.Files
import org.json4s
import org.json4s.JsonAST.{JInt, JNull, JString}
import org.json4s._
import org.json4s.ext.JodaTimeSerializers
import org.json4s.jackson.{JsonMethods, Serialization}

import scala.reflect.runtime.universe.TypeTag
import scala.util.Try

object GenericJsonFormats {
  val genericFormats: Formats = new DefaultFormats {
    override def dateFormatter = {
      val format = super.dateFormatter
      format.setTimeZone(DefaultFormats.UTC)
      format
    }

    override val strictOptionParsing: Boolean = true
  } ++ JodaTimeSerializers.all
}

object Json extends Logging {
  implicit val jsonFormats = GenericJsonFormats.genericFormats + LocalDateSerializer + LocalDateTimeSerializer + EditorModelSerializer + BlockOpiskeluoikeusSerializer

  def write(x: AnyRef, pretty: Boolean = false): String = {
    if (pretty) {
      writePretty(x)
    } else {
      Serialization.write(x);
    }
  }

  def writePretty(x: AnyRef): String = {
    Serialization.writePretty(x);
  }

  def read[A](json: String)(implicit mf : scala.reflect.Manifest[A]) : A = {
    Serialization.read(json)
  }

  def read[A](json: InputStream)(implicit mf : scala.reflect.Manifest[A]) : A = {
    Serialization.read(json)
  }

  def parse(json: String) = JsonMethods.parse(json)

  def tryParse(json: String): Try[JValue] = Try(parse(json))

  def toJValue(x: AnyRef): JValue = {
    Extraction.decompose(x)
  }

  def fromJValue[A](x: JValue)(implicit mf : scala.reflect.Manifest[A]): A = {
    x.extract[A]
  }

  def readFile(filename: String): json4s.JValue = {
    readFileIfExists(filename).get
  }

  def readFileIfExists(filename: String): Option[json4s.JValue] = Files.asString(filename).map(parse(_))

  def readResourceIfExists(resourcename: String): Option[json4s.JValue] = {
    try {
      Option(getClass().getResource(resourcename)).map { r =>
        val resource = r.openConnection()
        resource.setUseCaches(false) // To avoid a random "stream closed exception" caused by JRE bug (probably this: https://bugs.openjdk.java.net/browse/JDK-8155607)
        val is = resource.getInputStream()
        try {
          JsonMethods.parse(StreamInput(is))
        } finally {
          is.close()
        }
      }
    } catch {
      case e: Exception =>
        logger.error("Load resource " + resourcename + " failed")
        throw e
    }
  }

  def readResource(resourcename: String): json4s.JValue = readResourceIfExists(resourcename).getOrElse(throw new RuntimeException(s"Resource $resourcename not found"))

  def writeFile[T : TypeTag](filename: String, json: T) = {
    Files.writeFile(filename, JsonMethods.pretty(JsonSerializer.serializeWithRoot(json)))
  }

  def maskSensitiveInformation(parsedJson: JValue): JValue = {
    val maskedJson = parsedJson.mapField {
      case ("hetu", JString(_)) => ("hetu", JString("******-****"))
      case field: (String, JsonAST.JValue) => field
    }
    maskedJson
  }

  def jsonDiff(oldValue: JValue, newValue: JValue): JArray = {
    JsonMethods.fromJsonNode(JsonDiff.asJson(JsonMethods.asJsonNode(oldValue), JsonMethods.asJsonNode(newValue))).asInstanceOf[JArray]
  }
}

object LocalDateSerializer extends CustomSerializer[LocalDate](format => (
  {
    case JString(s) => ExtractionHelper.tryExtract(LocalDate.parse(s))(KoskiErrorCategory.badRequest.format.pvm("Virheellinen päivämäärä: " + s))
    case JInt(i) => ExtractionHelper.tryExtract(LocalDateTime.ofInstant(Instant.ofEpochMilli(i.longValue()), ZoneId.of("UTC")).toLocalDate())(KoskiErrorCategory.badRequest.format.pvm("Virheellinen päivämäärä: " + i))
    case JNull => throw new InvalidRequestException(KoskiErrorCategory.badRequest.format.pvm("Virheellinen päivämäärä: null"))
  },
  {
    case d: LocalDate => JString(d.toString)
  }
  )
)

object LocalDateTimeSerializer extends CustomSerializer[LocalDateTime](format => (
  {
    case JString(s) => ExtractionHelper.tryExtract(LocalDateTime.parse(s))(KoskiErrorCategory.badRequest.format.pvm("Virheellinen päivämäärä: " + s))
    case JDouble(i) => ExtractionHelper.tryExtract(LocalDateTime.ofInstant(Instant.ofEpochMilli(i.longValue()), ZoneId.of("UTC")))(KoskiErrorCategory.badRequest.format.pvm("Virheellinen päivämäärä: " + i))
    case JInt(i) => ExtractionHelper.tryExtract(LocalDateTime.ofInstant(Instant.ofEpochMilli(i.longValue()), ZoneId.of("UTC")))(KoskiErrorCategory.badRequest.format.pvm("Virheellinen päivämäärä: " + i))
    case JNull => throw new InvalidRequestException(KoskiErrorCategory.badRequest.format.pvm("Virheellinen päivämäärä: null"))
  },
  {
    case d: LocalDateTime => JString(d.toString)
  }
  )
)

// Estää opiskeluoikeuksien serialisoimisen vahingosssa ilman arkaluontoisten kenttien filtteröintiä
private object BlockOpiskeluoikeusSerializer extends Serializer[Opiskeluoikeus] {
  override def deserialize(implicit format: Formats) = PartialFunction.empty
  override def serialize(implicit format: Formats) = {
    case x: Opiskeluoikeus => fail(x.getClass.getName)
    case x: Suoritus => fail(x.getClass.getName)
  }
  def fail(name: String) = throw new RuntimeException(s"$name-luokan serialisointi estetty, käytä fi.oph.scalaschema.Serializer-luokkaa")
}

private object ExtractionHelper {
  def tryExtract[T](block: => T)(status: => HttpStatus) = {
    try {
      block
    } catch {
      case e: Exception => throw new InvalidRequestException(status)
    }
  }
}