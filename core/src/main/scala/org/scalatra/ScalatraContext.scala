package org.scalatra

import servlet.ServletApiImplicits
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import util.{MapWithIndifferentAccess, MultiMapHeadView}
import javax.servlet.ServletContext

class ScalatraParams(protected val multiMap: Map[String, Seq[String]]) extends MultiMapHeadView[String, String] with MapWithIndifferentAccess[String]

/*
  Needs contexts for
  FlashMap, file upload support
 */

object ScalatraContext {
  private class StableValuesContext(implicit val request: HttpServletRequest, val response: HttpServletResponse, val servletContext: ServletContext) extends ScalatraContext
}
trait ScalatraContext extends SessionSupport with CookieContext {
  import ScalatraContext.StableValuesContext
  implicit def request: HttpServletRequest
  implicit def response: HttpServletResponse
  def servletContext: ServletContext

  /**
   * Gets the content type of the current response.
   */
  def contentType: String = response.contentType getOrElse null

  /**
   * Sets the content type of the current response.
   */
  def contentType_=(contentType: String) {
    response.contentType = Option(contentType)
  }

  @deprecated("Use status_=(Int) instead", "2.1.0")
  def status(code: Int) { status_=(code) }

  /**
   * Sets the status code of the current response.
   */
  def status_=(code: Int) { response.status = ResponseStatus(code) }

  /**
   * Gets the status code of the current response.
   */
  def status: Int = response.status.code

  /**
   * The current multiparams.  Multiparams are a result of merging the
   * standard request params (query string or post params) with the route
   * parameters extracted from the route matchers of the current route.
   * The default value for an unknown param is the empty sequence.  Invalid
   * outside `handle`.
   */
  def multiParams: MultiParams = {
    val read = request.contains("MultiParamsRead")
    val found = request.get(MultiParamsKey) map (
     _.asInstanceOf[MultiParams] ++ (if (read) Map.empty else request.multiParameters)
    )
    val multi = found getOrElse request.multiParameters
    request("MultiParamsRead") = new {}
    request(MultiParamsKey) = multi
    multi.withDefaultValue(Seq.empty)
  }

  def params: Params = new ScalatraParams(multiParams)
  implicit def scalatraContext: ScalatraContext  = new StableValuesContext()(request, response, servletContext)
}