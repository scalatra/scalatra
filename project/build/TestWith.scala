import sbt._

// http://groups.google.com/group/simple-build-tool/msg/c32741357ac58f18
trait TestWith extends BasicScalaProject {
  def testWithCompileClasspath: Seq[BasicScalaProject] = Nil
  def testWithTestClasspath: Seq[BasicScalaProject] = Nil
  override def testCompileAction = super.testCompileAction dependsOn((testWithTestClasspath.map(_.testCompile) ++ testWithCompileClasspath.map(_.compile)) : _*)
  override def testClasspath = (super.testClasspath /: (testWithTestClasspath.map(_.testClasspath) ++  testWithCompileClasspath.map(_.compileClasspath) ))(_ +++ _)
  // Our test-with dependencies need to publish before we deliver ourselves...
  override def dependencies = super.dependencies ++ testWithCompileClasspath ++ testWithTestClasspath
  // ... but we still want them in test scope.
  override def deliverProjectDependencies = {
    val testDeps = testWithTestClasspath map { _.projectID }
    super.deliverProjectDependencies map { dep =>
      if (testDeps.contains(dep)) { dep % "test" } else dep
    }
  }
}
