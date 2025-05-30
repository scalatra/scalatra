package org.scalatra

import java.util.Locale.ENGLISH
import org.scalatra.ServletCompat.http.{HttpServletRequest, HttpServletResponse}

import org.scalatra.util.RicherString.*
import org.slf4j.LoggerFactory

import scala.jdk.CollectionConverters.*

object CorsSupport {

  val OriginHeader: String                        = "Origin"
  val AccessControlRequestMethodHeader: String    = "Access-Control-Request-Method"
  val AccessControlRequestHeadersHeader: String   = "Access-Control-Request-Headers"
  val AccessControlAllowOriginHeader: String      = "Access-Control-Allow-Origin"
  val AccessControlAllowMethodsHeader: String     = "Access-Control-Allow-Methods"
  val AccessControlAllowHeadersHeader: String     = "Access-Control-Allow-Headers"
  val AccessControlMaxAgeHeader: String           = "Access-Control-Max-Age"
  val AccessControlAllowCredentialsHeader: String = "Access-Control-Allow-Credentials"

  private val AnyOrigin: String          = "*"
  private val SimpleHeaders: Seq[String] =
    List(OriginHeader.toUpperCase(ENGLISH), "ACCEPT", "ACCEPT-LANGUAGE", "CONTENT-LANGUAGE")
  private val SimpleContentTypes: Seq[String] =
    List("APPLICATION/X-WWW-FORM-URLENCODED", "MULTIPART/FORM-DATA", "TEXT/PLAIN")

  val CorsHeaders: Seq[String] = List(
    OriginHeader,
    AccessControlAllowCredentialsHeader,
    AccessControlAllowHeadersHeader,
    AccessControlAllowMethodsHeader,
    AccessControlAllowOriginHeader,
    AccessControlMaxAgeHeader,
    AccessControlRequestHeadersHeader,
    AccessControlRequestMethodHeader
  )

  case class CORSConfig(
      allowedOrigins: Seq[String],
      allowedMethods: Seq[String],
      allowedHeaders: Seq[String],
      allowCredentials: Boolean,
      preflightMaxAge: Int = 0,
      enabled: Boolean
  )

  private[this] def configKey(name: String): String = "org.scalatra.cors." + name

  val AllowedOriginsKey: String   = configKey("allowedOrigins")
  val AllowedMethodsKey: String   = configKey("allowedMethods")
  val AllowedHeadersKey: String   = configKey("allowedHeaders")
  val AllowCredentialsKey: String = configKey("allowCredentials")
  val PreflightMaxAgeKey: String  = configKey("preflightMaxAge")
  val EnableKey: String           = configKey("enable")
  val CorsConfigKey: String       = configKey("corsConfig")

  private val DefaultMethods: String = "GET,POST,PUT,DELETE,HEAD,OPTIONS,PATCH"

  private val DefaultHeaders: String = Seq(
    "Cookie",
    "Host",
    "X-Forwarded-For",
    "Accept-Charset",
    "If-Modified-Since",
    "Accept-Language",
    "X-Forwarded-Port",
    "Connection",
    "X-Forwarded-Proto",
    "User-Agent",
    "Referer",
    "Accept-Encoding",
    "X-Requested-With",
    "Authorization",
    "Accept",
    "Content-Type"
  ).mkString(",")

}
trait CorsSupport extends Handler with Initializable { self: ScalatraBase =>

  import org.scalatra.CorsSupport.*

  private[this] lazy val logger = LoggerFactory.getLogger(getClass)

  abstract override def initialize(config: ConfigT): Unit = {
    super.initialize(config)
    def createDefault: CORSConfig = CORSConfig(
      Option(config.context.getInitParameter(AllowedOriginsKey))
        .getOrElse(AnyOrigin)
        .split(",")
        .toIndexedSeq
        .map(_.trim),
      Option(config.context.getInitParameter(AllowedMethodsKey))
        .getOrElse(DefaultMethods)
        .split(",")
        .toIndexedSeq
        .map(_.trim),
      Option(config.context.getInitParameter(AllowedHeadersKey))
        .getOrElse(DefaultHeaders)
        .split(",")
        .toIndexedSeq
        .map(_.trim),
      Option(config.context.getInitParameter(AllowCredentialsKey)).map(_.toBoolean).getOrElse(true),
      Option(config.context.getInitParameter(PreflightMaxAgeKey)).map(_.toInt).getOrElse(1800),
      Option(config.context.getInitParameter(EnableKey)).map(_.toBoolean).getOrElse(true)
    )

    val corsCfg = config.context.getOrElseUpdate(CorsConfigKey, createDefault).asInstanceOf[CORSConfig]
    import corsCfg.*
    if (enabled) {
      logger debug "Enabled CORS Support with:\nallowedOrigins:\n\t%s\nallowedMethods:\n\t%s\nallowedHeaders:\n\t%s"
        .format(allowedOrigins mkString ", ", allowedMethods mkString ", ", allowedHeaders mkString ", ")
    } else {
      logger debug "Cors support is disabled"
    }
  }

