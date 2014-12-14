package org.scalatra

import javax.servlet.http.{ HttpServletResponse, HttpServletRequest }
import util.RicherString._
import java.util.Locale.ENGLISH
import grizzled.slf4j.Logger
import collection.JavaConverters._

object CorsSupport {
  val OriginHeader: String = "Origin"
  val AccessControlRequestMethodHeader: String = "Access-Control-Request-Method"
  val AccessControlRequestHeadersHeader: String = "Access-Control-Request-Headers"
  val AccessControlAllowOriginHeader: String = "Access-Control-Allow-Origin"
  val AccessControlAllowMethodsHeader: String = "Access-Control-Allow-Methods"
  val AccessControlAllowHeadersHeader: String = "Access-Control-Allow-Headers"
  val AccessControlMaxAgeHeader: String = "Access-Control-Max-Age"
  val AccessControlAllowCredentialsHeader: String = "Access-Control-Allow-Credentials"

  private val AnyOrigin: String = "*"
  private val SimpleHeaders = List(OriginHeader.toUpperCase(ENGLISH), "ACCEPT", "ACCEPT-LANGUAGE", "CONTENT-LANGUAGE")
  private val SimpleContentTypes = List("APPLICATION/X-WWW-FORM-URLENCODED", "MULTIPART/FORM-DATA", "TEXT/PLAIN")
  val CorsHeaders = List(
    OriginHeader,
    AccessControlAllowCredentialsHeader,
    AccessControlAllowHeadersHeader,
    AccessControlAllowMethodsHeader,
    AccessControlAllowOriginHeader,
    AccessControlMaxAgeHeader,
    AccessControlRequestHeadersHeader,
    AccessControlRequestMethodHeader)

  case class CORSConfig(
    allowedOrigins: Seq[String],
    allowedMethods: Seq[String],
    allowedHeaders: Seq[String],
    allowCredentials: Boolean,
    preflightMaxAge: Int = 0)

  private[this] def configKey(name: String) = "org.scalatra.cors." + name
  val AllowedOriginsKey = configKey("allowedOrigins")
  val AllowedMethodsKey = configKey("allowedMethods")
  val AllowedHeadersKey = configKey("allowedHeaders")
  val AllowCredentialsKey = configKey("allowCredentials")
  val PreflightMaxAgeKey = configKey("preflightMaxAge")
  val CorsConfigKey = configKey("corsConfig")
  private val DefaultMethods = "GET,POST,PUT,DELETE,HEAD,OPTIONS,PATCH"
  private val DefaultHeaders = Seq("Cookie",
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
    "Content-Type").mkString(",")

}
trait CorsSupport extends Handler with Initializable { self: ScalatraBase ⇒

  import CorsSupport._

  private[this] lazy val logger = Logger(getClass)

  abstract override def initialize(config: ConfigT) {
    super.initialize(config)
    def createDefault = CORSConfig(
      Option(config.context.getInitParameter(AllowedOriginsKey)).getOrElse(AnyOrigin).split(",").map(_.trim),
      Option(config.context.getInitParameter(AllowedMethodsKey)).getOrElse(DefaultMethods).split(",").map(_.trim),
      Option(config.context.getInitParameter(AllowedHeadersKey)).getOrElse(DefaultHeaders).split(",").map(_.trim),
      Option(config.context.getInitParameter(AllowCredentialsKey)).map(_.toBoolean).getOrElse(true),
      Option(config.context.getInitParameter(PreflightMaxAgeKey)).map(_.toInt).getOrElse(1800))

    val corsCfg = config.context.getOrElseUpdate(CorsConfigKey, createDefault).asInstanceOf[CORSConfig]
    import corsCfg._
    logger debug "Enabled CORS Support with:\nallowedOrigins:\n\t%s\nallowedMethods:\n\t%s\nallowedHeaders:\n\t%s".format(
      allowedOrigins mkString ", ",
      allowedMethods mkString ", ",
      allowedHeaders mkString ", ")
  }

  protected def handlePreflightRequest() {
    logger debug "handling preflight request"
    // 5.2.7
    augmentSimpleRequest()
    // 5.2.8
    if (corsConfig.preflightMaxAge > 0) response.headers(AccessControlMaxAgeHeader) = corsConfig.preflightMaxAge.toString
    // 5.2.9
    response.headers(AccessControlAllowMethodsHeader) = corsConfig.allowedMethods mkString ","
    // 5.2.10
    val rh = corsConfig.allowedHeaders ++ request.getHeaders(AccessControlRequestHeadersHeader).asScala.flatMap(_ split (","))
    response.headers(AccessControlAllowHeadersHeader) = rh mkString ","
    response.end()

  }

