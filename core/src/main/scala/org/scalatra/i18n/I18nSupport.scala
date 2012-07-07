package org.scalatra
package i18n

import java.util.Locale

object I18nSupport {
  def localeKey = "locale"

  def userLocalesKey = "userLocales"

  def messagesKey = "messages"
}

trait I18nSupport {

  this: ScalatraBase with CookieSupport =>

  import I18nSupport._


  def locale  = if (request == null) {
    throw new ScalatraException("There needs to be a request in scope to call locale")
  } else {
    request.get(localeKey).map(_.asInstanceOf[Locale]).orNull
  }

  def userLocales = if (request == null) {
    throw new ScalatraException("There needs to be a request in scope to call userLocales")
  } else {
    request.get(userLocalesKey).map(_.asInstanceOf[Array[Locale]]).orNull
  }

  def messages = if (request == null) {
    throw new ScalatraException("There needs to be a request in scope to call messages")
  } else {
    request.get(messagesKey).map(_.asInstanceOf[Messages]).orNull
  }


  before() {
    request.setAttribute(localeKey, resolveLocale)
    request.setAttribute(messagesKey, new Messages(locale))
  }

  /*
  * Resolve Locale based on HTTP request parameter or Cookie
  */
  private def resolveLocale: Locale = {
    resolveHttpLocale getOrElse defaultLocale
  }

  /*
   * Get locale either from HTTP param, Cookie or Accept-Language header.
   * 
   * If locale string is found in HTTP param, it will be set
   * in cookie. Later requests will read locale string directly from this
   *
   * If it's not found, then look at Accept-Language header.
   *
   * Locale strings are transformed to [[java.util.Locale]]
   *
   */
  private def resolveHttpLocale: Option[Locale] = {
    (params.get(localeKey) match {
      case Some(localeValue) =>
        cookies.set(localeKey, localeValue)
        Some(localeValue)
      case _ => cookies.get(localeKey)
    }).map(localeFromString(_)) orElse resolveHttpLocaleFromUserAgent
  }

  /**
   * Accept-Language header looks like "de-DE,de;q=0.8,en-US;q=0.6,en;q=0.4"
   * Specification see [[http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html]]
   * 
   * @return first preferred found locale or None
   */
  private def resolveHttpLocaleFromUserAgent: Option[Locale] = {

    request.headers.get("Accept-Language") map { s =>
        val locales = s.split(",").map(s => {
          def splitLanguageCountry(s: String): Locale = {
            val langCountry = s.split("-")
            if (langCountry.length > 1) {
              new Locale(langCountry.head, langCountry.last)
            } else {
              new Locale(langCountry.head)
            }
          }
          // If this language has a quality index:
          if (s.indexOf(";") > 0) {
            val qualityLocale = s.split(";")
            splitLanguageCountry(qualityLocale.head)
          } else {
            splitLanguageCountry(s)
          }
        })
        // save all found locales for later user
        request.setAttribute(userLocalesKey, locales)

        // We assume that all accept-languages are stored in order of quality
        // (so first language is preferred)
        locales.head
    }
  }

  /**
   * Reads a locale from a String
   * @param in a string like en_GB or de_DE
   */
  private def localeFromString(in: String): Locale = {

      val token = in.split("_")
      new Locale(token.head, token.last)
  }

  private def defaultLocale: Locale = {
    Locale.getDefault
  }
}
