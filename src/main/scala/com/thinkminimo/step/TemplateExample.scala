package com.thinkminimo.step

import xml.{Text, Node}

class TemplateExample extends Step with UrlSupport {

  before {
    contentType = "text/html"
  }

  get("/") {
	val content = "this is some fake content for the web page"
	Template.render("index.scaml",("content"-> content))
    )
  }

  protected def contextPath = request.getContextPath   
}
