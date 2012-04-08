package org.scalatra.fileupload

import scala.collection.JavaConversions._
import org.scalatra.test.specs2.MutableScalatraSpec
import org.scalatra.ScalatraServlet
import java.io.File

class FileUploadSupportServlet3TestServlet extends ScalatraServlet with FileUploadSupportServlet3 {
  def headersToHeaders() {
    request.getHeaderNames.filter(_.startsWith("X")).foreach(header =>
      response.setHeader(header, request.getHeader(header))
    )
  }

  def fileParamsToHeaders() {
    fileParams.foreach(fileParam => {
      response.setHeader("File-" + fileParam._1 + "-Name", fileParam._2.name)
      response.setHeader("File-" + fileParam._1 + "-Size", fileParam._2.size.toString)
      response.setHeader("File-" + fileParam._1 + "-SHA", DigestUtils.shaHex(fileParam._2.bytes))
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
      val name   = file._1
      val items  = file._2
      val first  = fileParams(name)
      var i     = 0

      response.setHeader("File-" + name + "-First", first.name)

      items.foreach(item => {
        response.setHeader("File-" + name + i + "-Name", item.name)
        response.setHeader("File-" + name + i + "-Size", item.size.toString)
        response.setHeader("File-" + name + i+ "-SHA", DigestUtils.shaHex(item.bytes))

        i += 1
      })
    })

    "post(/uploadFileMultiParams)"
  }

  post("/regular") {
    paramsToHeaders()
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

  def postMultiExample[A](f: => A): A = {
    val files  =
      ("files[]", new File("fileupload/src/test/resources/org/scalatra/fileupload/lorem_ipsum.txt")) ::
      ("files[]", new File("fileupload/src/test/resources/org/scalatra/fileupload/smiley.png")) :: Nil

    post("/uploadFileMultiParams", Map(), files) { f }
  }

  def postPass[A](f: => A): A = {
    val params = Map("param1" -> "one", "param2" -> "two")
    val files  = Map("text" -> new File("fileupload/src/test/resources/org/scalatra/fileupload/lorem_ipsum.txt"))

    post("/passUpload/file", params, files) { f }
  }

  "POST with multipart/form-data" should {
    "route correctly to action" in {
      postExample {
        status must_== 200
        body must_== "post(/upload)"
      }
    }

    "make multipart form params available through params" in {
      postExample {
        header("param1") must_== "one"
        header("param2") must_== "two"
      }
    }

    "make query string params available from params" in {
      postExample {
        header("qsparam1") must_== "three"
        header("qsparam2") must_== "four"
      }
    }

    "keep headers as they were in the request" in {
      postExample {
        header("X-Header")  must_== "I'm a header"
        header("X-Header2") must_== "I'm another header"
      }
    }

    "make all files available through fileParams" in {
      postExample {
        header("File-text-Name") must_== "lorem_ipsum.txt"
        header("File-text-Size") must_== "651"
        header("File-text-SHA")  must_== "b3572a890c5005aed6409cf81d13fd19f6d004f0"

        header("File-binary-Name") must_== "smiley.png"
        header("File-binary-Size") must_== "3432"
        header("File-binary-SHA")  must_== "0e777b71581c631d056ee810b4550c5dcd9eb856"
      }
    }

    "make multiple files with [] syntax available through fileMultiParams" in {
      postMultiExample {
        header("File-files[]0-Name") must_== "lorem_ipsum.txt"
        header("File-files[]0-Size") must_== "651"
        header("File-files[]0-SHA")  must_== "b3572a890c5005aed6409cf81d13fd19f6d004f0"

        header("File-files[]1-Name") must_== "smiley.png"
        header("File-files[]1-Size") must_== "3432"
        header("File-files[]1-SHA")  must_== "0e777b71581c631d056ee810b4550c5dcd9eb856"
      }
    }

    "make first file available of multiple file params through fileParams" in {
      postMultiExample {
        header("File-files[]-First") must_== "lorem_ipsum.txt"
      }
    }

    "not make the fileParams available through params" in {
      postExample {
        Option(header("text")) must_== None
        Option(header("binary")) must_== None
      }
    }

    "keep file params on pass" in {
      postPass {
        header("File-text-Name") must_== "lorem_ipsum.txt"
        header("File-text-Size") must_== "651"
        header("File-text-SHA")  must_== "b3572a890c5005aed6409cf81d13fd19f6d004f0"
      }
    }

    "keep params on pass" in {
      postPass {
        header("param1") must_== "one"
        header("param2") must_== "two"
      }
    }
  }

  "regular POST" should {
    "not be affected by FileUploadSupport handling" in {
      post("/regular", Map("param1" -> "one", "param2" -> "two")) {
        header("param1") must_== "one"
        header("param2") must_== "two"
      }
    }
  }
}
