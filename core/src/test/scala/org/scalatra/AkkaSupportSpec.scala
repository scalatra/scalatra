package org.scalatra

import _root_.akka.actor._
import scala.concurrent.{Future, Await}
import _root_.akka.pattern.ask
import _root_.akka.actor.SupervisorStrategy._
import scala.concurrent.duration._

import _root_.akka.util.Timeout
import test.specs2.MutableScalatraSpec
import scala.util.control.ControlThrowable
import runtime.NonLocalReturnControl
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}

class AkkaSupportServlet extends ScalatraServlet with FutureSupport {
  val system = ActorSystem()
  protected implicit val executor = system.dispatcher
  override def asyncTimeout = 2 seconds

  get("/redirect") {
    new AsyncResult {
      val is = Future {
        redirect("redirected")
      }
    }
  }

  get("/redirected") {
    "redirected"
  }

  get("/working") {
    Future("the-working-reply")
  }


  get("/future/w/request") {
    Future {
      val uri = request.getPathInfo
      uri
    }
  }

  get("/timeout") {
    Future(Thread.sleep((asyncTimeout plus 1.second).toMillis))
  }

  class FailException extends RuntimeException

  get("/fail") {
    Future(throw new FailException)
  }

  class FailHarderException extends RuntimeException

  get("/fail-harder") {
    Future(throw new FailHarderException)
  }

  get("/halt") {
    Future(halt(419))
  }

  get("/*.jpg") {
    Future("jpeg")
  }

  override protected def contentTypeInferrer(implicit req: HttpServletRequest, resp: HttpServletResponse) = {
    case "jpeg" => "image/jpeg"
    case x => super.contentTypeInferrer(req, resp)(x)
  }

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

  addServlet(new AkkaSupportServlet, "/*")

  "The AkkaSupport" should {
    "render the reply of an actor" in {
      get("/working") {
        body must_== "the-working-reply"
      }
    }

    "handle calls to the request across threads" in {
      get("/future/w/request") {
        body must_== "/future/w/request"
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
        header("Content-Type") must startWith ("image/jpeg")
      }
    }

    "redirect with the redirect method" in {
      get("/redirect") {
        status must_== 302
        response.header("Location") must_== (baseUrl + "/redirected")
      }
    }
  }
}
