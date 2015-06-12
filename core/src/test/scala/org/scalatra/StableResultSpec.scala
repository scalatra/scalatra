package org.scalatra

import javax.servlet.http.HttpServletRequest

import org.scalatra.test.specs2.MutableScalatraSpec

import scala.concurrent.Future

class StableResultServlet extends ScalatraServlet with FutureSupport {

  override implicit val executor = scala.concurrent.ExecutionContext.global

  get("/ok") {
    Ok(123)
  }

  get("/future-as-result") {
    Future {
      // request is the same as in the request handling thread _and_ it is not invalidated by the servlet container
      println(request.getSession)

      Ok(123)
    }
  }

  // rewritten to:
  //
  //  {
  //    class cls$macro$3 extends org.scalatra.StableResult {
  //      val is = scala.concurrent.Future.apply[org.scalatra.ActionResult]({
  //        scala.Predef.println(request.getSession());
  //        Ok.apply(123, Ok.apply$default$2, Ok.apply$default$3)
  //      })(StableResultServlet.this.executor)
  //    };
  //    val res$macro$4 = new cls$macro$3();
  //    res$macro$4.is
  //  }

  // here are some more issues which will be addressed in the future:
  //
  //  var futureEffect = false
  //
  //  // here the request will be invalidated by the servlet container and will lead to an IllegalStateException when accessing it from the Future
  //  get("/future-as-sideeffect") {
  //    Future {
  //
  //      // wait for Jetty to invalidate the request/response objects
  //      Thread.sleep(1000)
  //
  //      // try to access the session
  //      println(request.getSession)
  //      // java.lang.IllegalStateException: No SessionManager
  //      //   at org.eclipse.jetty.server.Request.getSession(Request.java:1402)
  //      //   at org.eclipse.jetty.server.Request.getSession(Request.java:1377)
  //
  //      futureEffect = true
  //
  //    } recover {
  //      case t => println(t.getMessage); t.printStackTrace()
  //    }
  //
  //    Ok(123)
  //  }
  //
  //  get("/future-effect") {
  //    futureEffect
  //  }
  //
  //  def doFooUnsafe = Future {
  //    request
  //  }
  //
  //  def doFooSafe(implicit request: HttpServletRequest) = Future {
  //    request
  //  }
  //
  //  // here the doFooUnsafe does close over the ThreadLocal-backed request from ScalatraBase
  //  get("/future-unsafe") {
  //    doFooUnsafe
  //  }
  //
  //  // this is the correct way at the moment, doFooSafe uses the request from StableResult
  //  get("/future-safe") {
  //    doFooSafe
  //  }

}

class StableResultSpec extends MutableScalatraSpec {
  mount(classOf[StableResultServlet], "/*")

  "An ActionResult is rendered" in {
    get("/ok") {
      status must beEqualTo(200)
      body must beEqualTo("123")
    }
  }

  "A Future can be returned as a result and there one can safely close over request/response" in {
    get("/future-as-result") {
      status must beEqualTo(200)
      body must beEqualTo("123")
    }
  }

  //  "It is safe to close over request/response in a Future which is not returned as a result" in {
  //    get("/future-as-sideeffect") {
  //      status must beEqualTo(200)
  //      body must beEqualTo("123")
  //    }
  //
  //    Thread.sleep(5000)
  //
  //    get("/future-effect") {
  //      status must beEqualTo(200)
  //      body must beEqualTo("true")
  //    }
  //  }

}
