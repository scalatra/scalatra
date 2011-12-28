package org.scalatra

import docs._

class DocumentExample extends ScalatraServlet with DocumentationSupport {

  val helloParam = Param(name = "hello", description = Some("The hello parameter"))
  val nameParam = Param(name = "name", description = Some("The name to be returned"))

  before(){
    contentType = "text/html"
  }

  get("/hello",
    name("hello world"),
    description("The hello world endpoint returns hello"),
    optionalParams(List(helloParam)),
    requiredParams(List(helloParam)),
    document(true)){
    "hello world"
  }

  get("/hi/:name") {
    "hi " + params("name")
  }

  get("/hello/:name", requiredParams(List(nameParam))) {
    "hello " + params("name")
  }

  get("/render", document(false)) {
    allDocumentedRoutesAsHtml()
  }


}