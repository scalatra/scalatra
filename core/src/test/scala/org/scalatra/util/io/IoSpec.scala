package org.scalatra.util
package io

import scala.io.Source
import java.io._
import org.specs2.Specification

class IoSpec extends Specification { def is =
  "copy should" ^
   "copy an input stream smaller than the buffer size to the output stream" ! {
      testCopy(100, 256)
    } ^
    "copy an input stream smaller equal to the buffer size to the output stream" ! {
      testCopy(256, 256)
    } ^
    "copy an input stream smaller greater than the buffer size to the output stream" ! {
      testCopy(300, 256)
    } ^
    "copy an empty input stream to the output stream" ! {
      testCopy(0, 256)
    } ^
    "not blow the stack" ! {
      testCopy(10000, 10000)
    } ^
    "close the input stream even if copying throws" ! {
       var isClosed = false
       val in = new InputStream {
         def read() = throw new RuntimeException
         override def close() = isClosed = true
       }
       try {
         org.scalatra.util.io.copy(in, new ByteArrayOutputStream)
       }
       catch { case _ => }
       isClosed should beTrue
     } ^
     "throw any exception during copy" ! {
       val e = new RuntimeException
       val in = new InputStream {
         def read() = throw e
       }
       val caught = try {
         org.scalatra.util.io.copy(in, new ByteArrayOutputStream)
         None
       }
       catch { case ex => Some(ex) }
       caught should beSome(e)
     } ^ bt ^
    "with TempFile should" ^
      "create a temp file with the specified contents" ! {
        withTempFile(text) { f =>
          Source.fromFile(f).mkString must_== text
        }
      } ^
      "remove the temp file on completion of the block" ! {
        var f: File = null
        withTempFile(text) { myF =>
          f = myF
          f.exists() should beTrue
        } and {
          f.exists() should beFalse
        }
      } ^
      "remove the temp file even if the block throws" ! {
        var f: File = null
        try {
          withTempFile(text) { myF =>
            f = myF
            throw new RuntimeException()
          }
        }
        catch {
          case _ => f.exists() should beFalse
        }
      } ^ end

  val text = "content"
  def testCopy(len: Int, bufferSize: Int) = {
    val bytes: Array[Byte] = (0 until len) map { x => x.toByte } toArray
    val in = new ByteArrayInputStream(bytes)
    val out = new ByteArrayOutputStream
    org.scalatra.util.io.copy(in, out, bufferSize)
    out.toByteArray must_== bytes
  }



}


