package org.scalatra
package akka

import _root_.akka.actor._
import Actor._
import org.scalatest.matchers.MustMatchers
import test.scalatest.ScalatraWordSpec
import test.specs.ScalatraSpecification

object AkkaSupportSpec {
  val probe = actorOf(new Actor {
    protected def receive = {
      case "working" => self reply "the-working-reply"
      case "dontreply" =>
      case "throw" => self.channel.sendException(new RuntimeException("The error"))
    }
  }).start()
  class AkkaSupportServlet extends ScalatraServlet with AkkaSupport {
    
    get("/") {
      probe ? "working"
    }
    
    get("/timeout") {
      probe ? "dontreply"
    }
    
    get("/throw") {
      probe ? "throw"
    }
  }
}

class AkkaSupportSpec extends ScalatraSpecification {

  import AkkaSupportSpec.AkkaSupportServlet
  
  addServlet(new AkkaSupportServlet, "/*")
  
  
  "The AkkaSupport" should {
    
    "render the reply of an actor" in {
      get("/") {
        body must_== "the-working-reply"
      }
    }
    
    "render the error response" in {
      get("/throw") {
        body must_== "hello"
      }
    }
  }

  doAfterSpec {
    Actor.registry.shutdownAll()
    Scheduler.restart()
  }
}
