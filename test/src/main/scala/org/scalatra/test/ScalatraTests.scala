package org.scalatra.test

import java.net.{URISyntaxException, URI}
import grizzled.slf4j.Logger
import java.io._
import org.scalatra.{WebServer, Mountable}
import util.control.Exception._

/**
* Provides a framework-agnostic way to test your Scalatra app.  You probably want to extend this with
* either <code>org.scalatra.test.scalatest.ScalatraSuite</code> or
* <code>org.scalatra.test.specs.ScalatraSpecification</code>.
*
* Cookies are crudely supported within session blocks.  No attempt is made
* to match domains, paths, or max-ages; the request sends a Cookie header
* to match whatever Set-Cookie call it received on the previous response.
*/
trait ScalatraTests extends Client {
  private[this] lazy val log = Logger(getClass)

  def backend: WebServer
  lazy val backendClient = new AhcClient("127.0.0.1", backend.port)

  protected implicit def fileToSeq(file: File): Seq[File] = Seq(file)


  override def start() {
    backend.start()
    backendClient.start()
  }

  override def stop() {
    backendClient.stop()
    backend.stop()
  }

  def mount[TheApp <: Mountable](mountable: => TheApp) {
    backend.mount(mountable)
  }

  def mount[TheApp <: Mountable](path: String, mountable: => TheApp) {
    backend.mount(path, mountable)
  }


  def submit[A](method: String, uri: String, params: Iterable[(String, String)], headers: Map[String, String], files: Seq[File], body: String)(f: => A) =
    backendClient.submit(method, uri, params, headers, files, body){
      withResponse(backendClient.response)(f)
    }


  override def session[A](f: => A) = {
    backendClient._cookies.withValue(Nil) {
      backendClient._useSession.withValue(true) {
        _cookies.withValue(backendClient.cookies) {
          _useSession.withValue(backendClient.useSession)(f)
        }
      }
    }
  }

  def classpathFile(path: String) = {
    val cl = allCatch.opt(Thread.currentThread.getContextClassLoader) getOrElse getClass.getClassLoader
    try{
      new File(new URI(cl.getResource(path).toString).getSchemeSpecificPart)
    } catch {
      case e: URISyntaxException => throw new FileNotFoundException(path)
    }
  }
}

