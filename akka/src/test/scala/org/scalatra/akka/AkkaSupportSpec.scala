package org.scalatra
package akka

import _root_.akka.actor._
import Actor._
import test.specs.ScalatraSpecification

object AkkaSupportSpec {

  val probe = actorOf(new Actor {
    protected def receive = {
      case "working" => self reply "the-working-reply"
      case "dontreply" =>
      case "throw" => halt(500, "The error")
    }
  }).start()

  class AkkaSupportServlet extends ScalatraServlet with AkkaSupport {
    
    get("/working") {
      probe ? "working"
    }
    
    get("/timeout") {
      probe ? "dontreply"
    }
    
    get("/throw") {
      probe ? "throw"
    }
    
    get("/supervised_error") {
      probe ? "throw"
    }
    
  }
}

class AkkaSupportSpec extends ScalatraSpecification {

  import AkkaSupportSpec.AkkaSupportServlet
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

    "respond with error message" in {
      get("/throw") {
        body must startWith("The error")
      }
    }
  }

  doAfterSpec {
    Actor.registry.shutdownAll()
    Scheduler.restart()
  }
}
