import sbt._

import scala.xml._
import java.io.File
import org.fusesource.scalate.sbt._

class ScalatraProject(info: ProjectInfo) 
  extends ParentProject(info)
  with UnifiedScaladoc
  with posterous.Publish
{
  val jettyGroupId = "org.eclipse.jetty"
  val jettyVersion = "7.3.1.v20110307"
  val slf4jVersion = "1.6.1"
  val scalateVersion = buildScalaVersion match {
    case "2.8.0" => "1.3.2"
    case _ => "1.4.1"
  }

  trait ScalatraSubproject
    extends MavenCentralScalaProject
    with MavenCentralProject
    with BasicPackagePaths
  {
    def licenses =
      <licenses>
        <license>
          <name>BSD</name>
          <url>http://github.com/scalatra/scalatra/raw/HEAD/LICENSE</url>
          <distribution>repo</distribution>
        </license>
      </licenses>

    def developers =
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
      </developers>

    def projectUrl = "http://www.scalatra.org/"
    def scmUrl = "http://github.com/scalatra/scalatra"
    def scmConnection = "scm:git:git://github.com/scalatra/scalatra.git"

    def organizationName = "Scalatra Project"

    override def pomExtra = super.pomExtra ++ (
      <inceptionYear>2009</inceptionYear>
      <organization>
        <name>{organizationName}</name>
        <url>http://www.scalatra.org/</url>
      </organization>
      <mailingLists>
        <mailingList>
          <name>Scalatra user group</name>
          <archive>http://groups.google.com/group/scalatra-user</archive>
          <post>scalatra-user@googlegroups.com</post>
          <subscribe>scalatra-user+subscribe@googlegroups.com</subscribe>
          <unsubscribe>scalatra-user+unsubscribe@googlegroups.com</unsubscribe>
        </mailingList>
      </mailingLists>
    )

    val servletApi = "javax.servlet" % "servlet-api" % "2.5" % "provided"
    override def managedStyle = ManagedStyle.Maven

    override def packageOptions = ManifestAttributes(
      "Created-By" -> "Simple Build Tool",
      "Built-By" -> system[String]("user.name").get.get,
      "Build-Jdk" -> system[String]("java.version").get.get,
      "Specification-Title" -> projectName.get.get,
      "Specification-Version" -> projectVersion.get.get.toString,
      "Specification-Vendor" -> organizationName,
      "Implementation-Title" -> projectName.get.get,
      "Implementation-Version" -> projectVersion.get.get.toString,
      "Implementation-Vendor-Id" -> projectOrganization.get.get,
      "Implementation-Vendor" -> organizationName
    ) :: super.packageOptions.toList

    // specs2 has a bad checksum that is crashing the build.
    override def resolverChecksumAlgorithms =
      ({ case "Scala-Tools Maven2 Snapshots Repository" => Nil }: PartialFunction[String, Seq[String]]) orElse super.resolverChecksumAlgorithms
  }

  trait TestWithScalatraTest extends TestWith {
    override def testWithTestClasspath = List(scalatest, specs, specs2)
  }

  lazy val core = project("core", "scalatra", new CoreProject(_))
  class CoreProject(info: ProjectInfo) extends DefaultProject(info) with ScalatraSubproject with TestWithScalatraTest {
    val mockito = "org.mockito" % "mockito-all" % "1.8.4" % "test"
    val description = "The core Scalatra library"
  }

  lazy val auth = project("auth", "scalatra-auth", new AuthProject(_), core)
  class AuthProject(info: ProjectInfo) extends DefaultProject(info) with ScalatraSubproject with TestWithScalatraTest {
    val mockito = "org.mockito" % "mockito-all" % "1.8.4" % "test"
    val description = "Supplies optional Scalatra authentication support"
    val base64 = "net.iharder" % "base64" % "2.3.8" % "compile"
  }

  lazy val fileupload = project("fileupload", "scalatra-fileupload", new FileuploadProject(_), core)
  class FileuploadProject(info: ProjectInfo) extends DefaultProject(info) with ScalatraSubproject with TestWithScalatraTest {
    val commonsFileupload = "commons-fileupload" % "commons-fileupload" % "1.2.1" % "compile"
    val commonsIo = "commons-io" % "commons-io" % "1.4" % "compile"
    val description = "Supplies the optional Scalatra file upload support"
  }

  lazy val scalate = project("scalate", "scalatra-scalate", new ScalateProject(_), core)
  class ScalateProject(info: ProjectInfo) extends DefaultProject(info) with ScalatraSubproject with TestWithScalatraTest {
    val scalate = "org.fusesource.scalate" % "scalate-core" % scalateVersion
    val description = "Supplies the optional Scalatra Scalate support"
  }

  lazy val socketio = project("socketio", "scalatra-socketio", new SocketIOProject(_), core)
  class SocketIOProject(info: ProjectInfo) extends DefaultProject(info) with ScalatraSubproject with TestWithScalatraTest {
    val websocket = jettyGroupId % "jetty-websocket" % jettyVersion % "provided"
    val socketIoJava = "org.scalatra.socketio-java" % "socketio-core" % projectVersion.get.get.toString
    val description = "Supplies optional SocketIO support for scalatra"
  }

  lazy val example = project("example", "scalatra-example", new ExampleProject(_), core, fileupload, scalate, auth, socketio)
  class ExampleProject(info: ProjectInfo) extends DefaultWebProject(info) with ScalatraSubproject with UnpublishedProject {
    val jetty7 = jettyGroupId % "jetty-webapp" % jettyVersion % "test"
    val jetty7websocket = jettyGroupId % "jetty-websocket" % jettyVersion % "compile"
    val sfl4japi = "org.slf4j" % "slf4j-api" % slf4jVersion % "compile"
    val sfl4jnop = "org.slf4j" % "slf4j-nop" % slf4jVersion % "runtime"

    override def webappClasspath = super.webappClasspath +++ buildCompilerJar

    val description = "An example Scalatra application"
  }

  lazy val website = project("website", "scalatra-website", new WebsiteProject(_), core)
  class WebsiteProject(info: ProjectInfo)
    extends DefaultWebProject(info)
    with SiteGenWebProject
    with UnpublishedProject
  {
    override val jettyPort = 8081
    val scalatePage = "org.fusesource.scalate" % "scalate-page" % scalateVersion
    val jetty7 = jettyGroupId % "jetty-webapp" % jettyVersion % "test"
    val logback = "org.slf4j" % "slf4j-nop" % slf4jVersion % "runtime"
    val markdown = "org.fusesource.scalamd" % "scalamd" % "1.5" % "runtime"
    val description = "Runs www.scalatra.org"

    override lazy val generateSite =
      if (scalateVersion.startsWith("1.3."))
        task {
          log.info("sitegen only supported by Scalate 1.4.0 and above")
          None
        }
      else
        super.generateSiteAction

    // Embed our sources as resources so we can include them in the site and
    // ensure that they're compilable.
    override def webappClasspath = super.webappClasspath +++ mainScalaSourcePath
  }

  lazy val scalatraTest = project("test", "scalatra-test", new DefaultProject(_) with ScalatraSubproject {
    val description = "Scalatra test framework"
    val jettytester = jettyGroupId % "test-jetty-servlet" % jettyVersion % "compile"
  })

  lazy val scalatest = project("scalatest", "scalatra-scalatest", new DefaultProject(_) with ScalatraSubproject {
    val scalatest = "org.scalatest" % "scalatest" % "1.3" % "compile"
    val junit = "junit" % "junit" % "4.8.1" % "compile"
    val description = "ScalaTest support for the Scalatra test framework"
  }, scalatraTest)

  lazy val specs = project("specs", "scalatra-specs", new DefaultProject(_) with ScalatraSubproject {
    val specsVersion = buildScalaVersion match {
      case "2.8.0" => "1.6.5"
      case _ => "1.6.7"
    }
    val specs = "org.scala-tools.testing" %% "specs" % specsVersion % "compile"
    val description = "Specs support for the Scalatra test framework"
  }, scalatraTest)

  lazy val specs2 = project("specs2", "scalatra-specs2", new DefaultProject(_) with ScalatraSubproject {
    val specsVersion = "1.1"
    val specsScalaVersion = buildScalaVersion match {
      case "2.8.0" => "2.8.1" // Not released for 2.8.0, but should be compatible
      case x => x
    }
    val specs = "org.specs2" % ("specs2_"+specsScalaVersion) % specsVersion % "compile"
    val description = "Specs 2 support for the Scalatra test framework"
  }, scalatraTest)

  lazy val archetype = project("archetype", "simple-scalatra-archetype", new ArchetypeProject(_))
  class ArchetypeProject(info: ProjectInfo) extends DefaultProject(info) with ScalatraSubproject

  val fuseSourceSnapshots = "FuseSource Snapshot Repository" at "http://repo.fusesource.com/nexus/content/repositories/snapshots"
  val scalaToolsSnapshots = "Scala-Tools Maven2 Snapshots Repository" at "http://scala-tools.org/repo-snapshots"


  val publishTo = {
    val local = System.getenv("MOJOLLY_HOME")
    if(local == null || local.trim.length == 0 || local == ".") {
      Credentials(Path.userHome / ".ivy2" / "credentials" / "oss.sonatype.org", log)
      if (version.toString.endsWith("-SNAPSHOT"))
        "Sonatype Nexus Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
      else
        "Sonatype Nexus Release Staging" at "https://oss.sonatype.org/service/local/staging/deploy/maven2"
    } else {
      System.setProperty("gpg.skip", "true")
      Credentials(Path.userHome / ".ivy2" / ".credentials", log)

      if(version.toString.endsWith("-SNAPSHOT"))
        "Mojolly Snapshots" at "https://maven.mojolly.com/content/repositories/thirdparty-snapshots/"
      else
        "Mojolly Releases" at "https://maven.mojolly.com/content/repositories/thirdparty/"

    }

  }
  // Without this, scalatra-scalatest and scalatra-specs can't find scalatra-test
  val scalatraRepo = publishTo

  val sonatypeRepo = "Sonatype Nexus Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

  override def deliverProjectDependencies = Nil

  // Tweak posterous settings
  override def postTitle(vers: String) = "%s %s".format("Scalatra", vers)
  override def postTags = "Scalatra" :: crossScalaVersions.map { "Scala " + _ }.toList
}
