package org.scalatra

import java.io.PrintWriter
import java.util.zip.GZIPOutputStream
import javax.servlet.ServletOutputStream
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.servlet.http.HttpServletResponseWrapper
import util.RicherString._

/**
 * Scalatra handler for gzipped responses.
 */
trait GZipSupport extends Handler {
  self: ScalatraBase =>

  abstract override def handle(req: HttpServletRequest, res: HttpServletResponse) {
    withRequestResponse(req, res) {
      if (isGzip) {
        val gzos = new GZIPOutputStream(res.getOutputStream)
        val w = new PrintWriter(gzos)
        val gzsos = new ServletOutputStream { def write(b: Int) { gzos.write(b) } }

        val response = new HttpServletResponseWrapper(res) {
          override def getOutputStream: ServletOutputStream = gzsos
          override def getWriter: PrintWriter = w
          override def setContentLength(i: Int) = {} // ignoring content length as it won't be the same when gzipped
        }

        ScalatraBase onCompleted { _ => response.addHeader("Content-Encoding", "gzip") }

        ScalatraBase onRenderedCompleted { _ =>
          w.flush()
          w.close()
        }

        withRequestResponse(req, response) { super.handle(req, response) }
      } else {
        super.handle(req, res)
      }
    }
  }

  /**
   * Returns true if Accept-Encoding contains gzip.
   */
  private[this] def isGzip(implicit request: HttpServletRequest): Boolean = {
    Option(request.getHeader("Accept-Encoding")).getOrElse("")
      .toUpperCase.contains("GZIP")
  }
}
