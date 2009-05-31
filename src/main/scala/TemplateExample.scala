package com.thinkminimo.step
import scala.xml.Node

object Template {

  def style() = 
    """
    pre { border: 1px solid black; padding: 10px; } 
    body { font-family: Helvetica, sans-serif; } 
    h1 { color: #8b2323 }"
    """

  def page(title:String, content:Seq[Node]) = {
    <html>
      <head>
        <title>{ title }</title>
        <style>{ Template.style }</style>
      </head>
      <body>
        <h1>{ title }</h1>
        { content }
        <hr/>
        <a href='/date/2009/12/26'>date example</a>
        <a href='/form'>form example</a>
        <a href='/'>hello world</a>
      </body>
    </html>
  }
}

class TemplateExample extends Step {

  before() {
    contentType = "text/html"
  }

  get("/date/:year/:month/:day") {
    Template.page("Step: Date Example", 
    <ul>
      <li>Year: {params("year")}</li>
      <li>Month: {params("month")}</li>
      <li>Day: {params("day")}</li>
    </ul>
    <pre>Route: /date/:year/:month/:day</pre>
    )
  }

  get("/form") {
    Template.page("Step: Form Post Example",
    <form action='/post' method='POST'>
      Post something: <input name='submission' type='text'/>
      <input type='submit'/>
    </form>
    <pre>Route: /form</pre>
    )
  }

  post("/post") {
    Template.page("Step: Form Post Result",
    <p>You posted: {params("submission")}</p>
    <pre>Route: /post</pre>
    )
  }

  get("/") {
    Template.page("Step: Hello World",
    <h2>Hello world!</h2>
    <pre>Route: /</pre>
    )
  }
}
