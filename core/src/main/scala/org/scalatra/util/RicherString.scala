package org.scalatra.util

import java.nio.charset.{ Charset, StandardCharsets }
import java.util.regex.Pattern
import org.apache.commons.lang3.StringUtils

package object RicherString {

  private val emptyCharSet = Set[Int]()

  implicit final class RicherStringImplicitClass(private val orig: String) extends AnyVal {

    def blankOption: Option[String] = if (StringUtils.isBlank(orig)) None else Some(orig)
    def nonBlank: Boolean = !StringUtils.isBlank(orig)

    def urlEncode: String = UrlCodingUtils.urlEncode(orig)
    def formEncode: String = UrlCodingUtils.urlEncode(orig, spaceIsPlus = true)
    def urlDecode: String = UrlCodingUtils.urlDecode(orig, charset = StandardCharsets.UTF_8, plusIsSpace = false, skip = emptyCharSet)
    def formDecode: String = UrlCodingUtils.urlDecode(orig, charset = StandardCharsets.UTF_8, plusIsSpace = true, skip = emptyCharSet)

    def urlEncode(charset: Charset): String = UrlCodingUtils.urlEncode(orig, charset)
    def formEncode(charset: Charset): String = UrlCodingUtils.urlEncode(orig, charset, spaceIsPlus = true)
    def urlDecode(charset: Charset): String = UrlCodingUtils.urlDecode(orig, charset, plusIsSpace = false, skip = emptyCharSet)
    def formDecode(charset: Charset): String = UrlCodingUtils.urlDecode(orig, charset, plusIsSpace = true, skip = emptyCharSet)

    def /(path: String): String = {
      (orig.endsWith("/"), path.startsWith("/")) match {
        case (true, false) | (false, true) => orig + path
        case (false, false) => orig + "/" + path
        case (true, true) => orig + path substring 1
      }
    }

    def regexEscape: String = Pattern.quote(orig)

    def toCheckboxBool: Boolean = {
      orig.toUpperCase match {
        case "ON" | "TRUE" | "OK" | "1" | "CHECKED" | "YES" | "ENABLE" | "ENABLED" => true
        case _ => false
      }
    }
  }

}
