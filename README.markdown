Scalatra is a tiny, [Sinatra](http://www.sinatrarb.com/)-like web framework for [Scala](http://www.scala-lang.org/).

## Example

    import org.scalatra._

    class ScalatraExample extends ScalatraServlet {
      get("/") {
        <h1>Hello, world!</h1>
      }
    }

## Quick start (SBT 0.7.x)

  1. Git-clone the prototype.  Alternatively, download and extract a [tarball](https://github.com/scalatra/scalatra-sbt-prototype/tarball/master) or [zip](https://github.com/scalatra/scalatra-sbt-prototype/zipball/master).

         $ git clone git://github.com/scalatra/scalatra-sbt-prototype.git my-app

  2. Change directory into your clone.

         $ cd my-app

  3. Launch [SBT](http://code.google.com/p/simple-build-tool).

         $ sbt

  4. Fetch the dependencies.

         > update

  5. Start Jetty, enabling continuous compilation and reloading.

         > jetty-run
         > ~prepare-webapp

  6. Browse to http://localhost:8080/.

  7. Start hacking on `src/main/scala/MyScalatraFilter.scala`.

Note: if you keep getting frequent OutOfMemory errors from `sbt` you can try changing its script as described in [this document](http://www.assembla.com/wiki/show/liftweb/Using_SBT) so that it executes this command line:

     java -XX:+CMSClassUnloadingEnabled -XX:MaxPermSize=256m -Xmx512M -Xss2M -jar `dirname $0`/sbt-launch.jar "$@"

Note 2: if you already have a checkout, and after a `git pull` the build fails, try to explicitly run the `update` and `clean` sbt tasks before running `compile`. 

### Alternative Maven quickstart.

See the [simple-scalatra-archetype](http://github.com/Srirangan/simple-scalatra-archetype).

## Quick start (SBT 0.10.x)

### Setup (one time)

  1. Install [conscript](https://github.com/n8han/conscript#readme)
  	
         $ curl https://raw.github.com/n8han/conscript/master/setup.sh | sh

  2. Install [giter8](https://github.com/n8han/giter8)
  
         $ ~/bin/cs n8han/giter8
	
### Scalatra template
  
  3. Giter8 the Scalatra template
  
         $ ~/bin/g8 scalatra/scalatra-sbt
	
  4. Change directory into your clone.

         $ cd my-app
	
  5. Launch [SBT](https://github.com/harrah/xsbt).

         $ sbt

  6. Start Jetty, enabling continuous compilation and reloading.

         > jetty-run
         > ~prepare-webapp

  7. Browse to http://localhost:8080/.

  8. Start hacking on `src/main/scala/*your name here*.scala`.
	 
## Community

### Mailing list

The [scalatra-user](http://groups.google.com/group/scalatra-user) mailing list is open to anybody.  It is the best place to ask questions, so everybody can see the answer.

### IRC channel

For those topics that are easier to discuss in real time, or just to hang out with some fun people, join us on the #scalatra channel on irc.freenode.org.

## Routes

In Scalatra, a route is an HTTP method paired with a URL matching pattern.

    get("/") { 
      // show something 
    }
   
    post("/") { 
      // submit/create something 
    }

    put("/") { 
      // update something 
    }

    delete("/") { 
      // delete something 
    }

### Route order

The first matching route is invoked.  Routes are matched from the bottom up.  _This is the opposite of Sinatra._  Route definitions are executed as part of a Scala constructor; by matching from the bottom up, routes can be overridden in child classes.

### Path patterns

Path patterns add parameters to the `params` map.  Repeated values are accessible through the `multiParams` map.

#### Named parameters

Route patterns may include named parameters:

    get("/hello/:name") {
      // Matches "GET /hello/foo" and "GET /hello/bar"
      // params("name") is "foo" or "bar"
      <p>Hello, {params("name")}</p>
    } 

#### Wildcards

Route patterns may also include wildcard parameters, accessible through the `splat` key.

    get("/say/*/to/*) {
      // Matches "GET /say/hello/to/world"
      multiParams("splat") // == Seq("hello", "world")
    }

    get("/download/*.*) {
      // Matches "GET /download/path/to/file.xml"
      multiParams("splat") // == Seq("path/to/file", "xml")
    }

#### Regular expressions

The route matcher may also be a regular expression.  Capture groups are accessible through the `captures` key.

    get("""^\/f(.*)/b(.*)""".r) {
      // Matches "GET /foo/bar"
      multiParams("captures") // == Seq("oo", "ar") 
    }

#### Rails-like pattern matching

By default, route patterns parsing is based on Sinatra.  Rails has a similar, but not identical, syntax, based on Rack::Mount's Strexp.  The path pattern parser is resolved implicitly, and may be overridden if you prefer an alternate syntax:

    import org.scalatra._

    class RailsLikeRouting extends ScalatraFilter {
      implicit override def string2RouteMatcher(path: String) =
        RailsPathPatternParser(path)

      get("/:file(.:ext)") { // matched Rails-style }
    }

#### Path patterns in the REPL

If you want to experiment with path patterns, it's very easy in the REPL.

    scala> import org.scalatra.SinatraPathPatternParser
    import org.scalatra.SinatraPathPatternParser

    scala> val pattern = SinatraPathPatternParser("/foo/:bar")
    pattern: PathPattern = PathPattern(^/foo/([^/?#]+)$,List(bar))

    scala> pattern("/y/x") // doesn't match 
    res1: Option[MultiParams] = None

    scala> pattern("/foo/x") // matches
    res2: Option[MultiParams] = Some(Map(bar -> ListBuffer(x)))

Alternatively, you may use the `RailsPathPatternParser` in place of the
`SinatraPathPatternParser`.

### Conditions

Routes may include conditions.  A condition is any expression that returns Boolean.  Conditions are evaluated by-name each time the route matcher runs.

    get("/foo") {
      // Matches "GET /foo"
    }

    get("/foo", request.getRemoteHost == "127.0.0.1") {
      // Overrides "GET /foo" for local users
    }

Multiple conditions can be chained together.  A route must match all conditions:

    get("/foo", request.getRemoteHost == "127.0.0.1", request.getRemoteUser == "admin") {
      // Only matches if you're the admin, and you're localhost
    }

No path pattern is necessary.  A route may consist of solely a condition:

    get(isMaintenanceMode) {
      <h1>Go away!</h1>
    }

### Actions 

Each route is followed by an action.  An Action may return any value, which is then rendered to the response according to the following rules:

<dl>
  <dt>`Array[Byte]`</dt>
  <dd>If no content-type is set, it is set to `application/octet-stream`.  The byte array is written to the response's output stream.</dd>

  <dt>`NodeSeq`</dt>
  <dd>If no content-type is set, it is set to`text/html`.  The node sequence is converted to a string and written to the response's writer.</dd>

  <dt>`Unit`</dt>
  <dd>This signifies that the action has rendered the entire response, and no further action is taken.</dd>

  <dt>Any</dt>
  <dd>For any other value, if the content type is not set, it is set to `text/plain`.  The value is converted to a string and written to the response's writer</dd>.
</dl>

This behavior may be customized for these or other return types by overriding `renderResponse`.


## Filters

Before filters are evaluated before each request within the same context as the routes.

    beforeSome('/some/path') {
      // Log access to /some/path
      println("Will match /some/path")
    }

    beforeAll() {
      // Default all responses to text/html
      contentType = "text/html"
    }

After filters are evaluated after each request, but before the action result is rendered, within the same context as the routes.

    afterSome('/some/path') {
      println("Will render /some/path")
    }

    afterAll() {
      if (status >= 500)
        println("OMG! ONOZ!")
    }

Note that in the last example, in order to get access to the `status` method,
the filter class must mix `org.scalatra.GetResponseStatusSupport` in.

## Halting

To immediately stop a request within a filter or route:

    halt()

You can also specify the status:

    halt(403)

Or the status and the body:

    halt(403, <h1>Go away!</h1>)

Or even the HTTP status reason and headers.  For more complex invocations, it 
is recommended to use named arguments:

    halt(status = 403,
         reason = "Forbidden",
         headers = Map("X-Your-Mother-Was-A" -> "hamster",
                       "X-And-Your-Father-Smelt-Of" -> "Elderberries"),
         body = <h1>Go away or I shall taunt you a second time!</h1>)

The `reason` argument is ignored unless `status` is not null.  The default 
arguments leave that part of the request unchanged.

## Passing

A route can punt processing to the next matching route using pass.  Remember, unlike Sinatra, routes are matched from the bottom up.

    get("/guess/*") {
      "You missed!"
    }

    get("/guess/:who") {
      params("who") match {
        case "Frank" => pass()
        case _ => "You got me!"
      }
    }

The route block is immediately exited and control continues with the next matching route.  If no matching route is found, a 404 is returned.

## Accessing the Servlet API

### HttpServletRequest

The request is available through the `request` variable.  The request is implicitly extended with the following methods:

1. `body`: to get the request body as a string
2. `isAjax`: to detect AJAX requests
3. `cookies` and `multiCookies`: a Map view of the request's cookies
4. Implements `scala.collection.mutable.Map` backed by request attributes

### HttpServletResponse

The response is available through the `response` variable.

### HttpSession

The session is available through the `session` variable.  The session implicitly implements `scala.collection.mutable.Map` backed by session attributes.  To avoid creating a session, it may be accessed through `sessionOption`.

### ServletContext

The servlet context is available through the `servletContext` variable.  The servlet context implicitly implements `scala.collection.mutable.Map` backed by servlet context attributes.

## Configuration

The environment is defined by:
1. The `org.scalatra.environment` system property.
2. The `org.scalatra.environment` init property.
3. A default of `development`.

If the environment starts with "dev", then `isDevelopmentMode` returns true.  This flag may be used by other modules, for example, to enable the Scalate console.

## Error handling

Error handlers run within the same context as routes and before filters.

### Not Found

Whenever no route matches, the `notFound` handler is invoked.  The default behavior is:

    notFound {
      <h1>Not found.  Bummer.</h1>
    }

* _ScalatraServlet_: send a 404 response
* _ScalatraFilter_: pass the request to the servlet filter chain

### Method not allowed

If a route matches for other methods, but not the requested method, the
`methodNotAllowed` handler is invoked.  The default handler sends HTTP
status 405 with an `Allow` header containing a comma-separated list of the
supported methods.

The default behavior is adequate for most apps.  The most likely reasons
to override this behavior are:

1. You don't want to expose the supported methods.

2. You are in a ScalatraFilter and know the servlet filter chain can 
handle the request.

        methodNotAllowed { allow: Set[HttpMethod] =>
          filterChain.doFilter(request, response) 
        }

### Error

The `error` handler is invoked any time an exception is raised from a route block or a filter.  The throwable can be obtained from the `caughtThrowable` instance variable.  This variable is not defined outside the `error` block.

    error {
      log.error(caughtThrowable)
      redirect("http://www.sadtrombone.com/")
    }

## Flash scope

Flash scope is available by mixing in `FlashMapSupport`, which provides a
mutable map named `flash`.  Values put into flash scope by default persist 
through the request that they are first retrieved, and then are discarded
at the end of the request.  This is particularly useful for messages when using the
[Post/Redirect/Get](http://en.wikipedia.org/wiki/Post/Redirect/Get) pattern.

### sweepUnusedFlashEntries

The `sweepUnusedFlashEntries` method may be overridden to fine tune exactly
when entries are discarded:

* The default, false, is consistent with [Rack::Flash](http://nakajima.github.com/rack-flash/).

* To clear all entries after the next request, regardless of whether they've been read, override to true.  This is consistent with [ActionDispatch::Flash](http://api.rubyonrails.org/classes/ActionDispatch/Flash/FlashHash.html).

* The method takes the current request as a parameter, so the strategy may be tuned dynamically.  This could be useful to give special treatment to AJAX requests.

## Templating with Scalate

Scalatra provides optional support for [Scalate](http://scalate.fusesource.org/), a Scala template engine.  

1. Depend on scalatra-scalate.jar and a [slf4j binding](http://www.slf4j.org/manual.html#binding).  In your SBT build:

        val scalatraScalate = "org.scalatra" %% "scalatra-scalate" % scalatraVersion
        val slf4jBinding = "ch.qos.logback" % "logback-classic" % "0.9.25" % runtime

2. Extend your application with `ScalateSupport`

        import org.scalatra._
        import org.scalatra.scalate._
         
        class MyApplication extends ScalatraServlet with ScalateSupport {
          // ....
        }

3. A template engine is created as the `templateEngine` variable.  This can be used to render templates and call layouts.

        get("/") {
          templateEngine.layout("index.scaml", Map("content" -> "yada yada yada"))
        }

Additionally, `createRenderContext` may be used to create a render context for the current request and response. 

Finally, the [Scalate Console](http://scalate.fusesource.org/documentation/console.html) is enabled in development mode to display any unhandled exceptions.

## File upload support

Scalatra provides optional support for file uploads with <a href="http://commons.apache.org/fileupload/">Commons FileUpload</a>.

1. Depend on scalatra-fileupload.jar.  In your SBT build:

        val scalatraFileUpload = "org.scalatra" %% "scalatra-fileupload" % scalatraVersion

2. Extend your application with `FileUploadSupport`

        import org.scalatra.ScalatraServlet
        import org.scalatra.fileupload.FileUploadSupport

        class MyApp extends ScalatraServlet with FileUploadSupport {
          // ...
        }

3. Be sure that your form is of type `multipart/form-data`:

        get("/") {
          <form method="post" enctype="multipart/form-data">
            <input type="file" name="foo" />
            <input type="submit" />
          </form>
        }

4. Your files are available through the `fileParams` or `fileMultiParams` maps:

        post("/") {
          processFile(fileParams("file"))
        }

## Anti-XML integration

Scalatra provides optional [Anti-XML](http://anti-xml.org/) integration:

1. Depend on scalatra-anti-xml.jar.  In your SBT build:

        val scalatraAntiXml = "org.scalatra" %% "scalatra-anti-xml" % scalatraVersion

2. Extend your application with `AntiXmlSupport`

        import org.scalatra.ScalatraServlet
        import org.scalatra.antixml.AntiXmlSupport
        import com.codecommit.antixml._

        class MyApp extends ScalatraServlet with AntiXmlSupport {
          // ...
        }

3. Actions results of type `com.codecommit.antixml.Elem` will be serialized
to the response body, and a content type of `text/html` will be inferred if
none is set.

        get("/") {
          XML.fromString("""<foo bar="baz"></foo>""")
        }

## WebSocket and Comet support through Socket.IO

Scalatra provides optional support for websockets and comet through [socket.io](http://socket.io). We depend on [the socketio-java project](http://code.google.com/p/socketio-java) to provide this support.

1. Depend on the scalatra-socketio.jar. In your SBT build:

        val scalatraSocketIO = "org.scalatra" %% "scalatra-socketio" % scalatraVersion

2. SocketIO mimics a socket connection so it's easiest if you just create a socketio servlet at /socket.io/*

        import org.scalatra.ScalatraServlet
        import org.scalatra.socketio.SocketIOSupport

        class MySocketIOServlet extends ScalatraServlet with SocketIOSupport {
          // ...
        }

3. Setup the callbacks

        socketio { socket =>

          socket.onConnect { connection =>
            // Do stuff on connection
          }

          socket.onMessage { (connection, frameType, message) =>
            // Receive a message
            // use `connection.send("string")` to send a message
            // use `connection.broadcast("to send")` to send a message to all connected clients except the current one
            // use `connection.disconnect` to disconnect the client.
          }

          socket.onDisconnect { (connection, reason, message) =>
            // Do stuff on disconnection
          }
        }

4. Add the necessary entries to web.xml

        <servlet>
          <servlet-name>SocketIOServlet</servlet-name>
          <servlet-class>com.example.SocketIOServlet</servlet-class>
          <init-param>
            <param-name>flashPolicyServerHost</param-name>
            <param-value>localhost</param-value>
          </init-param>
          <init-param>
            <param-name>flashPolicyServerPort</param-name>
            <param-value>843</param-value>
          </init-param>
          <init-param>
            <param-name>flashPolicyDomain</param-name>
            <param-value>localhost</param-value>
          </init-param>
          <init-param>
            <param-name>flashPolicyPorts</param-name>
            <param-value>8080</param-value>
          </init-param>
        </servlet>  

              
When you want to use websockets with jetty the sbt build tool gets in the way and that makes it look like the websocket stuff isn't working. If you deploy the war to a jetty distribution everything should work as expected.

## Testing Your Scalatra Applications

Scalatra includes a test framework for writing the unit tests for your Scalatra application.  The framework lets you send requests to your app and examine the response.  It can be mixed into the test framework of your choosing; integration with [ScalaTest](http://www.scalatest.org/) and [Specs](http://code.google.com/p/specs/) is already provided.  ScalatraTests supports HTTP GET/POST tests with or without request parameters and sessions.  For more examples, please refer to core/src/test/scala.

ScalaTest
---------

### Dependencies

- scalatra-scalatest

### Code

Extend ScalatraSuite with your preferred Suite implementation.  You get ShouldMatchers and MustMatchers for free.

    class MyScalatraServletTests extends ScalatraSuite with FunSuite {
      // `MyScalatraServlet` is your app which extends ScalatraServlet
      addServlet(classOf[MyScalatraServlet], "/*")

      test("simple get") {
        get("/path/to/something") {
          status should equal (200)
          body should include ("hi!")
        }
      }
    }

Convenience traits are provided for many Suite implementations:

* ScalatraSpec
* ScalatraFlatSpec
* ScalatraFreeSpec
* ScalatraWordSpec
* ScalatraFunSuite
* ScalatraFeatureSpec
* ScalatraJUnit3Suite
* ScalatraJUnitSuite (JUnit 4)
* ScalatraTestNGSuite

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

## Maven Repository

To make usage of Scalatra as a dependency convenient, Maven hosting is now available courtesy of [Sonatype](https://docs.sonatype.com/display/NX/OSS+Repository+Hosting).

* [Releases](https://oss.sonatype.org/content/repositories/releases)
* [Snapshots](https://oss.sonatype.org/content/repositories/snapshots)

Authentication
==============

There is a new authentication middleware in the auth directory, to be documented soon.  See an example at [usage example](http://gist.github.com/660701).
Another [example](https://gist.github.com/732347) for basic authentication can be found

To use it from an SBT project, add the following to your project:

    val auth = "org.scalatra" %% "scalatra-auth" % scalatraVersion

## FAQ

### It looks neat, but is it production-ready?

- It is use in the backend for [LinkedIn Signal](http://sna-projects.com/blog/2010/10/linkedin-signal-a-look-under-the-hood/).

- [ChaCha](http://www.chacha.com/) is using it in multiple internal applications.

- A project is in currently development to support a site with over one million unique users.

Are you using Scalatra in production?  Tell us your story on the [mailing list](http://groups.google.com/group/scalatra-user/).

### ScalatraServlet vs. ScalatraFilter

The main difference is the default behavior when a route is not found.  A filter will delegate to the next filter or servlet in the chain (as configured by web.xml), whereas a ScalatraServlet will return a 404 response.

Another difference is that ScalatraFilter matches routes relative to the WAR's context path.  ScalatraServlet matches routes relative to the servlet path.  This allows you to mount multiple servlets under in different namespaces in the same WAR.

### Use ScalatraFilter if:
- You are migrating a legacy application inside the same URL space
- You want to serve static content from the WAR rather than a dedicated web server

### Use ScalatraServlet if:
- You want to match routes with a prefix deeper than the context path.

## Migration Guide

### scalatra-2.0.0.M4 to scalatra-2.0.0.M5

1. `halt(Int, String)` no longer sets the HTTP status line reason.  Call
   it with the named overload parameter.

2. `halt(Int)` and `halt(Int, String)` no longer call `response.sendError`.
   If you depend on this behavior for routing to web.xml error pages, call 
   it explicitly and then call `halt()`, or override `halt`.

### scalatra-2.0.0.M3 to scalatra-2.0.0.M4

1. Remove socket-io-java-dev.jar if you manually copied it into your lib directory.
2. Flash map entries are no longer swept until accessed.  For the old behavior, override `sweepUnusedFlashEntries` to return true.
3. Scala 2.8.0 is no longer supported.  Please upgrade to 2.8.1 (easy) or 2.9.0-1 (better).

### scalatra-2.0.0.M2 to scalatra-2.0.0.M3

No issues.

### scalatra-2.0.0.M1 to scalatra-2.0.0.M2

1. Session has been retrofitted to a Map interface.  `get` now returns an option instead of the value.
2. ScalaTest support has been split off into `scalatra-scalatest` module.  ScalatraSuite moved to `org.scalatest.test.scalatest` package, and no longer extends FunSuite in order to permit mixing in a BDD trait.  You may either use ScalatraFunSuite or explicitly extend FunSuite yourself.

### Step to Scalatra

Scalatra was renamed from Step to Scalatra to avoid a naming conflict with (an unrelated web framework)[http://sourceforge.net/stepframework].  scalatra-1.2.1 is identical to step-1.2.0 with the following exceptions:

1. The package has changed from `com.thinkminimo.step` to `org.scalatra`.
1. The `Step` class has been renamed to `ScalatraServlet`.
1. All other `Step*` classes have been renamed to `Scalatra*`.

## Related Projects

- [SSGI](http://github.com/scalatra/ssgi): Work in progress. Will provide an abstraction layer allowing a future version of Scalatra to run on web servers other than Servlet containers.

- [Bowler](http://bowlerframework.org): A RESTful, multi-channel ready web framework in Scala with a functional flavour, built on top of Scalatra and [Scalate](http://scalate.fusesource.org/).
