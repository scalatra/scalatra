import sbt._
import Keys._
import scala.xml._
import java.net.URL
import com.github.siasia.WebPlugin.webSettings
// import posterous.Publish._
import ls.Plugin.LsKeys
import sbtbuildinfo.Plugin._

object ScalatraBuild extends Build {
  import Dependencies._
  import Resolvers._

  lazy val majorVersion = "2.1"

  lazy val scalatraSettings = Defaults.defaultSettings ++ ls.Plugin.lsSettings ++ Seq(
    organization := "org.scalatra",
    version := "%s.0-SNAPSHOT" format majorVersion,
    scalaVersion := "2.9.1",
    scalacOptions ++= Seq("-unchecked", "-deprecation"),
    javacOptions ++= Seq("-target", "1.6", "-source", "1.6"),
    manifestSetting,
    publishSetting,
    crossPaths := false,
    resolvers ++= Seq(sonatypeNexusSnapshots, akkaRepository),
    (LsKeys.tags in LsKeys.lsync) := Seq("web", "sinatra"),
    (LsKeys.docsUrl in LsKeys.lsync) := Some(new URL("http://www.scalatra.org/%s/book/" format majorVersion))
  ) ++ mavenCentralFrouFrou

  lazy val scalatraProject = Project(
    id = "scalatra-project",
    base = file("."),
    settings = scalatraSettings ++ Unidoc.settings ++ doNotPublish ++ Seq(
      description := "A tiny, Sinatra-like web framework for Scala",
      Unidoc.unidocExclude := Seq("scalatra-example"),
      LsKeys.skipWrite := true
    ),
    aggregate = Seq(
      scalatraCore, scalatraAuth, scalatraScalate, scalatraLiftJson,
      scalatraJerkson, scalatraAkka, scalatraSwagger, scalatraNetty, scalatraFrameworkTests,
      scalatraTest, scalatraScalatest, scalatraSpecs, scalatraSpecs2,
      scalatraExample, scalatraJetty, scalatraJetty, scalatraServlet)
  )

  lazy val scalatraCore = Project(
    id = "scalatra",
    base = file("core"),
    settings = scalatraSettings ++ buildInfoSettings ++ Seq(
      libraryDependencies ++= Seq(
        grizzledSlf4j,
        chardet,
        mimeUtil,
        backchatRl,
        httpParsers,
        twitterCollection,
        akkaActor,
        specs2 % "test",
        scalaCheck
      ),
      resolvers += twitterMaven,
      libraryDependencies ++= scalaIO,
      description := "The core Scalatra framework",
      sourceGenerators in Compile <+= buildInfo,
      buildInfoKeys := Seq[Scoped](name, version, scalaVersion, sbtVersion),
      buildInfoPackage := "org.scalatra"
    ))
//  ) dependsOn(Seq(scalatraSpecs2, scalatraSpecs, scalatraScalatest) map { _ % "test->compile" } :_*)

  lazy val scalatraAuth = Project(
    id = "scalatra-auth",
    base = file("auth"),
    settings = scalatraSettings ++ Seq(
      libraryDependencies ++= Seq(base64),
      description := "Scalatra authentication module"
    )
  ) dependsOn(
      scalatraCore % "compile;provided->provided",
      scalatraFrameworkTests % "test->compile;test->test;provided->provided",
      scalatraTest % "test->compile;provided->provided",
      scalatraSpecs % "test->compile;provided->provided")

  lazy val scalatraAkka = Project(
    id = "scalatra-akka",
    base = file("akka"),
    settings = scalatraSettings ++ Seq(
      libraryDependencies ++= Seq(akkaActor, akkaTestkit),
      resolvers += "Akka Repo" at "http://repo.akka.io/repository",
      description := "Scalatra akka integration module"
    ) 
  ) dependsOn(scalatraCore % "compile;provided->provided")

