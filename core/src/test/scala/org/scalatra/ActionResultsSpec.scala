package org.scalatra

import java.io.ByteArrayOutputStream
import java.io.File

import org.scalatra.test.specs2.MutableScalatraSpec

import scala.io.Codec

class ActionResultServlet extends ScalatraServlet with ActionResultTestBase

trait ActionResultTestBase {
  self: ScalatraBase =>
  error { case e =>
    BadRequest("something went wrong")
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

  get("/contentType") {
    val headerName =
      if (params.getOrElse("lcase", "false") == "true")
        "content-type"
      else
        "Content-Type"

    Ok("Hello, world!", headers = Map(headerName -> "application/vnd.ms-excel"))
  }

  get("/input-stream") {
    contentType = "image/png"
    getClass.getResourceAsStream("/org/scalatra/servlet/smiley.png")
  }

  get("/file") {
    val url = getClass.getResource("/org/scalatra/servlet/smiley.png")

    new File(url.toURI)
  }

  get("/defaults-to-call-by-value") {
    var state = "open"
    // close over mutable state
    def x: String = {
      state
    }

    val res = Ok(x)

    // modify state
    state = "closed"

    res
  }
}

class ActionResultServletSpec extends ActionResultsSpec {
  mount(classOf[ActionResultServlet], "/*")
}
abstract class ActionResultsSpec extends MutableScalatraSpec {

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
        response.getReason() mustEqual "Bad Request"
      }
    }

    "infer contentType for String" in {
      get("/ok") {
        response.getContentType() mustEqual "text/plain;charset=utf-8"
      }
    }

    "infer contentType for Array[Byte]" in {
      get("/bytes") {
        response
          .getContentType() mustEqual "text/plain;charset=" + Codec.defaultCharsetCodec.charSet.displayName.toLowerCase
      }
    }

    "render the inputStream" in {
      val expected = {
        val o = new ByteArrayOutputStream()
        val i = getClass.getResourceAsStream("/org/scalatra/servlet/smiley.png")
        org.eclipse.jetty.util.IO.copy(i, o)
        o.toByteArray
      }

      get("/input-stream") {
        response.mediaType must beSome("image/png")
        bodyBytes must_== expected
      }

      get("/file") {
        response.mediaType must beSome("image/png")
      }
    }
  }

  "returning ActionResult with additional headers" should {
    "set the headers" in {
      get("/headers") {
        header("X-Something") mustEqual "Something"
      }
    }

    "set the Content-Type header if it exists in the headers map" in {
      get("/contentType") {
        header("Content-Type") mustEqual "application/vnd.ms-excel;charset=utf-8"
      }
    }

    "set the Content-Type header if it's in lowercase in the headers map" in {
      get("/contentType?lcase=true") {
        header("Content-Type") mustEqual "application/vnd.ms-excel;charset=utf-8"
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

  "returning ActionResult" should {
    "defaults to call by value" in {
      get("/defaults-to-call-by-value") {
        body mustEqual "open"
      }
    }
  }
}
