import sbt._
import Keys._
import scala.xml._
import java.net.URL
import com.earldouglas.xsbtwebplugin.WebPlugin.webSettings

object ScalatraBuild extends Build {
  import Dependencies._
  import Resolvers._

  lazy val scalatraSettings = Defaults.defaultSettings ++ Seq(
    organization := "org.scalatra",
    version := "2.0.4",
    scalaVersion := "2.10.4",
    scalacOptions ++= Seq("-unchecked", "-deprecation"),
    manifestSetting,
    publishSetting,
    resolvers ++= Seq(sonatypeNexusSnapshots)
  ) ++ mavenCentralFrouFrou

  lazy val scalatraProject = Project(
    id = "scalatra-project",
    base = file("."),
    settings = scalatraSettings ++ doNotPublish ++ Seq(
      description := "A tiny, Sinatra-like web framework for Scala"
    ),
    aggregate = Seq(scalatraCore, scalatraAuth, scalatraFileupload,
      scalatraScalate, scalatraSocketio, scalatraLiftJson,
      scalatraTest, scalatraScalatest, scalatraSpecs, scalatraSpecs2,
      scalatraExample, scalatraJetty8Tests)
  )

  lazy val scalatraCore = Project(
    id = "scalatra",
    base = file("core"),
    settings = scalatraSettings ++ Seq(
      libraryDependencies ++= Seq(servletApi, slf4jSimple % "test"),
      description := "The core Scalatra framework"
    )
  ) dependsOn(Seq(scalatraSpecs2, scalatraSpecs, scalatraScalatest) map { _ % "test->compile" } :_*)

  lazy val scalatraAuth = Project(
    id = "scalatra-auth",
    base = file("auth"),
    settings = scalatraSettings ++ Seq(
      libraryDependencies ++= Seq(base64, liftTestkit),
      description := "Scalatra authentication module"
    )
  ) dependsOn(scalatraCore % "compile;test->test;provided->provided")

  lazy val scalatraFileupload = Project(
    id = "scalatra-fileupload",
    base = file("fileupload"),
    settings = scalatraSettings ++ Seq(
      libraryDependencies ++= Seq(commonsFileupload, commonsIo),
      description := "Commons-Fileupload integration with Scalatra"
    )
  ) dependsOn(scalatraCore % "compile;test->test;provided->provided")

  lazy val scalatraScalate = Project(
    id = "scalatra-scalate",
    base = file("scalate"),
    settings = scalatraSettings ++ Seq(
      libraryDependencies += scalate,
      description := "Scalate integration with Scalatra"
    )
  ) dependsOn(scalatraCore % "compile;test->test;provided->provided")

  lazy val scalatraSocketio = Project(
    id = "scalatra-socketio",
    base = file("socketio"),
    settings = scalatraSettings ++ Seq(
      libraryDependencies <++= version(v => Seq(jettyWebsocket, socketioCore(v))),
      description := "Socket IO support for Scalatra"
    )
  ) dependsOn(scalatraCore % "compile;test->test;provided->provided")

  lazy val scalatraLiftJson = Project(
    id = "scalatra-lift-json",
    base = file("lift-json"),
    settings = scalatraSettings ++ Seq(
      libraryDependencies += liftJson,
      description := "Lift JSON support for Scalatra"
    )
  ) dependsOn(scalatraCore % "compile;test->test;provided->provided")

  lazy val scalatraTest = Project(
    id = "scalatra-test",
    base = file("test"),
    settings = scalatraSettings ++ Seq(
      libraryDependencies ++= Seq(testJettyServlet, mockitoAll, commonsLang3, specs2 % "test"),
      description := "The abstract Scalatra test framework"
    )
  )

  lazy val scalatraScalatest = Project(
    id = "scalatra-scalatest",
    base = file("scalatest"),
    settings = scalatraSettings ++ Seq(
      libraryDependencies ++= Seq(scalatest, junit, testng),
      description := "ScalaTest support for the Scalatra test framework"
    )
  ) dependsOn(scalatraTest)

  lazy val scalatraSpecs = Project(
    id = "scalatra-specs",
    base = file("specs"),
    settings = scalatraSettings ++ Seq(
      libraryDependencies += specs,
      description := "Specs support for the Scalatra test framework"
      // The one in Maven Central has a bad checksum for 2.8.2.  
      // Try ScalaTools first.
      //resolvers ~= { rs => ScalaToolsReleases +: rs }
    )
  ) dependsOn(scalatraTest)

  lazy val scalatraSpecs2 = Project(
    id = "scalatra-specs2",
    base = file("specs2"),
    settings = scalatraSettings ++ Seq(
      libraryDependencies += specs2,
      description := "Specs2 support for the Scalatra test framework"
    )
  ) dependsOn(scalatraTest)

  lazy val scalatraExample = Project(
    id = "scalatra-example",
    base = file("example"),
    settings = scalatraSettings ++ webSettings ++ doNotPublish ++ Seq(
      libraryDependencies ++= Seq(servletApi, jettyWebapp % "container"),
      description := "Scalatra example project"
    )
  ) dependsOn(
    scalatraCore % "compile;test->test;provided->provided", scalatraScalate,
    scalatraAuth, scalatraFileupload, scalatraSocketio
  )

