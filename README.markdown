About
=====

Step is a tiny Scala web framework inspired by [Sinatra](http://www.sinatrarb.com/) and originally based on some code I found on an [awesome blog post](http://www.riffraff.info/2009/4/11/step-a-scala-web-picoframework).

It could probably use a lot of work; it's my first Scala project. I welcome comments, pull requests, and issues.

Example
=======

    package com.thinkminimo.step

    class StepExample extends Step {

      // send a text/html content type back each time
      before() {
        content_type = "text/html"
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
    }


Quick Start
===========
1.   __Install Java__

    You'll need Java installed; I have it running with 1.5

2.   __Install simple-build-tool__

    Step uses sbt, a fantastic tool for building Scala programs.  For instructions, see [the sbt site](http://code.google.com/p/simple-build-tool/wiki/Setup)

3.   __Run sbt__

    In the directory you downloaded step to, run `sbt`.
    sbt will download core dependencies, and Scala itself if it needs too.

4.   __Download dependencies__

    At the sbt prompt, type `update`.  This will download the Servlet API and Jetty.

5.   __Try it out__

    At the sbt prompt, type `jetty-run`.  This will run step with the example on port 8080.

6.   __Navigate to http://localhost:8080__

    You should see "Hello world."  You can poke around the example code in src/main/scala/StepExample.scala
    to see what's going on.


Supported Methods
=================

*   __before__

    Run some block before a request is returned.

*   __get__

    Respond to a GET request.

    Specify the route to match, with parameters to store prefixed with : like Sinatra.
    "/match/this/path/and/save/:this" would match that GET request, and provide you with a
    params("this") in your block.

*   __post__

    Respond to a POST request.

    Posted variables are available in the `params` hash.

*   __delete__

    Respond to a DELETE request.

*   __put__

    Respond to a PUT request.


Miscellaneous
=============
While Step can be run standalone, you can also package it up in a .jar for use in other projects.  At the sbt prompt, type `package`.  For more information, see the [sbt site](http://code.google.com/p/simple-build-tool/)

Props
=====
I'd like to thank [this awesome dude](http://www.riffraff.info/) for the inspirational blog post, and Mark Harrah for help on the sbt mailing list and for creating sbt.  Ant+Ivy by itself was a total bitch.

Todo
====
* tests
* docs
* more tests
