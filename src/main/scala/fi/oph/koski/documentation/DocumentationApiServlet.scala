package fi.oph.koski.documentation

import fi.oph.koski.http.KoskiErrorCategory
import fi.oph.koski.json.Json
import fi.oph.koski.koskiuser.Unauthenticated
import fi.oph.koski.schema.{JsonSerializer, KoskiSchema}
import fi.oph.koski.servlet.ApiServlet

import scala.reflect.runtime.{universe => ru}

class DocumentationApiServlet extends ApiServlet with Unauthenticated {
  get("/categoryNames.json") {
    KoskiTiedonSiirtoHtml.categoryNames
  }

  get("/categoryExampleMetadata.json") {
    KoskiTiedonSiirtoHtml.categoryExampleMetadata
  }

  get("/categoryExamples/:category/:name/table.html") {
    renderOption(KoskiErrorCategory.notFound)(KoskiTiedonSiirtoHtml.jsonTableHtmlContents(params("category"), params("name")))
  }

  get("/sections.html") {
    KoskiTiedonSiirtoHtml.htmlTextSections
  }

  get("/apiOperations.json") {
    KoskiTiedonSiirtoHtml.apiOperations
  }

  get("/examples/:name.json") {
    renderOption(KoskiErrorCategory.notFound)(Examples.allExamples.find(_.name == params("name")).map(_.data))
  }

  get("/koski-oppija-schema.json") {
    KoskiSchema.schemaJson
  }

  override def toJsonString[T: ru.TypeTag](x: T): String = Json.write(Json.toJValueDangerous(x.asInstanceOf[AnyRef]))
}
