package org.scalatra
package scalate

trait ScalateRenderSupport { self: ScalatraBaseBase with ScalateSupport =>

  val templateBaseDirectory = "/WEB-INF/scalate/templates"
  val scalateExtension = "ssp"

  lazy val none = 0
  lazy val oneMinute = 60
  lazy val oneHour = oneMinute * 60
  lazy val oneDay = oneHour * 24
  lazy val oneWeek = oneDay * 7
  lazy val oneMonth = oneWeek * 4
  lazy val oneYear = oneWeek * 52

  def render(file: String, params: Map[String, Any] = Map(), responseContentType: String = "text/html", cacheMaxAge: Int = none, statusCode: Int = 200) {
    contentType = responseContentType
    response.setHeader("Cache-Control", "public, max-age=%d" format cacheMaxAge)
    response.setStatus(statusCode)
    renderResponseBody(templateEngine.layout("%s/%s.%s".format(templateBaseDirectory, file, scalateExtension), params))
  }

}
