package org.scalatra
package i18n

import java.util.Locale
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

object I18nSupport {
  def localeKey = "locale"
  def messagesKey = "messages"
}

trait I18nSupport {
  this: ScalatraKernel with CookieSupport =>
    
  import I18nSupport._

  var locale: Locale = _
  var messages: Messages = _

  before() {
    locale = resolveLocale
    messages = new Messages(locale)
  }
  
  /*
   * Resolve Locale based on HTTP request parameter or Cookie
   */
  private def resolveLocale: Locale = {
    resolveHttpLocale.map(localeFromString(_)) getOrElse defaultLocale
  }

  /*
   * Get locale string either from HTTP param or Cookie.
   * 
   * If locale string is found in HTTP param, it will be set
   * in cookie.
   * 
   * If it's not found, then look at Accept-Language header.
   *
   */
  private def resolveHttpLocale: Option[String] = {
    
    params.get(localeKey) match {
      case Some(localeValue) =>
        cookies.set(localeKey, localeValue)
        Some(localeValue)
      case _ => cookies.get(localeKey) orElse Option(request.getHeader("Accept-Language"))
    }
  }

  private def localeFromString(in: String): Locale = {
    val x = in.split("_")
    new Locale(x.head, x.last)
  }

  private def defaultLocale: Locale = {
    Locale.getDefault
  }
}
