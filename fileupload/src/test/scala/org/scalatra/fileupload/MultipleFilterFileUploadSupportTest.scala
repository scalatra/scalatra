package org.scalatra
package fileupload

import java.io.File

import org.scalatra.test.scalatest.ScalatraFunSuite

class MultipleFilterFileUploadSupportTest extends ScalatraFunSuite {
  addFilter(new ScalatraFilter with FileUploadSupport {
    post("/some-other-url-with-file-upload-support") {}
  }, "/*")
  addFilter(new ScalatraFilter with FileUploadSupport {
    post("/multipart") {
      fileParams.get("file") foreach { file => response.setHeader("file", new String(file.get)) }
    }
  }, "/*")

  test("keeps input parameters on multipart request") {
    post("/multipart", params = Map(), files = Map("file" -> new File("fileupload/src/test/resources/org/scalatra/fileupload/one.txt"))) {
      header("file") should equal("one")
    }
  }
}
