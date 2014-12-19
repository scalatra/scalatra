package org.scalatra
import java.io._
import java.nio.charset.Charset
import java.util.zip.{ DeflaterOutputStream, GZIPInputStream, GZIPOutputStream, InflaterInputStream }
import javax.servlet.http.{ HttpServletRequest, HttpServletRequestWrapper, HttpServletResponse, HttpServletResponseWrapper }
import javax.servlet.{ ReadListener, ServletInputStream, ServletOutputStream, WriteListener }

import scala.util.Try

// - Content encoding --------------------------------------------------------------------------------------------------
// ---------------------------------------------------------------------------------------------------------------------
/** Represents an HTTP content encoding. */
trait ContentEncoding {
  /** Name of the encoding, as used in the `Content-Encoding` and `Accept-Encoding` headers. */
  def name: String

  /** Wraps the specified output stream into an encoding one. */
  def encode(out: OutputStream): OutputStream

  /** Wraps the specified input stream into a decoding one. */
  def decode(in: InputStream): InputStream

  override def toString = name
  def apply(response: HttpServletResponse): HttpServletResponse = new EncodedServletResponse(response, this)
  def apply(request: HttpServletRequest): HttpServletRequest = new DecodedServletRequest(request, this)

}

object ContentEncoding {
  private def create(id: String, e: OutputStream => OutputStream, d: InputStream => InputStream): ContentEncoding =
    new ContentEncoding {
      override def name: String = id
      override def encode(out: OutputStream): OutputStream = e(out)
      override def decode(in: InputStream): InputStream = d(in)
    }

  val GZip = create("gzip", out => new GZIPOutputStream(out), in => new GZIPInputStream(in))
  val Deflate = create("deflate", out => new DeflaterOutputStream(out), in => new InflaterInputStream(in))

  def forName(name: String): Option[ContentEncoding] = name.toLowerCase match {
    case "gzip" => Some(GZip)
    case "deflate" => Some(Deflate)
    case _ => None
  }
}

// - Request decoding --------------------------------------------------------------------------------------------------
// ---------------------------------------------------------------------------------------------------------------------
private class DecodedServletRequest(req: HttpServletRequest, enc: ContentEncoding) extends HttpServletRequestWrapper(req) {
  override lazy val getInputStream = {
    val raw = req.getInputStream
    new EncodedInputStream(enc.decode(raw), raw)
  }
  override lazy val getReader = new BufferedReader(new InputStreamReader(getInputStream, getCharacterEncoding))
  override def getContentLength: Int = -1
}

private class EncodedInputStream(encoded: InputStream, raw: ServletInputStream) extends ServletInputStream {
  override def isFinished: Boolean = raw.isFinished
  override def isReady: Boolean = raw.isReady
  override def setReadListener(readListener: ReadListener): Unit = raw.setReadListener(readListener)

  override def read(): Int = encoded.read()
  override def read(b: Array[Byte]): Int = read(b, 0, b.length)
  override def read(b: Array[Byte], off: Int, len: Int) = encoded.read(b, off, len)
}

// - Response encoding -------------------------------------------------------------------------------------------------
// ---------------------------------------------------------------------------------------------------------------------
/** Encodes any output written to a servlet response. */
private class EncodedServletResponse(res: HttpServletResponse, enc: ContentEncoding) extends HttpServletResponseWrapper(res) {
  // Object to flush when complete, if any.
  // Note that while this is essentially a mutable shared state, it's not really an issue here - or rather, if multiple
  // threads are accessing your output stream at the same time, you have other, more important issues to deal with.
  private var toFlush: Option[Flushable] = None

  override lazy val getOutputStream = {
    val raw = super.getOutputStream
    val out = new EncodedOutputStream(enc.encode(raw), raw)

    addHeader("Content-Encoding", enc.name)
    toFlush = Some(out)
    out
  }

  override lazy val getWriter: PrintWriter = {
    val writer = new PrintWriter(new OutputStreamWriter(getOutputStream, getCharset))
    toFlush = Some(writer)
    writer
  }

  /** Returns the charset with which to encode the response. */
  private def getCharset: Charset = (for {
    name <- Option(getCharacterEncoding)
    charset <- Try(Charset.forName(name)).toOption
  } yield charset).getOrElse {
    // The charset is either not known or not supported, defaults to ISO 8859 1, as per RFC and servlet documentation.
    setCharacterEncoding("ISO-8859-1")
    Charset.forName("ISO-8859-1")
  }

  /** Ensures that whatever byte- or char-stream we have open is properly flushed. */
  override def flushBuffer(): Unit = {
    toFlush.foreach(_.flush())
    super.flushBuffer()
  }

  // Encoded responses do not have a content length.
  override def setContentLength(i: Int) = {}
  override def setContentLengthLong(len: Long): Unit = {}
}

/** Wraps the specified raw and servlet output streams into one servlet output stream. */
private class EncodedOutputStream(out: OutputStream, orig: ServletOutputStream) extends ServletOutputStream {
  // - Raw writing -----------------------------------------------------------------------------------------------------
  // -------------------------------------------------------------------------------------------------------------------
  override def write(b: Int) = out.write(b)
  override def write(b: Array[Byte]) = write(b, 0, b.length)
  override def write(b: Array[Byte], off: Int, len: Int) = out.write(b, off, len)

  // - Cleanup ---------------------------------------------------------------------------------------------------------
  // -------------------------------------------------------------------------------------------------------------------
  override def flush() = out.flush()
  override def close() = out.close()

  // - ServletOutputStream  --------------------------------------------------------------------------------------------
  // -------------------------------------------------------------------------------------------------------------------
  override def setWriteListener(writeListener: WriteListener) = orig.setWriteListener(writeListener)
  override def isReady = orig.isReady
}