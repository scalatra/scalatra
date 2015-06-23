package org.scalatra

import org.scalatra.test.specs2.MutableScalatraSpec

import scala.concurrent.Future

class StableResultServlet extends ScalatraServlet with FutureSupport {

  override implicit val executor = scala.concurrent.ExecutionContext.global

  before("/*") {
    contentType = "text/html"
  }

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

  notFound {
    halt(404, <h1>Not found.</h1>)
  }

  // rewritten to:
  //
  // doNotFound = (() => {
  //   class cls$macro$5 extends org.scalatra.StableResult {
  //     def <init>() = {
  //       super.<init>();
  //       ()
  //     };
  //     val is = StableResultServlet.this.halt[scala.xml.Elem](scala.this.Predef.int2Integer(404), {
  //       {
  //         new scala.xml.Elem(null, "h1", scala.xml.Null, scala.this.xml.TopScope, false, ({
  //           val $buf = new scala.xml.NodeBuffer();
  //           $buf.&+(new scala.xml.Text("Not found."));
  //           $buf
  //         }: _*))
  //       }
  //     }, StableResultServlet.this.halt$default$3[Nothing], StableResultServlet.this.halt$default$4[Nothing])(reflect.this.ManifestFactory.classType[scala.xml.Elem](classOf[scala.xml.Elem]))
  //   };
  //   val res$macro$6 = new cls$macro$5();
  //   res$macro$6.is
  // })

  //  // make sure that ScalatraBase methods are invoked
  //  class X {
  //    val routes: Int = 100
  //    val addRoute: Int = 100
  //    val addStatusRoute: Int = 100
  //    val doNotFound: Int = 100
  //    val doMethodNotAllowed: Int = 100
  //    val errorHandler: Int = 100
  //    val asynchronously: Int = 100
  //
  //    before("/*") {
  //      contentType = "text/html"
  //    }
  //
  //    after("/*") {
  //      contentType = "text/html"
  //    }
  //
  //    get("/foo") {
  //
  //    }
  //
  //    asyncGet("/foo") {
  //
  //    }
  //
  //    error {
  //      case e =>
  //    }
  //
  //    methodNotAllowed {
  //      case methods =>
  //    }
  //
  //    trap(100) {
  //
  //    }
  //
  //    notFound {
  //
  //    }
  //  }

  case class Route(x: Int)

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
