import sbt._
import Keys._
import scala.xml._
import java.net.URL
import com.github.siasia.WebPlugin.webSettings

object ScalatraBuild extends Build {
  import Dependencies._
  import Resolvers._

  lazy val scalatraSettings = Defaults.defaultSettings ++ Seq(
    organization := "org.scalatra",
    version := "2.0.5-SNAPSHOT",
    crossScalaVersions := Seq("2.10.0", "2.9.2", "2.9.1", "2.9.0-1", "2.8.2", "2.8.1"),
    scalaVersion <<= (crossScalaVersions) { versions => versions.head },
    scalacOptions ++= Seq("-unchecked", "-deprecation", "-Xcheckinit", "-encoding", "utf8"),
    javacOptions ++= Seq("-target", "1.6", "-source", "1.6", "-Xlint:deprecation"),
    manifestSetting,
    publishSetting,
    resolvers ++= Seq(sonatypeNexusSnapshots)
  ) ++ mavenCentralFrouFrou

  lazy val scalatraProject = Project(
    id = "scalatra-project",
    base = file("."),
    settings = scalatraSettings ++ Unidoc.settings ++ doNotPublish ++ Seq(
      description := "A tiny, Sinatra-like web framework for Scala",
      Unidoc.unidocExclude := Seq("scalatra-example")
    ),
    aggregate = Seq(scalatraCore, scalatraAuth, scalatraFileupload,
      scalatraScalate, scalatraSocketio, scalatraLiftJson, scalatraAntiXml,
      scalatraTest, scalatraScalatest, scalatraSpecs, scalatraSpecs2,
      scalatraExample, scalatraJetty8Tests)
  )

  lazy val scalatraCore = Project(
    id = "scalatra",
    base = file("core"),
    settings = scalatraSettings ++ Seq(
      libraryDependencies ++= Seq(servletApi % "provided", slf4jSimple % "test"),
      description := "The core Scalatra framework"
    )
  ) dependsOn(Seq(scalatraSpecs2, scalatraSpecs, scalatraScalatest) map { _ % "test->compile" } :_*)

  lazy val scalatraAuth = Project(
    id = "scalatra-auth",
    base = file("auth"),
    settings = scalatraSettings ++ Seq(
      libraryDependencies <++= scalaVersion(sv => Seq(base64, liftTestkit(sv))),
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
      libraryDependencies <+= scalaVersion(scalate),
      description := "Scalate integration with Scalatra"
    )
  ) dependsOn(scalatraCore % "compile;test->test;provided->provided")

  lazy val scalatraSocketio = Project(
    id = "scalatra-socketio",
    base = file("socketio"),
    settings = scalatraSettings ++ Seq(
      libraryDependencies ++= Seq(jettyWebsocket, socketioCore),
      description := "Socket IO support for Scalatra"
    )
  ) dependsOn(scalatraCore % "compile;test->test;provided->provided")

  lazy val scalatraLiftJson = Project(
    id = "scalatra-lift-json",
    base = file("lift-json"),
    settings = scalatraSettings ++ Seq(
      libraryDependencies <+= scalaVersion(liftJson),
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
      libraryDependencies <++= scalaVersion(sv => Seq(testJettyServlet, mockitoAll, commonsLang3, specs2(sv) % "test")),
      description := "The abstract Scalatra test framework"
    )
  )

  lazy val scalatraScalatest = Project(
    id = "scalatra-scalatest",
    base = file("scalatest"),
    settings = scalatraSettings ++ Seq(
      libraryDependencies <++= scalaVersion(sv => Seq(scalatest(sv), junit, testng % "optional", guice % "optional")),
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
      libraryDependencies ++= Seq(servletApi % "container;provided", jettyWebapp % "container"),
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
      libraryDependencies ++= Seq(servletApi_3_0 % "test", testJettyServlet8 % "test"),
      description := "Compatibility tests for Jetty 8"
    )
  ) dependsOn(scalatraSpecs2 % "test->compile")

  object Dependencies {

