package org.scalatra.test

import scala.util.DynamicVariable
import java.net.URLEncoder.encode
import org.eclipse.jetty.testing.HttpTester
import org.eclipse.jetty.testing.ServletTester
import org.eclipse.jetty.servlet.{FilterHolder, FilterMapping, DefaultServlet, ServletHolder}
import java.nio.charset.Charset
import javax.servlet.http.HttpServlet
import javax.servlet.Filter

/**
 * Provides a framework-agnostic way to test your Scalatra app.  You probably want to extend this with
 * either <code>org.scalatra.test.scalatest.ScalatraSuite</code> or
 * <code>org.scalatra.test.specs.ScalatraSpecification</code>.
 */
trait ScalatraTests {
  implicit def httpTesterToScalatraHttpTester(t: HttpTester) = new ScalatraHttpTester(t)

  def tester: ServletTester
  private val _response = new DynamicVariable[HttpTester](new HttpTester("iso-8859-1"))
  private val _session = new DynamicVariable(Map[String,String]())
  private val _useSession = new DynamicVariable(false)

  protected def start() = tester.start()
  protected def stop() = tester.stop()

  private def toQueryString(params: Traversable[(String, String)]) =
    params.map(t => List(t._1, t._2).map(encode(_, "UTF-8")).mkString("=")).mkString("&")

  private def httpRequest(method: String, uri: String, queryParams: Iterable[(String, String)] = Map.empty,
                          headers: Map[String, String] = Map.empty, body: String = null) = {
    val req = {
      val r = new HttpTester("iso-8859-1")
      r.setVersion("HTTP/1.0")
      r.setMethod(method)
      val queryString = toQueryString(queryParams)
      r.setURI(uri + (if (queryString == "") "" else "?") + queryString)
      r.setContent(body)
      (headers ++ _session.value).foreach(t => r.setHeader(t._1, t._2))
      r
    }

    val res = new HttpTester("iso-8859-1")

    res.parse(tester.getResponses(req.generate))
    res.setContent(res.getContent match {
      case null => ""
      case content => content
    })
    res
  }

  private def withResponse(r: HttpTester, f: => Unit) = {
    _response.withValue(r)(f)
    if(_useSession.value && r.getHeader("Set-Cookie") != null)
      _session.value ++= Map("Cookie" -> r.getHeader("Set-Cookie"))
  }

  @deprecated("use addServlet(Class, String) or addFilter(Class, String)")
  def route(klass: Class[_], path: String) = klass match {
    case servlet if classOf[HttpServlet].isAssignableFrom(servlet) =>
      addServlet(servlet.asInstanceOf[Class[_ <: HttpServlet]], path)
    case filter if classOf[Filter].isAssignableFrom(filter) =>
      addFilter(filter.asInstanceOf[Class[_ <: Filter]], path)
    case _ =>
      throw new IllegalArgumentException(klass + " is not assignable to either HttpServlet or Filter")
  }

  @deprecated("renamed to addServlet")
  def route(servlet: HttpServlet, path: String) = addServlet(servlet, path)

  def addServlet(servlet: HttpServlet, path: String) =
    tester.getContext().addServlet(new ServletHolder(servlet), path)

  def addServlet(servlet: Class[_ <: HttpServlet], path: String) =
    tester.addServlet(servlet, path)

  def addFilter(filter: Filter, path: String) =
    tester.getContext().addFilter(new FilterHolder(filter), path, FilterMapping.DEFAULT)

  def addFilter(filter: Class[_ <: Filter], path: String) =
    tester.addFilter(filter, path, FilterMapping.DEFAULT)

  @deprecated("renamed to addFilter")
  def routeFilter(filter: Class[_ <: Filter], path: String) =
    addFilter(filter, path)

  def get(uri: String)(f: => Unit): Unit = withResponse(httpRequest("GET", uri), f)
  def get(uri: String, params: Tuple2[String, String]*)(f: => Unit): Unit =
    get(uri, params, Map[String, String]())(f)
  def get(uri: String, params: Iterable[(String, String)] = Seq.empty, headers: Map[String, String] = Map.empty)
         (f: => Unit) =
    withResponse(httpRequest("GET", uri, params, headers), f)

  def post(uri: String, params: Tuple2[String, String]*)(f: => Unit): Unit =
    post(uri, params)(f)
  def post(uri: String, params: Iterable[(String,String)])(f: => Unit): Unit =
    post(uri, params, Map[String, String]())(f)
  def post(uri: String, params: Iterable[(String,String)], headers: Map[String, String])(f: => Unit): Unit =
    post(uri, toQueryString(params), Map("Content-Type" -> "application/x-www-form-urlencoded; charset=utf-8") ++ headers)(f)
  def post(uri: String, body: String = "", headers: Map[String, String] = Map.empty)(f: => Unit) =
    withResponse(httpRequest("POST", uri, Seq.empty, headers, body), f)
  // @todo support POST multipart/form-data for file uploads

  def put(uri: String, params: Iterable[(String, String)] = Seq.empty, headers: Map[String, String] = Map.empty)(f: => Unit) = {
    withResponse(httpRequest("PUT", uri, params, headers), f)
  }

  def delete(uri: String, params: Iterable[(String, String)] = Seq.empty, headers: Map[String, String] = Map.empty)(f: => Unit) = {
    withResponse(httpRequest("DELETE", uri, params, headers), f)
  }

  def options(uri: String, params: Iterable[(String, String)] = Seq.empty, headers: Map[String, String] = Map.empty)(f: => Unit) = {
    withResponse(httpRequest("OPTIONS", uri, params, headers), f)
  }

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

  // Add a default servlet.  If there is no underlying servlet, then
  // filters just return 404.
  addServlet(classOf[DefaultServlet], "/")

  // So servletContext.getRealPath doesn't crash.
  tester.setResourceBase("./src/main/webapp")
}
