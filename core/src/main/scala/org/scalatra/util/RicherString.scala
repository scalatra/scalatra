package org.scalatra.util

class RicherString(orig: String) {
  def isBlank = orig == null || orig.trim.isEmpty
  @deprecated("Use nonBlank instead", "2.0")
  def isNonBlank = !isBlank

  @deprecated("Use blankOption instead", "2.0")
  def toOption = blankOption
  def blankOption = if (isBlank) None else Some(orig)
  def nonBlank = !isBlank
//  def urlDecode(charset: Charset =)
}

object RicherString {
  implicit def stringToRicherString(s: String) = new RicherString(s)
}
