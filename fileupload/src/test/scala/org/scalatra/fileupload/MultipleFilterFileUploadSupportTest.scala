package org.scalatra
package fileupload

import org.scalatest.matchers.ShouldMatchers
import test.scalatest.ScalatraFunSuite
import org.mortbay.jetty.testing.{ServletTester, HttpTester}
import org.apache.commons.io.IOUtils
class MultipleFilterFileUploadSupportTest extends ScalatraFunSuite with ShouldMatchers {
  addFilter(new ScalatraFilter with FileUploadSupport {
    post("/some-other-url-with-file-upload-support") {}
  }, "/*")
  addFilter(new ScalatraFilter with FileUploadSupport {
    post("/multipart") {
      fileParams.get("file") foreach { file => response.setHeader("file", new String(file.get)) }
    }
  }, "/*")

  test("keeps input parameters on multipart request") {
    val request = IOUtils.toString(getClass.getResourceAsStream("multipart_request.txt"))
      .replace("${PATH}", "/multipart")
    val response = new HttpTester
    response.parse(tester.getResponses(request))

    response.getHeader("file") should equal ("one")
  }
}