  lazy val scalatraScalate = Project(
    id = "scalatra-scalate",
    base = file("scalate"),
    settings = scalatraSettings ++ Seq(
      libraryDependencies ++= Seq(scalate),
      resolvers ++= Seq(sonatypeNexusSnapshots),
      description := "Scalate integration with Scalatra"
    )
  ) dependsOn(
    scalatraCore % "compile;provided->provided",
    scalatraFrameworkTests % "test->compile;test->test;provided->provided",
    scalatraTest % "test->compile;provided->provided",
    scalatraSpecs2 % "test->compile;provided->provided")

  lazy val scalatraLiftJson = Project(
    id = "scalatra-lift-json",
    base = file("lift-json"),
    settings = scalatraSettings ++ Seq(
      libraryDependencies += liftJson,
      description := "Lift JSON support for Scalatra"
    )
  ) dependsOn(
    scalatraCore % "compile;provided->provided",
    scalatraFrameworkTests % "test->compile;test->test;provided->provided",
    scalatraTest % "test->compile;provided->provided",
    scalatraSpecs % "test->compile;provided->provided")

  lazy val scalatraJerkson = Project(
    id = "scalatra-jerkson",
    base = file("jerkson"),
    settings = scalatraSettings ++ Seq(
      libraryDependencies += jerkson,
      description := "Jackson/Jerkson JSON support for Scalatra"
    )
  ) dependsOn(scalatraCore % "compile;provided->provided")

  lazy val scalatraServlet = Project(
    id = "scalatra-servlet",
    base = file("servlet"),
    settings = scalatraSettings ++ Seq(
      libraryDependencies ++= Seq(
        servletApi % "compile;test"
      ),
      description := "Servlet backend for Scalatra apps"
    )
  ) dependsOn(scalatraCore % "compile;provided->provided")

  lazy val scalatraJetty = Project(
    id = "scalatra-jetty",
    base = file("jetty"),
    settings = scalatraSettings ++ jettyOrbitHack ++ Seq(
      libraryDependencies ++= Seq(
        servletApi % "compile;test",
        jettyServlet
      ),
      description := "Embedded Jetty server for Scalatra apps"
    )
  ) dependsOn(scalatraServlet % "compile;test->test;provided->provided")

  lazy val scalatraNetty = Project(
    id = "scalatra-netty",
    base = file("netty"),
    settings = scalatraSettings ++ Seq(
      libraryDependencies ++= Seq(netty, nettyExtension, httpParsers),
      resolvers += goldenGate,
      description := "Netty backend for Scalatra"
    )
  ) dependsOn(scalatraCore % "compile;provided->provided", scalatraSpecs2 % "test->compile")

  lazy val scalatraTest = Project(
    id = "scalatra-test",
    base = file("test"),
    settings = scalatraSettings ++ Seq(
      libraryDependencies ++= Seq(
        grizzledSlf4j,
        mockitoAll,
        commonsLang3,
        asyncHttpClient,
        specs2 % "test"
      ),
      description := "The abstract Scalatra test framework"
    )
  ) dependsOn(scalatraCore % "compile;provided->provided")

  lazy val scalatraScalatest = Project(
    id = "scalatra-scalatest",
    base = file("scalatest"),
    settings = scalatraSettings ++ Seq(
      libraryDependencies ++= Seq(scalatest, junit, testng),
      description := "ScalaTest support for the Scalatra test framework"
    )
  ) dependsOn(scalatraTest % "compile;test->test;provided->provided")

  lazy val scalatraSpecs = Project(
    id = "scalatra-specs",
    base = file("specs"),
    settings = scalatraSettings ++ Seq(
      libraryDependencies += specs,
      description := "Specs support for the Scalatra test framework", 
      // The one in Maven Central has a bad checksum for 2.8.2.  
      // Try ScalaTools first.
      resolvers ~= { rs => ScalaToolsReleases +: rs }
    )
  ) dependsOn(scalatraTest % "compile;test->test;provided->provided")

