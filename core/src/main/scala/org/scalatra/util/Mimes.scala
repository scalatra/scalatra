package org.scalatra.util

import eu.medsea.util.EncodingGuesser
import eu.medsea.mimeutil.{ MimeType, MimeUtil2 }
import collection.JavaConversions._
import java.io.{ InputStream, File }
import java.net.{ URI, URL }
import grizzled.slf4j.Logger
import util.control.Exception._

object Mimes {

  val DefaultMime = "application/octet-stream"
  /**
   * Sets supported encodings for the mime-util library if they have not been
   * set. Since the supported encodings is stored as a static Set we
   * synchronize access.
   */
  private def registerEncodingsIfNotSet() {
    synchronized {
      if (EncodingGuesser.getSupportedEncodings.isEmpty) {
        val enc = Set("UTF-8", "ISO-8859-1", "windows-1252", "MacRoman", EncodingGuesser.getDefaultEncoding)
        EncodingGuesser.setSupportedEncodings(enc)
      }
    }
  }
  registerEncodingsIfNotSet
}

/**
 * A utility to help with mime type detection for a given file path or url
 */
trait Mimes {

  @transient private[this] val internalLogger = Logger(getClass)
  import Mimes._

  protected[this] def mimeUtil: MimeUtil2 = new MimeUtil2()
  quiet { mimeUtil.registerMimeDetector("eu.medsea.mimeutil.detector.MagicMimeMimeDetector") }
  quiet { mimeUtil.registerMimeDetector("eu.medsea.mimeutil.detector.ExtensionMimeDetector") }

  def bytesMime(content: Array[Byte], fallback: String = DefaultMime) = {
    detectMime(fallback) {
      MimeUtil2.getMostSpecificMimeType(mimeUtil.getMimeTypes(content, new MimeType(fallback))).toString
    }
  }
  def fileMime(file: File, fallback: String = DefaultMime) = {
    detectMime(fallback) {
      MimeUtil2.getMostSpecificMimeType(mimeUtil.getMimeTypes(file, new MimeType(fallback))).toString
    }
  }
  def inputStreamMime(input: InputStream, fallback: String = DefaultMime) = {
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
  def mimeType(path: String, fallback: String = DefaultMime) = {
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
  def urlMime(url: String, fallback: String = DefaultMime) = {
    detectMime(fallback) {
      MimeUtil2.getMostSpecificMimeType(mimeUtil.getMimeTypes(new URL(url), new MimeType(fallback))).toString
    }
  }

  private def detectMime(fallback: String = DefaultMime)(mimeDetect: ⇒ String) = {
    def errorHandler(t: Throwable) = {
      internalLogger.warn("There was an error detecting the mime type. ", t)
      fallback
    }
    allCatch.withApply(errorHandler)(mimeDetect)
  }

  def isTextMime(mime: String) = MimeUtil2.isTextMimeType(new MimeType(mime))

  private def quiet(fn: ⇒ Unit) =
    allCatch.withApply(internalLogger.warn("An error occurred while registering a mime type detector.", _))(fn)

  def apply(input: InputStream) = inputStreamMime(input)
  def apply(file: File) = fileMime(file)
  def apply(bytes: Array[Byte]) = bytesMime(bytes)
  def apply(uri: URI) = urlMime(uri.toASCIIString)
}

object MimeTypes extends Mimes