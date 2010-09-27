import sbt._

import scala.xml._
import scala.util.Sorting._
import com.rossabaker.sbt.gpg._

class ScalatraProject(info: ProjectInfo) 
  extends ParentProject(info)
  with GpgPlugin
  with ChecksumPlugin
{
  override def shouldCheckOutputDirectories = false

  val jettyGroupId = "org.mortbay.jetty"
  val jettyVersion = "6.1.22"
  val slf4jVersion = "1.6.0"

  trait UnpublishedProject
    extends BasicManagedProject
  {
    override def publishLocalAction = task { None }
    override def deliverLocalAction = task { None }
    override def publishAction = task { None }
    override def deliverAction = task { None }
    override def artifacts = Set.empty
  }

  trait ScalatraSubProject 
    extends BasicScalaProject 
    with BasicPackagePaths 
    with GpgPlugin
    with ChecksumPlugin
  {
    def description: String

    val servletApi = "org.mortbay.jetty" % "servlet-api" % "2.5-20081211" % "provided"

    override def pomExtra = (
      <parent>
        <groupId>{organization}</groupId>
        <artifactId>{ScalatraProject.this.artifactID}</artifactId>
        <version>{version}</version>
      </parent>
      <name>{name}</name>
      <description>{description}</description>)

    override def packageDocsJar = defaultJarPath("-javadoc.jar")
    override def packageSrcJar= defaultJarPath("-sources.jar")
    // If these aren't lazy, then the build crashes looking for
    // ${moduleName}/project/build.properties.
    lazy val sourceArtifact = Artifact.sources(artifactID)
    lazy val docsArtifact = Artifact.javadoc(artifactID)
    override def packageToPublishActions = super.packageToPublishActions ++ Seq(packageDocs, packageSrc)
  }

  lazy val core = project("core", "scalatra", new CoreProject(_), scalatest, specs)
  class CoreProject(info: ProjectInfo) extends DefaultProject(info) with ScalatraSubProject {
    val mockito = "org.mockito" % "mockito-all" % "1.8.4" % "test"
    val description = "The core Scalatra library"
    override def deliverProjectDependencies =
      super.deliverProjectDependencies.toList - scalatest.projectID ++ Seq(scalatest.projectID % "test")
  }

  lazy val scentry = project("scentry", "scalatra-auth", new ScentryProject(_), core)
  class ScentryProject(info: ProjectInfo) extends DefaultProject(info) with ScalatraSubProject {
    val mockito = "org.mockito" % "mockito-all" % "1.8.4" % "test"
    val description = "Supplies optional Scalatra authentication support"
  }

  lazy val fileupload = project("fileupload", "scalatra-fileupload", new FileuploadProject(_), core, scalatest)
  class FileuploadProject(info: ProjectInfo) extends DefaultProject(info) with ScalatraSubProject {
    val commonsFileupload = "commons-fileupload" % "commons-fileupload" % "1.2.1" % "compile"
    val commonsIo = "commons-io" % "commons-io" % "1.4" % "compile"
    val description = "Supplies the optional Scalatra file upload support"
    override def deliverProjectDependencies =
      super.deliverProjectDependencies.toList - scalatest.projectID ++ Seq(scalatest.projectID % "test")
  }

  lazy val scalate = project("scalate", "scalatra-scalate", new ScalateProject(_), core, scalatest)
  class ScalateProject(info: ProjectInfo) extends DefaultProject(info) with ScalatraSubProject {
    val scalate = "org.fusesource.scalate" % "scalate-core" % "1.3-SNAPSHOT"
    val description = "Supplies the optional Scalatra Scalate support"
    override def deliverProjectDependencies =
      super.deliverProjectDependencies.toList - scalatest.projectID ++ Seq(scalatest.projectID % "test")
  }

  lazy val example = project("example", "scalatra-example", new ExampleProject(_), core, fileupload, scalate)
  class ExampleProject(info: ProjectInfo) extends DefaultWebProject(info) with ScalatraSubProject with UnpublishedProject {
    val jetty6 = jettyGroupId % "jetty" % jettyVersion % "test"
    val sfl4japi = "org.slf4j" % "slf4j-api" % slf4jVersion % "compile" 
    val sfl4jnop = "org.slf4j" % "slf4j-nop" % slf4jVersion % "runtime"
    val description = "An example Scalatra application"
  }

  lazy val website = project("website", "scalatra-website", new WebsiteProject(_), core, scalate)
  class WebsiteProject(info: ProjectInfo) extends DefaultWebProject(info) with ScalatraSubProject with UnpublishedProject {
    override val jettyPort = 8081
    val jetty6 = jettyGroupId % "jetty" % jettyVersion % "test"
    val sfl4japi = "org.slf4j" % "slf4j-api" % slf4jVersion % "compile" 
    val sfl4jnop = "org.slf4j" % "slf4j-nop" % slf4jVersion % "runtime"
    val markdown = "org.markdownj" % "markdownj" % "0.3.0-1.0.2b4" % "runtime"
    val description = "Runs www.scalatra.org"
  }

  lazy val test = project("test", "scalatra-test", new DefaultProject(_) with ScalatraSubProject {
    val description = "Scalatra test framework"
    val jettytester = jettyGroupId % "jetty-servlet-tester" % jettyVersion % "compile"
  })

  lazy val scalatest = project("scalatest", "scalatra-scalatest", new DefaultProject(_) with ScalatraSubProject {
    val scalatest = "org.scalatest" % "scalatest" % "1.2" % "compile"
    val junit = "junit" % "junit" % "4.8.1" % "compile"
    val description = "ScalaTest support for the Scalatra test framework"
  }, test)

  lazy val specs = project("specs", "scalatra-specs", new DefaultProject(_) with ScalatraSubProject {
    val specs = "org.scala-tools.testing" %% "specs" % "1.6.5" % "compile"
    val description = "Specs support for the Scalatra test framework"
  }, test)

  val fuseSourceSnapshots = "FuseSource Snapshot Repository" at "http://repo.fusesource.com/nexus/content/repositories/snapshots"
  val scalaToolsSnapshots = "Scala-Tools Maven2 Snapshots Repository" at "http://scala-tools.org/repo-snapshots"

  override def pomExtra = (
    <name>{name}</name>
    <description>Scalatra Project POM</description>
    <url>http://www.scalatra.org/</url>
    <inceptionYear>2009</inceptionYear>
    <organization>
      <name>Scalatra Project</name>
      <url>http://www.scalatra.org/</url>
    </organization>
    <licenses>
      <license>
        <name>BSD</name>
        <url>http://github.com/scalatra/scalatra/raw/HEAD/LICENSE</url>
        <distribution>repo</distribution>
      </license>
    </licenses>
    <mailingLists>
      <mailingList>
        <name>Scalatra user group</name>
        <archive>http://groups.google.com/group/scalatra-user</archive>
        <post>scalatra-user@googlegroups.com</post>
        <subscribe>scalatra-user+subscribe@googlegroups.com</subscribe>
        <unsubscribe>scalatra-user+unsubscribe@googlegroups.com</unsubscribe>
      </mailingList>
    </mailingLists>
    <scm>
      <connection>scm:git:git://github.com/scalatra/scalatra.git</connection>
      <url>http://github.com/scalatra/scalatra</url>
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
    </developers>)

  override def pomPostProcess(pom: Node) =
    super.pomPostProcess(pom) match { 
      case Elem(prefix, label, attr, scope, c @ _*) =>
        val children = c flatMap {
          case Elem(_, "repositories", _, _, repos @ _*) =>
            <profiles>
              <!-- poms deployed to maven central CANNOT have a repositories
                   section defined.  This download profile lets you 
                   download dependencies other repos during development time. -->
              <profile>
                <id>download</id>
                <repositories>
                  {repos}
                </repositories>
              </profile>
            </profiles>
          case Elem(_, "dependencies", _, _, _ @ _*) =>
            // In SBT, parent projects depend on their children.  They should
            // not in Maven.
            None
          case x => x
        }
        Elem(prefix, label, attr, scope, children : _*)
    }

  override def managedStyle = ManagedStyle.Maven
  val publishTo = {
    val local = System.getenv("SOCIALINSIGHT_HOME")
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
        "Mojolly Snapshots" at "http://maven/content/repositories/thirdparty-snapshots/"
      else
        "Mojolly Releases" at "http://maven/content/repositories/thirdparty-releases/"

    }

  }


  override def deliverProjectDependencies = Nil
}
