package org.scalatra.auth

import org.scalatra.RouteMatcher
import javax.servlet.http.{HttpServletResponse, HttpServletRequest, HttpServlet, HttpSession}

/**
 * Created by IntelliJ IDEA.
 * User: ivan
 * Date: Aug 21, 2010
 * Time: 10:04:05 AM
 * 
 */

class ScalatraKernelProxy {
  private var _session: () => HttpSession = _
  private var _params: () => collection.Map[String, String] = _
  private var _redirect: String => Unit = _

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

}

object ScalatraKernelProxy {
  def apply(session: => HttpSession, params: => collection.Map[String, String], redirect: String => Unit) ={
    val ctxt = new ScalatraKernelProxy
    ctxt.session = session
    ctxt.params = params
    ctxt.redirect_=(redirect)
    ctxt
  }

}
