## Scalatra ![Scala CI](https://github.com/scalatra/scalatra/workflows/build/badge.svg?branch=main)

[![Join the chat at https://gitter.im/scalatra/scalatra](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/scalatra/scalatra?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![scalatra Scala version support](https://index.scala-lang.org/scalatra/scalatra/scalatra-jakarta/latest-by-scala-version.svg?platform=jvm)](https://index.scala-lang.org/scalatra/scalatra/artifacts/scalatra-jakarta)

Scalatra is a tiny, [Sinatra](https://sinatrarb.com/)-like web framework for
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

If you're just starting out, see the [installation](https://scalatra.org/getting-started/installation.html) and [first project](https://scalatra.org/getting-started/first-project.html) sections of our website.

Once you've done that, take a look at the [Scalatra Guides](https://scalatra.org/guides/) for documentation on all aspects of the framework, code examples, and more. We also have an extensive selection of [Example Applications](https://github.com/scalatra/scalatra-website-examples) which accompany the tutorials in the Scalatra Guides.

## Latest version

The latest version of Scalatra is `3.1.+`, and is published to [Maven Central](https://repo1.maven.org/maven2/org/scalatra).

```scala
// for javax
libraryDependencies += "org.scalatra" %% "scalatra-javax" % "3.1.+"

// for jakarta
libraryDependencies += "org.scalatra" %% "scalatra-jakarta" % "3.1.+"
```

## Community

* Gitter: [Scalatra/Scalatra](https://gitter.im/scalatra/scalatra)
* Mailing list: [scalatra-user](https://groups.google.com/group/scalatra-user)
* IRC: #scalatra on irc.freenode.org
* [Guidelines for contributing](CONTRIBUTING.markdown)
