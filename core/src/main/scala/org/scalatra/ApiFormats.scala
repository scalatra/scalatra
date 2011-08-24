package org.scalatra

import collection.JavaConversions._
import collection.mutable.ConcurrentMap
import java.util.concurrent.ConcurrentHashMap
import org.scalatra.util.RicherString._
import java.util.Locale.ENGLISH


object ApiFormats {
  val FormatKey = "org.scalatra.FormatKey".intern
}
trait ApiFormats extends ScalatraKernel {
  val formats: ConcurrentMap[String, String] = new ConcurrentHashMap[String, String](Map(
    "json" -> "application/json",
    "xml" -> "application/xml",
    "atom" -> "application/atom+xml",
    "rss" -> "application/rss+xml",
    "xslt" -> "application/xslt+xml",
    "svg" -> "application/svg+xml",
    "pdf" -> "application/pdf",
    "swf" -> "application/x-shockwave-flash",
    "flv" -> "video/x-flv",
    "js" -> "text/javascript",
    "css" -> "text/stylesheet",
    "txt" -> "text/plain",
    "html" -> "text/html",
    "html5" -> "text/html",
    "xhtml" -> "application/xhtml+xml"))

  val mimeTypes: ConcurrentMap[String, String] = new ConcurrentHashMap[String, String](Map(
    "application/json" -> "json",
    "application/xml" -> "xml",
    "application/atom+xml" -> "atom",
    "application/rss+xml" -> "rss",
    "application/xslt+xml" -> "xslt",
    "application/pdf" -> "pdf",
    "application/x-www-form-urlencoded" -> "html",
    "multipart/form-data" -> "html",
    "application/svg+xml" -> "svg",
    "application/x-shockwave-flash" -> "swf",
    "video/x-flv" -> "flv",
    "text/javascript" -> "json",
    "application/javascript" -> "json",
    "application/ecmascript" -> "json",
    "application/x-ecmascript" -> "json",
    "text/stylesheet" -> "css",
    "text/html" -> "html",
    "application/xhtml+xml" -> "html"))

  def defaultFormat = 'html
  def defaultAcceptedFormats: List[Symbol] = List.empty

  def acceptHeader = {
    val s = request.getHeader("Accept")
    if (s == null || s.isEmpty) List[String]() else s.split(";").map(_.trim).toList
  }

  private def getFromParams = {
    params.get('format).find(p ⇒ formats.contains(p.toLowerCase(ENGLISH)))
  }

  private def getFromAcceptHeader = {
    val hdrs = if (request.getContentType != null) (acceptHeader ::: List(request.getContentType)).distinct else acceptHeader
    formatForMimeTypes(hdrs: _*)
  }

  protected def formatForMimeTypes(mimeTypes: String*) = {
    val defaultMimeType = formats(defaultFormat.name)
    def matchMimeType(tm: String, f: String) = {
      tm.toLowerCase(ENGLISH).startsWith(f) || (defaultMimeType == f && tm.contains(defaultMimeType))
    }
    mimeTypes find { hdr ⇒
      formats exists { case (k, v) ⇒ matchMimeType(hdr, v) }
    } flatMap { hdr ⇒
      formats find { case (k, v) ⇒ matchMimeType(hdr, v) } map { _._1 }
    }
  }

  protected def inferFromFormats: ContentTypeInferrer = {
    case _ if format.isNonBlank ⇒ formats.get(format) getOrElse "application/octetstream"
  }

  override protected def contentTypeInferrer: ContentTypeInferrer = inferFromFormats orElse super.contentTypeInferrer

  protected def acceptedFormats(accepted: Symbol*) = {
    val conditions = if (accepted.isEmpty) defaultAcceptedFormats else accepted.toList
    conditions.isEmpty || (conditions filter { s ⇒ formats.get(s.name).isDefined } contains contentType)
  }

  private def getFormat = getFromParams orElse getFromAcceptHeader getOrElse defaultFormat.name

  import ApiFormats.FormatKey
  def format = {
    request.get(FormatKey).map(_.asInstanceOf[String]) getOrElse {
      val fmt = getFormat
      request(FormatKey) = fmt
      fmt
    }
  }
  def format_=(formatValue: Symbol) {
    request(FormatKey) = formatValue.name
  }
  def format_=(formatValue: String) {
    request(FormatKey) = formatValue
  }
}
