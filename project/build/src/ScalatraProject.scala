import sbt._

import scala.xml._
import scala.xml.transform._

class ScalatraProject(info: ProjectInfo) extends ParentProject(info)
{
  override def shouldCheckOutputDirectories = false

  val jettyGroupId = "org.mortbay.jetty"
  val jettyVersion = "6.1.22"

  lazy val core = project("core", "scalatra", new CoreProject(_)) 
  class CoreProject(info: ProjectInfo) extends DefaultProject(info) {
    val jettytester = jettyGroupId % "jetty-servlet-tester" % jettyVersion % "provided"
    val scalatest = "org.scalatest" % "scalatest" % scalatestVersion(crossScalaVersionString) % "provided->default"
    val mockito = "org.mockito" % "mockito-core" % "1.8.2" % "test"

    override def pomExtra = parentPom ++ (
      <name>scalatra</name>
      <description>The core Scalatra library</description>
    )
    override def pomIncludeRepository(repo: MavenRepository) = false
  }

  lazy val fileupload = project("fileupload", "scalatra-fileupload", new FileuploadProject(_), core)
  class FileuploadProject(info: ProjectInfo) extends DefaultProject(info) {
    val commonsFileupload = "commons-fileupload" % "commons-fileupload" % "1.2.1" % "compile"
    val commonsIo = "commons-io" % "commons-io" % "1.4" % "compile"

    override def pomExtra = parentPom ++ (
      <name>scalatra-fileupload</name>
      <description>Supplies the optional Scalatra file upload support</description>
    )
    override def pomIncludeRepository(repo: MavenRepository) = false
  }

  lazy val scalate = project("scalate", "scalatra-scalate", new ScalateProject(_), core)
  class ScalateProject(info: ProjectInfo) extends DefaultProject(info) {
    val scalate = "org.fusesource.scalate" % "scalate-core" % "1.2-SNAPSHOT"
    override def pomExtra = parentPom ++ (
      <name>scalatra-scalate</name>
      <description>Supplies the optional Scalatra Scalate support</description>
    )
    override def pomIncludeRepository(repo: MavenRepository) = false
  }

  lazy val example = project("example", "scalatra-example", new ExampleProject(_), core, fileupload, scalate)
  class ExampleProject(info: ProjectInfo) extends DefaultWebProject(info) {
    val jetty6 = jettyGroupId % "jetty" % jettyVersion % "test"
    val sfl4jnop = "org.slf4j" % "slf4j-nop" % "1.5.11" % "runtime" // Scalate needs a slf4j binding.
    override def pomExtra = parentPom ++ (
      <name>scalatra-example</name>
      <description>An example Scalatra application</description>
    )
    override def pomIncludeRepository(repo: MavenRepository) = false
  }

  def scalatestVersion(scalaVersion: String) = {
    scalaVersion match {
      case "2.8.0.Beta1" =>
        "1.0.1-for-scala-2.8.0.Beta1-with-test-interfaces-0.3-SNAPSHOT"
      case "2.8.0.RC1" =>
        "1.0.1-for-scala-2.8.0.RC1-SNAPSHOT"
      case x =>
        "1.2-for-scala-"+x+"-SNAPSHOT"
    } 
  }

  val fuseSourceSnapshots = "FuseSource Snapshot Repository" at "http://repo.fusesource.com/nexus/content/repositories/snapshots"
  val scalaToolsSnapshots = "Scala-Tools Maven2 Snapshots Repository" at "http://scala-tools.org/repo-snapshots"

