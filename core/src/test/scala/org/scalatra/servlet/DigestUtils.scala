package org.scalatra.servlet

import java.security.MessageDigest

object DigestUtils {
  def shaHex(bytes: Array[Byte]): String = {
    val digest = MessageDigest.getInstance("SHA")
    val digestBytes = digest.digest(bytes)

    hexEncode(digestBytes.toList)
  }

  def hexEncode(bytes: List[Byte]): String = {
    bytes
      .map { b =>
        String.format("%02X", java.lang.Byte.valueOf(b))
      }
      .mkString("")
      .toLowerCase
  }
}
