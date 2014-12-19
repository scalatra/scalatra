package org.scalatra.util
package io

import scala.io.Source
import java.io._
import org.scalatest.WordSpec
import org.scalatest.Matchers

class IoSpec extends WordSpec with Matchers {
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
      out.toByteArray should equal(bytes)
    }

    "close the input stream even if copying throws" in {
      var isClosed = false
      val in = new InputStream {
        def read() = throw new RuntimeException
        override def close() = isClosed = true
      }
      try {
        copy(in, new ByteArrayOutputStream)
      } catch { case ignore: Throwable => }
      isClosed should equal(true)
    }

    "throw any exception during copy" in {
      val e = new RuntimeException
      val in = new InputStream {
        def read() = throw e
      }
      val caught = try {
        copy(in, new ByteArrayOutputStream)
        None
      } catch { case ex: Throwable => Some(ex) }
      caught should equal(Some(e))
    }
  }

  "withTempFile" should {
    val content = "content"

    "create a temp file with the specified contents" in {
      withTempFile(content) { f =>
        Source.fromFile(f).mkString should equal(content)
      }
    }

    "remove the temp file on completion of the block" in {
      var f: File = null
      withTempFile(content) { myF =>
        f = myF
        f.exists() should be(true)
      }
      f.exists() should be(false)
    }

    "remove the temp file even if the block throws" in {
      var f: File = null
      try {
        withTempFile(content) { myF =>
          f = myF
          throw new RuntimeException()
        }
      } catch {
        case _: Throwable => f.exists() should be(false)
      }
    }
  }
}

