package org.scalatra.servlet

import scala.collection.JavaConversions._
import org.scalatra.test.specs2.MutableScalatraSpec
import org.scalatra.ScalatraServlet
import java.io.File
import org.eclipse.jetty.testing.HttpTester
import org.eclipse.jetty.servlet.ServletHolder
import javax.servlet.MultipartConfigElement
import org.specs2.execute.Pending

class FileUploadSupportSpecServlet extends ScalatraServlet with FileUploadSupport {
  def headersToHeaders() {
    request.getHeaderNames.filter(_.startsWith("X")).foreach(header =>
      response.setHeader(header, request.getHeader(header))
    )
  }

  def fileParamsToHeaders() {
    fileParams.foreach(fileParam => {
      response.setHeader("File-" + fileParam._1 + "-Name", fileParam._2.name)
      response.setHeader("File-" + fileParam._1 + "-Size", fileParam._2.size.toString)
      response.setHeader("File-" + fileParam._1 + "-SHA", DigestUtils.shaHex(fileParam._2.get()))
    })
  }

  def paramsToHeaders() {
    params.foreach(param =>
      response.setHeader(param._1, param._2)
    )
  }

  post("/upload") {
    headersToHeaders()
    paramsToHeaders()
    fileParamsToHeaders()

    "post(/upload)"
  }

  post("/params") {
    paramsToHeaders()

    "post(/params)"
  }

  post("/passUpload/*") {
    fileParamsToHeaders()
    paramsToHeaders()

    "post(/passUpload/*)"
  }

  post("/passUpload/file") {
    pass()
  }

  post("/uploadFileMultiParams") {
    fileMultiParams.foreach(file => {
      val name = file._1
      val items = file._2
      val first = fileParams(name)
      var i = 0

      response.setHeader("File-" + name + "-First", first.name)

      items.foreach(item => {
        response.setHeader("File-" + name + i + "-Name", item.name)
        response.setHeader("File-" + name + i + "-Size", item.size.toString)
        response.setHeader("File-" + name + i + "-SHA", DigestUtils.shaHex(item.get()))

        i += 1
      })
    })

    "post(/uploadFileMultiParams)"
  }

  post("/regular") {
    paramsToHeaders()
  }
}

class FileUploadSupportMaxSizeTestServlet extends ScalatraServlet with FileUploadSupport {
  error {
    case e: IllegalStateException => {
      status = 413

      "too much!"
    }
  }

  post("/upload") {
    "ok"
  }
}

class FileUploadSupportSpec extends MutableScalatraSpec {
  addServlet(classOf[FileUploadSupportSpecServlet], "/*")

  // this is needed because embedded Jetty doesn't support
  // reading annotations
  val holder = new ServletHolder(new FileUploadSupportMaxSizeTestServlet)
  holder.getRegistration.setMultipartConfig(new MultipartConfigElement("", 1024, -1, 1024*1024*1024))
  servletContextHandler.addServlet(holder, "/max-size/*")

  def postExample[A](f: => A): A = {
    val params = Map("param1" -> "one", "param2" -> "two")
    val files = Map(
      "text" -> new File("core/src/test/resources/org/scalatra/servlet/lorem_ipsum.txt"),
      "binary" -> new File("core/src/test/resources/org/scalatra/servlet/smiley.png")
    )

    val headers = Map(
      "X-Header" -> "I'm a header",
      "X-Header2" -> "I'm another header"
    )

    post("/upload?qsparam1=three&qsparam2=four", params, files, headers) {
      f
    }
  }

  def postMultiExample[A](f: => A): A = {
    val files =
      ("files[]", new File("core/src/test/resources/org/scalatra/servlet/lorem_ipsum.txt")) ::
        ("files[]", new File("core/src/test/resources/org/scalatra/servlet/smiley.png")) :: Nil

    post("/uploadFileMultiParams", Map(), files) {
      f
    }
  }

  def postPass[A](f: => A): A = {
    val params = Map("param1" -> "one", "param2" -> "two")
    val files = Map("text" -> new File("core/src/test/resources/org/scalatra/servlet/lorem_ipsum.txt"))

    post("/passUpload/file", params, files) {
      f
    }
  }

