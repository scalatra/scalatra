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
    def extraPlugins: NodeSeq = Seq.empty

    val jettytester = jettyGroupId % "jetty-servlet-tester" % jettyVersion % "provided"
    val servletApi = "org.mortbay.jetty" % "servlet-api" % "2.5-20081211" % "provided"
    val scalatest = "org.scalatest" % "scalatest" % "1.2" % "test"
    val junit = "junit" % "junit" % "4.8.1" % "test"

    override def pomPostProcess(pom: Node) =
      <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
        <modelVersion>4.0.0</modelVersion>
        <parent>
          <groupId>{organization}</groupId>
          <artifactId>{crossScalaName(ScalatraProject.this.name)}</artifactId>
          <version>{version}</version>
        </parent>
        <groupId>{organization}</groupId>
        <artifactId>{crossScalaName(name)}</artifactId>
        <version>{version}</version>
        {pom \ "packaging"}
        <name>{name}</name>
        <description>{description}</description>
        {pom \ "properties"}
        <dependencies>
          {sortDependencies(pom \ "dependencies" \ "dependency")}
        </dependencies>
      </project>

    override def makePomConfiguration = 
      new MakePomConfiguration(deliverProjectDependencies,
        Some(Configurations.defaultMavenConfigurations),
        pomExtra, pomPostProcess, pomIncludeRepository)

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
    override val extraPlugins = 
      <plugin>
        <artifactId>maven-surefire-plugin</artifactId>
        <configuration>
          <excludes>
            <!-- No tests here, by design.  *.java instead of *.scala also intentional. --> 
            <exclude>**/TestScalateScalatraFilter.java</exclude>
          </excludes>
        </configuration>
      </plugin>
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
    override val extraPlugins = 
      <plugin>
        <groupId>org.mortbay.jetty</groupId>
        <artifactId>maven-jetty-plugin</artifactId>
        <version>{jettyVersion}</version>
      </plugin>
  }

  val fuseSourceSnapshots = "FuseSource Snapshot Repository" at "http://repo.fusesource.com/nexus/content/repositories/snapshots"
  val scalaToolsSnapshots = "Scala-Tools Maven2 Snapshots Repository" at "http://scala-tools.org/repo-snapshots"

  override def pomPostProcess(pom: Node) = 
    <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
      <modelVersion>4.0.0</modelVersion>
      <groupId>{organization}</groupId>
      <artifactId>{crossScalaName(name)}</artifactId>
      <version>{version}</version>
      <packaging>pom</packaging>
      
      <name>{name}</name>
      <description>Step Project POM</description>
  
      <prerequisites>
        <maven>2.2.1</maven>
      </prerequisites>
  
      <properties>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <scala-version>{crossScalaVersionString}</scala-version>
        <scaladoc.dir>${{project.build.directory}}/scaladoc</scaladoc.dir>
      </properties>
  
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
        <developerConnection>scm:git:ssh://git@github.com:scalatra/scalatra.git</developerConnection>
        <url>http://github.com/scalatra/scalatra</url>
      </scm>
  
      <distributionManagement>
        <repository>
          <id>sonatype-nexus-staging</id>
          <name>Nexus Release Repository</name>
          <url>http://oss.sonatype.org/service/local/staging/deploy/maven2</url>
        </repository>
        <snapshotRepository>
          <id>sonatype-nexus-snapshots</id>
          <name>Sonatype Nexus Snapshots</name>
          <url>http://oss.sonatype.org/content/repositories/snapshots</url>
        </snapshotRepository>
      </distributionManagement>  
  
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
  
      <modules>
        <module>core</module>
        <module>fileupload</module>
        <module>scalate</module>
        <module>example</module>
      </modules>
  
      <build>
        <plugins>
          <plugin>
            <groupId>org.scala-tools</groupId>
            <artifactId>maven-scala-plugin</artifactId>
            <version>2.14</version>
            <executions>
              <execution>
                <goals>
                  <goal>compile</goal>
                  <goal>testCompile</goal>
                </goals>
              </execution>
              <execution>
                <id>doc</id>
                <phase>process-classes</phase>
                <goals>
                  <goal>doc</goal>
                </goals>
                <configuration>
                  <reportOutputDirectory>${{project.build.directory}}</reportOutputDirectory>
                  <outputDirectory>apidocs</outputDirectory>
                </configuration>                
              </execution>                    
            </executions>
            <configuration>
              <scaladocClassName>scala.tools.nsc.ScalaDoc</scaladocClassName>
              <jvmArgs>
                <jvmArg>-Xmx1024m</jvmArg>
              </jvmArgs>
              <args>
                <arg>-deprecation</arg>
              </args>
              <scalaVersion>${{scala-version}}</scalaVersion>
            </configuration>
          </plugin>

          <!-- We want to sign the artifact, the POM, and all attached artifacts -->
          <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-gpg-plugin</artifactId>
            <executions>
              <execution>
                <id>sign-artifacts</id>
                <phase>verify</phase>
                <goals>
                  <goal>sign</goal>
                </goals>
              </execution>
            </executions>
          </plugin>

          <!-- attach a source jar -->
          <plugin>
            <artifactId>maven-source-plugin</artifactId>
            <executions>
              <execution>
                <id>attach-sources</id>
                <goals>
                  <goal>jar</goal>
                </goals>
              </execution>
            </executions>
          </plugin>

          <!-- attach a javadoc jar -->
          <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-javadoc-plugin</artifactId>
            <version>2.6</version>
            <configuration>
              <encoding>${{project.build.sourceEncoding}}</encoding>
            </configuration>
            <executions>
              <execution>
                <id>attach-javadocs</id>
                <goals>
                  <goal>jar</goal>
                </goals>
              </execution>
            </executions>
          </plugin>

        </plugins>
      </build>
  
      <profiles>
        <!-- poms deployed to maven central CANNOT have a repositories
             section defined.  This download profile lets you 
             download dependencies other repos during development time. -->
        <profile>
          <id>download</id>
          {pom \ "repositories"}
  
          <pluginRepositories>
            <pluginRepository>
              <id>scalatools.releases</id>
              <url>http://scala-tools.org/repo-releases</url>
              <snapshots><enabled>false</enabled></snapshots>
              <releases><enabled>true</enabled></releases>
            </pluginRepository>
            <pluginRepository>
              <id>scalatools.snapshots</id>
              <url>http://scala-tools.org/repo-snapshots</url>
              <snapshots><enabled>true</enabled></snapshots>
              <releases><enabled>false</enabled></releases>
            </pluginRepository>
          </pluginRepositories>
        </profile>
      </profiles>
    </project>

  def crossScalaName(name: String) = name+"_"+crossScalaVersionString

  def sortDependencies(deps: Seq[Node]) = {
    def sortKey(dep: Node) = (dep \ "artifactId")(0).text
    stableSort(deps, {(x: Node, y:Node) => sortKey(x) < sortKey(y)})
  }

  override def managedStyle = ManagedStyle.Maven
  val publishTo = "Sonatype Nexus Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
  Credentials(Path.userHome / ".ivy2" / ".credentials", log)
  
}
