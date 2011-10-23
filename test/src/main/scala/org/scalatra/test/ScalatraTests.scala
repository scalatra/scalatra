package org.scalatra.test

import scala.collection.JavaConversions._
import scala.util.DynamicVariable
import org.eclipse.jetty.testing.HttpTester
import org.eclipse.jetty.testing.ServletTester
import org.eclipse.jetty.servlet.ServletContextHandler
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
trait ScalatraTests extends JettyContainer with Client {
  implicit def httpTesterToScalatraHttpTester(t: HttpTester) = new ScalatraHttpTester(t)

  def tester: ServletTester
  def servletContextHandler = tester.getContext

  private val _cookies = new DynamicVariable[Seq[HttpCookie]](Nil)
  private val _useSession = new DynamicVariable(false)

  protected def start() = tester.start()
  protected def stop() = tester.stop()

  type Response = HttpTester

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
    withResponse(res) { f }
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

  def session[A](f: => A): A = {
    _cookies.withValue(Nil) {
      _useSession.withValue(true)(f)
    }
  }

  // shorthand for response.body
  def body = response.body
  // shorthand for response.header
  def header = response.header
  // shorthand for response.status
  def status = response.status

  // So servletContext.getRealPath doesn't crash.
  tester.setResourceBase("./src/main/webapp")
}