  def multipartResponse(path: String, file: String = "multipart_request.txt") = {
    // TODO We've had problems with the tester not running as iso-8859-1, even if the
    // request really isn't iso-8859-1.  This is a hack, but this hack passes iff the
    // browser behavior is correct.
    val req = new String(
      (new String(
        org.scalatra.util.io.readBytes(getClass.getResourceAsStream(file)
      )).replace("${PATH}", path)).getBytes, "iso-8859-1")
    
    val res = new HttpTester("iso-8859-1")
    res.parse(tester.getResponses(req))
    res
  }

  "POST with multipart/form-data" should {
    "route correctly to action" in {
      postExample {
        (status must_== 200) and
          (body must_== "post(/upload)")
      }
    }

    "make multipart form params available through params" in {
      postExample {
        (header("param1") must_== "one") and
          (header("param2") must_== "two")
      }
    }

    "make query string params available from params" in {
      postExample {
        (header("qsparam1") must_== "three") and
          (header("qsparam2") must_== "four")
      }
    }

    "keep headers as they were in the request" in {
      postExample {
        (header("X-Header") must_== "I'm a header") and
          (header("X-Header2") must_== "I'm another header")
      }
    }

    "make all files available through fileParams" in {
      postExample {
        (header("File-text-Name") must_== "lorem_ipsum.txt") and
          (header("File-text-Size") must_== "651") and
          (header("File-text-SHA") must_== "b3572a890c5005aed6409cf81d13fd19f6d004f0") and
          (header("File-binary-Name") must_== "smiley.png") and
          (header("File-binary-Size") must_== "3432") and
          (header("File-binary-SHA") must_== "0e777b71581c631d056ee810b4550c5dcd9eb856")
      }
    }

    "make multiple files with [] syntax available through fileMultiParams" in {
      postMultiExample {
        (header("File-files[]0-Name") must_== "lorem_ipsum.txt") and
          (header("File-files[]0-Size") must_== "651") and
          (header("File-files[]0-SHA") must_== "b3572a890c5005aed6409cf81d13fd19f6d004f0") and
          (header("File-files[]1-Name") must_== "smiley.png") and
          (header("File-files[]1-Size") must_== "3432") and
          (header("File-files[]1-SHA") must_== "0e777b71581c631d056ee810b4550c5dcd9eb856")
      }
    }

    "make first file available of multiple file params through fileParams" in {
      postMultiExample {
        header("File-files[]-First") must_== "lorem_ipsum.txt"
      }
    }

    "not make the fileParams available through params" in {
      postExample {
        (Option(header("text")) must_== None) and
          (Option(header("binary")) must_== None)
      }
    }

    "keep file params on pass" in {
      postPass {
        (header("File-text-Name") must_== "lorem_ipsum.txt") and
          (header("File-text-Size") must_== "651") and
          (header("File-text-SHA") must_== "b3572a890c5005aed6409cf81d13fd19f6d004f0")
      }
    }

    "keep params on pass" in {
      postPass {
        (header("param1") must_== "one") and
          (header("param2") must_== "two")
      }
    }

    "use default charset (UTF-8) for decoding form params if not explicitly set to something else" in {
      val res = multipartResponse("/params")
      res.header("utf8-string") must_== "föo"
    }

    "use the charset specified in Content-Type header of a part for decoding form params" in {
      val res = multipartResponse("/params", "multipart_request_charset_handling.txt")
      res.header("latin1-string") must_== "äöööölfldflfldfdföödfödfödfåååååå"
    }
  }

  "POST with multipart/form-data and maxFileSize set" should {
    "handle too large file by throwing IllegalStateException error handled by the default error handler" in {
      Pending("Waiting for Jetty 8.1.3")
    }

    "allow file uploads smaller than the specified max file size" in {
      post("/max-size/upload", Map(), Map("file" -> new File("core/src/test/resources/org/scalatra/servlet/lorem_ipsum.txt"))) {
        body must_== "ok"
      }
    }
  }

  "regular POST" should {
    "not be affected by FileUploadSupport handling" in {
      post("/regular", Map("param1" -> "one", "param2" -> "two")) {
        (header("param1") must_== "one") and
          (header("param2") must_== "two")
      }
    }
  }
}
