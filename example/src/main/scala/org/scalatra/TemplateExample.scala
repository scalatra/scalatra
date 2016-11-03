package org.scalatra

import org.scalatra.scalate.ScalateSupport

import scala.xml.{ Node, Text }

object Template {

  def page(title: String, content: Seq[Node], url: String => String = identity _, head: Seq[Node] = Nil, scripts: Seq[String] = Seq.empty, defaultScripts: Seq[String] = Seq("/assets/js/jquery.min.js", "/assets/js/bootstrap.min.js")) = {
    <html lang="en">
      <head>
        <title>{ title }</title>
        <meta charset="utf-8"/>
        <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
        <meta name="description" content=""/>
        <meta name="author" content=""/>
        <!-- Le styles -->
        <link href={ url("/assets/css/bootstrap.css") } rel="stylesheet"/>
        <link href={ url("/assets/css/bootstrap-responsive.css") } rel="stylesheet"/>
        <link href={ url("/assets/css/syntax.css") } rel="stylesheet"/>
        <link href={ url("/assets/css/scalatra.css") } rel="stylesheet"/>
        <!-- Le HTML5 shim, for IE6-8 support of HTML5 elements -->
        <!--[if lt IE 9]>
            <script src="http://html5shim.googlecode.com/svn/trunk/html5.js"></script>
          <![endif]-->
        { head }
      </head>
      <body>
        <div class="navbar navbar-inverse navbar-fixed-top">
          <div class="navbar-inner">
            <div class="container">
              <a class="btn btn-navbar" data-toggle="collapse" data-target=".nav-collapse">
                <span class="icon-bar"></span>
                <span class="icon-bar"></span>
                <span class="icon-bar"></span>
              </a>
              <a class="brand" href="/">Scalatra Examples</a>
              <div class="nav-collapse collapse">
              </div><!--/.nav-collapse -->
            </div>
          </div>
        </div>
        <div class="container">
          <div class="content">
            <div class="page-header">
              <h1>{ title }</h1>
            </div>
            { content }
            <hr/>
            <a href={ url("/date/2009/12/26") }>date example</a><br/>
            <a href={ url("/form") }>form example</a><br/>
            <a href={ url("/upload") }>upload</a><br/>
            <a href={ url("/") }>hello world</a><br/>
            <a href={ url("/flash-map/form") }>flash scope</a><br/>
            <a href={ url("/login") }>login</a><br/>
            <a href={ url("/logout") }>logout</a><br/>
            <a href={ url("/basic-auth") }>basic auth</a><br/>
            <a href={ url("/filter-example") }>filter example</a><br/>
            <a href={ url("/cookies-example") }>cookies example</a><br/>
            <a href={ url("/atmosphere") }>atmosphere chat demo</a><br/>
          </div><!-- /content -->
        </div><!-- /container -->
        <!-- Le javascript
            ================================================== -->
        <!-- Placed at the end of the document so the pages load faster -->
        {
          (defaultScripts ++ scripts) map { pth =>
            <script type="text/javascript" src={ url(pth) }></script>
          }
        }
      </body>
    </html>
  }
}

class TemplateExample extends ScalatraServlet with FlashMapSupport with ScalateSupport {

  private def displayPage(title: String, content: Seq[Node]) = Template.page(title, content, url(_, includeServletPath = false))

  get("/date/:year/:month/:day") {
    displayPage("Scalatra: Date Example",
      <ul>
        <li>Year: { params("year") }</li>
        <li>Month: { params("month") }</li>
        <li>Day: { params("day") }</li>
      </ul>
      <pre>Route: /date/:year/:month/:day</pre>
    )
  }

  get("/form") {
    displayPage("Scalatra: Form Post Example",
      <form action={ url("/post") } method='POST'>
        Post something:<input name="submission" type='text'/>
        <input type='submit'/>
      </form>
      <pre>Route: /form</pre>
    )
  }

  post("/post") {
    displayPage("Scalatra: Form Post Result",
      <p>You posted: { params("submission") }</p>
      <pre>Route: /post</pre>
    )
  }

  get("/login") {
    (session.get("first"), session.get("last")) match {
      case (Some(first: String), Some(last: String)) =>
        displayPage("Scalatra: Session Example",
          <pre>You have logged in as: { first + "-" + last }</pre>
          <pre>Route: /login</pre>)
      case x =>
        displayPage("Scalatra: Session Example" + x.toString,
          <form action={ url("/login") } method='POST'>
            First Name:<input name="first" type='text'/>
            Last Name:<input name="last" type='text'/>
            <input type='submit'/>
          </form>
          <pre>Route: /login</pre>)
    }
  }

  post("/login") {
    (params("first"), params("last")) match {
      case (first: String, last: String) => {
        session("first") = first
        session("last") = last
        displayPage("Scalatra: Session Example",
          <pre>You have just logged in as: { first + " " + last }</pre>
          <pre>Route: /login</pre>)
      }
    }
  }

  get("/logout") {
    session.invalidate
    displayPage("Scalatra: Session Example",
      <pre>You have logged out</pre>
      <pre>Route: /logout</pre>)
  }

  get("/") {
    displayPage("Scalatra: Hello World",
      <h2>Hello world!</h2>
      <p>Referer: { (request referrer) map { Text(_) } getOrElse { <i>none</i> } }</p>
      <pre>Route: /</pre>)
  }

  get("/scalate") {
    val content = "this is some fake content for the web page"
    layoutTemplate("index.scaml", "content" -> content)
  }

  get("/flash-map/form") {
    displayPage("Scalatra: Flash Map Example",
      <span>Supports the post-then-redirect pattern</span>
      <br/>
      <form method="post">
        <label>Message: <input type="text" name="message"/></label><br/>
        <input type="submit"/>
      </form>)
  }

  post("/flash-map/form") {
    flash("message") = params.getOrElse("message", "")
    redirect("/flash-map/result")
  }

  get("/flash-map/result") {
    displayPage(
      title = "Scalatra: Flash  Example",
      content = <span>Message = { flash.getOrElse("message", "") }</span>
    )
  }

  post("/echo") {
    import org.scalatra.util.RicherString._
    params("echo").urlDecode
  }
}