  protected def augmentSimpleRequest() {
    val anyOriginAllowed: Boolean = corsConfig.allowedOrigins.contains(AnyOrigin)
    val hdr = if (anyOriginAllowed && !corsConfig.allowCredentials)
      AnyOrigin
    else
      request.headers.get(OriginHeader).getOrElse("")

    response.headers(AccessControlAllowOriginHeader) = hdr
    if (corsConfig.allowCredentials) response.headers(AccessControlAllowCredentialsHeader) = "true"
    response.setHeader(AccessControlAllowHeadersHeader, request.getHeader(AccessControlRequestHeadersHeader))
  }

  private[this] def corsConfig = servletContext.get(CorsConfigKey).orNull.asInstanceOf[CORSConfig]

  private[this] def originMatches = // 6.2.2
    corsConfig.allowedOrigins.contains(AnyOrigin) ||
      (corsConfig.allowedOrigins contains request.headers.get(OriginHeader).getOrElse(""))

  private[this] def isEnabled =
    !("Upgrade".equalsIgnoreCase(request.headers.get("Connection").getOrElse("")) &&
      "WebSocket".equalsIgnoreCase(request.headers.get("Upgrade").getOrElse(""))) &&
      !requestPath.contains("eb_ping") // don't do anything for the ping endpoint

  private[this] def isValidRoute: Boolean = routes.matchingMethods(requestPath).nonEmpty
  private[this] def isPreflightRequest = {
    val isCors = isCORSRequest
    val validRoute = isValidRoute
    val isPreflight = request.headers.get(AccessControlRequestMethodHeader).flatMap(_.blankOption).isDefined
    val enabled = isEnabled
    val matchesOrigin = originMatches
    val methodAllowed = allowsMethod
    val allowsHeaders = headersAreAllowed
    val result = isCors && validRoute && isPreflight && enabled && matchesOrigin && methodAllowed && allowsHeaders
    //    logger debug "This is a preflight validation check. valid? %s".format(result)
    //    logger debug "cors? %s, route? %s, preflight? %s, enabled? %s, origin? %s, method? %s, header? %s".format(
    //      isCors, validRoute, isPreflight, enabled, matchesOrigin, methodAllowed, allowsHeaders)
    result
  }

  private[this] def isCORSRequest = request.headers.get(OriginHeader).flatMap(_.blankOption).isDefined // 6.x.1

  private[this] def isSimpleHeader(header: String) = {
    val ho = header.blankOption
    ho.isDefined && (ho forall { h ⇒
      val hu = h.toUpperCase(ENGLISH)
      SimpleHeaders.contains(hu) || (hu == "CONTENT-TYPE" &&
        SimpleContentTypes.exists((request.contentType.getOrElse("")).toUpperCase(ENGLISH).startsWith))
    })
  }

  private[this] def allOriginsMatch = { // 6.1.2
    val h = request.headers.get(OriginHeader).flatMap(_.blankOption)
    h.isDefined && h.get.split(" ").nonEmpty && h.get.split(" ").forall(corsConfig.allowedOrigins.contains)
  }

  private[this] def isSimpleRequest = {
    val isCors = isCORSRequest
    val enabled = isEnabled
    val allOrigins = allOriginsMatch
    val res = isCors && enabled && allOrigins && request.headers.keys.forall(isSimpleHeader)
    //    logger debug "This is a simple request: %s, because: %s, %s, %s".format(res, isCors, enabled, allOrigins)
    res
  }

  private[this] def allowsMethod = { // 5.2.3 and 5.2.5
    val accessControlRequestMethod = request.headers.get(AccessControlRequestMethodHeader).flatMap(_.blankOption).getOrElse("")
    //    logger.debug("%s is %s" format (ACCESS_CONTROL_REQUEST_METHOD_HEADER, accessControlRequestMethod))
    val result = accessControlRequestMethod.nonBlank && corsConfig.allowedMethods.contains(accessControlRequestMethod.toUpperCase(ENGLISH))
    //    logger.debug("Method %s is %s among allowed methods %s".format(accessControlRequestMethod, if (result) "" else " not", allowedMethods))
    result
  }

  private[this] def headersAreAllowed = { // 5.2.4 and 5.2.6
    val allowedHeaders = corsConfig.allowedHeaders.map(_.trim.toUpperCase(ENGLISH))
    val requestedHeaders = for (
      header <- request.headers.getMulti(AccessControlRequestHeadersHeader) if header.nonBlank
    ) yield header.toUpperCase(ENGLISH)

    requestedHeaders.forall(h => isSimpleHeader(h) || allowedHeaders.contains(h))
  }

  abstract override def handle(req: HttpServletRequest, res: HttpServletResponse) {
    withRequestResponse(req, res) {
      //      logger debug "the headers are: %s".format(req.getHeaderNames.mkString(", "))
      request.requestMethod match {
        case Options if isPreflightRequest ⇒ {
          handlePreflightRequest()
        }
        case Get | Post | Head if isSimpleRequest ⇒ {
          augmentSimpleRequest()
          super.handle(req, res)
        }
        case _ if isCORSRequest ⇒ {
          augmentSimpleRequest()
          super.handle(req, res)
        }
        case _ ⇒ super.handle(req, res)
      }
    }
  }

}

