package org.scalatra.util

import java.io.{File, InputStream, ByteArrayInputStream}
import java.net.{URI, URLConnection}

object Mimes {
  val DefaultMime = "application/octet-stream"
}

/** A utility to help with mime type detection for a given file path or url.
  */

trait Mimes {

  import org.scalatra.util.Mimes.*

  /** Detects the MIME type of a given Byte array.
    *
    * When appropriate MIME Type can not be inferred, "text/pain" is returned.
    *
    * @param content
    *   The Byte array for which to detect the MIME type
    * @param fallback
    *   A fallback value in case no MIME type can be found
    */
  def bytesMime(content: Array[Byte], fallback: String = DefaultMime): String = {
    // for backward compatibility...even when it is empty, "text/plain" may be good.
    if (content.isEmpty) {
      fallback
    } else {
      val is       = new ByteArrayInputStream(content)
      val mimeType = URLConnection.guessContentTypeFromStream(is)

      if (mimeType != null) mimeType else "text/plain"
    }
  }

  /** Detects the MIME type of a given File.
    *
    * When appropriate MIME Type can not be inferred, "application/octet-stream" is returned.
    *
    * This method guesses the MIME type using `java.net.URLConnection.guessContentTypeFromName`. Therefore, by defining
    * an arbitrary MIME type in the configuration file specified by the `content.types.user.table` property, an
    * arbitrary MIME type can be guessed.
    *
    * @param file
    *   The File for which to detect the MIME type
    * @param fallback
    *   A fallback value in case no MIME type can be found
    */
  def fileMime(file: File, fallback: String = DefaultMime): String = {
    val mimeType = URLConnection.guessContentTypeFromName(file.getName)

    if (mimeType != null) mimeType else fallback
  }

  /** Detects the MIME type of a given InputStream.
    *
    * When appropriate MIME Type can not be inferred, "application/octet-stream" is returned.
    *
    * @param input
    *   The InputStream for which to detect the MIME type
    * @param fallback
    *   A fallback value in case no MIME type can be found
    */
  def inputStreamMime(input: InputStream, fallback: String = DefaultMime): String = {
    val mimeType = URLConnection.guessContentTypeFromStream(input)

    if (mimeType != null) mimeType else fallback
  }

  /** Detects the MIME type of a given url.
    *
    * When inappropriate MIME Type can not be inferred, "application/octet-stream" is returned.
    *
    * This method guesses the MIME type using `java.net.URLConnection.guessContentTypeFromName`. Therefore, by defining
    * an arbitrary MIME type in the configuration file specified by the `content.types.user.table` property, an
    * arbitrary MIME type can be guessed.
    *
    * @param url
    *   The url for which to detect the mime type
    * @param fallback
    *   A fallback value in case no mime type can be found
    */
  def urlMime(url: String, fallback: String = DefaultMime): String = {
    val mimeType = URLConnection.guessContentTypeFromName(url)

    if (mimeType != null) mimeType else fallback
  }

  def apply(input: InputStream) = inputStreamMime(input)
  def apply(file: File)         = fileMime(file)
  def apply(bytes: Array[Byte]) = bytesMime(bytes)
  def apply(uri: URI)           = urlMime(uri.toASCIIString)
}

object MimeTypes extends Mimes
