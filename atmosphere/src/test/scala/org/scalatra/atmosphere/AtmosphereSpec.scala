package org.scalatra
package atmosphere

import java.util.concurrent.{ TimeUnit, CountDownLatch }

import _root_.akka.actor.ActorSystem
import org.atmosphere.wasync._
import org.atmosphere.wasync.impl.{ DefaultRequestBuilder, DefaultOptionsBuilder, DefaultOptions }
import org.json4s.{ Formats, DefaultFormats }
import org.scalatra.json.JacksonJsonSupport
import org.json4s._
import JsonDSL._
import org.scalatra.test.specs2.MutableScalatraSpec

import scala.concurrent.duration._

import javax.servlet.http.{ HttpServletResponse, HttpServletRequest }
import org.specs2.specification.{ Step, Fragments }

class AtmosphereSpecServlet(implicit override protected val scalatraActorSystem: ActorSystem)
    extends ScalatraServlet with JacksonJsonSupport with SessionSupport with AtmosphereSupport {

  implicit protected def jsonFormats: Formats = DefaultFormats
  implicit val system = scalatraActorSystem.dispatcher

  get("/echo") {
    "echo ok"
  }

  atmosphere("/test1") {
    new AtmosphereClient {
      def receive: AtmoReceive = {
        case Connected =>
          println("connected client")
          broadcast("connected", to = Everyone)
        case TextMessage(txt) =>
          println("text message: " + txt)
          send(("seen" -> txt): JValue)
        case JsonMessage(json) =>
          println("json message: " + json)
          send(("seen" -> "test1") ~ ("data" -> json))
        case m =>
          println("Got unknown message " + m.getClass + " " + m.toString)
      }
    }
  }

  error {
    case t: Throwable => t.printStackTrace()
  }

  override def handle(request: HttpServletRequest, response: HttpServletResponse) {
    withRequestResponse(request, response) {
      println(request.headers)
      println("routeBasePath: " + routeBasePath(request))
      println("requestPath: " + requestPath(request))

      super.handle(request, response)
    }
  }
}

//object WaSync {
//
//
//  val Get = Request.METHOD.GET
//  val Post = Request.METHOD.POST
//  val Trace = Request.METHOD.TRACE
//  val Put = Request.METHOD.PUT
//  val Delete = Request.METHOD.DELETE
//  val Options = Request.METHOD.OPTIONS
//
//  val WebSocket = Request.TRANSPORT.WEBSOCKET
//  val Sse = Request.TRANSPORT.SSE
//  val Streaming = Request.TRANSPORT.STREAMING
//  val LongPolling = Request.TRANSPORT.LONG_POLLING
//
//  type ErrorHandler = PartialFunction[Throwable, Unit]
//
//  def printIOException: ErrorHandler = {
//    case e: IOException => e.printStackTrace()
//  }
//
//
//  implicit def scalaFunction2atmoFunction[T](fn: T => Unit): Function[T] = new Function[T] { def on(t: T) { fn(t) } }
//  implicit def scalaFunction2atmoEncoder[T, S](fn: T => S): Encoder[T, S] = new Encoder[T, S] { def encode(s: T): S = fn(s) }
//
//  implicit def scalaFunction2atmoDecoder[T <: AnyRef, S](fn: T => S): Decoder[T, S] = new Decoder[T, S] {
//    def decode(e: EVENT_TYPE, s: T): S = fn(s)
//  }
//  implicit def errorHandler2atmoFunction(fn: PartialFunction[Throwable, Unit]): Function[Throwable] = new Function[Throwable] {
//    def on(t: Throwable) {
//      if (fn.isDefinedAt(t)) fn(t)
//      else throw t
//    }
//  }
//
//
//}

class AtmosphereSpec extends MutableScalatraSpec {

  implicit val system = ActorSystem("scalatra")

  mount(new AtmosphereSpecServlet, "/*")

  implicit val formats = DefaultFormats

  sequential

  "To support Atmosphere, Scalatra" should {

    "allow regular requests" in {
      get("/echo") {
        println(header)
        status must_== 200
        body must_== "echo ok"
      }
    }

    "allow one client to connect" in {
      val latch = new CountDownLatch(1)

      // yay?
      val client: Client[DefaultOptions, DefaultOptionsBuilder, DefaultRequestBuilder] = ClientFactory.getDefault.newClient.asInstanceOf[Client[DefaultOptions, DefaultOptionsBuilder, DefaultRequestBuilder]]

      val req = client.newRequestBuilder
        .method(Request.METHOD.GET)
        .uri(baseUrl + "/test1")
        .transport(Request.TRANSPORT.WEBSOCKET)

      val opts = client.newOptionsBuilder().reconnect(false).build()

      val socket = client.create(opts).on(Event.MESSAGE, new Function[String] {
        def on(r: String) = {
          latch.countDown()
          println(r)
        }
      }).on(new Function[Throwable] {
        def on(t: Throwable) = {
          t.printStackTrace
        }
      })

      socket.open(req.build()).fire("echo");

      latch.await(5, TimeUnit.SECONDS) must beTrue
    }

  }

  private def stopSystem {
    system.shutdown()
    system.awaitTermination(Duration(1, TimeUnit.MINUTES))
  }

  override def map(fs: => Fragments): Fragments = super.map(fs) ^ Step(stopSystem)
}
