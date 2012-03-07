package org.scalatra
package akka

import _root_.akka.actor._
import _root_.akka.dispatch.Await
import _root_.akka.pattern.ask
import _root_.akka.actor.SupervisorStrategy._
import _root_.akka.util.duration._

import _root_.akka.util.Timeout
import test.specs.ScalatraSpecification

class AkkaSupportServlet extends ScalatraServlet with AkkaSupport {
  val system = ActorSystem()
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

  error {
    case e => "caught"
  }

  override def destroy() { 
    super.destroy()
    system.shutdown() 
  }
}

class AkkaSupportSpec extends ScalatraSpecification {
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
	body must include("caught")
      }
    }

    "render a halt" in {
      get("/halt") {
        status must_== 419
      }
    }
  }
}
