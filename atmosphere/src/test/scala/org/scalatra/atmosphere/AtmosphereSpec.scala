
package org.scalatra
package atmosphere

import test.specs2.MutableScalatraSpec
import json.JacksonJsonSupport
import org.json4s._
import JsonDSL._
import org.atmosphere.wasync._
import java.io.{IOException, StringReader, Reader}
import java.util.concurrent.{TimeUnit, CountDownLatch}
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import  _root_.akka.actor.ActorSystem
import org.specs2.specification.{Step, Fragments}
import scala.concurrent.duration._
import org.specs2.time.NoTimeConversions
//import org.atmosphere.wasync.Transport.EVENT_TYPE
import scala.annotation.switch

class AtmosphereSpecServlet(implicit override protected val scalatraActorSystem: ActorSystem) extends ScalatraServlet with JacksonJsonSupport with AtmosphereSupport {
  implicit protected def jsonFormats: Formats = DefaultFormats

  implicit def executor = scala.concurrent.ExecutionContext.global

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
          send(("seen" -> txt):JValue)
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
    println(request.headers)
    println("routeBasePath: " + routeBasePath(request))
    println("requestPath: " + requestPath(request))

    super.handle(request, response)
  }
}

object WaSync {

  val Get = Request.METHOD.GET
  val Post = Request.METHOD.POST
  val Trace = Request.METHOD.TRACE
  val Put = Request.METHOD.PUT
  val Delete = Request.METHOD.DELETE
  val Options = Request.METHOD.OPTIONS

  val WebSocket = Request.TRANSPORT.WEBSOCKET
  val Sse = Request.TRANSPORT.SSE
  val Streaming = Request.TRANSPORT.STREAMING
  val LongPolling = Request.TRANSPORT.LONG_POLLING

  type ErrorHandler = PartialFunction[Throwable, Unit]

  def printIOException: ErrorHandler = {
    case e: IOException => e.printStackTrace()
  }

  implicit def scalaFunction2atmoFunction[T](fn: T => Unit): Function[T] = new Function[T] { def on(t: T) { fn(t) } }
  implicit def scalaFunction2atmoEncoder[T, S](fn: T => S): Encoder[T, S] = new Encoder[T, S] { def encode(s: T): S = fn(s) }


  implicit def scalaFunction2atmoDecoder[T <: AnyRef, S](fn: T => S): Decoder[T, S] = new Decoder[T, S] {
    def decode(e: Event, s: T): S = fn(s)
  }
  implicit def errorHandler2atmoFunction(fn: PartialFunction[Throwable, Unit]): Function[Throwable] = new Function[Throwable] {
    def on(t: Throwable) {
      if (fn.isDefinedAt(t)) fn(t)
      else throw t
    }
  }

}

class AtmosphereSpec extends MutableScalatraSpec with NoTimeConversions {

  import WaSync._
  implicit val system = ActorSystem("scalatra")

  mount(new AtmosphereSpecServlet, "/*")


  implicit val formats = DefaultFormats

  private val stringEncoder: Encoder[String, String] = (s: String) => s
  private val stringDecoder: Decoder[String, String] = (s: String) => s

  sequential

  "To support Atmosphere, Scalatra" should {

    "allow regular requests" in {
      get("/echo") {
        status must_== 200
        body must_== "echo ok"
      }
    }

    "allow one client to connect" in {
      val latch = new CountDownLatch(2)
      val client = ClientFactory.getDefault.newClient()
      val req = (client.newRequestBuilder().transport(LongPolling).method(Get).uri(baseUrl + "/test1"))
      val opts = client.newOptionsBuilder().reconnect(false).build()
      val conn = client.create() // client.create(opts)
      conn.on(new Function[AnyRef] {
        def on(t: AnyRef) {
          println("Got Anyref " + t)
        }
      })
//      (conn
//         on { (s: String) =>
//          println("Got String: " + s)
//          latch.countDown()
//         }
////         on printIOException
//         open req.build()
//         fire "hello"
//         fire """{"id":1}"""
//         done())
//
//      latch.await(5, TimeUnit.SECONDS) must beTrue

      pending
    }
  }
  private def stopSystem {
    system.shutdown()
    system.awaitTermination(1 minutes)
  }

  override def map(fs: => Fragments): Fragments = super.map(fs) ^ Step(stopSystem)
}
