package org.scalatra

import org.scalatra.test.specs2.MutableScalatraSpec

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

class StableResultServlet extends ScalatraServlet {

  import scala.concurrent.ExecutionContext.Implicits._

  get("/ok") {
    new StableResult() {
      override val is: Any = {
        Ok(123)
      }
    }
  }

  get("/future") {
    new StableResult() {
      override val is: Any = {
        Future {
          println(request.headers) // request is not read from a ThreadLocal here
          println(session(request).getOrElseUpdate("foo", "bar"))

          response.setHeader("foo", "bar")
        }

        Ok(123)
      }
    }
  }

  get("/macro") {
    val f = Future {
      println(request.headers)
      println(session(request).getOrElseUpdate("foo", "bar"))

      response.setHeader("foo", "bar")

      Ok(123)
    }

    Await.result(f, Duration(10, "seconds"))
  }

  // rewritten to:
  //
  //  {
  //    final class $anon extends org.scalatra.StableResult {
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
  //    new $anon()
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

  "A stable request/response value exists in the action" in {
    get("/macro") {
      status must beEqualTo(200)
      body must beEqualTo("123")
    }
  }

}
