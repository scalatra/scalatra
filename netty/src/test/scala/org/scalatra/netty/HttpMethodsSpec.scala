package org.scalatra
package netty

import test.specs2.ScalatraSpec

class HttpMethodsApp extends ScalatraApp {

  get("/hello/:param") {
    params('param)
  }

  get("/query") {
    params('val1) + " " + params('val2)
  }

  post("/urlencode") {
    params('first) + " " + params('last)
  }

  post("/multipart") {
    if (request.contentType.map(_.toLowerCase.startsWith("multipart/form-data")) getOrElse false)
      params('first) + " " + params('last)
    else "failed"
  }

  private def readFiles(separator: String = "") = request.files map (_.string) mkString separator
  post("/upload") {
    readFiles()
  }

  post("/upload-multi") {
    readFiles("\n")
  }

  put("/update") {
    params("first") + " " + params("last")
  }

  put("/update-multipart") {
    if (request.contentType.map(_.toLowerCase.startsWith("multipart/form-data")) getOrElse false)
      params('first) + " " + params('last)
    else "failed"
  }

  put("/update-upload") {
    readFiles()
  }

  put("/update-upload-multi") {
    readFiles("\n")
  }

  delete("/delete/:id") {
    params("id")
  }

  get("/some") { // get also does HEAD
    "head success"
  }

  options("/options") {
    "options success"
  }

  patch("/patching") {
    params("first") + " " + params("last")
  }

  patch("/patching-multipart") {
    if (request.contentType.map(_.toLowerCase.startsWith("multipart/form-data")) getOrElse false)
      params('first) + " " + params('last)
    else "failed"
  }


  patch("/patch-upload") {
    readFiles()
  }

  patch("/patch-upload-multi") {
    readFiles("\n")
  }


}

class HttpMethodsSpec extends ScalatraSpec with NettyBackend {

  mount(new HttpMethodsApp)

  def is =
    "The HttpMethod support should" ^
      "allow get requests with path params" ! getHelloParam ^
      "allow get requests with query params" ! getQuery ^
      "allow head requests" ! getHead ^
      "allow options requests" ! getOptions ^
      "allow delete requests" ! deleteRequest ^
      "allow url encoded posts" ! formEncodedPosts ^
      "allow multipart encoded posts" ! multipartEncodedPosts ^
      "allow single file post upload" ! singleFilePost ^
      "allow multi file post upload" ! multiFilePost ^
      "allow url encoded puts" ! formEncodedPuts ^
      "allow multipart encoded puts" ! multipartEncodedPuts ^
      "allow single file put upload" ! singleFilePut ^
      "allow multi file put upload" ! multiFilePut ^
      "allow url encoded patches" ! formEncodedPatches ^
      "allow multipart encoded patches" ! multipartEncodedPatches ^
      "allow single file patch upload" ! singleFilePatch ^
      "allow multi file patch upload" ! multiFilePatch ^
    end


  val smallExpected1 = "filecontent: hello"
  val smallExpected2 = "gzipcontent: hello"
  val smallExpected3 = "filecontent: hello2"
  val text1 = "textfile.txt"
  val gzipText = "gzip.txt.gz"
  val text2 = "textfile2.txt"

  def singleFilePost = {
    post("/upload", classpathFile(text1)) {
      response.body must_== smallExpected1
    }
  }

  def multiFilePost = {
    post("/upload-multi", Seq(classpathFile(text1), classpathFile(text2))) {
      response.body must_== (smallExpected1 + "\n" + smallExpected3)
    }
  }

  def formEncodedPosts = {
    post("/urlencode", "first" -> "hello", "last" -> "world") {
      response.body must_== "hello world"
    }
  }

  def multipartEncodedPosts = {
    post("/multipart", Map("first" -> "hello2", "last" -> "world2"), headers = Map("Content-Type" -> "multipart/form-data") ) {
      response.body must_== "hello2 world2"
    }
  }

  def formEncodedPuts = {
    put("/update", "first" -> "hello", "last" -> "world") {
      response.body must_== "hello world"
    }
  }

  def multipartEncodedPuts = {
    put("/update-multipart", Map("first" -> "hello2", "last" -> "world2"), Map("Content-Type" -> "multipart/form-data") ) {
      response.body must_== "hello2 world2"
    }
  }

  def singleFilePut = {
    put("/update-upload", classpathFile(text1)) {
      response.body must_== smallExpected1
    }
  }

  def multiFilePut = {
    put("/update-upload-multi", Seq(classpathFile(text1), classpathFile(text2))) {
      response.body must_== (smallExpected1 + "\n" + smallExpected3)
    }
  }

  def singleFilePatch = {
    patch("/patch-upload", classpathFile(text1)) {
      response.body must_== smallExpected1
    }
  }

  def multiFilePatch = {
    patch("/patch-upload-multi", Seq(classpathFile(text1), classpathFile(text2))) {
      response.body must_== (smallExpected1 + "\n" + smallExpected3)
    }
  }

  def formEncodedPatches = {
    patch("/patching", "first" -> "hello", "last" -> "world") {
      response.body must_== "hello world"
    }
  }

  def multipartEncodedPatches = {
    patch("/patching-multipart", Map("first" -> "hello2", "last" -> "world2"), Map("Content-Type" -> "multipart/form-data") ) {
      response.body must_== "hello2 world2"
    }
  }

  def getHelloParam = {
    get("/hello/world") {
      response.statusCode must_== 200
      response.body must_== "world"
    }
  }

  def getQuery = {
    get("/query", Map("val1" -> "hello", "val2" -> "world")) {
      response.statusCode must_== 200
      response.body must_== "hello world"
    }
  }

  def getHead = {
    head("/some") {
      response.statusCode must_== 200
    }
  }

  def getOptions = {
    options("/options") {
      response.body must_== "options success"
    }
  }

  def deleteRequest = {
    deleteReq("/delete/blah") {
      response.body must_== "blah"
    }
  }
}