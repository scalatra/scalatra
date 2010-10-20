package org.scalatra.auth

import javax.servlet.http._
import org.scalatra.{RichRequest, RichSession, SweetCookies, RouteMatcher}

/**
 * Created by IntelliJ IDEA.
 * User: ivan
 * Date: Aug 21, 2010
 * Time: 10:04:05 AM
 * 
 */

class ScalatraKernelProxy {

  protected implicit def sessionWrapper(s: HttpSession) = new RichSession(s)
  protected implicit def requestWrapper(s: HttpServletRequest) = new RichRequest(s)
  protected implicit def cookieWrapper(s: Array[Cookie]) = new SweetCookies(s, response)

  private var _session: () => HttpSession = _
  private var _params: () => collection.Map[String, String] = _
  private var _redirect: String => Unit = _
  private var _request: () => HttpServletRequest = _
  private var _response: () => HttpServletResponse = _
  private var _cookies: () => Array[Cookie] = _

  /**
   * Provides a proxy to the session object with the same interface as in a <code>ScalatraFilter</code> or
   * a <code>ScalatraServlet</code>.
   *
   * @return a session object
   */
  def session = _session()
  private[auth] def session_=(sess: => HttpSession) = {
    _session = () => sess
  }

  def params = _params()
  private[auth] def params_=(paramsBag: => collection.Map[String, String]) = {
    _params = () => paramsBag
  }

  def redirect(uri: String) = _redirect(uri)
  private[auth] def redirect_=(redirectFunction: String => Unit) = _redirect = redirectFunction

  def request = _request()
  def response = _response()
  def request_=(req: => HttpServletRequest) = _request = () => req
  def response_=(res: => HttpServletResponse) = _response = () => res

  def cookies = _cookies()
  def cookies_=(cookieJar: => Array[Cookie]) = _cookies = () => cookieJar

}

object ScalatraKernelProxy {
  def apply(session: => HttpSession, params: => collection.Map[String, String], redirect: String => Unit,
            request: => HttpServletRequest, response: => HttpServletResponse, cookies: => Array[Cookie] ) ={
    val ctxt = new ScalatraKernelProxy
    ctxt.session = session
    ctxt.params = params
    ctxt.redirect_=(redirect)
    ctxt.response = response
    ctxt.request = request
    ctxt.cookies = cookies
    ctxt
  }



}
