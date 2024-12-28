package org.scalatra.util.io

import java.io.File
import java.nio.charset.Charset

import org.mozilla.universalchardet.UniversalDetector
import org.slf4j.LoggerFactory

import scala.io.Codec

object FileCharset {

  @transient private[this] val logger = LoggerFactory.getLogger(getClass)

  private val CheckByteLength = 8192

  def apply(file: File): Charset =
    try
      getCharset(UniversalDetector.detectCharset(file))
    catch {
      case t: Throwable =>
        logger.warn(
          "Failed to detect charset for file: " + file.getPath + ".",
          t
        )
        Codec.defaultCharsetCodec.charSet
    }

  private[this] def getCharset(cs: String): Charset =
    // US-ASCII is compatible with UTF-8, so if the result is US-ASCII, replace it with UTF-8.
    if (cs == "US-ASCII" || cs == null || cs.trim().isEmpty) {

      // Codec.fileEncodingCodec points to UTF-8
      // unless explicitly specified in the form `JAVA_OPTS="-Dfile.encoding=Foo"`.
      Codec.fileEncodingCodec.charSet
    } else {
      Charset.forName(cs)
    }

  def apply(barr: Array[Byte]): Charset = {
    val detector = new UniversalDetector(null)

    var idx = 0
    while (idx < barr.length && idx < CheckByteLength && !detector.isDone) {
      if (idx > 0) detector.handleData(barr, 0, idx)
      idx += 1
    }
    detector.dataEnd()

    getCharset(detector.getDetectedCharset)
  }
}
