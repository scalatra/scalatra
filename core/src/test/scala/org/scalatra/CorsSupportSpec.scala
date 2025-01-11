package org.scalatra

import org.scalatra.test.specs2.ScalatraSpec

class CorsSupportSpec extends ScalatraSpec {

  addServlet(
    new ScalatraServlet with CorsSupport {

      get("/") { "OK" }

      override def initialize(config: ConfigT): Unit = {
        config.context.setInitParameter(CorsSupport.AllowedOriginsKey, "http://www.example.com")
        config.context
          .setInitParameter(CorsSupport.AllowedHeadersKey, "X-Requested-With,Authorization,Content-Type,Accept,Origin")
        config.context.setInitParameter(CorsSupport.AllowedMethodsKey, "GET,HEAD,POST")
        super.initialize(config)
      }
    },
    "/*"
  )

  def is =
    "The CORS support should" ^
      "augment a valid simple request" ! context.validSimpleRequest ^
      "not respond with allow origin header if request from unallowed origin" ! context.dontReturnAllowOriginHeaderForRequestFromOriginNotInAllowedOriginsList ^
      "not touch a regular request" ! context.dontTouchRegularRequest ^
      "respond to a valid preflight request" ! context.validPreflightRequest ^
      "respond to a valid preflight request with headers" ! context.validPreflightRequestWithHeaders ^ end

  object context {
    def validSimpleRequest = {
      get("/", headers = Map(CorsSupport.OriginHeader -> "http://www.example.com")) {
        response.getHeader(CorsSupport.AccessControlAllowOriginHeader) must_== "http://www.example.com"
      }
    }

    def dontReturnAllowOriginHeaderForRequestFromOriginNotInAllowedOriginsList = {
      get("/", headers = Map(CorsSupport.OriginHeader -> "http://www.hello.com")) {
        response.getHeader(CorsSupport.AccessControlAllowOriginHeader) must beNull
      }
    }

    def dontTouchRegularRequest = {
      get("/") {
        response.getHeader(CorsSupport.AccessControlAllowOriginHeader) must beNull
      }
    }

    def validPreflightRequest = {
      options(
        "/",
        headers = Map(
          CorsSupport.OriginHeader                     -> "http://www.example.com",
          CorsSupport.AccessControlRequestMethodHeader -> "GET",
          "Content-Type"                               -> "application/json"
        )
      ) {
        response.getHeader(CorsSupport.AccessControlAllowOriginHeader) must_== "http://www.example.com"
      }
    }

    def validPreflightRequestWithHeaders = {
      val hdrs = Map(
        CorsSupport.OriginHeader                      -> "http://www.example.com",
        CorsSupport.AccessControlRequestMethodHeader  -> "GET",
        CorsSupport.AccessControlRequestHeadersHeader -> "Origin, Authorization, Accept",
        "Content-Type"                                -> "application/json"
      )
      options("/", headers = hdrs) {
        response.getHeader(CorsSupport.AccessControlAllowOriginHeader) must_== "http://www.example.com"
        response.getHeader(CorsSupport.AccessControlAllowMethodsHeader) must_== "GET,HEAD,POST"
      }
    }
  }
}

class DisabledCorsSupportSpec extends ScalatraSpec {

  addServlet(
    new ScalatraServlet with CorsSupport {

      get("/") { "OK" }

      override def initialize(config: ConfigT): Unit = {
        config.context.setInitParameter(CorsSupport.AllowedOriginsKey, "http://www.example.com")
        config.context
          .setInitParameter(CorsSupport.AllowedHeadersKey, "X-Requested-With,Authorization,Content-Type,Accept,Origin")
        config.context.setInitParameter(CorsSupport.AllowedMethodsKey, "GET,HEAD,POST")
        config.context.setInitParameter(CorsSupport.EnableKey, "false")
        super.initialize(config)
      }
    },
    "/disabled"
  )

  def is =
    "The CORS support should" ^
      "be disabled with configuration" ! context.simpleRequestToDisabledCors ^ end

  object context {
    def simpleRequestToDisabledCors = {
      get("/disabled/", headers = Map(CorsSupport.OriginHeader -> "http://www.example.com")) {
        response.getHeader(CorsSupport.AccessControlAllowOriginHeader) must_== null
      }
    }
  }
}
