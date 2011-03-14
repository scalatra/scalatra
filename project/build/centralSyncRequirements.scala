/*
 * Isolates some of the ugliness involved in getting an sbt project to
 * meet the Central Sync Requirements:
 *
 * https://docs.sonatype.org/display/Repository/Central+Sync+Requirements
 */

import sbt._

import scala.xml._
import com.rossabaker.sbt.openpgp._

trait MavenCentralProject
  extends BasicManagedProject
  with SignWithOpenpgp
  with GenerateChecksums
{
  def projectDescription: String = projectName.get.get

  override def pomExtra = super.pomExtra ++ (
    <name>{projectName.get.get}</name>
    <description>{projectDescription}</description>
  )

  override def managedStyle = ManagedStyle.Maven

  /**
   * Moves repositories to a download profile.
   */
  override def pomPostProcess(pom: Node) =
    super.pomPostProcess(pom) match {
      case Elem(prefix, label, attr, scope, c @ _*) =>
        val children = c flatMap {
          case Elem(_, "repositories", _, _, repos @ _*) =>
            <profiles>
              <profile>
                <id>download</id>
                <repositories>
                  {repos}
                </repositories>
              </profile>
            </profiles>
          case x => x
        }
        Elem(prefix, label, attr, scope, children : _*)
    }
}

trait MavenCentralTopLevelProject extends MavenCentralProject {
  def projectUrl: String
  def licenses: NodeSeq
  def scmUrl: String
  def scmConnection: String
  def developers: NodeSeq

  override def pomExtra = super.pomExtra ++ 
    <url>{projectUrl}</url> ++
    licenses ++
    <scm>
      <url>{scmUrl}</url>
      <connection>{scmConnection}</connection>
    </scm> ++
    developers
}

trait MavenCentralSubproject extends MavenCentralProject {
  def parent: ManagedProject

  override def pomExtra = 
    <parent>
      <groupId>{parent.projectID.organization}</groupId>
      <artifactId>{parent.projectID.name}</artifactId>
      <version>{parent.projectID.revision}</version>
    </parent> ++ super.pomExtra
}

trait MavenCentralScalaProject 
  extends BasicScalaProject 
  with BasicPackagePaths 
{
  this: MavenCentralProject =>

  override def packageDocsJar = defaultJarPath("-javadoc.jar")
  override def packageSrcJar= defaultJarPath("-sources.jar")
  // If these aren't lazy, then the build crashes looking for
  // ${moduleName}/project/build.properties.
  lazy val sourceArtifact = Artifact.sources(artifactID)
  lazy val docsArtifact = Artifact.javadoc(artifactID)
  override def packageToPublishActions = super.packageToPublishActions ++ Seq(packageDocs, packageSrc)
}
