package org.scalatra.auth

import javax.servlet.http.HttpSession

/**
 * Created by IntelliJ IDEA.
 * User: ivan
 * Date: Aug 21, 2010
 * Time: 10:04:05 AM
 * 
 */

class AuthenticationContext {
  private var _session: () => HttpSession = _
  private var _params: () => collection.Map[String, String] = _
  private var _redirect: String => Unit = _

  def session = _session()
  def session_=(sess: => HttpSession) = {
    _session = () => sess
  }

  def params = _params()
  def params_=(paramsBag: => collection.Map[String, String]) = {
    _params = () => paramsBag
  }

  def redirect(uri: String) = _redirect(uri)
  def redirect_=(redirectFunction: String => Unit) = _redirect = redirectFunction

}

object AuthenticationContext {
  def apply(session: => HttpSession, params: => collection.Map[String, String], redirect: String => Unit) ={
    val ctxt = new AuthenticationContext
    ctxt.session = session
    ctxt.params = params
    ctxt.redirect_=(redirect)
    ctxt
  }

}
