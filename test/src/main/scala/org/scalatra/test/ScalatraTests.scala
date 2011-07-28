package org.scalatra.test

import scala.collection.JavaConversions._
import scala.util.DynamicVariable
import java.net.URLEncoder.encode
import org.eclipse.jetty.testing.HttpTester
import org.eclipse.jetty.testing.ServletTester
import org.eclipse.jetty.server.DispatcherType
import org.eclipse.jetty.servlet.{FilterHolder, DefaultServlet, ServletHolder}
import java.nio.charset.Charset
import javax.servlet.http.HttpServlet
import javax.servlet.Filter
import java.net.HttpCookie
import java.util.{Enumeration, EnumSet}

object ScalatraTests {
  val DefaultDispatcherTypes: EnumSet[DispatcherType] = 
    EnumSet.of(DispatcherType.REQUEST, DispatcherType.ASYNC)
}
import ScalatraTests._

/**
 * Provides a framework-agnostic way to test your Scalatra app.  You probably want to extend this with
 * either <code>org.scalatra.test.scalatest.ScalatraSuite</code> or
 * <code>org.scalatra.test.specs.ScalatraSpecification</code>.
 *
 * Cookies are crudely supported within session blocks.  No attempt is made 
 * to match domains, paths, or max-ages; the request sends a Cookie header
 * to match whatever Set-Cookie call it received on the previous response.
 */
trait ScalatraTests {
  implicit def httpTesterToScalatraHttpTester(t: HttpTester) = new ScalatraHttpTester(t)

  def tester: ServletTester
  private val _response = new DynamicVariable[HttpTester](new HttpTester("iso-8859-1"))
  private val _cookies = new DynamicVariable[Seq[HttpCookie]](Nil)
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
      headers.foreach(t => r.setHeader(t._1, t._2))
      _cookies.value foreach(c => r.setHeader("Cookie", c.toString))
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

  private def withResponse[A](r: HttpTester, f: => A) = {
    val result = _response.withValue(r)(f)
    if(_useSession.value && r.getHeader("Set-Cookie") != null) {
      val setCookies = r.getHeaderValues("Set-Cookie").asInstanceOf[Enumeration[String]]
      _cookies.value = setCookies flatMap { setCookie => 
        HttpCookie.parse(setCookie).iterator
      } toSeq
    }
    result
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

  def addFilter(filter: Filter, path: String): FilterHolder =
    addFilter(filter, path, DefaultDispatcherTypes)
  def addFilter(filter: Filter, path: String, dispatches: EnumSet[DispatcherType]): FilterHolder = {
    val holder = new FilterHolder(filter)
    tester.getContext.addFilter(holder, path, dispatches)
    holder
  }
  def addFilter(filter: Class[_ <: Filter], path: String): FilterHolder =
    addFilter(filter, path, DefaultDispatcherTypes)
  def addFilter(filter: Class[_ <: Filter], path: String, dispatches: EnumSet[DispatcherType]): FilterHolder =
    tester.getContext.addFilter(filter, path, dispatches)

  @deprecated("renamed to addFilter")
  def routeFilter(filter: Class[_ <: Filter], path: String) =
    addFilter(filter, path)

  def get[A](uri: String)(f: => A): A = withResponse(httpRequest("GET", uri), f)
  def get[A](uri: String, params: Tuple2[String, String]*)(f: => A): A =
    get(uri, params, Map[String, String]())(f)
  def get[A](uri: String, params: Iterable[(String, String)] = Seq.empty, headers: Map[String, String] = Map.empty)(f: => A): A =
    withResponse(httpRequest("GET", uri, params, headers), f)

  def head[A](uri: String)(f: => A): A = withResponse(httpRequest("HEAD", uri), f)
  def head[A](uri: String, params: Tuple2[String, String]*)(f: => A): A =
    get(uri, params, Map[String, String]())(f)
  def head[A](uri: String, params: Iterable[(String, String)] = Seq.empty, headers: Map[String, String] = Map.empty)(f: => A): A =
    withResponse(httpRequest("HEAD", uri, params, headers), f)

  def post[A](uri: String, params: Tuple2[String, String]*)(f: => A): A =
    post(uri, params)(f)
  def post[A](uri: String, params: Iterable[(String,String)])(f: => A): A =
    post(uri, params, Map[String, String]())(f)
  def post[A](uri: String, params: Iterable[(String,String)], headers: Map[String, String])(f: => A): A =
    post(uri, toQueryString(params), Map("Content-Type" -> "application/x-www-form-urlencoded; charset=utf-8") ++ headers)(f)
  def post[A](uri: String, body: String = "", headers: Map[String, String] = Map.empty)(f: => A): A =
    withResponse(httpRequest("POST", uri, Seq.empty, headers, body), f)
  // @todo support POST multipart/form-data for file uploads

  def put[A](uri: String, params: Tuple2[String, String]*)(f: => A): A =
    put(uri, params)(f)
  def put[A](uri: String, params: Iterable[(String,String)])(f: => A): A =
    put(uri, params, Map[String, String]())(f)
  def put[A](uri: String, params: Iterable[(String,String)], headers: Map[String, String])(f: => A): A =
    put(uri, toQueryString(params), Map("Content-Type" -> "application/x-www-form-urlencoded; charset=utf-8") ++ headers)(f)
  def put[A](uri: String, body: String = "", headers: Map[String, String] = Map.empty)(f: => A) =
    withResponse(httpRequest("PUT", uri, Seq.empty, headers, body), f)
  // @todo support PUT multipart/form-data for file uploads
  
  def delete[A](uri: String, params: Iterable[(String, String)] = Seq.empty, headers: Map[String, String] = Map.empty)(f: => A): A = {
    withResponse(httpRequest("DELETE", uri, params, headers), f)
  }

  def options[A](uri: String, params: Iterable[(String, String)] = Seq.empty, headers: Map[String, String] = Map.empty)(f: => A): A = {
    withResponse(httpRequest("OPTIONS", uri, params, headers), f)
  }

  def session[A](f: => A): A = {
    _cookies.withValue(Nil) {
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