  protected def handlePreflightRequest(): Unit = {
    logger debug "handling preflight request"
    // 5.2.7
    augmentSimpleRequest()
    // 5.2.8
    if (corsConfig.preflightMaxAge > 0)
      response.headers(AccessControlMaxAgeHeader) = corsConfig.preflightMaxAge.toString
    // 5.2.9
    response.headers(AccessControlAllowMethodsHeader) = corsConfig.allowedMethods mkString ","
    // 5.2.10
    val rh =
      corsConfig.allowedHeaders ++ request.getHeaders(AccessControlRequestHeadersHeader).asScala.flatMap(_ split (","))
    response.headers(AccessControlAllowHeadersHeader) = rh mkString ","
    response.end()

  }

  protected def augmentSimpleRequest(): Unit = {
    val anyOriginAllowed: Boolean = corsConfig.allowedOrigins.contains(AnyOrigin)
    val hdr                       =
      if (anyOriginAllowed && !corsConfig.allowCredentials)
        Some(AnyOrigin)
      else
        request.headers.get(OriginHeader).filter(corsConfig.allowedOrigins.contains)

    hdr.foreach(value => response.headers(AccessControlAllowOriginHeader) = value)
    if (corsConfig.allowCredentials) response.headers(AccessControlAllowCredentialsHeader) = "true"
    response.setHeader(AccessControlAllowHeadersHeader, request.getHeader(AccessControlRequestHeadersHeader))
  }

  private[this] def corsConfig: CORSConfig = {
    servletContext.get(CorsConfigKey).orNull.asInstanceOf[CORSConfig]
  }

  private[this] def originMatches: Boolean = // 6.2.2
    corsConfig.allowedOrigins.contains(AnyOrigin) ||
      (corsConfig.allowedOrigins contains request.headers.getOrElse(OriginHeader, ""))

  private[this] def isEnabled: Boolean =
    !("Upgrade".equalsIgnoreCase(request.headers.getOrElse("Connection", "")) &&
      "WebSocket".equalsIgnoreCase(request.headers.getOrElse("Upgrade", ""))) &&
      !requestPath.contains("eb_ping") // don't do anything for the ping endpoint

  private[this] def isValidRoute: Boolean       = routes.matchingMethods(requestPath).nonEmpty
  private[this] def isPreflightRequest: Boolean = {
    val isCors        = isCORSRequest
    val validRoute    = isValidRoute
    val isPreflight   = request.headers.get(AccessControlRequestMethodHeader).flatMap(_.blankOption).isDefined
    val enabled       = isEnabled
    val matchesOrigin = originMatches
    val methodAllowed = allowsMethod
    val allowsHeaders = headersAreAllowed
    val result = isCors && validRoute && isPreflight && enabled && matchesOrigin && methodAllowed && allowsHeaders
    result
  }

  private[this] def isCORSRequest: Boolean = {
    request.headers.get(OriginHeader).flatMap(_.blankOption).isDefined
  } // 6.x.1

  private[this] def isSimpleHeader(header: String): Boolean = {
    val ho = header.blankOption
    ho.isDefined && (ho forall { h =>
      val hu = h.toUpperCase(ENGLISH)
      SimpleHeaders.contains(hu) || (hu == "CONTENT-TYPE" &&
        SimpleContentTypes.exists((request.contentType.getOrElse("")).toUpperCase(ENGLISH).startsWith))
    })
  }

  private[this] def allOriginsMatch: Boolean = { // 6.1.2
    val h = request.headers.get(OriginHeader).flatMap(_.blankOption)
    h.isDefined && h.get.split(" ").nonEmpty && h.get.split(" ").forall(corsConfig.allowedOrigins.contains)
  }

  private[this] def isSimpleRequest: Boolean = {
    val isCors     = isCORSRequest
    val enabled    = isEnabled
    val allOrigins = allOriginsMatch
    val res        = isCors && enabled && allOrigins && request.headers.names.forall(isSimpleHeader)
    //    logger debug "This is a simple request: %s, because: %s, %s, %s".format(res, isCors, enabled, allOrigins)
    res
  }

  private[this] def allowsMethod: Boolean = { // 5.2.3 and 5.2.5
    val accessControlRequestMethod: String = {
      request.headers
        .get(AccessControlRequestMethodHeader)
        .flatMap(_.blankOption)
        .getOrElse("")
    }
    val result: Boolean = {
      accessControlRequestMethod.nonBlank &&
      corsConfig.allowedMethods.contains(accessControlRequestMethod.toUpperCase(ENGLISH))
    }
    result
  }

  private[this] def headersAreAllowed: Boolean = { // 5.2.4 and 5.2.6
    val allowedHeaders   = corsConfig.allowedHeaders.map(_.trim.toUpperCase(ENGLISH))
    val requestedHeaders =
      for (header <- request.headers.getMulti(AccessControlRequestHeadersHeader) if header.nonBlank)
        yield header.toUpperCase(ENGLISH)

    requestedHeaders.forall(h => isSimpleHeader(h) || allowedHeaders.contains(h))
  }

  abstract override def handle(req: HttpServletRequest, res: HttpServletResponse): Unit = {
    if (corsConfig.enabled) {
      withRequestResponse(req, res) {
        request.requestMethod match {
          case Options if isPreflightRequest => {
            handlePreflightRequest()
          }
          case Get | Post | Head if isSimpleRequest => {
            augmentSimpleRequest()
            super.handle(req, res)
          }
          case _ if isCORSRequest => {
            augmentSimpleRequest()
            super.handle(req, res)
          }
          case _ => super.handle(req, res)
        }
      }
    } else {
      super.handle(req, res)
    }
  }

}
