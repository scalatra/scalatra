package org.scalatra

import java.util.concurrent.Executors

import _root_.akka.actor._
import org.eclipse.jetty.server.{ Connector, ServerConnector, Server }
import org.eclipse.jetty.util.thread.QueuedThreadPool
import org.scalatra.test.HttpComponentsClient
import org.scalatra.test.specs2.MutableScalatraSpec

import scala.concurrent._
import scala.concurrent.duration._

class AkkaSupportServlet extends ScalatraServlet with FutureSupport {
  val system = ActorSystem()
  protected implicit val executor = system.dispatcher
  override def asyncTimeout = 2 seconds

  private val futureEC = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(1))

  get("/redirect") {
    new AsyncResult {
      val is: Future[_] = Future {
        redirect("redirected")
      }
    }
  }

  get("/async-oh-noes") {
    new AsyncResult {
      val is = Future {
        Thread.sleep(100) // To get the container to give up the request
        Ok(body = s"${request.getContextPath}")
      }
    }
  }

  get("/async-attributes/:mockSessionId") {
    request.setAttribute("sessionId", params("mockSessionId"))
    val handlingReq = request
    new AsyncResult {
      val is = Future {
        Thread.sleep(200)
        Ok(body = request.getAttribute("sessionId"))
      }(futureEC)
    }
  }

  get("/redirected") {
    "redirected"
  }

  asyncGet("/working") {
    "the-working-reply"
  }

  asyncGet("/timeout") {
    Thread.sleep((asyncTimeout plus 1.second).toMillis)
  }

  class FailException extends RuntimeException

  asyncGet("/fail") {
    throw new FailException
  }

  class FailHarderException extends RuntimeException

  asyncGet("/fail-harder") {
    throw new FailHarderException
  }

  asyncGet("/halt") {
    halt(419)
  }

  asyncGet("/*.jpg") {
    "jpeg"
  }

  override protected def contentTypeInferrer = ({
    case "jpeg" => "image/jpeg"
  }: ContentTypeInferrer) orElse super.contentTypeInferrer

  error {
    case e: FailException => "caught"
  }

  override def destroy() {
    super.destroy()
    system.shutdown()
  }
}

class AkkaSupportSpec extends MutableScalatraSpec {
  sequential

  override lazy val server = {
    /*
     Min threads for Jetty is 6 because: acceptors=1 + selectors=4 + request=1

     so 16 max and 6 min -> 10 worker threads
      */
    val threadPool = new QueuedThreadPool(16, 6)
    val server = new Server(threadPool)
    val connector: ServerConnector = new ServerConnector(server)
    connector.setPort(port)
    server.setConnectors(Array[Connector](connector))
    server
  }

  addServlet(new AkkaSupportServlet, "/*")

  "The AkkaSupport" should {
    "render the reply of an actor" in {
      get("/working") {
        body must_== "the-working-reply"
      }
    }

    "respond with timeout if no timely reply from the actor" in {
      get("/timeout") {
        status must_== 504
        body must_== "Gateway timeout"
      }
    }

    "handle an async exception" in {
      get("/fail") {
        body must contain("caught")
      }
    }

    "return 500 for an unhandled async exception" in {
      get("/fail-harder") {
        status must_== 500
      }
    }

    "render a halt" in {
      get("/halt") {
        status must_== 419
      }
    }

    "infers the content type of the future result" in {
      get("/foo.jpg") {
        header("Content-Type") must startWith("image/jpeg")
      }
    }

    "redirect with the redirect method" in {
      get("/redirect") {
        status must_== 302
        response.header("Location") must_== (baseUrl + "/redirected")
      }
    }

    "have a stable request" in {
      get("/async-oh-noes") {
        body must_== ""
        // body must not be_== "null"
      }
    }

    "should not leak attributes between requests" in {
      implicit val multiClentEc = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(50))
      val ids = (1 to 50).map(_ => scala.util.Random.nextInt())
      val serverBaseUrl = baseUrl
      val idsToResponseFs = ids.map { id =>
        val client = new HttpComponentsClient {
          override val baseUrl: String = serverBaseUrl
        }
        Future {
          blocking {
            id.toString -> client.get(s"/async-attributes/$id") {
              client.body
            }
          }
        }(multiClentEc)
      }
      val fIdsToresponses = Future.sequence(idsToResponseFs)
      val idsToResponses = Await.result(fIdsToresponses, 60.seconds)
      foreachWhen(idsToResponses) {
        case (expected, actual) => {
          expected must_== actual
        }
      }
    }
  }
}