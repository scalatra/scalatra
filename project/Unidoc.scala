// Pilfered from https://raw.github.com/akka/akka/ceb888b9a7764e070ed637d5c7cd536c59052065/project/Unidoc.scala

import sbt._
import sbt.Keys._
import sbt.Project.Initialize
import scala.language.postfixOps

object Unidoc extends Plugin {
  val unidocDirectory = SettingKey[File]("unidoc-directory")
  val unidocExclude = SettingKey[Seq[String]]("unidoc-exclude")
  val unidocAllSources = TaskKey[Seq[Seq[File]]]("unidoc-all-sources")
  val unidocSources = TaskKey[Seq[File]]("unidoc-sources")
  val unidocAllClasspaths = TaskKey[Seq[Classpath]]("unidoc-all-classpaths")
  val unidocClasspath = TaskKey[Seq[File]]("unidoc-classpath")
  val unidoc = TaskKey[File]("unidoc", "Create unified scaladoc for all aggregates")

  val unidocSettings = Seq(
    unidocDirectory <<= crossTarget { _ / "unidoc" }, 
    unidocExclude := Seq.empty,
    unidocAllSources <<= (thisProjectRef, buildStructure, unidocExclude) flatMap allSources,
    unidocSources <<= unidocAllSources map { _.flatten },
    unidocAllClasspaths <<= (thisProjectRef, buildStructure, unidocExclude) flatMap allClasspaths,
    unidocClasspath <<= unidocAllClasspaths map { _.flatten.map(_.data).distinct },
    unidoc <<= unidocTask
  )

  def allSources(projectRef: ProjectRef, structure: BuildStructure, exclude: Seq[String]): Task[Seq[Seq[File]]] = {
    val projects = aggregated(projectRef, structure, exclude)
    projects flatMap { sources in Compile in LocalProject(_) get structure.data } join
  }

  def allClasspaths(projectRef: ProjectRef, structure: BuildStructure, exclude: Seq[String]): Task[Seq[Classpath]] = {
    val projects = aggregated(projectRef, structure, exclude)
    projects flatMap { dependencyClasspath in Compile in LocalProject(_) get structure.data } join
  }

  def aggregated(projectRef: ProjectRef, structure: BuildStructure, exclude: Seq[String]): Seq[String] = {
    val aggregate = Project.getProject(projectRef, structure).toSeq.flatMap(_.aggregate)
    aggregate flatMap { ref =>
      if (exclude contains ref.project) Seq.empty
      else ref.project +: aggregated(ref, structure, exclude)
    }
  }

  def unidocTask: Def.Initialize[Task[File]] = {
    (compilers, unidocSources, unidocDirectory, scalacOptions in doc, streams) map {
      (compilers, sources, target, options, s) => {
        Doc.scaladoc("Scalatra", s.cacheDirectory, compilers.scalac, options)
        target
      }
    }
  }
}
