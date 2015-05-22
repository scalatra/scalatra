package org.scalatra

import org.scalatra.test.specs2.MutableScalatraSpec

import scala.concurrent.duration.Duration
import scala.concurrent.{ Await, Future }

class StableResultServlet extends ScalatraServlet {

  import scala.concurrent.ExecutionContext.Implicits._

  get("/ok") {
    Ok(123)
  }

  // rewritten to:
  //  {
  //    class cls$macro$1 extends org.scalatra.StableResult {
  //      val is = Ok.apply(123, Ok.apply$default$2, Ok.apply$default$3)
  //    };
  //    val res$macro$2 = new cls$macro$1();
  //    res$macro$2.is
  //  }

  get("/future") {
    val f = Future {
      println(request.headers)
      println(session(request).getOrElseUpdate("foo", "bar"))

      response.setHeader("foo", "bar")

      Ok(123)
    }

    Await.result(f, Duration(10, "seconds"))
  }

  // rewritten to:
  //  {
  //    class cls$macro$3 extends org.scalatra.StableResult {
  //      val is = {
  //        val f = scala.concurrent.Future.apply[org.scalatra.ActionResult]({
  //          scala.Predef.println(StableResultServlet.this.enrichRequest(request).headers);
  //          scala.Predef.println(StableResultServlet.this.enrichSession(StableResultServlet.this.session(request)).getOrElseUpdate("foo", "bar"));
  //          response.setHeader("foo", "bar");
  //          Ok.apply(123, Ok.apply$default$2, Ok.apply$default$3)
  //        })(scala.concurrent.ExecutionContext.Implicits.global);
  //        scala.concurrent.Await.result[org.scalatra.ActionResult](f, scala.concurrent.duration.Duration.apply(10L, "seconds"))
  //      }
  //    };
  //    val res$macro$4 = new cls$macro$3();
  //    res$macro$4.is
  //  }

}

class StableResultSpec extends MutableScalatraSpec {
  mount(classOf[StableResultServlet], "/*")

  "The result of a StableResult is rendered" in {
    get("/ok") {
      status must beEqualTo(200)
      body must beEqualTo("123")
    }
  }

  "A StableResult allows safely closing over request and response in the lexical scope of an action" in {
    get("/future") {
      status must beEqualTo(200)
      body must beEqualTo("123")
    }
  }

}
