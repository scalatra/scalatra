package org.scalatra
package util
package io

import java.io.{FileInputStream, File}
import org.mozilla.universalchardet.UniversalDetector
import java.nio.charset.Charset
import io.Codec
import scala.io.Codec

object FileCharset {
  def apply(file: File) = {
    val buf = Array.ofDim[Byte](4096)
    val detector = new UniversalDetector(null)
    try {
      using(new FileInputStream(file)) { fis =>
        var idx = fis.read(buf)
        while(idx > 0 && !detector.isDone) {
          detector.handleData(buf, 0, idx)
          idx = fis.read(buf)
        }
        detector.dataEnd()
      }
      detector.getDetectedCharset.blankOption map Charset.forName getOrElse Codec.fileEncodingCodec.charSet
    } finally {
      detector.reset()
    }
  }
}
