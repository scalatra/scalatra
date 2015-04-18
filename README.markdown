## Scalatra [![Build Status](https://travis-ci.org/scalatra/scalatra.svg?branch=2.4.x)](https://travis-ci.org/scalatra/scalatra)

[![Join the chat at https://gitter.im/scalatra/scalatra](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/scalatra/scalatra?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

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

If you're just starting out, see the [installation](http://www.scalatra.org/2.2/getting-started/installation.html) and [first project](http://www.scalatra.org/2.2/getting-started/first-project.html) sections of our website. 

Once you've done that, take a look at the [Scalatra Guides](http://www.scalatra.org/guides/) for documentation on all aspects of the framework, code examples, and more. We also have an extensive selection of [Example Applications](https://github.com/scalatra/scalatra-website-examples) which accompany the tutorials in the Scalatra Guides.

## Latest version

The latest stable version of Scalatra is `2.3.1`, and is published to [Maven Central](http://repo1.maven.org/maven2/org/scalatra).

```scala
libraryDependencies += "org.scalatra" %% "scalatra" % "2.3.1"
```

The upcoming 2.4.0 RC release is also available. Please git it a try and give us your feedback!

```scala
libraryDependencies += "org.scalatra" %% "scalatra" % "2.4.0.RC1"
```

### Development version

The 2.4.x branch is published as `2.4.0-SNAPSHOT` to [OSSRH](http://oss.sonatype.org/content/repositories/snapshots/org/scalatra). 

```scala
resolvers += "Sonatype Nexus Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

libraryDependencies += "org.scalatra" %% "scalatra" % "2.4.0-SNAPSHOT"
```

## Community

* Mailing list: [scalatra-user](http://groups.google.com/group/scalatra-user)
* IRC: #scalatra on irc.freenode.org
