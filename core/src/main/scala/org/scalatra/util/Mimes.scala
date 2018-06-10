package org.scalatra.util

import java.io.{ File, InputStream }
import java.net.{ URI, URL }

import eu.medsea.mimeutil.{ MimeType, MimeUtil2 }
import eu.medsea.util.EncodingGuesser
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._
import scala.util.control.Exception._

object Mimes {

  val DefaultMime = "application/octet-stream"
  /**
   * Sets supported encodings for the mime-util library if they have not been
   * set. Since the supported encodings is stored as a static Set we
   * synchronize access.
   */
  private def registerEncodingsIfNotSet(): Unit = {
    synchronized {
      if (EncodingGuesser.getSupportedEncodings.isEmpty) {
        val enc = Set("UTF-8", "ISO-8859-1", "windows-1252", "MacRoman", EncodingGuesser.getDefaultEncoding)
        EncodingGuesser.setSupportedEncodings(enc.asJava)
      }
    }
  }
  registerEncodingsIfNotSet
}

/**
 * A utility to help with mime type detection for a given file path or url
 */
trait Mimes {

  import org.scalatra.util.Mimes._

  @transient private[this] val internalLogger = LoggerFactory.getLogger(getClass)

  protected[this] def mimeUtil: MimeUtil2 = new MimeUtil2()
  quiet { mimeUtil.registerMimeDetector("eu.medsea.mimeutil.detector.MagicMimeMimeDetector") }
  quiet { mimeUtil.registerMimeDetector("eu.medsea.mimeutil.detector.ExtensionMimeDetector") }

  def bytesMime(content: Array[Byte], fallback: String = DefaultMime): String = {
    detectMime(fallback) {
      MimeUtil2.getMostSpecificMimeType(mimeUtil.getMimeTypes(content, new MimeType(fallback))).toString
    }
  }
  def fileMime(file: File, fallback: String = DefaultMime): String = {
    detectMime(fallback) {
      MimeUtil2.getMostSpecificMimeType(mimeUtil.getMimeTypes(file, new MimeType(fallback))).toString
    }
  }
  def inputStreamMime(input: InputStream, fallback: String = DefaultMime): String = {
    detectMime(fallback) {
      MimeUtil2.getMostSpecificMimeType(mimeUtil.getMimeTypes(input, new MimeType(fallback))).toString
    }
  }

  /**
   * Detects the mime type of a given url.
   *
   * @param url The url for which to detect the mime type
   * @param fallback A fallback value in case no mime type can be found
   */
  def urlMime(url: String, fallback: String = DefaultMime): String = {
    detectMime(fallback) {
      MimeUtil2.getMostSpecificMimeType(
        mimeUtil.getMimeTypes(new URL(url), new MimeType(fallback))).toString
    }
  }

  private def detectMime(fallback: String = DefaultMime)(mimeDetect: => String): String = {
    def errorHandler(t: Throwable) = {
      internalLogger.warn("There was an error detecting the mime type. ", t)
      fallback
    }
    allCatch.withApply(errorHandler)(mimeDetect)
  }

  def isTextMime(mime: String): Boolean = MimeUtil2.isTextMimeType(new MimeType(mime))

  private def quiet(fn: => Unit): Unit = {
    allCatch.withApply(
      internalLogger.warn("An error occurred while registering a mime type detector.", _))(fn)
  }

  def apply(input: InputStream) = inputStreamMime(input)
  def apply(file: File) = fileMime(file)
  def apply(bytes: Array[Byte]) = bytesMime(bytes)
  def apply(uri: URI) = urlMime(uri.toASCIIString)
}

object MimeTypes extends Mimes
