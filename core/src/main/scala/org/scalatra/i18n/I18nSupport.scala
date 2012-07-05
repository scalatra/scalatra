package org.scalatra
package i18n

import java.util.Locale
import util.HeaderWithQParser

object I18nSupport {
  val LocaleKey = "org.scalatra.i18n.Locale"

  val MessagesKey = "org.scalatra.i18n.Messages"
}

trait I18nSupport {

  this: ScalatraApp =>

  import I18nSupport._

  var userLocale: Locale = _
  var messages: Messages = _
  var userLocales: Array[Locale] = _

  before() {
    userLocale = resolveLocale
    messages = new Messages(userLocale)
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
    resolveFromCookie map localeFromString orElse resolveHttpLocaleFromUserAgent
  }

  private def resolveFromCookie: Option[String] =
    params get LocaleKey map { l => request.cookies.set(LocaleKey, l); l } orElse request.cookies.get(LocaleKey)

  /**
   * Accept-Language header looks like "de-DE,de;q=0.8,en-US;q=0.6,en;q=0.4"
   * Specification see [[http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html]]
   * 
   * @return first preferred found locale or None
   */
  private def resolveHttpLocaleFromUserAgent: Option[Locale] = {
    Option(request.locale)
//    request.headers.get("Accept-Language") map { s =>
//        val locales = s.split(",").map(s => {
//          def splitLanguageCountry(s: String): Locale = {
//            val langCountry = s.split("-")
//            if (langCountry.length > 1) {
//              new Locale(langCountry.head, langCountry.last)
//            } else {
//              new Locale(langCountry.head)
//            }
//          }
//          // If this language has a quality index:
//          if (s.indexOf(";") > 0) {
//            val qualityLocale = s.split(";")
//            splitLanguageCountry(qualityLocale.head)
//          } else {
//            splitLanguageCountry(s)
//          }
//        })
//        // save all found locales for later user
//        userLocales = locales
//        // We assume that all accept-languages are stored in order of quality
//        // (so first language is preferred)
//        locales.head
//    }
  }

  /**
   * Reads a locale from a String
   * @param in a string like en_GB or de-DE
   */
  private def localeFromString(in: String): Locale = {

      val token = in.split("_|-")
      new Locale(token.head, token.last)
  }

  private def defaultLocale: Locale = {
    Locale.getDefault
  }
}
