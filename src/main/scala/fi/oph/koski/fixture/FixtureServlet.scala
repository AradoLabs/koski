package fi.oph.koski.fixture

import fi.oph.koski.config.KoskiApplication
import fi.oph.koski.koskiuser.RequiresAuthentication
import fi.oph.koski.servlet.ApiServlet

class FixtureServlet(implicit val application: KoskiApplication) extends ApiServlet with RequiresAuthentication {
  post("/reset") {
    application.fixtureCreator.resetFixtures
    application.elasticSearch.refreshIndex
  }

  post("/refresh") {
    application.elasticSearch.refreshIndex
  }
}
