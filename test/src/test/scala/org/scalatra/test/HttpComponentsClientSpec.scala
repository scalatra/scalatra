package org.scalatra.test

import java.io.{ InputStream, OutputStream }
import javax.servlet.http.{ HttpServlet, HttpServletRequest, HttpServletResponse }

import org.specs2.mutable.Specification
import org.specs2.specification.{ Fragments, Step }

import scala.annotation.tailrec
import scala.collection.JavaConversions._

class HttpComponentsClientSpec
    extends Specification
    with HttpComponentsClient
    with EmbeddedJettyContainer {
  override def map(fs: => Fragments) = Step(start()) ^ super.map(fs) ^ Step(stop())

  addServlet(new HttpServlet {
    override def service(req: HttpServletRequest, resp: HttpServletResponse) {
      def copy(in: InputStream, out: OutputStream, bufferSize: Int = 4096) {
        val buf = new Array[Byte](bufferSize)
        @tailrec
        def loop() {
          val n = in.read(buf)
          if (n >= 0) {
            out.write(buf, 0, n)
            loop()
          }
        }
        loop()
      }

      resp.setHeader("Request-Method", req.getMethod.toUpperCase)
      resp.setHeader("Request-URI", req.getRequestURI)
      req.getHeaderNames.foreach(headerName =>
        resp.setHeader("Request-Header-%s".format(headerName), req.getHeader(headerName)))

      req.getParameterMap.foreach {
        case (name, values) =>
          resp.setHeader("Request-Param-%s".format(name), values.mkString(", "))
      }

      resp.getOutputStream.write("received: ".getBytes)
      copy(req.getInputStream, resp.getOutputStream)
    }
  }, "/*")

  "client" should {
    "support all HTTP methods" in {
      (doVerbGetActual("PUT") must equalTo("PUT")) and
        (doVerbGetActual("POST") must equalTo("POST")) and
        (doVerbGetActual("TRACE") must equalTo("TRACE")) and
        (doVerbGetActual("GET") must equalTo("GET")) and
        (doVerbGetActual("HEAD") must equalTo("HEAD")) and
        (doVerbGetActual("OPTIONS") must equalTo("OPTIONS")) and
        (doVerbGetActual("DELETE") must equalTo("DELETE")) and
        (doVerbGetActual("PATCH") must equalTo("PATCH"))
    }

    "submit query string parameters" in {
      get("/", Map("param1" -> "value1", "param2" -> "value2")) {
        (header("Request-Param-param1") must equalTo("value1")) and
          (header("Request-Param-param2") must equalTo("value2"))
      }
    }

    "submit headers" in {
      get("/", headers = Map("X-Hello-Server" -> "hello")) {
        (header("Request-Header-X-Hello-Server") must equalTo("hello"))
      }
    }

    "submit body for POST/PUT/PATCH requests" in {
      (doReqWithBody("POST", "post test") must equalTo("received: post test")) and
        (doReqWithBody("PUT", "put test") must equalTo("received: put test")) and
        (doReqWithBody("PATCH", "patch test") must equalTo("received: patch test"))
    }
  }

  private def doVerbGetActual(method: String) = {
    submit(method, "/") {
      header("Request-Method")
    }
  }

  private def doReqWithBody(method: String, reqBody: String) = {
    submit(method, "/", body = reqBody) {
      body
    }
  }
}
