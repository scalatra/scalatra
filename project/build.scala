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

  lazy val majorVersion = "2.2"

  lazy val scalatraSettings = Defaults.defaultSettings ++ ls.Plugin.lsSettings ++ Seq(
    organization := "org.scalatra",
    version := "%s.0-SNAPSHOT" format majorVersion,
    scalaVersion := "2.9.2",
    scalacOptions ++= Seq("-unchecked", "-deprecation"),
    javacOptions ++= Seq("-target", "1.6", "-source", "1.6"),
    manifestSetting,
    publishSetting,
    crossPaths := false,
    resolvers ++= Seq( sonatypeNexusSnapshots, sonatypeNexusReleases),
    (LsKeys.tags in LsKeys.lsync) := Seq("web", "sinatra"),
    (LsKeys.docsUrl in LsKeys.lsync) := Some(new URL("http://www.scalatra.org/%s/book/" format majorVersion))
  ) ++ jettyOrbitHack ++ mavenCentralFrouFrou

  lazy val scalatraProject = Project(
    id = "scalatra-project",
    base = file("."),
    settings = scalatraSettings ++ Unidoc.unidocSettings ++ doNotPublish ++ Seq(
      description := "A tiny, Sinatra-like web framework for Scala",
      Unidoc.unidocExclude := Seq("scalatra-example"),
      // (name in Posterous) := "scalatra",
      LsKeys.skipWrite := true
    ),
    aggregate = Seq(scalatraCore, scalatraAuth, scalatraFileupload, scalatraDatabinding,
      scalatraScalate, scalatraJson, scalatraJackson, scalatraLiftJson, scalatraSlf4j,
      scalatraTest, scalatraScalatest, scalatraSpecs, scalatraSpecs2,
     scalatraExample, scalatraAkka, scalatraSwagger, scalatraJetty)
  )

  lazy val scalatraCore = Project(
    id = "scalatra",
    base = file("core"),
    settings = scalatraSettings ++ Seq(
      libraryDependencies ++= Seq(
        servletApiProvided,
        grizzledSlf4j,
        backchatRl,
        jodaTime,
        jodaConvert
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

  lazy val scalatraJson = Project(
    id = "scalatra-json",
    base = file("json"),
    settings = scalatraSettings ++ Seq(
      description := "JSON support for Scalatra, contains only common marker interfaces"
    )
  ) dependsOn(scalatraCore % "compile;test->test;provided->provided")

  lazy val scalatraLiftJson = Project(
    id = "scalatra-lift-json",
    base = file("lift-json"),
    settings = scalatraSettings ++ Seq(
      libraryDependencies += liftJson,
      description := "Lift JSON support for Scalatra"
    )
  ) dependsOn(scalatraJson % "compile;test->test;provided->provided")

  lazy val scalatraJackson = Project(
    id = "scalatra-jackson",
    base = file("jackson"),
    settings = scalatraSettings ++ Seq(
      libraryDependencies ++= jackson,
      description := "Jackson support for Scalatra"
    )
  ) dependsOn(scalatraJson % "compile;test->test;provided->provided")

  lazy val scalatraDatabinding = Project(
    id = "scalatra-data-binding",
    base = file("data-binding"),
    settings = scalatraSettings ++ Seq(
      libraryDependencies ++= Seq(
        "commons-validator"       % "commons-validator"  % "1.4.0",
        "io.backchat.inflector"  %% "scala-inflector"    % "1.3.4"
      ),
      libraryDependencies ++= Seq(scalaz, jodaTime, jodaConvert),
      description := "Data binding and validation with scalaz for Scalatra"
    )
  ) dependsOn(
    scalatraJson % "compile;test->test;provided->provided",
    scalatraLiftJson % "provided->compile;test->compile",
    scalatraJackson % "provided->compile;test->compile")

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
        servletApi,
        testJettyServlet,
        mockitoAll,
        commonsLang3,
        specs2 % "test",
        httpClient,
        httpMime,
        jodaTime % "provided",
        jodaConvert % "provided"
      ),
      description := "The abstract Scalatra test framework"
    )
  )

  lazy val scalatraScalatest = Project(
    id = "scalatra-scalatest",
    base = file("scalatest"),
    settings = scalatraSettings ++ Seq(
      libraryDependencies ++= Seq(scalatest, junit, testng, guice),
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
  ) dependsOn(scalatraCore % "compile;test->test;provided->provided")

  lazy val scalatraSlf4j = Project(
    id = "scalatra-slf4j",
    base = file("slf4j"),
    settings = scalatraSettings ++ Seq(
      libraryDependencies ++= Seq(grizzledSlf4j, logback % "provided"),
      description := "Scalatra integration with SLF4J and Logback"
    )
  ) dependsOn(scalatraCore % "compile;test->test;provided->provided")

 lazy val scalatraExample = Project(
   id = "scalatra-example",
   base = file("example"),
   settings = scalatraSettings ++ webSettings ++ doNotPublish ++ Seq(
     resolvers ++= Seq(sonatypeNexusSnapshots),
     libraryDependencies += servletApiTest,
     libraryDependencies ++= Seq(atmosphere, jettyWebapp, slf4jSimple),
     description := "Scalatra example project"
   )
 ) dependsOn(
   scalatraCore % "compile;test->test;provided->provided", scalatraScalate,
   scalatraAuth, scalatraFileupload, scalatraAkka, scalatraJetty
 )

  object Dependencies {

    val atmosphere = "org.atmosphere" % "atmosphere-runtime" % "1.0.0.beta1"

    val base64 = "net.iharder" % "base64" % "2.3.8"

    val backchatRl = "io.backchat.rl" %% "rl" % "0.3.2"

    val akkaActor = "com.typesafe.akka" % "akka-actor" % "2.0.2"
    val akkaTestkit = "com.typesafe.akka" % "akka-testkit" % "2.0.2" % "test"

    val commonsFileupload = "commons-fileupload" % "commons-fileupload" % "1.2.1"
    val commonsIo = "commons-io" % "commons-io" % "2.1"
    val commonsLang3 = "org.apache.commons" % "commons-lang3" % "3.1"
//
//    val dispatch = "net.databinder.dispatch" % "core_2.9.2" % "0.9.0"

    val httpClient = "org.apache.httpcomponents" % "httpclient" % "4.2"

    val httpMime   = "org.apache.httpcomponents" % "httpmime"   % "4.2"

    val grizzledSlf4j = "org.clapper" %% "grizzled-slf4j" % "0.6.9"

    // See jettyOrbitHack below.
    private def jettyDep(name: String) = "org.eclipse.jetty" % name % "8.1.3.v20120416" 

    val testJettyServlet = jettyDep("test-jetty-servlet")
    val jettyServlet = jettyDep("jetty-servlet")
    val jettyWebapp = jettyDep("jetty-webapp") % "test;container"

    val junit = "junit" % "junit" % "4.10"

    val liftJson = "net.liftweb" % "lift-json_2.9.2" % "2.5-SNAPSHOT"
    val liftJsonExt = "net.liftweb" % "lift-json-ext_2.9.2" % "2.5-SNAPSHOT"

    val jackson = Seq(
      "com.fasterxml.jackson.core" % "jackson-databind" % "2.0.5",
      "com.fasterxml.jackson.module" % "jackson-module-scala" % "2.0.2",
      "com.fasterxml.jackson.dataformat" % "jackson-dataformat-xml" % "2.0.4")


    val mockitoAll = "org.mockito" % "mockito-all" % "1.9.0"

    val scalate = "org.fusesource.scalate" % "scalate-core" % "1.5.3"

    val scalatest = "org.scalatest" %% "scalatest" % "1.8"

    val testng = "org.testng" % "testng" % "6.7" % "optional"

    val guice = "com.google.inject" % "guice" % "3.0" % "optional"

    val specs = "org.scala-tools.testing" %% "specs" % "1.6.9"

    val specs2 = "org.specs2" %% "specs2" % "1.12"

    val servletApi = "org.eclipse.jetty.orbit" % "javax.servlet" % "3.0.0.v201112011016" artifacts (Artifact("javax.servlet", "jar", "jar"))

    val servletApiProvided = "org.eclipse.jetty.orbit" % "javax.servlet" % "3.0.0.v201112011016" % "provided;test" artifacts (Artifact("javax.servlet", "jar", "jar"))

    val servletApiTest = "org.eclipse.jetty.orbit" % "javax.servlet" % "3.0.0.v201112011016" % "container;test" artifacts (Artifact("javax.servlet", "jar", "jar"))

    val slf4jSimple = "org.slf4j" % "slf4j-simple" % "1.6.6"

    val logback = "ch.qos.logback" % "logback-classic" % "1.0.6"

    val scalaz = "org.scalaz" %% "scalaz-core" % "6.0.4"

    val jodaTime = "joda-time" % "joda-time" % "2.1"

    val jodaConvert = "org.joda" % "joda-convert" % "1.2"
  }

  object Resolvers {
    val sonatypeNexusSnapshots = "Sonatype Nexus Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
    val sonatypeNexusReleases = "Sonatype Nexus Releases" at "https://oss.sonatype.org/content/repositories/releases"
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
    classpathTypes ~= (_ + "orbit")
  )

}
