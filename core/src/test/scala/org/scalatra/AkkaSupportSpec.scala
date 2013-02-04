package org.scalatra

import _root_.akka.actor._
import scala.concurrent.Await
import _root_.akka.pattern.ask
import _root_.akka.actor.SupervisorStrategy._
import scala.concurrent.duration._

import _root_.akka.util.Timeout
import test.specs2.MutableScalatraSpec
import scala.util.control.ControlThrowable
import runtime.NonLocalReturnControl

class AkkaSupportServlet extends ScalatraServlet with FutureSupport {
  val system = ActorSystem()
  protected implicit val executor = system.dispatcher
  override def asyncTimeout = 2 seconds

  asyncGet("/working") {
    "the-working-reply"
  }

  asyncGet("/timeout") {
    Thread.sleep((asyncTimeout plus 1.second).toMillis)
  }

  asyncGet("/fail") {
    throw new RuntimeException
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
    case e => "caught"
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
  }
}
