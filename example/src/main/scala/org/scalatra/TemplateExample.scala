package org.scalatra

import scala.xml.{Text, Node}
import org.apache.commons.io.IOUtils
import fileupload.FileUploadSupport
import scalate.ScalateSupport

class TemplateExample extends ScalatraServlet with UrlSupport /*with FileUploadSupport*/ with FlashMapSupport with ScalateSupport {

  object Template {

    def style() =
      """
      pre { border: 1px solid black; padding: 10px; }
      body { font-family: Helvetica, sans-serif; }
      h1 { color: #8b2323 }
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
          <a href={url("/date/2009/12/26")}>date example</a>
          <a href={url("/form")}>form example</a>
          <a href={url("/upload")}>upload</a>
          <a href={url("/")}>hello world</a>
          <a href={url("/flash-map/form")}>flash scope</a>
          <a href={url("/login")}>login</a>
          <a href={url("/logout")}>logout</a>
          <a href={url("/filter-example")}>filter example</a>
          <a href={url("/cookies-example")}>cookies example</a>
          <a href={url("/chat")}>chat demo</a>
          <a href={url("/atmo_chat.html")}>Atmosphere chat demo</a>
          <a href={url("/chat_30.html")}>Servlet 3.0 async chat demo</a>
        </body>
      </html>
    }
  }

  before() {
    contentType = "text/html"
  }

  get("/date/:year/:month/:day") {
    Template.page("Scalatra: Date Example",
    <ul>
      <li>Year: {params("year")}</li>
      <li>Month: {params("month")}</li>
      <li>Day: {params("day")}</li>
    </ul>
    <pre>Route: /date/:year/:month/:day</pre>
    )
  }

  get("/form") {
    Template.page("Scalatra: Form Post Example",
    <form action={url("/post")} method='POST'>
      Post something: <input name="submission" type='text'/>
      <input type='submit'/>
    </form>
    <pre>Route: /form</pre>
    )
  }

  post("/post") {
    Template.page("Scalatra: Form Post Result",
    <p>You posted: {params("submission")}</p>
    <pre>Route: /post</pre>
    )
  }

  get("/login") {
    (session.get("first"), session.get("last")) match {
      case (Some(first:String), Some(last:String)) =>
        Template.page("Scalatra: Session Example",
        <pre>You have logged in as: {first + "-" + last}</pre>
        <pre>Route: /login</pre>
        )
      case x:AnyRef =>
        Template.page("Scalatra: Session Example" + x.toString,
        <form action={url("/login")} method='POST'>
        First Name: <input name="first" type='text'/>
        Last Name: <input name="last" type='text'/>
        <input type='submit'/>
        </form>
        <pre>Route: /login</pre>
        )
    }
  }

  get("/echoclient") {
    Template.page("Scalatra: Echo Server Client Example",
      <pre>
        <script type="text/javascript" src="/js/json.js" ></script>
        <script type="text/javascript" src="/socket.io/socket.io.js"></script>
        {"var socket = new io.Socket(null, { port: 8080, rememberTransport: false });"}
        {"""socket.on("message", function(messageType, data) { console.log(data) });"""}
        {"socket.connect();"}
        {"""socket.send("hello");"""}
      </pre>
    )
  }

  get("/chat") {
    layoutTemplate("chat.ssp")
  }

  post("/login") {
    (params("first"), params("last")) match {
      case (first:String, last:String) => {
        session("first") = first
	session("last") = last
        Template.page("Scalatra: Session Example",
        <pre>You have just logged in as: {first + " " + last}</pre>
        <pre>Route: /login</pre>
        )
      }
    }
  }

  get("/logout") {
    session.invalidate
    Template.page("Scalatra: Session Example",
    <pre>You have logged out</pre>
    <pre>Route: /logout</pre>
    )
  }

  get("/") {
    Template.page("Scalatra: Hello World",
    <h2>Hello world!</h2>
    <p>Referer: { (request referrer) map { Text(_) } getOrElse { <i>none</i> }}</p>
    <pre>Route: /</pre>
    )
  }

  get("/scalate") {
    val content = "this is some fake content for the web page"
    layoutTemplate("index.scaml", "content"-> content)
  }

  get("/upload") {
    Template.page("Scalatra: Session Example",
    <form method="post" enctype="multipart/form-data">
      Upload a file.  Its contents will be displayed in the browser.<br />
      <label>File: <input type="file" name="file" /></label><br />
      <input type="submit" />
    </form>
    )
  }

  /*
  post("/upload") {
    contentType = "text/plain"
    fileParams.get("file") foreach { file => IOUtils.copy(file.getInputStream, response.getOutputStream) }
  }
  */

  get("/flash-map/form") {
    Template.page("Scalatra: Flash Map Example",
    <span>Supports the post-then-redirect pattern</span><br />
    <form method="post">
      <label>Message: <input type="text" name="message" /></label><br />
      <input type="submit" />
    </form>
    )
  }

  post("/flash-map/form") {
    flash("message") = params.getOrElse("message", "")
    redirect("/flash-map/result")
  }

  get("/flash-map/result") {
    Template.page("Scalatra: Flash  Example",
    <span>Message = {flash.getOrElse("message", "")}</span>
    )
  }

  protected def contextPath = request.getContextPath

  post("/echo") {
    java.net.URLEncoder.encode(params("echo"), "UTF-8")
  }
}