    // Sort by artifact ID.
    lazy val antiXml: MM           = sv => antiXmlGroup(sv)          %% "anti-xml"           % antiXmlVersion(sv)
    lazy val base64                     =  "net.iharder"             %  "base64"             % "2.3.8"
    lazy val commonsFileupload          =  "commons-fileupload"      %  "commons-fileupload" % "1.2.2"
    lazy val commonsIo                  =  "commons-io"              %  "commons-io"         % "2.4"
    lazy val commonsLang3               =  "org.apache.commons"      %  "commons-lang3"      % "3.1"
    lazy val guice                      =  "com.google.inject"       %  "guice"              % "3.0"
    lazy val jettyWebsocket             =  "org.eclipse.jetty"       %  "jetty-websocket"    % jettyVersion
    lazy val jettyWebapp                =  "org.eclipse.jetty"       %  "jetty-webapp"       % jettyVersion
    lazy val junit                      =  "junit"                   %  "junit"              % "4.11"
    lazy val liftJson: MM          = sv => "net.liftweb"             %% "lift-json"          % liftVersion(sv)
    lazy val liftTestkit: MM       = sv => "net.liftweb"             %% "lift-testkit"       % liftVersion(sv)
    lazy val mockitoAll                 =  "org.mockito"             %  "mockito-all"        % "1.8.5"
    lazy val scalate: MM           = sv => "org.fusesource.scalate"  %  scalateArtifact(sv)  % scalateVersion(sv)
    lazy val scalatest: MM         = sv => "org.scalatest"           %% "scalatest"          % scalatestVersion(sv)
    lazy val servletApi                 =  "org.eclipse.jetty.orbit" %  "javax.servlet"      % "2.5.0.v201103041518" artifacts (Artifact("javax.servlet", "jar", "jar"))
    lazy val servletApi_3_0             =  "org.eclipse.jetty.orbit" %  "javax.servlet"      % "3.0.0.v201112011016" artifacts (Artifact("javax.servlet", "jar", "jar"))
    lazy val slf4jSimple                =  "org.slf4j"               % "slf4j-simple"        % "1.7.2"
    lazy val socketioCore               =  "org.scalatra.socketio-java" % "socketio-core"    % "2.0.0"
    lazy val specs: MM             = sv => "org.scala-tools.testing" %  "specs"              % specsVersion(sv)     cross specsCross
    lazy val specs2: MM            = sv => "org.specs2"              %% "specs2"             % specs2Version(sv)
    lazy val testJettyServlet           =  "org.eclipse.jetty"       %  "test-jetty-servlet" % jettyVersion
    lazy val testJettyServlet8          =  "org.eclipse.jetty"       %  "test-jetty-servlet" % jetty8Version
    lazy val testng                     =  "org.testng"              %  "testng"             % "6.8"

    type MM = String => ModuleID

    // Now entering Cross Build Hell

    private val antiXmlGroup: String => String = {
      case sv if sv startsWith "2.8."   => "com.codecommit"
      case "2.9.0-1"                    => "com.codecommit"
      case "2.9.1"                      => "com.codecommit"
      case _                            => "no.arktekk"
    }
    private val antiXmlVersion: String => String = {
      case sv if sv startsWith "2.8."   => "0.2"
      case "2.9.0-1"                    => "0.3"
      case "2.9.1"                      => "0.3"
      case _                            => "0.5.1"
    }

    private val jettyVersion  = "7.6.8.v20121106"
    private val jetty8Version = "8.1.8.v20121106"

    private val liftVersion: String => String = {
      case sv if sv startsWith "2.8."   => "2.4"
      case "2.9.0-1"                    => "2.4"
      case "2.9.1"                      => "2.4"
      case _                            => "2.5-M4"
    }

    private val scalateArtifact: String => String = {
      case sv if sv startsWith "2.8."   => "scalate-core"
      case "2.9.0-1"                    => "scalate-core"
      case sv if sv startsWith "2.9."   => "scalate-core_2.9"
      case sv if sv startsWith "2.10."  => "scalate-core_2.10"
    }
    private val scalateVersion: String => String = {
      case "2.8.1"                      => "1.5.2-scala_2.8.1"
      case "2.8.2"                      => "1.5.3-scala_2.8.2"
      case "2.9.0-1"                    => "1.5.1"
      case "2.9.1"                      => "1.6.1"
      case "2.9.2"                      => "1.6.1"
      case _                            => "1.6.1"
    }

    private val scalatestVersion: String => String = {
      case sv if sv startsWith "2.8."   => "1.8"
      case _                            => "1.9.1"
    }

    private val specsCross = CrossVersion.binaryMapped {
      case "2.8.2"                      => "2.8.1" // _2.8.2 published with bad checksum
      case "2.9.2"                      => "2.9.1"
      case "2.10.0"                     => "2.10"  // sbt bug?
      case bin                          => bin
    }
    private val specsVersion: String => String = {
      case sv if sv startsWith "2.8."   => "1.6.8"
      case "2.9.0-1"                    => "1.6.8"
      case _                            => "1.6.9"
    }

    private val specs2Version: String => String = {
      case sv if sv startsWith "2.8."   => "1.5"
      case "2.9.0-1"                    => "1.8.2"
      case sv if sv startsWith "2.9."   => "1.12.3"
      case _                            => "1.13"
    }
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
