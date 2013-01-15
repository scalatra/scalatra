package org.scalatra

import collection.JavaConverters._
import java.util.concurrent.ConcurrentHashMap
import org.scalatra.util.RicherString._
import java.util.Locale.ENGLISH
import scala.collection.concurrent

object ApiFormats {
  /**
   * The request attribute key in which the format is stored.
   */
  val FormatKey = "org.scalatra.FormatKey"

}

/**
 * Adds support for mapping and inferring formats to content types.
 *
 * $ - Provides a request-scoped format variable
 * $ - Maps formats to content types and vice versa
 * $ - Augments the content-type inferrer to use the format
 */
trait ApiFormats extends ScalatraSyntax {
  /**
   * A map of suffixes to content types.
   */
  val formats: concurrent.Map[String, String] = new ConcurrentHashMap[String, String](Map(
    "atom" -> "application/atom+xml",
    "css" -> "text/stylesheet",
    "flv" -> "video/x-flv",
    "html" -> "text/html",
    "html5" -> "text/html",
    "js" -> "text/javascript",
    "json" -> "application/json",
    "pdf" -> "application/pdf",
    "rss" -> "application/rss+xml",
    "svg" -> "application/svg+xml",
    "swf" -> "application/x-shockwave-flash",
    "txt" -> "text/plain",
    "xhtml" -> "application/xhtml+xml",
    "xml" -> "application/xml",
    "xslt" -> "application/xslt+xml"
  ).asJava).asScala

  /**
   * A map of content types to suffixes.  Not strictly a reverse of `formats`.
   */
  val mimeTypes: concurrent.Map[String, String] = new ConcurrentHashMap[String, String](Map(
    "application/atom+xml" -> "atom",
    "application/ecmascript" -> "json",
    "application/javascript" -> "json",
    "application/json" -> "json",
    "application/pdf" -> "pdf",
    "application/rss+xml" -> "rss",
    "application/svg+xml" -> "svg",
    "application/x-ecmascript" -> "json",
    "application/x-shockwave-flash" -> "swf",
    "application/x-www-form-urlencoded" -> "html",
    "application/xhtml+xml" -> "html",
    "application/xml" -> "xml",
    "application/xslt+xml" -> "xslt",
    "multipart/form-data" -> "html",
    "text/html" -> "html",
    "text/javascript" -> "json",
    "text/plain" -> "txt",
    "text/stylesheet" -> "css",
    "video/x-flv" -> "flv"
  ).asJava).asScala

  /**
   * The default format.
   */
  def defaultFormat: Symbol = 'html

  /**
   * A list of formats accepted by default.
   */
  def defaultAcceptedFormats: List[Symbol] = List.empty

  def responseFormat: String = {
    response.contentType flatMap ( ctt => ctt.split(";").headOption map mimeTypes.apply) getOrElse format
  }

  /**
   * The list of media types accepted by the current request.  Parsed from the
   * `Accept` header.
   */
  def acceptHeader: List[String] = parseAcceptHeader

  private def getFromParams = {
    params.get('format).find(p ⇒ formats.contains(p.toLowerCase(ENGLISH)))
  }

  private def getFromAcceptHeader = {
    val hdrs = request.contentType map { contentType =>
      (acceptHeader ::: List(contentType)).distinct 
    } getOrElse acceptHeader
    formatForMimeTypes(hdrs: _*)
  }

  private def parseAcceptHeader = {
    request.headers.get("Accept") map { s =>
      val fmts = s.split(",").map(_.trim)
      val accepted = (fmts.foldLeft(Map.empty[Int, List[String]]) { (acc, f) =>
        val parts = f.split(";").map(_.trim)
        val i = if (parts.size > 1) {
          val pars = parts(1).split("=").map(_.trim).grouped(2).map(a => a(0) -> a(1)).toSeq
          val a = Map(pars:_*)
          (a.get("q").map(_.toDouble).getOrElse(1.0) * 10).ceil.toInt
        } else 10
        acc + (i -> (parts(0) :: acc.get(i).getOrElse(List.empty)))
      })
      (accepted.toList sortWith ((kv1, kv2) => kv1._1 > kv2._1) flatMap (_._2.reverse) toList)
    } getOrElse Nil
  }

  protected def formatForMimeTypes(mimeTypes: String*): Option[String] = {
    val defaultMimeType = formats(defaultFormat.name)
    def matchMimeType(tm: String, f: String) = {
      tm.toLowerCase(ENGLISH).startsWith(f) || (defaultMimeType == f && tm.contains(defaultMimeType))
    }
    mimeTypes find { hdr =>
      formats exists { case (k, v) => matchMimeType(hdr, v) }
    } flatMap { hdr =>
      formats find { case (k, v) => matchMimeType(hdr, v) } map { _._1 }
    }
  }

  /**
   * A content type inferrer based on the `format` variable.  Looks up the media
   * type from the `formats` map.  If not found, returns
   * `application/octet-stream`.  This inferrer is prepended to the inherited
   * one.
   */
  protected def inferFromFormats: ContentTypeInferrer = {
    case _ if responseFormat.nonBlank => formats.get(responseFormat) getOrElse "application/octet-stream"
  }

  override protected def contentTypeInferrer: ContentTypeInferrer = inferFromFormats orElse super.contentTypeInferrer

  protected def acceptedFormats(accepted: Symbol*) = {
    val conditions = if (accepted.isEmpty) defaultAcceptedFormats else accepted.toList
    conditions.isEmpty || (conditions filter { s => formats.get(s.name).isDefined } contains contentType)
  }

  private def getFormat = getFromParams orElse getFromAcceptHeader getOrElse defaultFormat.name

  import ApiFormats.FormatKey

  /**
   * Returns the request-scoped format.  If not explicitly set, the format is:
   * $ - the `format` request parameter, if present in `formatParams`
   * $ - the first match from `Accept` header, looked up in `mimeTypes`
   * $ - the format from the `Content-Type` header, as looked up in `mimeTypes`
   * $ - the default format
   */
  def format = {
    request.get(FormatKey).map(_.asInstanceOf[String]) getOrElse {
      val fmt = getFormat
      request(FormatKey) = fmt
      fmt
    }
  }

  /**
   * Explicitly sets the request-scoped format.  This takes precedence over
   * whatever was inferred from the request.
   */
  def format_=(formatValue: Symbol) {
    request(FormatKey) = formatValue.name
  }

  /**
   * Explicitly sets the request-scoped format.  This takes precedence over
   * whatever was inferred from the request.
   */
  def format_=(formatValue: String) {
    request(FormatKey) = formatValue
  }
}
