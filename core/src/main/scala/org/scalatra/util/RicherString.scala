package org.scalatra.util

import java.nio.charset.Charset

class RicherString(orig: String) {
  def isBlank = orig == null || orig.trim.isEmpty
  def isNonBlank = !isBlank
  def toOption = nonBlank
  def nonBlank = if (isBlank) None else Some(orig)
//  def urlDecode(charset: Charset =)
}

object RicherString {
  implicit def stringToRicherString(s: String) = new RicherString(s)
}
