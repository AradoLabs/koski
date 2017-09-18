package fi.oph.koski.koskiuser

import fi.oph.koski.servlet.{ApiServletWithSchemaBasedSerialization, NoCache}

class UserServlet(implicit val application: UserAuthenticationContext) extends ApiServletWithSchemaBasedSerialization with AuthenticationSupport with NoCache {
  get("/") {
    renderEither(getUser.right.map(user => UserWithAccessRights(user.name, koskiSessionOption.map(_.hasAnyWriteAccess).getOrElse(false), koskiSessionOption.map(_.hasLocalizationWriteAccess).getOrElse(false))))
  }
}

case class UserWithAccessRights(name: String, hasWriteAccess: Boolean, hasLocalizationWriteAccess: Boolean)