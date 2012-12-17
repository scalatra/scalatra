package org.scalatra

import java.io.OutputStream
import java.io.PrintWriter
import java.util.zip.GZIPOutputStream
import javax.servlet.ServletOutputStream
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.servlet.http.HttpServletResponseWrapper

/**
 * Scalatra handler for gzipped responses.
 */
trait GZipSupport extends Handler with Initializable {
  self: ScalatraSyntax =>

  abstract override def handle(req: HttpServletRequest, res: HttpServletResponse) {

    if (isGzip(req)) {
      var w: PrintWriter = null
      var s = new ContentLengthOutputStream(res.getOutputStream())

      val response = new HttpServletResponseWrapper(res) {
        override def getOutputStream(): ServletOutputStream = {
          val gzip = new GZIPOutputStream(s)
          w = new PrintWriter(gzip)
          return new ServletOutputStream {
            override def write(b: Int) = gzip.write(b)
          }
        }
        override def getWriter(): PrintWriter = {
          w = new PrintWriter(new GZIPOutputStream(s));
          return w
        }
        override def setContentLength(i: Int) = {} // ignoring content length as it wont be the same when gzipped
      }

      super.handle(req, response)

      if (w != null) {
        response.addHeader("Content-Encoding", "gzip")

        w.flush
        w.close

        response.setContentLength(s.length)
      }
    } else {
      super.handle(req, res)
    }
  }

  /**
   * Returns true if Accept-Encoding contains gzip.
   */
  private def isGzip(request: HttpServletRequest): Boolean = {
    val encoding: java.util.Enumeration[_] = request.getHeaders("Accept-Encoding")

    while (encoding.hasMoreElements) {
      if (encoding.nextElement().toString.contains("gzip")) {
        return true
      }
    }

    return false
  }
}

/**
 * Wrapper output stream that counts the content length.
 */
private class ContentLengthOutputStream(s: OutputStream) extends OutputStream {
  var length = 0
  private val stream = s

  override def write(b: Int) = {
    stream.write(b)
    length += 1
  }
}
