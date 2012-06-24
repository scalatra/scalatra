import sbt._
import Keys._
import scala.xml._
import java.net.URL
import com.github.siasia.WebPlugin.webSettings
// import posterous.Publish._
import ls.Plugin.LsKeys

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
    resolvers ++= Seq(ScalaToolsSnapshots, sonatypeNexusSnapshots),
    (LsKeys.tags in LsKeys.lsync) := Seq("web", "sinatra"),
    (LsKeys.docsUrl in LsKeys.lsync) := Some(new URL("http://www.scalatra.org/%s/book/" format majorVersion))
  ) ++ jettyOrbitHack ++ mavenCentralFrouFrou

  lazy val scalatraProject = Project(
    id = "scalatra-project",
    base = file("."),
    settings = scalatraSettings ++ Unidoc.settings ++ doNotPublish ++ Seq(
      description := "A tiny, Sinatra-like web framework for Scala",
      Unidoc.unidocExclude := Seq("scalatra-example"),
      // (name in Posterous) := "scalatra",
      LsKeys.skipWrite := true
    ),
    aggregate = Seq(scalatraCore, scalatraAuth, scalatraFileupload,
      scalatraScalate, scalatraLiftJson, scalatraAntiXml, scalatraJerkson,
      scalatraTest, scalatraScalatest, scalatraSpecs, scalatraSpecs2,
      scalatraExample, scalatraAkka, scalatraDocs, scalatraSwagger, scalatraJetty)
  )

  lazy val scalatraCore = Project(
    id = "scalatra",
    base = file("core"),
    settings = scalatraSettings ++ Seq(
      libraryDependencies ++= Seq(
        servletApi % "provided;test",
        grizzledSlf4j,
        backchatRl
      ),
      description := "The core Scalatra framework"
    )
  ) dependsOn(Seq(scalatraSpecs2, scalatraSpecs, scalatraScalatest) map { _ % "test->compile" } :_*)

  lazy val scalatraAuth = Project(
    id = "scalatra-auth",
    base = file("auth"),
    settings = scalatraSettings ++ Seq(
      libraryDependencies ++= Seq(base64),
      description := "Scalatra authentication module"
    )
  ) dependsOn(scalatraCore % "compile;test->test;provided->provided")

  lazy val scalatraAkka = Project(
    id = "scalatra-akka",
    base = file("akka"),
    settings = scalatraSettings ++ Seq(
      libraryDependencies ++= Seq(akkaActor, akkaTestkit),
      resolvers += "Akka Repo" at "http://repo.akka.io/repository",
      description := "Scalatra akka integration module"
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
      libraryDependencies ++= Seq(scalate),
      resolvers ++= Seq(sonatypeNexusSnapshots),
      description := "Scalate integration with Scalatra"
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

  lazy val scalatraJerkson = Project(
    id = "scalatra-jerkson",
    base = file("jerkson"),
    settings = scalatraSettings ++ Seq(
      libraryDependencies += jerkson,
      description := "Jackson/Jerkson JSON support for Scalatra"
    )
  ) dependsOn(scalatraCore % "compile;test->test;provided->provided")

  lazy val scalatraAntiXml = Project(
    id = "scalatra-anti-xml",
    base = file("anti-xml"),
    settings = scalatraSettings ++ Seq(
      libraryDependencies += antiXml,
      description := "Anti-XML support for Scalatra"
    )
  ) dependsOn(scalatraCore % "compile;test->test;provided->provided")

  lazy val scalatraJetty = Project(
    id = "scalatra-jetty",
    base = file("jetty"),
    settings = scalatraSettings ++ Seq(
      libraryDependencies ++= Seq(
        servletApi,
        jettyServlet
      ),
      description := "Embedded Jetty server for Scalatra apps"
    )
  ) dependsOn(scalatraCore % "compile;test->test;provided->provided")

  lazy val scalatraTest = Project(
    id = "scalatra-test",
    base = file("test"),
    settings = scalatraSettings ++ Seq(
      libraryDependencies ++= Seq(
        grizzledSlf4j,
        testJettyServlet,
	servletApi % "provided",
        mockitoAll,
        commonsLang3,
        specs2 % "test",
        dispatch
      ),
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

  lazy val scalatraDocs = Project(
    id = "scalatra-docs",
    base = file("docs"),
    settings = scalatraSettings ++ Seq(
      description := "Scalatra legacy documentation; see scalatra-swagger"
    )
  ) dependsOn(scalatraCore % "compile;test->test;provided->provided")

  lazy val scalatraSwagger = Project(
    id = "scalatra-swagger",
    base = file("swagger"),
    settings = scalatraSettings ++ Seq(
      libraryDependencies ++= Seq(liftJson, liftJsonExt),
      description := "Scalatra integration with Swagger"
    )
  ) dependsOn(scalatraCore % "compile;test->test;provided->provided")

  lazy val scalatraExample = Project(
    id = "scalatra-example",
    base = file("example"),
    settings = scalatraSettings ++ webSettings ++ doNotPublish ++ Seq(
      resolvers ++= Seq(sonatypeNexusSnapshots),
      libraryDependencies += servletApi % "container;test",
      libraryDependencies ++= Seq(atmosphere, jettyWebapp, slf4jSimple),
      description := "Scalatra example project"
    )
  ) dependsOn(
    scalatraCore % "compile;test->test;provided->provided", scalatraScalate,
    scalatraAuth, scalatraFileupload, scalatraAkka, scalatraDocs, scalatraJetty
  )

  object Dependencies {
    def antiXml = "com.codecommit" %% "anti-xml" % "0.3"

    val atmosphere = "org.atmosphere" % "atmosphere-runtime" % "1.0.0.beta1"

    val base64 = "net.iharder" % "base64" % "2.3.8"

    val backchatRl = "io.backchat.rl" %% "rl" % "0.3.2-SNAPSHOT"

    val akkaActor = "com.typesafe.akka" % "akka-actor" % "2.0.2"
    val akkaTestkit = "com.typesafe.akka" % "akka-testkit" % "2.0.2" % "test"

    val commonsFileupload = "commons-fileupload" % "commons-fileupload" % "1.2.1"
    val commonsIo = "commons-io" % "commons-io" % "2.1"
    val commonsLang3 = "org.apache.commons" % "commons-lang3" % "3.1"

    val dispatch = "net.databinder" %% "dispatch-http" % "0.8.7"

    def grizzledSlf4j = "org.clapper" %% "grizzled-slf4j" % "0.6.6"

    // See jettyOrbitHack below.
    private def jettyDep(name: String) = "org.eclipse.jetty" % name % "8.1.3.v20120416" exclude("org.eclipse.jetty.orbit", "javax.servlet")

    val testJettyServlet = jettyDep("test-jetty-servlet")    
    val jettyServlet = jettyDep("jetty-servlet") 
    val jettyServer = jettyDep("jetty-server") 
    val jettyWebsocket = "org.eclipse.jetty" % "jetty-websocket" % "8.1.3.v20120416"  % "provided" exclude("org.eclipse.jetty.orbit", "javax.servlet")
    val jettyWebapp = jettyDep("jetty-webapp") % "test;container"

    val junit = "junit" % "junit" % "4.10"

    val liftJson = "net.liftweb" %% "lift-json" % "2.4"
    val liftJsonExt = "net.liftweb" %% "lift-json-ext" % "2.4"

    val jerkson = "io.backchat.jerkson" %% "jerkson" % "0.7.0-SNAPSHOT"

    val mockitoAll = "org.mockito" % "mockito-all" % "1.8.5"

    def scalate = "org.fusesource.scalate" % "scalate-core" % "1.5.3"

    def scalatest = "org.scalatest" %% "scalatest" % "1.6.1"

    def specs = "org.scala-tools.testing" %% "specs" % "1.6.9"

    def specs2 = "org.specs2" %% "specs2" % "1.11"

    val servletApi = "javax.servlet" % "javax.servlet-api" % "3.0.1"

    val slf4jSimple = "org.slf4j" % "slf4j-simple" % "1.6.4"

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
