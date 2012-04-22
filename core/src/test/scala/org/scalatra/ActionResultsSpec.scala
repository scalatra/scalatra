package org.scalatra

import ActionResult._

import test.specs2.MutableScalatraSpec

class ActionResultServlet extends ScalatraServlet {
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

class ActionResultsSpec extends MutableScalatraSpec {
  addServlet(classOf[ActionResultServlet], "/*")

  "returning ActionResult from action with status and body" should {
    "set the status code" in {
      get("/bad") {
        status mustEqual 400
      }
    }

    "render the body" in {
      get("/ok") {
        body mustEqual "Hello, world!"
      }
    }

    "set default reason" in {
      get("/bad") {
        response.getReason mustEqual "Bad Request"
      }
    }

    "infer contentType for String" in {
      get("/ok") {
        response.getContentType mustEqual "text/plain;charset=UTF-8"
      }
    }

    "infer contentType for Array[Byte]" in {
      get("/bytes") {
        response.getContentType mustEqual "application/octet-stream;charset=UTF-8"
      }
    }
  }

  "returning ActionResult with additional headers" should {
    "set the headers" in {
      get("/headers") {
        header("X-Something") mustEqual "Something"
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
        status mustEqual 400
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
        status mustEqual 302
      }
    }

    "set the Location header" in {
      get("/redirect") {
        header("Location") mustEqual "/ok"
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
        body mustEqual ""
      }
    }
  }

  "returning ActionResult with custom reason" should {
    "set a custom reason on status line" in {
      get("/custom-reason") {
        response.getReason mustEqual "Bad Bad Bad"
      }
    }
  }
}
