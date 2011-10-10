package org.scalatra.test

import scala.collection.JavaConversions._
import scala.util.DynamicVariable
import java.net.URLEncoder.encode
import org.eclipse.jetty.testing.HttpTester
import org.eclipse.jetty.testing.ServletTester
import org.eclipse.jetty.servlet.{FilterHolder, DefaultServlet, ServletHolder}
import java.nio.charset.Charset
import javax.servlet.http.HttpServlet
import java.net.HttpCookie
import java.util.{Enumeration, EnumSet}
import javax.servlet.{DispatcherType, Filter}

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

  def submit[A](req: HttpTester)(f: => A): A = {
    val res = new HttpTester("iso-8859-1")
    val reqString = req.generate
    //println(reqString)
    //println()
    val resString = tester.getResponses(req.generate)
    //println(resString)
    //println()
    res.parse(resString)
    res.setContent(res.getContent match {
      case null => ""
      case content => content
    })
    if (_useSession.value && res.getHeader("Set-Cookie") != null) {
      val setCookies = res.getHeaderValues("Set-Cookie").asInstanceOf[Enumeration[String]]
      _cookies.value = setCookies flatMap { setCookie => 
        HttpCookie.parse(setCookie).iterator
      } toSeq
    }
    _response.withValue(res) { f }
  }

  def submit[A](method: String, uri: String, queryParams: Iterable[(String, String)] = Map.empty,
                headers: Map[String, String] = Map.empty, body: String = null)(f: => A): A = {
    val req = new HttpTester("iso-8859-1")
    req.setVersion("HTTP/1.0")
    req.setMethod(method)
    val queryString = toQueryString(queryParams)
    req.setURI(uri + (if (queryString == "") "" else "?") + queryString)
    req.setContent(body)
    headers.foreach(t => req.setHeader(t._1, t._2))
    _cookies.value foreach(c => req.addHeader("Cookie", c.toString))
    submit(req) { f }
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
    def tryToAddFilter(dispatches: AnyRef) = Reflection.invokeMethod(
      tester.getContext, "addFilter", holder, path, dispatches)
    // HACK: Jetty7 and Jetty8 have incompatible interfaces.  Call it reflectively
    // so we support both.
    for {
      _ <- tryToAddFilter(DispatcherType.intValue(dispatches): java.lang.Integer).left
      result <- tryToAddFilter(DispatcherType.convert(dispatches, "javax.servlet.DispatcherType")).left
    } yield (throw result)
    holder
  }

  def addFilter(filter: Class[_ <: Filter], path: String): FilterHolder =
    addFilter(filter, path, DefaultDispatcherTypes)

  def addFilter(filter: Class[_ <: Filter], path: String, dispatches: EnumSet[DispatcherType]): FilterHolder = {
    def tryToAddFilter(dispatches: AnyRef): Either[Throwable, AnyRef] =
      Reflection.invokeMethod(tester.getContext, "addFilter",
        filter, path, dispatches)
    // HACK: Jetty7 and Jetty8 have incompatible interfaces.  Call it reflectively
    // so we support both.
    (tryToAddFilter(DispatcherType.intValue(dispatches): java.lang.Integer).left map {
      t: Throwable => tryToAddFilter(DispatcherType.convert(dispatches, "javax.servlet.DispatcherType"))
    }).joinLeft fold ({ throw _ }, { x => x.asInstanceOf[FilterHolder] })
  }

  @deprecated("renamed to addFilter")
  def routeFilter(filter: Class[_ <: Filter], path: String) =
    addFilter(filter, path)

  def get[A](uri: String)(f: => A): A = submit("GET", uri) { f }
  def get[A](uri: String, params: Tuple2[String, String]*)(f: => A): A =
    get(uri, params, Map[String, String]())(f)
  def get[A](uri: String, params: Iterable[(String, String)] = Seq.empty, headers: Map[String, String] = Map.empty)(f: => A): A =
    submit("GET", uri, params, headers) { f }

  def head[A](uri: String)(f: => A): A = submit("HEAD", uri) { f }
  def head[A](uri: String, params: Tuple2[String, String]*)(f: => A): A =
    get(uri, params, Map[String, String]())(f)
  def head[A](uri: String, params: Iterable[(String, String)] = Seq.empty, headers: Map[String, String] = Map.empty)(f: => A): A =
    submit("HEAD", uri, params, headers) { f }

  def post[A](uri: String, params: Tuple2[String, String]*)(f: => A): A =
    post(uri, params)(f)
  def post[A](uri: String, params: Iterable[(String,String)])(f: => A): A =
    post(uri, params, Map[String, String]())(f)
  def post[A](uri: String, params: Iterable[(String,String)], headers: Map[String, String])(f: => A): A =
    post(uri, toQueryString(params), Map("Content-Type" -> "application/x-www-form-urlencoded; charset=utf-8") ++ headers)(f)
  def post[A](uri: String, body: String = "", headers: Map[String, String] = Map.empty)(f: => A): A =
    submit("POST", uri, Seq.empty, headers, body) { f }
  // @todo support POST multipart/form-data for file uploads

  def put[A](uri: String, params: Tuple2[String, String]*)(f: => A): A =
    put(uri, params)(f)
  def put[A](uri: String, params: Iterable[(String,String)])(f: => A): A =
    put(uri, params, Map[String, String]())(f)
  def put[A](uri: String, params: Iterable[(String,String)], headers: Map[String, String])(f: => A): A =
    put(uri, toQueryString(params), Map("Content-Type" -> "application/x-www-form-urlencoded; charset=utf-8") ++ headers)(f)
  def put[A](uri: String, body: String = "", headers: Map[String, String] = Map.empty)(f: => A) =
    submit("PUT", uri, Seq.empty, headers, body) { f }
  // @todo support PUT multipart/form-data for file uploads

  def delete[A](uri: String, params: Iterable[(String, String)] = Seq.empty, headers: Map[String, String] = Map.empty)(f: => A): A =
    submit("DELETE", uri, params, headers) { f }

  def options[A](uri: String, params: Iterable[(String, String)] = Seq.empty, headers: Map[String, String] = Map.empty)(f: => A): A =
    submit("OPTIONS", uri, params, headers) { f }

  def trace[A](uri: String, params: Iterable[(String, String)] = Seq.empty, headers: Map[String, String] = Map.empty)(f: => A): A =
    submit("TRACE", uri, params, headers) { f }

  def connect[A](uri: String, params: Iterable[(String, String)] = Seq.empty, headers: Map[String, String] = Map.empty)(f: => A): A =
    submit("CONNECT", uri, params, headers) { f }

  def patch[A](uri: String, params: Tuple2[String, String]*)(f: => A): A =
    patch(uri, params)(f)
  def patch[A](uri: String, params: Iterable[(String,String)])(f: => A): A =
    patch(uri, params, Map[String, String]())(f)
  def patch[A](uri: String, params: Iterable[(String,String)], headers: Map[String, String])(f: => A): A =
    patch(uri, toQueryString(params), Map("Content-Type" -> "application/x-www-form-urlencoded; charset=utf-8") ++ headers)(f)
  def patch[A](uri: String, body: String = "", headers: Map[String, String] = Map.empty)(f: => A): A =
    submit("PATCH", uri, Seq.empty, headers, body) { f }

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

