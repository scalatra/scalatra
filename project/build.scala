import sbt._
import Keys._
import scala.xml._
import java.net.URL
import com.github.siasia.WebPlugin.webSettings
import posterous.Publish._

object ScalatraBuild extends Build {
  import Dependencies._
  import Resolvers._

  lazy val scalatraSettings = Defaults.defaultSettings ++ Seq(
    organization := "org.scalatra",
    version := "2.1.0-SNAPSHOT",
    crossScalaVersions := Seq("2.9.1", "2.9.0-1", "2.9.0", "2.8.1"),
    scalaVersion <<= (crossScalaVersions) { versions => versions.head },
    scalacOptions ++= Seq("-unchecked", "-deprecation"),
    manifestSetting,
    publishSetting,
    resolvers += ScalaToolsSnapshots
  ) ++ mavenCentralFrouFrou

  lazy val scalatraProject = Project(
    id = "scalatra-project",
    base = file("."),
    settings = scalatraSettings ++ Unidoc.settings ++ doNotPublish ++ Seq(
      description := "A tiny, Sinatra-like web framework for Scala",
      Unidoc.unidocExclude := Seq("scalatra-example"),
      (name in Posterous) := "scalatra"
    ),
    aggregate = Seq(scalatraCore, scalatraAuth, scalatraFileupload,
      scalatraScalate, scalatraSocketio, scalatraLiftJson, scalatraAntiXml,
      scalatraTest, scalatraScalatest, scalatraSpecs, scalatraSpecs2,
      scalatraExample, scalatraAkka)
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
      libraryDependencies += base64,
      description := "Scalatra authentication module"
    )
  ) dependsOn(scalatraCore % "compile;test->test;provided->provided")

  lazy val scalatraAkka = Project(
    id = "scalatra-akka",
    base = file("akka"),
    settings = scalatraSettings ++ Seq(
      libraryDependencies ++= Seq(akka, akkaTestkit),
      resolvers += "Akka Repo" at "http://akka.io/repository",
      description := "Scalatra akka integration module",
      // Akka only supports 2.9.x, so don't build this module for 2.8.x.
      skip <<= scalaVersion map { v => v startsWith "2.8." }
      // TODO don't publish or publish-local when we skip
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
      libraryDependencies <+= scalaVersion(scalate),
      resolvers ++= Seq(sonatypeNexusSnapshots),
      description := "Scalate integration with Scalatra"
    )
  ) dependsOn(scalatraCore % "compile;test->test;provided->provided")

  lazy val scalatraSocketio = Project(
    id = "scalatra-socketio",
    base = file("socketio"),
    settings = scalatraSettings ++ Seq(
      libraryDependencies <++= version(v => Seq(jettyWebsocket, socketioCore(v))),
      resolvers += sonatypeNexusSnapshots,
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

  lazy val scalatraAntiXml = Project(
    id = "scalatra-anti-xml",
    base = file("anti-xml"),
    settings = scalatraSettings ++ Seq(
      libraryDependencies <+= scalaVersion(antiXml),
      description := "Anti-XML support for Scalatra"
    )
  ) dependsOn(scalatraCore % "compile;test->test;provided->provided")

  lazy val scalatraTest = Project(
    id = "scalatra-test",
    base = file("test"),
    settings = scalatraSettings ++ Seq(
      libraryDependencies <++= scalaVersion(sv => Seq(
        slf4s(sv),
        testJettyServlet,
        mockitoAll,
        commonsLang3,
        specs2(sv) % "test",
        dispatch
      )),
      description := "The abstract Scalatra test framework"
    )
  )

  lazy val scalatraScalatest = Project(
    id = "scalatra-scalatest",
    base = file("scalatest"),
    settings = scalatraSettings ++ Seq(
      libraryDependencies <++= scalaVersion(sv => Seq(scalatest(sv), junit, testng)),
      description := "ScalaTest support for the Scalatra test framework"
    )
  ) dependsOn(scalatraTest)

  lazy val scalatraSpecs = Project(
    id = "scalatra-specs",
    base = file("specs"),
    settings = scalatraSettings ++ Seq(
      libraryDependencies <+= scalaVersion(specs),
      description := "Specs support for the Scalatra test framework"
    )
  ) dependsOn(scalatraTest)

  lazy val scalatraSpecs2 = Project(
    id = "scalatra-specs2",
    base = file("specs2"),
    settings = scalatraSettings ++ Seq(
      libraryDependencies <+= scalaVersion(specs2),
      description := "Specs2 support for the Scalatra test framework"
    )
  ) dependsOn(scalatraTest)

  lazy val scalatraExample = Project(
    id = "scalatra-example",
    base = file("example"),
    settings = scalatraSettings ++ webSettings ++ doNotPublish ++ Seq(
      resolvers ++= Seq(sonatypeNexusSnapshots),
      libraryDependencies ++= Seq(atmosphere, jettyWebapp),
      description := "Scalatra example project"
    )
  ) dependsOn(
    scalatraCore % "compile;test->test;provided->provided", scalatraScalate,
    scalatraAuth, scalatraFileupload, scalatraSocketio, scalatraAkka
  )

  object Dependencies {
    def antiXml(scalaVersion: String) = {
      val libVersion = scalaVersion match {
        case x if x startsWith "2.8." => "0.2"
        case _ => "0.3"
      }
      "com.codecommit" %% "anti-xml" % libVersion
    }

    val atmosphere = "org.atmosphere" % "atmosphere-runtime" % "0.7.2"

    val base64 = "net.iharder" % "base64" % "2.3.8"

    val akka = "se.scalablesolutions.akka" % "akka-actor" % "1.3-RC4"
    val akkaTestkit = "se.scalablesolutions.akka" % "akka-testkit" % "1.3-RC4" % "test"

    val commonsFileupload = "commons-fileupload" % "commons-fileupload" % "1.2.1"
    val commonsIo = "commons-io" % "commons-io" % "2.1"
    val commonsLang3 = "org.apache.commons" % "commons-lang3" % "3.1"

    val dispatch = "net.databinder" %% "dispatch-http" % "0.8.5"

    private def jettyDep(name: String) = "org.eclipse.jetty" % name % "8.1.0.RC1"
    val testJettyServlet = jettyDep("test-jetty-servlet")
    val jettyWebsocket = jettyDep("jetty-websocket") % "provided"
    val jettyWebapp = jettyDep("jetty-webapp") % "test;container"

    val junit = "junit" % "junit" % "4.10"

    val liftJson = "net.liftweb" %% "lift-json" % "2.4-M5"

    val mockitoAll = "org.mockito" % "mockito-all" % "1.8.5"

    def scalate(scalaVersion: String) = {
      val libVersion = scalaVersion match {
        // 1.5.3-scala_2.8.2 fails on 2.8.1 loading
        // scala/tools/nsc/interactive/Global$
        case x if x startsWith "2.8.1" => "1.5.2-scala_2.8.1"
        case _ => "1.5.3"
      }
      "org.fusesource.scalate" % "scalate-core" % libVersion
    }

    def scalatest(scalaVersion: String) = {
      val libVersion = scalaVersion match {
        case x if x startsWith "2.8." => "1.5.1"
        case _ => "1.6.1"
      }
      "org.scalatest" %% "scalatest" % libVersion
    }

    def specs(scalaVersion: String) = {
      val libVersion = scalaVersion match {
        case "2.9.1" => "1.6.9"
        case _ => "1.6.8"
      }
      "org.scala-tools.testing" %% "specs" % libVersion
    }

    def specs2(scalaVersion: String) = {
      val libVersion = scalaVersion match {
        case x if x startsWith "2.8." => "1.5"
        case "2.9.0" => "1.5" // https://github.com/etorreborre/specs2/issues/33
        case _ => "1.6.1"
      }
      "org.specs2" %% "specs2" % libVersion
    }

    val servletApi = "javax.servlet" % "javax.servlet-api" % "3.0.1" % "provided"

    val slf4jSimple = "org.slf4j" % "slf4j-simple" % "1.6.4"

    def slf4s(scalaVersion: String) = {
      val libVersion = scalaVersion match {
        case "2.9.0" => "1.0.6"
        case _ => "1.0.7"
      }
      "com.weiglewilczek.slf4s" %% "slf4s" % libVersion
    }

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
