package fi.oph.koski.jettylauncher

import java.nio.file.{Paths, Files}

import fi.oph.koski.util.{Pools, PortChecker}
import org.eclipse.jetty.server.handler.{ContextHandler, ResourceHandler}
import org.eclipse.jetty.server.{Server, ServerConnector, Slf4jRequestLog}
import org.eclipse.jetty.util.thread.QueuedThreadPool
import org.eclipse.jetty.webapp.WebAppContext

object JettyLauncher extends App {
  lazy val globalPort = System.getProperty("tor.port","7021").toInt
  new JettyLauncher(globalPort).start.join
}

class JettyLauncher(val port: Int, overrides: Map[String, String] = Map.empty) {
  lazy val threadPool = new QueuedThreadPool(Pools.jettyThreads, 10);
  lazy val server = new Server(threadPool)
  lazy val requestLog = new Slf4jRequestLog()

  requestLog.setLogLatency(true)
  server.setRequestLog(requestLog);

  lazy val connector: ServerConnector = new ServerConnector(server)
  connector.setPort(port)
  server.addConnector(connector)

  def resourceBase = if (Files.exists(Paths.get("./target/webapp"))) "./target/webapp" else "./web"

  val context = new WebAppContext()
  context.setContextPath("/koski")
  context.setResourceBase(resourceBase)
  context.setAttribute("tor.overrides", overrides)

  server.setHandler(context)

  def start = {
    server.start
    server
  }

  def baseUrl = "http://localhost:" + port + "/koski"
}

object SharedJetty extends JettyLauncher(PortChecker.findFreeLocalPort, Map("db.name" -> "tortest", "fixtures.use" -> "true"))