package com.thinkminimo.step

import scala.util.DynamicVariable
import java.net.URLEncoder.encode
import org.mortbay.jetty.testing.HttpTester
import org.mortbay.jetty.testing.ServletTester
import org.mortbay.jetty.{Handler => JettyHandler}
import org.mortbay.jetty.servlet.ServletHolder
import java.nio.charset.Charset
import javax.servlet.http.HttpServlet

trait StepTests {
  implicit def httpTesterToStepHttpTester(t: HttpTester) = new StepHttpTester(t)

  val tester = new ServletTester()
  private val _response = new DynamicVariable[HttpTester](new HttpTester)
  private val _session = new DynamicVariable(Map[String,String]())
  private val _useSession = new DynamicVariable(false)

  private def httpRequest(method: String, uri: String): HttpTester =
    httpRequest(method, uri, Map.empty, Map.empty)
  private def httpRequest(method: String, uri: String, params: Iterable[(String, String)]): HttpTester =
    httpRequest(method, uri, params, Map.empty)
  private def httpRequest(method: String, uri: String, params: Iterable[(String, String)], headers: Map[String, String]) = {
    def req = {
      val paramStr = params.map(t => List(t._1, t._2).map(encode(_, "UTF-8")).mkString("=")).mkString("&")
      val r = new HttpTester(Charset.defaultCharset.name)

      r.setVersion("HTTP/1.0")
      r.setMethod(method)
      r.setURI(uri)
      method.toLowerCase match {
	case "get" => r.setURI(uri + "?" + paramStr)
	case "post" => r.setContent(paramStr)
	// @todo case "put"
	// @todo case "delete"
      }
      (headers ++ _session.value).foreach(t => r.setHeader(t._1, t._2))
      r
    }

    tester.start()

    val res = new HttpTester(Charset.defaultCharset.name)

    res.parse(tester.getResponses(req.generate))
    res.setContent(res.getContent match {
      case null => ""
      case content => content
    })
    res
  }

  private def withResponse(r: HttpTester, f: => Unit) = {
    _response.withValue(r)(f)
    if(_useSession.value) _session.value ++= Map("Cookie" -> r.getHeader("Set-Cookie"))
  }

  // map a servlet to the path
  def route(servlet: Class[_], path: String) = tester.addServlet(servlet, path)

  def route(servlet: HttpServlet, path: String) = tester.getContext().addServlet(new ServletHolder(servlet), path)

  def routeFilter(filter: Class[_], path: String) = tester.addFilter(filter, path, JettyHandler.DEFAULT)

  def get(uri: String)(f: => Unit): Unit = withResponse(httpRequest("GET", uri), f)
  def get(uri: String, params: Tuple2[String, String]*)(f: => Unit): Unit =
    get(uri, params, Map[String, String]())(f)
  def get(uri: String, params: Iterable[(String, String)], headers: Map[String, String])(f: => Unit) =
    withResponse(httpRequest("GET", uri, params, headers), f)
  def post(uri: String, params: Tuple2[String, String]*)(f: => Unit): Unit =
    post(uri, params)(f)
  def post(uri: String, params: Iterable[(String,String)])(f: => Unit): Unit =
    post(uri, params, Map[String, String]())(f)
  def post(uri: String, params: Iterable[(String,String)], headers: Map[String, String])(f: => Unit) =
    withResponse(httpRequest("POST", uri, params, Map("Content-Type" -> "application/x-www-form-urlencoded") ++ headers), f)
  // @todo support POST multipart/form-data for file uploads
  // @todo def put
  // @todo def delete

  def session(f: => Unit) = {
    _session.withValue(Map[String,String]()) {
      _useSession.withValue(true)(f)
    }
  }

  // return the last response
  def response = _response value
  // shorthand for response.body
  def body = response.body
  // shorthand for response.header
  def header = response.header
  // shorthand for response.status
  def status = response.status
}
