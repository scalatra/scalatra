package org.scalatra

import java.io.{ByteArrayOutputStream, OutputStream, IOException, PrintWriter}
import java.util.zip.GZIPOutputStream
import javax.servlet.{WriteListener, ServletOutputStream}
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.servlet.http.HttpServletResponseWrapper
import java.nio.charset.Charset

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
        val r = response

        ScalatraBase onRenderedCompleted { _ =>
          w.flush()
          w.close()
        }

        val gzsos = new GZipServletOutputStream(gzos, r.getOutputStream)
        val wrapped = new WrappedGZipResponse(r, gzsos, w)
        ScalatraBase.onCompleted { _ =>
          wrapped.addHeader("Content-Encoding", "gzip")
        }
        super.handle(req, wrapped)
      } else {
        super.handle(req, res)
      }
    }
  }

  private class GZipServletOutputStream(gzos: GZIPOutputStream, orig: ServletOutputStream) extends ServletOutputStream {
    override def write(b: Int): Unit = gzos.write(b)
    override def setWriteListener(writeListener: WriteListener): Unit = orig.setWriteListener(writeListener)
    override def isReady: Boolean = orig.isReady()
  }

  private class WrappedGZipResponse(res: HttpServletResponse, gzsos: ServletOutputStream, w: PrintWriter) extends HttpServletResponseWrapper(res) {
    override def getOutputStream: ServletOutputStream = gzsos
    override def getWriter: PrintWriter = w
    override def setContentLength(i: Int) = {} // ignoring content length as it won't be the same when gzipped
  }

  /**
   * Returns true if Accept-Encoding contains gzip.
   */
  private[this] def isGzip(implicit request: HttpServletRequest): Boolean = {
    Option(request.getHeader("Accept-Encoding")).getOrElse("").toUpperCase.contains("GZIP")
  }
}
