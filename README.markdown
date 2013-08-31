## Scalatra [![Build Status](http://jenkins.backchat.io/job/scalatra/badge/icon)](http://jenkins.backchat.io/job/scalatra/)

Scalatra is a tiny, [Sinatra](http://www.sinatrarb.com/)-like web framework for
[Scala](http://www.scala-lang.org/).

## Example

```scala
import org.scalatra._

class ScalatraExample extends ScalatraServlet {
  get("/") {
    <h1>Hello, world!</h1>
  }
}
```

## Documentation

Please see [The Scalatra Book](http://www.scalatra.org/guides/) for more.


## Latest version

The latest version of Scalatra is `2.2.1`, and is published to [Maven Central](http://repo1.maven.org/maven2/org/scalatra).

```scala
libraryDependencies += "org.scalatra" %% "scalatra" % "2.2.1"
```

### Development version

The develop branch is published as `2.2.2-SNAPSHOT` to [OSSRH](http://oss.sonatype.org/content/repositories/snapshots/org/scalatra). 

```scala
resolvers += "Sonatype Nexus Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

libraryDependencies += "org.scalatra" %% "scalatra" % "2.2.2-SNAPSHOT"
```

## Community

* Mailing list: [scalatra-user](http://groups.google.com/group/scalatra-user)
* IRC: #scalatra on irc.freenode.org