  lazy val scalatraJetty8Tests = Project(
    id = "scalatra-jetty8-tests",
    base = file("test/jetty8"),
    settings = scalatraSettings ++ doNotPublish ++ Seq(
      libraryDependencies ++= Seq(servletApi_3_0, testJettyServlet_8 % "test"),
      description := "Compatibility tests for Jetty 8"
    )
  ) dependsOn(scalatraSpecs2 % "test->compile")

  object Dependencies {

    val base64 = "net.iharder" % "base64" % "2.3.8"

    val commonsFileupload = "commons-fileupload" % "commons-fileupload" % "1.2.1"
    val commonsIo = "commons-io" % "commons-io" % "2.1"
    val commonsLang3 = "org.apache.commons" % "commons-lang3" % "3.1"

    // Exclude is due to http://jira.codehaus.org/browse/JETTY-1493
    private def jettyDep(name: String, version: String = "7.6.0.v20120127") =
      "org.eclipse.jetty" % name % version
    val testJettyServlet = jettyDep("test-jetty-servlet")
    val testJettyServlet_8 = jettyDep("test-jetty-servlet", "8.1.0.v20120127")
    val jettyWebsocket = jettyDep("jetty-websocket")
    val jettyWebapp = jettyDep("jetty-webapp")

    val junit = "junit" % "junit" % "4.10"

    private def liftDep(name: String) = "net.liftweb" % name % "2.5"
    val liftJson = liftDep("lift-json_2.10")
    val liftTestkit = liftDep("lift-testkit_2.10") % "test"

    val mockitoAll = "org.mockito" % "mockito-all" % "1.8.5"

    val scalate = "org.fusesource.scalate" %% "scalate-core" % "1.6.1"

    val scalatest = "org.scalatest" % "scalatest_2.10" % "1.9.1"

    val specs = "org.scala-tools.testing" % "specs_2.10" % "1.6.9"

    val specs2 = "org.specs2" % "specs2_2.10" % "2.3.11"

    val servletApi = "javax.servlet" % "servlet-api" % "2.5" % "provided"
    val servletApi_3_0 = "javax.servlet" % "javax.servlet-api" % "3.0.1" % "provided"

    val slf4jSimple = "org.slf4j" % "slf4j-simple" % "1.6.4"

    def socketioCore(version: String) = "org.scalatra.socketio-java" % "socketio-core" % "2.0.0"

    val testng = "org.testng" % "testng" % "6.3" % "optional"
  }

  object Resolvers {
    val sonatypeNexusSnapshots = "Sonatype Nexus Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
    val sonatypeNexusStaging = "Sonatype Nexus Staging" at "https://oss.sonatype.org/service/local/staging/deploy/maven2"
  }

  lazy val manifestSetting = packageOptions <+= (name, version, organization) map {
    (title, version, vendor) =>
      Package.ManifestAttributes(
        "Created-By" -> "Simple Build Tool",
        "Built-By" -> System.getProperty("user.name"),
        "Build-Jdk" -> System.getProperty("java.version"),
        "Specification-Title" -> title,
        "Specification-Version" -> version,
        "Specification-Vendor" -> vendor,
        "Implementation-Title" -> title,
        "Implementation-Version" -> version,
        "Implementation-Vendor-Id" -> vendor,
        "Implementation-Vendor" -> vendor
      )
  }

  lazy val publishSetting = publishTo <<= (version) { version: String =>
    if (version.trim.endsWith("SNAPSHOT"))
      Some(sonatypeNexusSnapshots)
    else
      Some(sonatypeNexusStaging)
  }

  // Things we care about primarily because Maven Central demands them
  lazy val mavenCentralFrouFrou = Seq(
    homepage := Some(new URL("http://www.scalatra.org/")),
    startYear := Some(2009),
    licenses := Seq(("BSD", new URL("http://github.com/scalatra/scalatra/raw/HEAD/LICENSE"))),
    pomExtra <<= (pomExtra, name, description) {(pom, name, desc) => pom ++ Group(
      <scm>
        <url>http://github.com/scalatra/scalatra</url>
        <connection>scm:git:git://github.com/scalatra/scalatra.git</connection>
      </scm>
      <developers>
        <developer>
          <id>riffraff</id>
          <name>Gabriele Renzi</name>
          <url>http://www.riffraff.info</url>
        </developer>
        <developer>
          <id>alandipert</id>
          <name>Alan Dipert</name>
          <url>http://alan.dipert.org</url>
        </developer>
        <developer>
          <id>rossabaker</id>
          <name>Ross A. Baker</name>
          <url>http://www.rossabaker.com/</url>
        </developer>
        <developer>
          <id>chirino</id>
          <name>Hiram Chirino</name>
          <url>http://hiramchirino.com/blog/</url>
        </developer>
        <developer>
          <id>casualjim</id>
          <name>Ivan Porto Carrero</name>
          <url>http://flanders.co.nz/</url>
        </developer>
        <developer>
          <id>jlarmstrong</id>
          <name>Jared Armstrong</name>
          <url>http://www.jaredarmstrong.name/</url>
        </developer>
      </developers>
    )}
  )

  lazy val doNotPublish = Seq(publish := {}, publishLocal := {})
}
