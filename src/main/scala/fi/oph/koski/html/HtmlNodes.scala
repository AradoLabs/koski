package fi.oph.koski.html

import java.io.File

import fi.oph.koski.config.KoskiApplication
import fi.oph.koski.http.HttpStatus
import fi.oph.koski.json.{Json, JsonSerializer}
import fi.oph.koski.localization.LocalizationRepository
import fi.oph.koski.servlet.KoskiBaseServlet
import fi.oph.koski.util.XML.CommentedPCData

import scala.xml.NodeSeq.Empty
import scala.xml.{Elem, NodeSeq, Unparsed}

trait HtmlNodes extends KoskiBaseServlet with PiwikNodes {
  def application: KoskiApplication
  def buildVersion: Option[String]
  def localizations: LocalizationRepository = application.localizationRepository

  def htmlIndex(scriptBundleName: String, piwikHttpStatusCode: Option[Int] = None, raamitEnabled: Boolean = false): Elem = {
    <html>
      <head>
        {commonHead ++ raamit(raamitEnabled) ++ piwikTrackingScriptLoader(piwikHttpStatusCode)}
      </head>
      <body>
        <div data-inraamit={if (raamitEnabled) "true" else ""} id="content"></div>
      </body>
      <script id="localization">
        {Unparsed("window.koskiLocalizationMap="+JsonSerializer.writeWithRoot(localizations.localizations))}
      </script>
      <script id="bundle" src={"/koski/js/" + scriptBundleName + "?" + buildVersion.getOrElse(scriptTimestamp(scriptBundleName))}></script>
    </html>
  }

  def commonHead: NodeSeq =
    <title>Koski - Opintopolku.fi</title> ++
    <meta http-equiv="X-UA-Compatible" content="IE=edge" /> ++
    <meta charset="UTF-8" /> ++
    <link rel="shortcut icon" href="/koski/favicon.ico" /> ++
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/normalize/3.0.3/normalize.min.css" /> ++
    <link href="https://fonts.googleapis.com/css?family=Open+Sans:400,600,700,800" rel="stylesheet"/> ++
    <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/4.7.0/css/font-awesome.min.css" rel="stylesheet" type="text/css" /> ++
    <link rel="stylesheet" type="text/css" href="https://cdnjs.cloudflare.com/ajax/libs/highlight.js/9.1.0/styles/default.min.css"/> ++
    <link rel="stylesheet" type="text/css" href="/koski/css/codemirror/codemirror.css"/>



  private def raamit(enabled: Boolean) = if (enabled) <script type="text/javascript" src="/virkailija-raamit/apply-raamit.js"/> else Empty

  def htmlErrorObjectScript(status: HttpStatus): Elem =
    <script type="text/javascript">
      {CommentedPCData("""
        window.koskiError = {
          httpStatus: """ + status.statusCode + """,
          text: '""" + errorString(status).getOrElse(localizations.get("httpStatus." + status.statusCode).get(lang)).replace("'", "\\'") + """',
          topLevel: true
        }
      """)}
    </script>

  private def scriptTimestamp(scriptBundleName: String) = new File(s"./target/webapp/js/$scriptBundleName").lastModified()
}