  lazy val scalatraSpecs2 = Project(
    id = "scalatra-specs2",
    base = file("specs2"),
    settings = scalatraSettings ++ Seq(
      libraryDependencies += specs2,
      description := "Specs2 support for the Scalatra test framework"
    )
  ) dependsOn(scalatraTest % "compile;test->test;provided->provided")

  lazy val scalatraSwagger = Project(
    id = "scalatra-swagger",
    base = file("swagger"),
    settings = scalatraSettings ++ Seq(
      libraryDependencies ++= Seq(liftJson, liftJsonExt),
      description := "Scalatra integration with Swagger"
    )
  ) dependsOn(scalatraCore % "compile;provided->provided",
      scalatraFrameworkTests % "test->compile;test->test;provided->provided")

  lazy val scalatraExample = Project(
    id = "scalatra-example",
    base = file("example"),
    settings = scalatraSettings ++ webSettings ++ jettyOrbitHack ++ doNotPublish ++ Seq(
      resolvers ++= Seq(sonatypeNexusSnapshots),
      libraryDependencies += servletApi % "container;test",
      libraryDependencies ++= Seq(atmosphere, jettyWebapp, slf4jSimple),
      description := "Scalatra example project"
    )
  ) dependsOn(
    scalatraCore % "compile;provided->provided", scalatraScalate,
    scalatraAuth, scalatraAkka, scalatraJetty
  )

  lazy val scalatraFrameworkTests = Project(
    id = "scalatra-framework-tests",
    base = file("framework-tests"),
    settings = scalatraSettings ++ jettyOrbitHack ++ doNotPublish ++ Seq(
      libraryDependencies ++= Seq(httpParsers, logback, servletApi),
      description := "Scalatra framework tests"
    )
  ) dependsOn(
    scalatraCore % "compile;provided->provided",
    scalatraNetty % "compile;provided->provided",
    scalatraJetty % "compile;provided->provided",
    scalatraScalatest % "test->compile;provided->provided",
    scalatraSpecs % "test->compile;provided->provided",
    scalatraSpecs2 % "test->compile;provided->provided"
  )

  object V {
    val antiXml = "0.3"
    val atmosphere = "1.0.0.beta1"
    val jetty = "8.1.3.v20120416"
    val lift = "2.4"
    val akka = "2.0.2"
    val dispatch = "0.8.7"
    val grizzledSlf4j = "0.6.6"
    val netty = "3.5.2.Final"
    val asyncHttpClient = "1.7.5"
    val logback = "1.0.6"
  }

  object Dependencies {
    def antiXml = "com.codecommit" %% "anti-xml" % V.antiXml

    val atmosphere = "org.atmosphere" % "atmosphere-runtime" % V.atmosphere

    val base64 = "net.iharder" % "base64" % "2.3.8"

    val backchatRl = "io.backchat.rl" %% "rl" % "0.3.2-SNAPSHOT"

    val httpParsers = "io.backchat.http" %% "http-parsers" % "0.3.2-SNAPSHOT"

    val chardet = "com.googlecode.juniversalchardet"  % "juniversalchardet" % "1.0.3"

    val twitterCollection = "com.twitter" %% "util-codec" % "4.0.1"

    val mimeUtil = "eu.medsea.mimeutil"                % "mime-util"         % "2.1.3" exclude("org.slf4j", "slf4j-log4j12")

    val akkaActor = "com.typesafe.akka" % "akka-actor" % V.akka
    val akkaTestkit = "com.typesafe.akka" % "akka-testkit" % V.akka % "test"

    val commonsLang3 = "org.apache.commons" % "commons-lang3" % "3.1"

    val asyncHttpClient = "com.ning" % "async-http-client" % V.asyncHttpClient

