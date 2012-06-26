package org.scalatra
package util
package io

import eu.medsea.util.EncodingGuesser
import eu.medsea.mimeutil.{ MimeType, MimeUtil2 }
import collection.JavaConversions._
import java.io.{ InputStream, File }
import java.net.{URI, URL}

object Mimes {
  /**
   * Sets supported encodings for the mime-util library if they have not been
   * set. Since the supported encodings is stored as a static Set we
   * synchronize access.
   */
  private def registerEncodingsIfNotSet: Unit = synchronized {
    if (EncodingGuesser.getSupportedEncodings.isEmpty) {
      val enc = Set("UTF-8", "ISO-8859-1", "windows-1252", "MacRoman", EncodingGuesser.getDefaultEncoding)
      EncodingGuesser.setSupportedEncodings(enc)
    }
  }
  registerEncodingsIfNotSet
}

/**
 * A utility to help with mime type detection for a given file path or url
 */
trait Mimes {

  lazy val DEFAULT_MIME = "application/octet-stream"

  private val mimeUtil = new MimeUtil2()
  quiet { mimeUtil.registerMimeDetector("eu.medsea.mimeutil.detector.MagicMimeMimeDetector") }
  quiet { mimeUtil.registerMimeDetector("eu.medsea.mimeutil.detector.ExtensionMimeDetector") }



  def bytesMime(content: Array[Byte], fallback: String = DEFAULT_MIME) = {
    detectMime(fallback) {
      MimeUtil2.getMostSpecificMimeType(mimeUtil.getMimeTypes(content, new MimeType(fallback))).toString
    }
  }
  def fileMime(file: File, fallback: String = DEFAULT_MIME) = {
    detectMime(fallback) {
      MimeUtil2.getMostSpecificMimeType(mimeUtil.getMimeTypes(file, new MimeType(fallback))).toString
    }
  }
  def inputStreamMime(input: InputStream, fallback: String = DEFAULT_MIME) = {
    detectMime(fallback) {
      MimeUtil2.getMostSpecificMimeType(mimeUtil.getMimeTypes(input, new MimeType(fallback))).toString
    }
  }

  /**
   * Detects the mime type of a given file path.
   *
   * @param path The path for which to detect the mime type
   * @param fallback A fallback value in case no mime type can be found
   */
  def mimeType(path: String, fallback: String = DEFAULT_MIME) = {
    detectMime(fallback) {
      MimeUtil2.getMostSpecificMimeType(mimeUtil.getMimeTypes(path, new MimeType(fallback))).toString
    }
  }

  /**
   * Detects the mime type of a given url.
   *
   * @param url The url for which to detect the mime type
   * @param fallback A fallback value in case no mime type can be found
   */
  def urlMime(url: String, fallback: String = DEFAULT_MIME) = {
    detectMime(fallback) {
      MimeUtil2.getMostSpecificMimeType(mimeUtil.getMimeTypes(new URL(url), new MimeType(fallback))).toString
    }
  }

  private def detectMime(fallback: String = DEFAULT_MIME)(mimeDetect: ⇒ String) = {
    try {
      mimeDetect
    } catch {
      case e ⇒ {
        warn("There was an error detecting the mime type. " + e.getMessage)
        fallback
      }
    }
  }

  private def quiet(fn: ⇒ Unit) = {
    try { fn }
    catch { case e ⇒ warn("An error occurred while registering a mime type detector. " + e.getMessage) }
  }

  protected def warn(message: String)

  def apply(input: InputStream) = inputStreamMime(input)
  def apply(file: File) = fileMime(file)
  def apply(bytes: Array[Byte]) = bytesMime(bytes)
  def apply(uri: URI) = urlMime(uri.toASCIIString)
}

class MimeTypes(implicit val appContext: AppContext) extends ScalatraLogging with Mimes {

  protected def warn(message: String) = logger warn message
}
