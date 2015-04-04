package org.scalatra

/** Provides the using function and extension methods for String objects */
package object util {

  /**
   * Executes a block with a closeable resource, and closes it after the block runs
   *
   * @tparam A the return type of the block
   * @tparam B the closeable resource type
   * @param closeable the closeable resource
   * @param f the block
   */
  def using[A, B <: { def close(): Unit }](closeable: B)(f: B => A): A = {
    try {
      f(closeable)
    } finally {
      if (closeable != null) {
        closeable.close()
      }
    }
  }

  implicit class RicherString(val orig: String) extends AnyVal {

    import java.nio.charset.Charset
    import java.util.regex.Pattern

    import rl.UrlCodingUtils

    def isBlank: Boolean = {
      orig == null || orig.trim.isEmpty
    }

    @deprecated("Use nonBlank instead", "2.0")
    def isNonBlank: Boolean = !isBlank

    @deprecated("Use blankOption instead", "2.0")
    def toOption: Option[String] = blankOption
    def blankOption: Option[String] = if (isBlank) None else Some(orig)
    def nonBlank: Boolean = !isBlank

    def urlEncode: String = UrlCodingUtils.urlEncode(orig)
    def formEncode: String = UrlCodingUtils.urlEncode(orig, spaceIsPlus = true)
    def urlDecode: String = UrlCodingUtils.urlDecode(orig)
    def formDecode: String = UrlCodingUtils.urlDecode(orig, plusIsSpace = true)

    def urlEncode(charset: Charset): String = UrlCodingUtils.urlEncode(orig, charset)
    def formEncode(charset: Charset): String = UrlCodingUtils.urlEncode(orig, charset, spaceIsPlus = true)
    def urlDecode(charset: Charset): String = UrlCodingUtils.urlDecode(orig, charset)
    def formDecode(charset: Charset): String = UrlCodingUtils.urlDecode(orig, charset, plusIsSpace = true)

    def /(path: String): String = {
      (orig.endsWith("/"), path.startsWith("/")) match {
        case (true, false) | (false, true) ⇒ orig + path
        case (false, false) ⇒ orig + "/" + path
        case (true, true) ⇒ orig + path substring 1
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
