import sbt._
import Keys._

// TODO: Crossbuild support
// TODO: Maven Central metadata
// TODO: Sign with PGP
// TODO: Define repositories for publishing
// TODO: Build example project
// TODO: Build website project
object ScalatraBuild extends Build {
  val scalatraSettings = Defaults.defaultSettings ++ Seq(
    organization := "org.scalatra",
    version := "2.0.0-SNAPSHOT",
    scalaVersion := "2.9.0-1",
    packageOptions <<= (packageOptions, name, version, organization) map {
      (opts, title, version, vendor) => 
        opts :+ Package.ManifestAttributes(
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
  )

  val sonatypeSnapshots = "Sonatype Nexus Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

  object Dependencies {
    val base64 = "net.iharder" % "base64" % "2.3.8"

    val commonsFileupload = "commons-fileupload" % "commons-fileupload" % "1.2.1"

    val commonsIo = "commons-io" % "commons-io" % "1.4"

    private def jettyDep(name: String) = "org.eclipse.jetty" % name % "7.4.1.v20110513"
    val testJettyServlet = jettyDep("test-jetty-servlet")
    val jettyWebsocket = jettyDep("jetty-websocket")

    val junit = "junit" % "junit" % "4.8.1"

    val scalate = "org.fusesource.scalate" % "scalate-core" % "1.5.0"

    val scalatest = "org.scalatest" % "scalatest_2.9.0" % "1.4.1"

    val specs = "org.scala-tools.testing" % "specs_2.9.0" % "1.6.8"

    val specs2 = "org.specs2" %% "specs2" % "1.3"

    val servletApi = "javax.servlet" % "servlet-api" % "2.5" % "provided"

    def socketioCore(version: String) = "org.scalatra.socketio-java" % "socketio-core" % version
  }
  import Dependencies._

  lazy val scalatraProject = Project("scalatra-project", file("."), 
    settings = scalatraSettings)
    .settings(publishArtifact in Compile := false)
    .aggregate(scalatraCore, scalatraAuth, scalatraFileupload,
      scalatraScalate, scalatraSocketio,
      scalatraTest, scalatraScalatest, scalatraSpecs, scalatraSpecs2)

  lazy val scalatraCore = Project("scalatra", file("core"), 
    settings = scalatraSettings)
    .settings(libraryDependencies := Seq(servletApi))
    .testWithScalatraTest

  lazy val scalatraAuth = Project("scalatra-auth", file("auth"), 
    settings = scalatraSettings)
    .settings(libraryDependencies := Seq(servletApi, base64))
    .dependsOn(scalatraCore)
    .testWithScalatraTest

  lazy val scalatraFileupload = Project("scalatra-fileupload", file("fileupload"), 
    settings = scalatraSettings)
    .settings(libraryDependencies := Seq(servletApi, commonsFileupload, commonsIo))
    .dependsOn(scalatraCore)
    .testWithScalatraTest

  lazy val scalatraScalate = Project("scalatra-scalate", file("scalate"), 
    settings = scalatraSettings)
    .settings(libraryDependencies := Seq(scalate, servletApi))
    .dependsOn(scalatraCore)
    .testWithScalatraTest

  lazy val scalatraSocketio = Project("scalatra-socketio", file("socketio"), 
    settings = scalatraSettings)
    .settings(
      libraryDependencies <<= (version, libraryDependencies) {
        (v, deps) => deps ++ Seq(jettyWebsocket, socketioCore(v))
      },
      resolvers += sonatypeSnapshots
    )
    .dependsOn(scalatraCore)
    .testWithScalatraTest

  lazy val scalatraTest = Project("scalatra-test", file("test"), 
    settings = scalatraSettings)
    .settings(libraryDependencies := Seq(testJettyServlet))

  lazy val scalatraScalatest = Project("scalatra-scalatest", file("scalatest"), 
    settings = scalatraSettings)
    .settings(libraryDependencies := Seq(scalatest, junit))
    .dependsOn(scalatraTest)

  lazy val scalatraSpecs = Project("scalatra-specs", file("specs"), 
    settings = scalatraSettings)
    .settings(libraryDependencies := Seq(specs))
    .dependsOn(scalatraTest)

  lazy val scalatraSpecs2 = Project("scalatra-specs2", file("specs2"), 
    settings = scalatraSettings)
    .settings(libraryDependencies := Seq(specs2))
    .dependsOn(scalatraTest)

  class RichProject(project: Project) {
    def testWithScalatraTest = {
      val testProjects = Seq(scalatraScalatest, scalatraSpecs, scalatraSpecs2)
      val testDeps = testProjects map { _ % "test" }
      project.dependsOn(testDeps : _*)
    }
  }
  implicit def project2RichProject(project: Project): RichProject = new RichProject(project) 
}