  override def pomExtra = (
    <name>Scalatra Project</name>

    <prerequisites>
      <maven>2.2.1</maven>
    </prerequisites>

    <properties>
      <!-- build settings -->
      <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
      <compiler.fork>false</compiler.fork>
      <release-altGitURL>scm:git:ssh://git@github.com:alandipert/step.git</release-altGitURL>

      <!-- maven plugin versions -->
      <surefire-version>2.5</surefire-version>
      <scala-plugin-version>2.13.1</scala-plugin-version>
    </properties>

    <inceptionYear>2009</inceptionYear>

    <organization>
      <name>Scalatra Project</name>
      <url>http://github.com/alandipert/step</url>
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

    <scm>
      <connection>scm:git:git://github.com/alandipert/step.git</connection>
      <!-- Work around for issue: http://jira.codehaus.org/browse/SCM-444 -->
      <developerConnection>${{release-altGitURL}}</developerConnection>
      <url>http://github.com/alandipert/step</url>
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
      <!-- 
        <site>
          <id>scalatra.github.org</id>
          <url>dav:http://scalatra.github.org/</url>
        </site> 
      -->
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
      <defaultGoal>install</defaultGoal>
      <sourceDirectory>src/main/scala</sourceDirectory>
      <testSourceDirectory>src/test/scala</testSourceDirectory>

      <extensions>
        <extension>
          <groupId>org.apache.maven.wagon</groupId>
          <artifactId>wagon-webdav-jackrabbit</artifactId>
          <version>1.0-beta-6</version>
        </extension>
      </extensions>

      <plugins>
        <plugin>
          <groupId>org.scala-tools</groupId>
          <artifactId>maven-scala-plugin</artifactId>
          <version>${{scala-plugin-version}}</version>
          <executions>
            <execution>
              <goals>
                <goal>compile</goal>
                <goal>testCompile</goal>
              </goals>
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

        <plugin>
          <artifactId>maven-surefire-plugin</artifactId>
          <version>${{surefire-version}}</version>
          <configuration>
            <!-- we must turn off the use of system class loader so our tests can find 
                stuff - otherwise scala compiler can't find stuff -->
            <useSystemClassLoader>false</useSystemClassLoader>
            <forkMode>pertest</forkMode>
            <childDelegation>false</childDelegation>
            <useFile>true</useFile>
            <failIfNoTests>false</failIfNoTests>
          </configuration>
        </plugin>

        <plugin>
          <groupId>org.apache.maven.plugins</groupId>
          <artifactId>maven-release-plugin</artifactId>
          <version>2.0</version>
          <configuration>
            <autoVersionSubmodules>true</autoVersionSubmodules>
            <allowTimestampedSnapshots>false</allowTimestampedSnapshots>
            <preparationGoals>clean install</preparationGoals>
            <goals>deploy</goals>
            <arguments>-Prelease</arguments>
          </configuration>
        </plugin>

        <plugin>
          <groupId>org.apache.maven.plugins</groupId>
          <artifactId>maven-scm-plugin</artifactId>
          <version>1.3</version>
          <dependencies>
            <dependency>
              <groupId>org.apache.maven.scm</groupId>
              <artifactId>maven-scm-provider-gitexe</artifactId>
              <version>1.3</version>
            </dependency>
          </dependencies>
        </plugin>
        
      </plugins>
    </build>

    <reporting>
      <plugins>
        <plugin>
          <groupId>org.codehaus.mojo</groupId>
          <artifactId>jxr-maven-plugin</artifactId>
          <version>2.0-beta-1</version>
          <configuration>
            <aggregate>true</aggregate>
          </configuration>
        </plugin>

        <!--
          <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-javadoc-plugin</artifactId>
            <version>2.6</version>
            <configuration>
              <detectLinks>true</detectLinks>
              <linksource>true</linksource>
            </configuration>
            <reportSets>
              <reportSet>
                <reports>
                  <report>javadoc</report>
                  &lt;!&ndash;<report>aggregate</report>&ndash;&gt;
                  <report>test-javadoc</report>
                </reports>
              </reportSet>
            </reportSets>
          </plugin>
        -->

        <plugin>
          <groupId>org.apache.maven.plugins</groupId>
          <artifactId>maven-project-info-reports-plugin</artifactId>
          <version>2.1.1</version>
          <reportSets>
            <reportSet>
              <reports>
                <report>index</report>
                <report>summary</report>
                <report>plugins</report>
                <report>dependencies</report>
                <report>dependency-convergence</report>
                <report>dependency-management</report>
                <report>license</report>
                <report>mailing-list</report>
                <report>project-team</report>
                <report>issue-tracking</report>
                <report>cim</report>
              </reports>
            </reportSet>
          </reportSets>
          <configuration>
            <webAccessUrl>http://github.com/scalate/scalate</webAccessUrl>
          </configuration>
        </plugin>

        <plugin>
          <groupId>org.apache.maven.plugins</groupId>
          <artifactId>maven-surefire-report-plugin</artifactId>
          <version>2.5</version>
          <reportSets>
            <reportSet>
              <reports>
                <report>report-only</report>
              </reports>
            </reportSet>
          </reportSets>
        </plugin>

        <plugin>
          <groupId>org.apache.maven.plugins</groupId>
          <artifactId>maven-site-plugin</artifactId>
          <version>2.1</version>
          <configuration>
            <generateSitemap>true</generateSitemap>
          </configuration>
        </plugin>
        <plugin>
          <groupId>org.codehaus.mojo</groupId>
          <artifactId>taglist-maven-plugin</artifactId>
        </plugin>

      </plugins>

    </reporting>

    <profiles>
      <!-- poms deployed to maven central CANNOT have a repositories
           section defined.  This download profile lets you 
           download dependencies other repos during development time. -->
      <profile>
        <id>download</id>
        <repositories />

        <pluginRepositories>
          <!-- 
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
          -->
        </pluginRepositories>
      </profile>

      <!-- use a profile for generating the maven site since we require a snapshot version for 2.8.0 Scala -->
      <profile>
        <id>site</id>
        <properties>
          <!-- this snapshot release works with 2.8.0-beta1 -->
          <scala-plugin-version>2.13.2-SNAPSHOT</scala-plugin-version>
        </properties>
        <reporting>
          <plugins>
            <plugin>
              <groupId>org.scala-tools</groupId>
              <artifactId>maven-scala-plugin</artifactId>
              <configuration>
                <scalaVersion>${{scala-version}}</scalaVersion>
              </configuration>
            </plugin>
          </plugins>
        </reporting>
      </profile>

      <profile>
        <id>release</id>
        <build>
          <plugins>
            <!-- We want to sign the artifact, the POM, and all attached artifacts -->
            <plugin>
              <groupId>org.apache.maven.plugins</groupId>
              <artifactId>maven-gpg-plugin</artifactId>
              <version>1.0</version>
              <configuration>
                <passphrase>${{gpg.passphrase}}</passphrase>
              </configuration>
              <executions>
                <execution>
                  <goals>
                    <goal>sign</goal>
                  </goals>
                </execution>
              </executions>
            </plugin>

            <plugin>
              <groupId>org.apache.maven.plugins</groupId>
              <artifactId>maven-source-plugin</artifactId>
              <version>2.1.1</version>
              <executions>
                <execution>
                  <id>attach-sources</id>
                  <goals>
                    <goal>jar-no-fork</goal>
                  </goals>
                </execution>
              </executions>
            </plugin>

            <!-- temporary work around until scala doc works with 2.8:  Generate empty javadoc artifacts -->
            <plugin>
              <groupId>org.apache.maven.plugins</groupId>
              <artifactId>maven-antrun-plugin</artifactId>
              
              <!-- copied dependency from scalate-website as this plugin might zap it -->
              <dependencies>
                <dependency>
                  <groupId>org.markdownj</groupId>
                  <artifactId>markdownj</artifactId>
                  <version>${{markdownj-version}}</version>
                </dependency>
              </dependencies>
              
              <executions>
                <execution>
                  <id>javadoc-work-around</id>
                  <phase>compile</phase>
                  <configuration>
                    <tasks>
                      <mkdir dir="${project.build.directory}/apidocs" />
                      <echo file="${project.build.directory}/apidocs/readme.txt" message="comming soon." />
                    </tasks>
                  </configuration>
                  <goals>
                    <goal>run</goal>
                  </goals>
                </execution>
              </executions>
            </plugin>

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
      </profile>
      
      <!--
        To generate a graph of the project dependencies, run: mvn -P graph
        graph:project
      -->
      <profile>
        <id>graph</id>
        <build>
          <plugins>
            <plugin>
              <groupId>org.fusesource.mvnplugins</groupId>
              <artifactId>maven-graph-plugin</artifactId>
              <version>1.5</version>
            </plugin>
          </plugins>
        </build>
      </profile>
       
    </profiles>
  )

  // The repositories added by sbt need to be moved to a profile
  // to satisfy Maven Central requirements.
  override def pomPostProcess(pom: Node) = {
    val repos = (pom \ "repositories")(0)
    def moveRepos(nodes: Seq[Node], inProfile: Boolean): Seq[Node] =
      nodes flatMap {
        case Elem(prefix, "profile", attribs, scope, children @_ *) =>
          Some(Elem(prefix, "profile", attribs, scope, moveRepos(children, true) : _*))
        case _ @ Elem(_, "repositories", _, _,  _*) =>
          if (inProfile) Some(repos) else NodeSeq.Empty
        case Elem(prefix, label, attribs, scope, children @_ *) =>
          Some(Elem(prefix, label, attribs, scope, moveRepos(children, inProfile) : _*))
        case other => other
      }
    moveRepos(pom.theSeq, false)(0)
  }

  val parentPom = 
    <parent>
      <groupId>org.scalatra</groupId>
      <artifactId>scalatra-project</artifactId>
      <version>${version}</version>
    </parent>
}
