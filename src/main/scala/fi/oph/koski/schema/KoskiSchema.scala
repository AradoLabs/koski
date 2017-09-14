package fi.oph.koski.schema

import fi.oph.koski.json.Json
import fi.oph.scalaschema._
import org.json4s.JValue

object KoskiSchema {
  private val metadataTypes = SchemaFactory.defaultAnnotations ++ List(classOf[KoodistoUri], classOf[KoodistoKoodiarvo], classOf[ReadOnly], classOf[OksaUri], classOf[Hidden], classOf[Representative], classOf[ComplexObject], classOf[Flatten], classOf[Tabular], classOf[ClassName], classOf[MultiLineString], classOf[UnitOfMeasure], classOf[Example], classOf[RequiresRole])
  lazy val schemaFactory: SchemaFactory = SchemaFactory(metadataTypes)
  lazy val schema = createSchema(classOf[Oppija]).asInstanceOf[ClassSchema]
  lazy val schemaJson: JValue = SchemaToJson.toJsonSchema(schema)
  lazy val schemaJsonString = Json.write(schemaJson)
  lazy implicit val deserializationContext = ExtractionContext(schemaFactory)

  def createSchema(clazz: Class[_]) = schemaFactory.createSchema(clazz) match {
    case s: AnyOfSchema => s
    case s: ClassSchema => s.moveDefinitionsToTopLevel
  }
}