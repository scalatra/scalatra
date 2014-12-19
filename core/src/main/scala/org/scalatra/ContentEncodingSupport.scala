package org.scalatra
import javax.servlet.http.{ HttpServletRequest, HttpServletResponse }

/**
 * Scalatra handler for gzipped responses.
 */
trait ContentEncodingSupport extends Handler {
  self: ScalatraBase =>

  /** Encodes the response if necessary. */
  private def encodedResponse(req: HttpServletRequest, res: HttpServletResponse): HttpServletResponse =
    Conneg.preferredEncoding.map { encoding =>
      val encoded = encoding(res)
      ScalatraBase.onRenderedCompleted { _ => encoded.end() }
      encoded
    }.getOrElse(res)

  /** Decodes the request if necessary. */
  private def decodedRequest(req: HttpServletRequest): HttpServletRequest = (for {
    name <- Option(req.getHeader("Content-Encoding"))
    enc <- ContentEncoding.forName(name)
  } yield enc(req)).getOrElse(req)

  abstract override def handle(req: HttpServletRequest, res: HttpServletResponse) {
    withRequestResponse(req, res) {
      super.handle(decodedRequest(req), encodedResponse(req, res))
    }
  }
}