package org.scalatra

import scala.annotation.tailrec
import java.io.{InputStream, OutputStream}

/**
 * A collection of I/O ulility methods.
 */
package object io {
  /**
   * Copies the input stream to the output stream.
   *
   * @param in the input stream to read
   * @param out the output stream to write
   * @param bufferSize the size of buffer to use for each read
   */
  def copy(in: InputStream, out: OutputStream, bufferSize: Int = 4096): Unit = {
    val buf = new Array[Byte](bufferSize)
    @tailrec
    def loop() {
      val n = in.read(buf)
      if (n >= 0) {
        out.write(buf, 0, n)
        loop()
      }
    }
    loop()
    in.close()
  }
}