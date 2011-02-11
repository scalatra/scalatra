import sbt._

// http://groups.google.com/group/simple-build-tool/msg/c32741357ac58f18
trait TestWith extends BasicScalaProject {
  def testWithCompileClasspath: Seq[BasicScalaProject] = Nil
  def testWithTestClasspath: Seq[BasicScalaProject] = Nil
  override def testCompileAction = super.testCompileAction dependsOn((testWithTestClasspath.map(_.testCompile) ++ testWithCompileClasspath.map(_.compile)) : _*)
  override def testClasspath = (super.testClasspath /: (testWithTestClasspath.map(_.testClasspath) ++  testWithCompileClasspath.map(_.compileClasspath) ))(_ +++ _)
  override def deliverProjectDependencies = super.deliverProjectDependencies ++ testWithTestClasspath.map(_.projectID % "test")
}
