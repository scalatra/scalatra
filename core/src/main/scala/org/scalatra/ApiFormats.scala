package org.scalatra

import java.util.Locale.ENGLISH
import java.util.concurrent.ConcurrentHashMap
import javax.servlet.http.{ HttpServletRequest, HttpServletResponse }

import org.scalatra.util.RicherString._

import scala.collection.JavaConverters._
import scala.collection.concurrent

object ApiFormats {

  /**
   * The request attribute key in which the format is stored.
   */
  val FormatKey: String = "org.scalatra.FormatKey"

}

/**
 * Adds support for mapping and inferring formats to content types.
 *
 * $ - Provides a request-scoped format variable
 * $ - Maps formats to content types and vice versa
 * $ - Augments the content-type inferrer to use the format
 */
trait ApiFormats extends ScalatraBase {

  import org.scalatra.ApiFormats.FormatKey

  /**
   * A map of suffixes to content types.
   */
  val formats: concurrent.Map[String, String] = new ConcurrentHashMap[String, String](Map(
    "atom" -> "application/atom+xml",
    "css" -> "text/css",
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
    "text/css" -> "css",
    "video/x-flv" -> "flv"
  ).asJava).asScala

  protected def addMimeMapping(mime: String, extension: String) {
    mimeTypes += mime -> extension
    formats += extension -> mime
  }

  /**
   * The default format.
   */
  def defaultFormat: Symbol = 'html

  /**
   * A list of formats accepted by default.
   */
  def defaultAcceptedFormats: List[Symbol] = List.empty

  @deprecated("`format` now means the same as `responseFormat`, `responseFormat` will be removed eventually", "2.3")
  def responseFormat(implicit request: HttpServletRequest, response: HttpServletResponse): String = format

  /**
   * The list of media types accepted by the current request.  Parsed from the
   * `Accept` header.
   */
  def acceptHeader(implicit request: HttpServletRequest): List[String] = parseAcceptHeader

  private[this] def getFromParams(implicit request: HttpServletRequest): Option[String] = {
    params.get("format").find(p ⇒ formats.contains(p.toLowerCase(ENGLISH)))
  }

  private[this] def getFromAcceptHeader(implicit request: HttpServletRequest): Option[String] = {
    val hdrs = request.contentType.fold(acceptHeader)(contentType =>
      (acceptHeader ::: List(contentType)).distinct
    )
    formatForMimeTypes(hdrs: _*)
  }

  private[this] def getFromResponseHeader(implicit response: HttpServletResponse): Option[String] = {
    response.contentType flatMap (ctt => ctt.split(";").headOption flatMap mimeTypes.get)
  }

  private def parseAcceptHeader(implicit request: HttpServletRequest): List[String] = {
    def isValidQPair(a: Array[String]) = {
      val validRange = Range.Double.inclusive(0, 1, 0.1)
      a.length == 2 && a(0) == "q" && validRange.contains(a(1).toDouble)
    }

    request.headers.get("Accept") map { s =>
      val fmts = s.split(",").map(_.trim)
      val accepted = fmts.foldLeft(Map.empty[Int, List[String]]) { (acc, f) =>
        val parts = f.split(";").map(_.trim)
        val i = if (parts.size > 1) {
          val pars = parts(1).split("=").map(_.trim).grouped(2).find(isValidQPair).getOrElse(Array("q", "0"))
          (pars(1).toDouble * 10).ceil.toInt
        } else 10
        acc + (i -> (parts(0) :: acc.get(i).getOrElse(List.empty)))
      }
      accepted.toList.sortWith((kv1, kv2) => kv1._1 > kv2._1).flatMap(_._2.reverse)
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
    case _ if format.nonBlank => formats.get(format) getOrElse "application/octet-stream"
  }

  override protected def contentTypeInferrer: ContentTypeInferrer = {
    inferFromFormats orElse super.contentTypeInferrer
  }

  protected def acceptedFormats(accepted: Symbol*): Boolean = {
    val conditions: List[String] = {
      if (accepted.isEmpty) defaultAcceptedFormats.map(_.name)
      else accepted.map(_.name).toList
    }
    conditions.isEmpty || (conditions filter { s => formats.get(s).isDefined } contains contentType)
  }

  private def getFormat(implicit request: HttpServletRequest, response: HttpServletResponse): String = {
    getFromResponseHeader orElse getFromParams orElse getFromAcceptHeader getOrElse defaultFormat.name
  }

  protected override def withRouteMultiParams[S](matchedRoute: Option[MatchedRoute])(thunk: => S): S = {
    val originalParams: MultiParams = multiParams
    val routeParams: Map[String, Seq[String]] = {
      matchedRoute.map(_.multiParams).getOrElse(Map.empty).map {
        case (key, values) =>
          key -> values.map(s => if (s.nonBlank) UriDecoder.secondStep(s) else s)
      }
    }
    if (routeParams.contains("format")) {
      request(FormatKey) = routeParams.apply("format").head
    }
    request(MultiParamsKey) = originalParams ++ routeParams

    try {
      thunk
    } finally {
      request(MultiParamsKey) = originalParams
    }
  }

  def requestFormat(implicit request: HttpServletRequest): String = {
    request.contentType
      .flatMap(t => t.split(";").headOption flatMap mimeTypes.get)
      .getOrElse(format)
  }

  /**
   * Returns the request-scoped format.  If not explicitly set, the format is:
   * $ - the `format` request parameter, if present in `formatParams`
   * $ - the first match from `Accept` header, looked up in `mimeTypes`
   * $ - the format from the `Content-Type` header, as looked up in `mimeTypes`
   * $ - the default format
   */
  def format(implicit request: HttpServletRequest, response: HttpServletResponse): String = {
    request.get(FormatKey).fold({
      val fmt = getFormat
      request(FormatKey) = fmt
      fmt
    })(_.asInstanceOf[String])
  }

}