    val scalaCheck = "org.scala-tools.testing" %% "scalacheck" % "1.9" % "test"

//    val dispatch = "net.databinder" %% "dispatch-http" % V.dispatch

    val grizzledSlf4j = "org.clapper" %% "grizzled-slf4j" % V.grizzledSlf4j

    // See jettyOrbitHack below.
    private def jettyDep(name: String) = "org.eclipse.jetty" % name % V.jetty exclude("org.eclipse.jetty.orbit", "javax.servlet")

//    val testJettyServlet = jettyDep("test-jetty-servlet")
    val jettyServlet = jettyDep("jetty-servlet") 
    val jettyServer = jettyDep("jetty-server") 
    val jettyWebsocket = "org.eclipse.jetty" % "jetty-websocket" % V.jetty  % "provided" exclude("org.eclipse.jetty.orbit", "javax.servlet")
    val jettyWebapp = jettyDep("jetty-webapp") % "test;container"

    val netty = "io.netty" % "netty" % V.netty
    val nettyExtension = "NettyExtension" % "NettyExtension" % "1.1.13"

    val scalaIO = Seq(
      "com.github.scala-incubator.io"    %% "scala-io-core"     % "0.4.0",
      "com.github.scala-incubator.io"    %% "scala-io-file"     % "0.4.0")


    val junit = "junit" % "junit" % "4.10"

    val liftJson = "net.liftweb" %% "lift-json" % V.lift
    val liftJsonExt = "net.liftweb" %% "lift-json-ext" % V.lift

    val jerkson = "io.backchat.jerkson" %% "jerkson" % "0.7.0-SNAPSHOT"

    val mockitoAll = "org.mockito" % "mockito-all" % "1.8.5"

    val scalate = "org.fusesource.scalate" % "scalate-core" % "1.5.3"

    val scalatest = "org.scalatest" %% "scalatest" % "1.6.1"

    val specs = "org.scala-tools.testing" %% "specs" % "1.6.9"

    val specs2 = "org.specs2" %% "specs2" % "1.11"

    val servletApi = "javax.servlet" % "javax.servlet-api" % "3.0.1"

    val slf4jSimple = "org.slf4j" % "slf4j-simple" % "1.6.4"

    val testng = "org.testng" % "testng" % "6.3" % "optional"

    val logback = "ch.qos.logback" % "logback-classic" % V.logback % "provided"
  }

  object Resolvers {
    val sonatypeNexusSnapshots = "Sonatype Nexus Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
    val sonatypeNexusStaging = "Sonatype Nexus Staging" at "https://oss.sonatype.org/service/local/staging/deploy/maven2"
    val goldenGate = "GoldenGate" at "http://openr66.free.fr/maven2"
    val akkaRepository = "Akka Repository" at "http://repo.akka.io/releases/"
    val twitterMaven = "Twitter Maven" at "http://maven.twttr.com/"
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
        <developer>
          <id>sdeboey</id>
          <name>Stefan De Boey</name>
          <url>http://twitter.com/sdeboey</url>
        </developer>
        <developer>
          <id>mnylen</id>
          <name>Mikko Nylén</name>
          <url>http://mnylen.tumblr.com/</url>
        </developer>
        <developer>
          <id>dozed</id>
          <name>Stefan Ollinger</name>
          <url>https://twitter.com/elmac0r</url>
        </developer>
      </developers>
    )}
  )

  lazy val doNotPublish = Seq(publish := {}, publishLocal := {})

  // http://jira.codehaus.org/browse/JETTY-1493
  // https://issues.apache.org/jira/browse/IVY-899
  //
  // This prevents Ivy from attempting to resolve these dependencies,
  // but does not put the exclusions in the pom.  For that, every
  // module that depends on this atrocity needs an explicit exclude
  // statement.
  lazy val jettyOrbitHack = Seq(
    ivyXML := <dependencies>
      <exclude org="org.eclipse.jetty.orbit" />
    </dependencies>
  )
}
