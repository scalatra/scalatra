package org.scalatra.io

import java.io._
import org.scalatest.WordSpec
import org.scalatest.matchers.ShouldMatchers

class IoSpec extends WordSpec with ShouldMatchers {
  "copy" should {
   "copy an input stream smaller than the buffer size to the output stream" in {
      testCopy(100, 256)
    }

    "copy an input stream smaller equal to the buffer size to the output stream" in {
      testCopy(256, 256)
    }

    "copy an input stream smaller greater than the buffer size to the output stream" in {
      testCopy(300, 256)
    }

    "copy an empty input stream to the output stream" in {
      testCopy(0, 256)
    }

    "not blow the stack" in {
      testCopy(10000, 10000)
    }

    def testCopy(len: Int, bufferSize: Int) {
      val bytes: Array[Byte] = (0 until len) map { x => x.toByte } toArray
      val in = new ByteArrayInputStream(bytes)
      val out = new ByteArrayOutputStream
      copy(in, out, bufferSize)
      out.toByteArray should equal (bytes)
    }
  }
}


