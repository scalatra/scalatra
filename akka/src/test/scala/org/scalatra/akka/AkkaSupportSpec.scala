package org.scalatra
package akka

import _root_.akka.actor._
import Actor._
import org.scalatest.matchers.MustMatchers
import test.scalatest.ScalatraWordSpec
import test.specs.ScalatraSpecification

object AkkaSupportSpec {
  val probe = actorOf(new Actor with ScalatraSupport {
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
    
    error {
      case t => {
        status = 500
        response.getWriter.println(t.getMessage)
      }
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
