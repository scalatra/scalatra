package org.scalatra.util

import scala.util.matching.Regex
import scala.util.matching.Regex.Match
import java.util.Locale
import java.nio.charset.Charset
import java.nio.{ CharBuffer, ByteBuffer }
import collection.immutable.BitSet

trait UrlCodingUtils {

  private val toSkip = BitSet((('a' to 'z') ++ ('A' to 'Z') ++ ('0' to '9') ++ "!$&'()*+,;=:/?@-._~".toSet).map(_.toInt): _*)
  private val toSkipEncoding = BitSet((('a' to 'z') ++ ('A' to 'Z') ++ ('0' to '9') ++ ":@-._~".toSet).map(_.toInt): _*)
  private val space = ' '.toInt
  private val PctEncoded = """%([0-9a-fA-F][0-9a-fA-F])""".r
  private val LowerPctEncoded = """%([0-9a-f][0-9a-f])""".r
  private val InvalidChars = "[^\\.a-zA-Z0-9!$&'()*+,;=:/?#\\[\\]@-_~]".r

  private val HexUpperCaseChars = (0 until 16) map { i ⇒ Character.toUpperCase(Character.forDigit(i, 16)) }

  private val UTF_8 = "UTF-8"
  private val Utf8 = Charset.forName(UTF_8)

  def isUrlEncoded(string: String) = {
    PctEncoded.findFirstIn(string).isDefined
  }

  def containsInvalidUriChars(string: String) = {
    InvalidChars.findFirstIn(string).isDefined
  }

  def needsUrlEncoding(string: String) = {
    !isUrlEncoded(string) && containsInvalidUriChars(string)
  }

  def ensureUrlEncoding(string: String) = if (needsUrlEncoding(string)) urlEncode(string, toSkip = toSkip) else string

  def ensureUppercasedEncodings(string: String) = {
    LowerPctEncoded.replaceAllIn(string, (_: Match) match {
      case Regex.Groups(v) ⇒ "%" + v.toUpperCase(Locale.ENGLISH)
    })
  }

  def pathPartEncode(toEncode: String, charset: Charset = Utf8, spaceIsPlus: Boolean = false) = {
    urlEncode(toEncode, charset, spaceIsPlus, toSkipEncoding)
  }

  def queryPartEncode(toEncode: String, charset: Charset = Utf8, spaceIsPlus: Boolean = false) = {
    urlEncode(toEncode, charset, spaceIsPlus, toSkipEncoding ++ BitSet('/', '?'))
  }

  def urlEncode(toEncode: String, charset: Charset = Utf8, spaceIsPlus: Boolean = false, toSkip: BitSet = toSkip) = {
    val in = charset.encode(ensureUppercasedEncodings(toEncode))
    val out = CharBuffer.allocate((in.remaining() * 3).ceil.toInt)
    while (in.hasRemaining) {
      val b = in.get() & 0xFF
      if (toSkip.contains(b)) {
        out.put(b.toChar)
      } else if (b == space && spaceIsPlus) {
        out.put('+')
      } else {
        out.put('%')
        out.put(HexUpperCaseChars((b >> 4) & 0xF))
        out.put(HexUpperCaseChars(b & 0xF))
      }
    }
    out.flip()
    out.toString
  }

  def urlDecode(toDecode: String, charset: Charset = Utf8, plusIsSpace: Boolean = false, toSkip: String = ""): String = {
    urlDecode(toDecode, charset, plusIsSpace, toSkip.toSet[Char].map(c ⇒ c.toInt))
  }

  def urlDecode(toDecode: String, charset: Charset, plusIsSpace: Boolean, skip: Set[Int]): String = {
    val in = CharBuffer.wrap(toDecode)
    // reserve enough space for 3-byte chars like japanese, and hope nobody uses a string of only 4-byte chars
    val out = ByteBuffer.allocate(in.remaining() * 3)
    while (in.hasRemaining) {
      val mark = in.position()
      val c = in.get()
      if (c == '%') {
        if (in.remaining() >= 2) {
          val xc = in.get()
          val yc = in.get()
          val x = Character.digit(xc, 0x10)
          val y = Character.digit(yc, 0x10)
          if (x != -1 && y != -1) {
            val oo = (x << 4) + y
            if (!skip.contains(oo)) {
              out.put(oo.toByte)
            } else {
              out.put('%'.toByte)
              out.put(xc.toByte)
              out.put(yc.toByte)
            }
          } else {
            out.put('%'.toByte)
            in.position(mark + 1)
          }
        } else {
          out.put('%'.toByte)
        }
      } else if (c == '+' && plusIsSpace) {
        out.put(' '.toByte)
      } else {
        // normally `out.put(c.toByte)` would be enough since the url is %-encoded,
        // however there are cases where a string can be partially decoded
        // so we have to make sure the non us-ascii chars get preserved properly.
        if (this.toSkip.contains(c))
          out.put(c.toByte)
        else {
          out.put(charset.encode(String.valueOf(c)))
        }
      }
    }
    out.flip()
    charset.decode(out).toString
  }

}

object UrlCodingUtils extends UrlCodingUtils
