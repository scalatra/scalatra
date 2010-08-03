import sbt._

import scala.xml._
import scala.util.Sorting._

class ScalatraProject(info: ProjectInfo) extends ParentProject(info)
{
  override def shouldCheckOutputDirectories = false

  val jettyGroupId = "org.mortbay.jetty"
  val jettyVersion = "6.1.22"
  val slf4jVersion = "1.6.0"

  trait ScalatraSubProject extends BasicScalaProject with BasicPackagePaths {
    def description: String

    val jettytester = jettyGroupId % "jetty-servlet-tester" % jettyVersion % "provided"
    val servletApi = "org.mortbay.jetty" % "servlet-api" % "2.5-20081211" % "provided"
    val scalatest = "org.scalatest" % "scalatest" % "1.2" % "test"
    val junit = "junit" % "junit" % "4.8.1" % "test"

    override def pomExtra = (
      <parent>
        <groupId>{organization}</groupId>
        <artifactId>{artifactID}</artifactId>
        <version>{version}</version>
      </parent>
      <name>{name}</name>
      <description>{description}</description>
    )

    override def packageDocsJar = defaultJarPath("-javadoc.jar")
    override def packageSrcJar= defaultJarPath("-sources.jar")
    // If these aren't lazy, then the build crashes looking for
    // ${moduleName}/project/build.properties.
    lazy val sourceArtifact = Artifact.sources(artifactID)
    lazy val docsArtifact = Artifact.javadoc(artifactID)
    override def packageToPublishActions = super.packageToPublishActions ++ Seq(packageDocs, packageSrc)
  }

  lazy val core = project("core", "scalatra", new CoreProject(_)) 
  class CoreProject(info: ProjectInfo) extends DefaultProject(info) with ScalatraSubProject {
    val mockito = "org.mockito" % "mockito-core" % "1.8.2" % "test"
    val description = "The core Scalatra library"
  }

  lazy val fileupload = project("fileupload", "scalatra-fileupload", new FileuploadProject(_), core)
  class FileuploadProject(info: ProjectInfo) extends DefaultProject(info) with ScalatraSubProject {
    val commonsFileupload = "commons-fileupload" % "commons-fileupload" % "1.2.1" % "compile"
    val commonsIo = "commons-io" % "commons-io" % "1.4" % "compile"
    val description = "Supplies the optional Scalatra file upload support"
  }

  lazy val scalate = project("scalate", "scalatra-scalate", new ScalateProject(_), core)
  class ScalateProject(info: ProjectInfo) extends DefaultProject(info) with ScalatraSubProject {
    val scalate = "org.fusesource.scalate" % "scalate-core" % "1.2"
    val description = "Supplies the optional Scalatra Scalate support"
  }

  lazy val example = project("example", "scalatra-example", new ExampleProject(_), core, fileupload, scalate)
  class ExampleProject(info: ProjectInfo) extends DefaultWebProject(info) with ScalatraSubProject {
    val jetty6 = jettyGroupId % "jetty" % jettyVersion % "test"
    val sfl4japi = "org.slf4j" % "slf4j-api" % slf4jVersion % "compile" 
    val sfl4jnop = "org.slf4j" % "slf4j-nop" % slf4jVersion % "runtime"
    val description = "An example Scalatra application"
    override def publishLocalAction = Empty
    override def deliverLocalAction = Empty
    override def publishAction = Empty
    override def deliverAction = Empty
    override def artifacts = Set.empty
  }

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
    </developers>
  )

  override def pomPostProcess(pom: Node) = 
    pom match { 
      case Elem(prefix, label, attr, scope, c @ _*) =>
        val children = c flatMap {
          case Elem(_, "repositories", _, _, repos @ _*) =>
            <profiles>
              <!-- poms deployed to maven central CANNOT have a repositories
                   section defined.  This download profile lets you 
                   download dependencies other repos during development time. -->
              <profile>
                <id>download</id>
                {repos}
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
  val publishTo = "Sonatype Nexus Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
  Credentials(Path.userHome / ".ivy2" / ".credentials", log)
}
