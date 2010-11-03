About
=====

Scalatra is a tiny Scala web framework inspired by [Sinatra](http://www.sinatrarb.com/) and originally based on some code I found on an [awesome blog post](http://www.riffraff.info/2009/4/11/step-a-scala-web-picoframework).  

Comments, issues, and pull requests are welcome.  Please also see the [scalatra-user](http://groups.google.com/group/scalatra-user) mailing list, or drop in on IRC at #scalatra on irc.freenode.org

Example
=======

    package org.scalatra

    class ScalatraExample extends ScalatraServlet {

      // send a text/html content type back each time
      before {
        contentType = "text/html"
      }

      // parse matching requests, saving things prefixed with ':' as params
      get("/date/:year/:month/:day") {
        <ul>
          <li>Year: {params("year")}</li>
          <li>Month: {params("month")}</li>
          <li>Day: {params("day")}</li>
        </ul>
      }

      // produce a simple HTML form
      get("/form") {
        <form action='/post' method='POST'>
          Post something: <input name='submission' type='text'/>
          <input type='submit'/>
        </form>
      }

      // handle POSTs from the form generated above
      post("/post") {
        <h1>You posted: {params("submission")}</h1>
      }

      // respond to '/' with a greeting
      get("/") {
        <h1>Hello world!</h1>
      }

      // send redirect headers
      get("/see_ya") {
        redirect("http://google.com")
      }

      // set a session var
      get("/set/:session_val") {
        session("val") = params("session_val")
        <h1>Session var set</h1>
      }

      // see session var
      get("/see") {
        session.getOrElse("val", "No session var set")
      }

      // Actions that return byte arrays render a binary response
      get("/report.pdf") {
        contentType = "application/pdf"
        val pdf = generatePdf()
        pdf.toBytes
      }

      notFound {
        response.setStatus(404)
        "Not found"
      }
    }


Quick Start
===========
1.   __Install Java__

    You'll need Java installed; I have it running with 1.5

2.   __Install simple-build-tool__

    Scalatra uses sbt (0.7 or above), a fantastic tool for building Scala programs.  For instructions, see [the sbt site](http://code.google.com/p/simple-build-tool/wiki/Setup)

3.   __Run sbt__

    In the directory you downloaded Scalatra to, run `sbt`.
    sbt will download core dependencies, and Scala itself if it needs to.

4.   __Download dependencies__

    At the sbt prompt, type `update`.  This will download required dependencies.

5.   __Try it out__

    At the sbt prompt, type `jetty-run`.  This will run Scalatra with the example servlet on port 8080.

6.   __Navigate to http://localhost:8080__

    You should see "Hello world."  You can poke around the example code in example/src/main/scala/TemplateExample.scala to see what's going on.


Maven Repository
================

To make usage of Scalatra as a dependency convenient, Maven hosting is now available courtesy of [Sonatype](https://docs.sonatype.com/display/NX/OSS+Repository+Hosting).

* [Releases](https://oss.sonatype.org/content/repositories/releases)
* [Snapshots](https://oss.sonatype.org/content/repositories/snapshots)

Supported Methods
=================

*   __before__

    Run some block before a request is returned.

*   __get(`path`)__

    Respond to a GET request.

    Specify the route to match, with parameters to store prefixed with : like Sinatra.
    "/match/this/path/and/save/:this" would match that GET request, and provide you with a
    params("this") in your block.

*   __post(`path`)__

    Respond to a POST request.

    Posted variables are available in the `params` hash.

*   __delete(`path`)__

    Respond to a DELETE request.

*   __put(`path`)__

    Respond to a PUT request.

*   __error__

    Run some block when an error is caught.  The error is available in the variable `caughtThrowable`.

*   __after__

    Run some block after the matching get/post/delete/put block is run.

Sessions
========
Session support has recently been added.  To see how to use sessions in your Scalatra apps, check out the test servlet, at core/src/test/scala/ScalatraTest.scala

Flash scope
===========
Flash scope is available by mixing in FlashScopeSupport, which provides a mutable map named `flash`.  Values put into flash scope during the current request are stored in the session through the next request and then discarded.  This is particularly useful for messages when using the [Post/Redirect/Get](http://en.wikipedia.org/wiki/Post/Redirect/Get) pattern.

File Upload Support
===================
File uploads are now supported.

File upload dependencies
------------------------
- scalatra-fileupload.jar
- commons-fileupload.jar
- commons-io.jar

Usage
-----
* Mix in `org.scalatra.fileupload.FileUploadSupport`.
* Be sure that your form has an enctype of `multipart/form-data`
* Uploaded files will be available in a map of `fileParams` or `fileMultiParams`

Example
-------

    import org.scalatra.ScalatraServlet
    import org.scalatra.fileupload.FileUploadSupport

    class MyApp extends ScalatraServlet with FileUploadSupport {
      get("/") {
        <form method="post" enctype="multipart/form-data">
          <input type="file" name="foo" />
          <input type="submit" />
        </form>
      }

      post("/") {
        processFile(fileParams("file"))
      }
    }

Scalate Integration
===================
Scalatra has experimental support for integration with [Scalate](http://scalate.fusesource.org).

Scalate Dependencies
--------------------
- scalatra-scalate.jar
- scalate-core-1.2.jar
- a [slf4j binding](http://www.slf4j.org/manual.html#binding); I like logback

Setup
-----
* Mix in ScalateSupport
* Create template in src/main/webapp
* Call renderTemplate with a path to the template (relative to webapp) and attributes

Example
-------

    import org.scalatra.ScalatraServlet
    import org.scalatra.scalate.ScalateSupport

    class MyApp extends ScalatraServlet with ScalateSupport {
      get("/") {
        renderTemplate("index.scaml", "content" -> "yada yada yada")
      }
    }

Testing Your Scalatra Applications
==================================

Scalatra includes a test framework for writing the unit tests for your Scalatra application.  The framework lets you send requests to your app and examine the response.  It can be mixed into the test framework of your choosing; integration with [ScalaTest](http://www.scalatest.org/) and [Specs](http://code.google.com/p/specs/) is already provided.  ScalatraTests supports HTTP GET/POST tests with or without request parameters and sessions.  For more examples, please refer to core/src/test/scala.

ScalaTest
---------

### Dependencies

- scalatra-scalatest

### Code

Mix in ShouldMatchers or MustMatchers to your taste...

    class MyScalatraServletTests extends ScalatraFunSuite with ShouldMatchers {
      // `MyScalatraServlet` is your app which extends ScalatraServlet
      addServlet(classOf[MyScalatraServlet], "/*")

      test("simple get") {
        get("/path/to/something") {
          status should equal (200)
          body should include ("hi!")
        }
      }
    }

Specs
-----

### Dependencies

- scalatra-specs

### Example

    object MyScalatraServletTests extends ScalatraSpecification {
      addServlet(classOf[MyScalatraServlet], "/*")
      
      "MyScalatraServlet when using GET" should {
        "/path/to/something should return 'hi!'" in {
          get("/") {
            status mustEqual(200)
            body mustEqual("hi!")
          }
        }
      }
    }                      

Other test frameworks
---------------------

### Dependencies
- scalatra-test

### Usage guide
Create an instance of org.scalatra.test.ScalatraTests.  Be sure to call `start()` and `stop()` before and after your test suite.

Authentication
==============

There is a new authentication middleware in the auth directory, to be documented soon.  See an example at [usage example](http://gist.github.com/660701).

To use it from an SBT project, add the following to your project:

    val auth = "org.scalatra" %% "scalatra-auth" % "2.0.0-SNAPSHOT"

Miscellaneous
=============
While Scalatra can be run standalone for testing and meddling, you can also package it up in a .jar for use in other projects.  At the sbt prompt, type `package`.  Scalatra's only dependency is a recent version of the servlet API. For more information, see the [sbt site](http://code.google.com/p/simple-build-tool/)

Migrating from Step to Scalatra
===============================
Scalatra was renamed from Step to Scalatra to avoid a naming conflict with (an unrelated web framework)[http://sourceforge.net/stepframework].  scalatra-1.2.0 is identical to step-1.2.0 with the following exceptions:

1. The package has changed from `com.thinkminimo.step` to `org.scalatra`.
1. The `Step` class has been renamed to `ScalatraServlet`.
1. All other `Step*` classes have been renamed to `Scalatra*`.

Props
=====

- [Gabriele Renzi](http://www.riffraff.info/) for the inspirational blog post and continual help
- [Ross A. Baker](http://www.rossabaker.com/) for porting to 2.8 and loads of other patches and help
- [Mark Harrah](http://github.com/harrah) for help on the sbt mailing list and for creating sbt. Ant+Ivy by itself was a total bitch.
- [Yusuke Kuoka](http://github.com/mumoshu) for adding sessions and header support
- [Miso Korkiakoski](http://github.com/mwing) for various patches.
- [Ivan Willig](http://github.com/iwillig) for his work on [Scalate](http://scalate.fusesource.org/) integration.
- [Hiram Chirino](http://hiramchirino.com) for Maven integration and the new name.
- [Phil Wills](http://github.com/philwills) for the path parser cleanup.
- [Ivan Porto Carrero](http://flanders.co.nz) for the authentication framework.
