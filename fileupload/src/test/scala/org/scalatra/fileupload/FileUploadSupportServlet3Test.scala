package org.scalatra.fileupload

import scala.collection.JavaConversions._
import org.scalatra.test.specs2.MutableScalatraSpec
import org.scalatra.ScalatraServlet
import java.io.File

class FileUploadSupportServlet3TestServlet extends ScalatraServlet with FileUploadSupportServlet3 {
  post("/upload") {
    params.foreach(param =>
      response.setHeader(param._1, param._2)
    )

    request.getHeaderNames.filter(_.startsWith("X")).foreach(header =>
      response.setHeader(header, request.getHeader(header))
    )

    fileParams.foreach(fileParam => {
      response.setHeader("File-" + fileParam._1 + "-Name", fileParam._2.name)
      response.setHeader("File-" + fileParam._1 + "-Size", fileParam._2.size.toString)
      response.setHeader("File-" + fileParam._1 + "-SHA", DigestUtils.shaHex(fileParam._2.bytes))
    })

    "post(/upload)"
  }
}

class FileUploadSupportServlet3Test extends MutableScalatraSpec {
  addServlet(classOf[FileUploadSupportServlet3TestServlet], "/*")
  def postExample[A](f: => A): A = {
    val params = Map("param1" -> "one", "param2" -> "two")
    val files  = Map(
      "text"   -> new File("fileupload/src/test/resources/org/scalatra/fileupload/lorem_ipsum.txt"),
      "binary" -> new File("fileupload/src/test/resources/org/scalatra/fileupload/smiley.png")
    )

    val headers = Map(
      "X-Header"  -> "I'm a header",
      "X-Header2" -> "I'm another header"
    )

    post("/upload?qsparam1=three&qsparam2=four", params, files, headers) { f }
  }

  "POST with multipart/form-data" should {
    "routes correctly to action" in {
      postExample {
        status must_== 200
        body must_== "post(/upload)"
      }
    }

    "makes multipart form params available from params" in {
      postExample {
        header("param1") must_== "one"
        header("param2") must_== "two"
      }
    }

    "makes querystring params available from params" in {
      postExample {
        header("qsparam1") must_== "three"
        header("qsparam2") must_== "four"
      }
    }

    "does not twiddle with the headers" in {
      postExample {
        header("X-Header")  must_== "I'm a header"
        header("X-Header2") must_== "I'm another header"
      }
    }

    "makes files available through fileParams" in {
      postExample {
        header("File-text-Name") must_== "lorem_ipsum.txt"
        header("File-text-Size") must_== "651"
        header("File-text-SHA")  must_== "b3572a890c5005aed6409cf81d13fd19f6d004f0"

        header("File-binary-Name") must_== "smiley.png"
        header("File-binary-Size") must_== "3432"
        header("File-binary-SHA")  must_== "0e777b71581c631d056ee810b4550c5dcd9eb856"
      }
    }
  }
}
