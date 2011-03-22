package org.scalatra.util

import java.nio.charset.Charset

class RicherString(orig: String) {
  def isBlank = orig == null || orig.trim.isEmpty
  def isNonBlank = !isBlank
//  def urlDecode(charset: Charset =)
}

object RicherString {
  implicit def stringToRicherString(s: String) = new RicherString(s)
}
