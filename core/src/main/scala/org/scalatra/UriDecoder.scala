package org.scalatra

import java.nio.charset.StandardCharsets

import util.UrlCodingUtils

object UriDecoder {

  def decode(uri: String): String =
    UrlCodingUtils.urlDecode(
      toDecode = UrlCodingUtils.ensureUrlEncoding(uri),
      charset = StandardCharsets.UTF_8,
      plusIsSpace = false
    )

}
