Scalatra is a tiny, [Sinatra](http://www.sinatrarb.com/)-like web framework for
[Scala](http://www.scala-lang.org/).

## Example

    import org.scalatra._

    class ScalatraExample extends ScalatraServlet {
      get("/") {
        <h1>Hello, world!</h1>
      }
    }

## Documentation

Please see [The Scalatra Book](http://www.scalatra.org/stable/book/) for more.

The [old readme](https://github.com/scalatra/scalatra/tree/scalatra_2.9.0-1-2.0.0.RC1/README.markdown) 
is deprecated in favor of the book, but may still be more advanced in a few 
subjects.  If you find one, please [let us know](http://github.com/scalatra/scalatra-book/issues).

## Latest version 

The latest version of Scalatra is `2.0.1`, and is published to [Maven Central](http://repo1.maven.org/maven2/org/scalatra).

    libraryDependencies += "org.scalatra" %% "scalatra" % "2.0.1"

### Development version

The develop branch is published as `2.1.0-SNAPSHOT` to [OSSRH](http://oss.sonatype.org/content/repositories/snapshots/org/scalatra).

    resolvers += "Sonatype Nexus Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

    libraryDependencies += "org.scalatra" %% "scalatra" % "2.1.0-SNAPSHOT"

## Community

* Mailing list: [scalatra-user](http://groups.google.com/scalatra-user)
* IRC: #scalatra on irc.freenode.org
