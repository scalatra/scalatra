package org.scalatra.util

import java.nio.charset.Charset

class RicherString(orig: String) {
  def isBlank = orig == null || orig.trim.isEmpty
  @deprecated("Use nonBlank instead", "2.0")
  def isNonBlank = !isBlank
  
  @deprecated("Use blankOption instead", "2.0")
  def toOption = if (isBlank) None else Some(orig)
  def blankOption = toOption
  def nonBlank = !isBlank
//  def urlDecode(charset: Charset =)
}

object RicherString {
  implicit def stringToRicherString(s: String) = new RicherString(s)
}
