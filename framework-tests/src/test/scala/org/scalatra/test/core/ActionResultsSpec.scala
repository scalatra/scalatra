package org.scalatra

import ActionResult._

import test.NettyBackend
import test.specs2.MutableScalatraSpec

class ActionResultApp extends ScalatraApp {
  error {
    case e => BadRequest("something went wrong")
  }

  get("/ok") {
    Ok("Hello, world!")
  }

  get("/ok-no-body") {
    Ok()
  }

  get("/bad") {
    BadRequest("Hello")
  }

  get("/headers") {
    Ok("Hello, World!", Map("X-Something" -> "Something"))
  }

  get("/bytes") {
    Ok("Hello, world!".getBytes)
  }

  get("/error") {
    throw new RuntimeException()
  }

  get("/redirect") {
    Found("/ok")
  }

  get("/custom-reason") {
    BadRequest(body = "abc", reason = "Bad Bad Bad")
  }
}

abstract class ActionResultsSpec extends MutableScalatraSpec {
  mount(new ActionResultApp)

  "returning ActionResult from action with status and body" should {
    "set the status code" in {
      get("/bad") {
        status.code mustEqual 400
      }
    }

    "render the body" in {
      get("/ok") {
        body mustEqual "Hello, world!"
      }
    }

    "set default reason" in {
      get("/bad") {
        response.status.message mustEqual "Bad Request"
      }
    }

    "infer contentType for String" in {
      get("/ok") {
        response.contentType must beMatching("text/plain;\\s*charset=UTF-8")
      }
    }

    "infer contentType for Array[Byte]" in {
      get("/bytes") {
        response.contentType must beMatching("application/octet-stream;\\s*charset=UTF-8")
      }
    }
  }

  "returning ActionResult with additional headers" should {
    "set the headers" in {
      get("/headers") {
        headers("X-Something") mustEqual "Something"
      }
    }
  }

  "returing ActionResult with Array[Byte] as body" should {
    "render the body using render pipe line" in {
      get("/bytes") {
        body mustEqual "Hello, world!"
      }
    }
  }

  "returning ActionResult from error handler" should {
    "set the status" in {
      get("/error") {
        status.code mustEqual 400
      }
    }

    "render the body" in {
      get("/error") {
        body mustEqual "something went wrong"
      }
    }
  }

  "returning Found" should {
    "set status to 302" in {
      get("/redirect") {
        status.code mustEqual 302
      }
    }

    "set the Location header" in {
      get("/redirect") {
        headers("Location") mustEqual "/ok"
      }
    }

    "keep body empty" in {
      get("/redirect") {
        body mustEqual ""
      }
    }
  }

  "return Ok without body" should {
    "keep body empty" in {
      get("/ok-no-body") {
        body must beEmpty
      }
    }
  }

  "returning ActionResult with custom reason" should {
    "set a custom reason on status line" in {
      get("/custom-reason") {
        response.status.message mustEqual "Bad Bad Bad"
      }
    }
  }
}

class NettyActionResultsSpec extends ActionResultsSpec with NettyBackend
