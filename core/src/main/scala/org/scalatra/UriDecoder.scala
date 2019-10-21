package org.scalatra

import java.nio.charset.StandardCharsets

import util.UrlCodingUtils

object UriDecoder {

  private val pathReservedCharsSet = PathPatternParser.PathReservedCharacters.toSet[Char].map(_.toInt)

  def firstStep(uri: String): String = {
    UrlCodingUtils.urlDecode(
      toDecode = UrlCodingUtils.ensureUrlEncoding(uri),
      charset = StandardCharsets.UTF_8,
      plusIsSpace = false,
      skip = pathReservedCharsSet)
  }

}
