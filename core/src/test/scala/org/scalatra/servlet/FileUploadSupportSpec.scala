package org.scalatra.servlet

import scala.collection.JavaConversions._
import org.scalatra.test.specs2.MutableScalatraSpec
import org.scalatra.ScalatraServlet
import java.io.File
import org.eclipse.jetty.servlet.ServletHolder
import javax.servlet.{MultipartConfigElement, ServletException}
import javax.servlet.http.HttpServlet

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

  post("/file-item-write") {
    val document = fileParams("document")
    val tempFile = File.createTempFile("scalatra-test-", document.name)
    document.write(tempFile)
    tempFile.deleteOnExit
    "file size: " + tempFile.length
  }

  error {
    case e => e.printStackTrace()
  }
}

class FileUploadSupportMaxSizeTestServlet extends ScalatraServlet with FileUploadSupport {
  configureMultipartHandling(MultipartConfig(
    maxFileSize = Some(1024),
    fileSizeThreshold = Some(1024*1024*1024)
  ))

  error {
    case e: SizeConstraintExceededException => {
      status = 413

      "too much!"
    }
  }

  post("/upload") {
    "ok"
  }
}

class FileUploadSupportSpec extends MutableScalatraSpec {

  mount(new FileUploadSupportSpecServlet, "/*")
  mount(new FileUploadSupportMaxSizeTestServlet, "/max-size/*")

  def postExample[A](f: => A): A = {
    val params = Map("param1" -> "one", "param2" -> "two")
    val files = Map(
      "text"   -> new File("core/src/test/resources/org/scalatra/servlet/lorem_ipsum.txt"),
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

  def multipartHeaders = {
    Map("Content-Type" -> "multipart/form-data; boundary=XyXyXy")
  }

  "POST with multipart/form-data" should {
//    "route correctly to action" in {
//      postExample {
//        (status must_== 200) and
//          (body must_== "post(/upload)")
//      }
//    }
//
//    "make multipart form params available through params" in {
//      postExample {
//        (header("param1") must_== "one") and
//          (header("param2") must_== "two")
//      }
//    }
//
//    "make query string params available from params" in {
//      postExample {
//        (header("qsparam1") must_== "three") and
//          (header("qsparam2") must_== "four")
//      }
//    }
//
//    "keep headers as they were in the request" in {
//      postExample {
//        (header("X-Header") must_== "I'm a header") and
//          (header("X-Header2") must_== "I'm another header")
//      }
//    }
//
//    "make all files available through fileParams" in {
//      postExample {
//        (header("File-text-Name") must_== "lorem_ipsum.txt") and
//          (header("File-text-Size") must_== "651") and
//          (header("File-text-SHA") must_== "b3572a890c5005aed6409cf81d13fd19f6d004f0") and
//          (header("File-binary-Name") must_== "smiley.png") and
//          (header("File-binary-Size") must_== "3432") and
//          (header("File-binary-SHA") must_== "0e777b71581c631d056ee810b4550c5dcd9eb856")
//      }
//    }
//
//    "make multiple files with [] syntax available through fileMultiParams" in {
//      postMultiExample {
//        (header("File-files[]0-Name") must_== "lorem_ipsum.txt") and
//          (header("File-files[]0-Size") must_== "651") and
//          (header("File-files[]0-SHA") must_== "b3572a890c5005aed6409cf81d13fd19f6d004f0") and
//          (header("File-files[]1-Name") must_== "smiley.png") and
//          (header("File-files[]1-Size") must_== "3432") and
//          (header("File-files[]1-SHA") must_== "0e777b71581c631d056ee810b4550c5dcd9eb856")
//      }
//    }
//
//    "make first file available of multiple file params through fileParams" in {
//      postMultiExample {
//        header("File-files[]-First") must_== "lorem_ipsum.txt"
//      }
//    }
//
//    "not make the fileParams available through params" in {
//      postExample {
//        (Option(header("text")) must_== None) and
//          (Option(header("binary")) must_== None)
//      }
//    }
//
//    "keep file params on pass" in {
//      postPass {
//        (header("File-text-Name") must_== "lorem_ipsum.txt") and
//          (header("File-text-Size") must_== "651") and
//          (header("File-text-SHA") must_== "b3572a890c5005aed6409cf81d13fd19f6d004f0")
//      }
//    }
//
//    "keep params on pass" in {
//      postPass {
//        (header("param1") must_== "one") and
//          (header("param2") must_== "two")
//      }
//    }

    "use default charset (UTF-8) for decoding form params if not excplicitly set to something else" in {
      val boundary = "XyXyXy"
      val reqBody  = ("--{boundary}\r\n" +
                      "Content-Disposition: form-data; name=\"utf8-string\"\r\n" +
                      "Content-Type: text/plain\r\n" +
                      "\r\n" +
                      "föo\r\n" +
                      "--{boundary}--\r\n").replace("{boundary}", boundary).getBytes("UTF-8")

      post("/params", headers = multipartHeaders, body = reqBody) {
        header("utf8-string") must_== "föo"
      }
    }
//
//    "use the charset specified in Content-Type header of a part for decoding form params" in {
//      val reqBody = ("--XyXyXy\r\n" +
//                     "Content-Disposition: form-data; name=\"latin1-string\"\r\n" +
//                     "Content-Type: text/plain; charset=ISO-8859-1\r\n" +
//                     "\r\n" +
//                     "äöööölfldflfldfdföödfödfödfåååååå\r\n" +
//                     "--XyXyXy--").getBytes("ISO-8859-1")
//
//      post("/params", headers = multipartHeaders, body = reqBody) {
//        header("latin1-string") must_== "äöööölfldflfldfdföödfödfödfåååååå"
//      }
//    }
  }


//  "POST with multipart/form-data and maxFileSize set" should {
//    "handle IllegalStateException by wrapping it as SizeConstraintExceededException handled by error handler" in {
//      post("/max-size/upload", Map(), Map("file" -> new File("core/src/test/resources/org/scalatra/servlet/smiley.png"))) {
//        (status mustEqual 413) and
//        (body mustEqual "too much!")
//      }
//    }
//
//    "allow file uploads smaller than the specified max file size" in {
//      post("/max-size/upload", Map(), Map("file" -> new File("core/src/test/resources/org/scalatra/servlet/lorem_ipsum.txt"))) {
//        body must_== "ok"
//      }
//    }
//  }
//
//  "regular POST" should {
//    "not be affected by FileUploadSupport handling" in {
//      post("/regular", Map("param1" -> "one", "param2" -> "two")) {
//        (header("param1") must_== "one") and
//        (header("param2") must_== "two")
//      }
//    }
//  }
//
//  "FileItem.write" should {
//    "know how to write to a File instance" in {
//      val params = Map()
//      val files = Map("document" -> new File("core/src/test/resources/org/scalatra/servlet/lorem_ipsum.txt"))
//
//      post("/file-item-write", params, files) {
//        body must_== "file size: 651"
//      }
//    }
//  }
}
