import sbt._

trait UnifiedScaladoc extends ParentProject {
  def compositePath(f: Project => PathFinder) =
    dependencies.foldLeft(Path.emptyPathFinder){ (pf, proj) => pf +++ f(proj) }

  def mainSources = compositePath {
    case up: UnpublishedProject => Path.emptyPathFinder
    case sp: ScalaPaths => sp.mainSources
    case _ => Path.emptyPathFinder
  }

  def mainDocPath = outputPath / "doc" / "main" / "api"

  def docClasspath = compositePath {
    case up: UnpublishedProject => Path.emptyPathFinder
    case sp: BasicScalaProject => sp.compileClasspath
    case _ => Path.emptyPathFinder
  }

  lazy val doc = docAction

  protected def docAction =
    docTask("main", mainSources, mainDocPath, docClasspath)
      .describedAs("Builds unified API documentation for all dependencies")

  def docTask(label: String, sources: PathFinder, outputDirectory: Path, classpath: PathFinder): Task =
    task {
      (new Scaladoc(100, buildCompiler))(label, sources.get, classpath.get, outputDirectory, Nil, log)
    }
}