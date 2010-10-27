package org.scalatra.util

class RicherString(orig: String) {
  def isBlank = orig == null || orig.trim.isEmpty
  def isNonBlank = !isBlank
}

object RicherString {
  implicit def stringToRicherString(s: String) = new RicherString(s)
}
