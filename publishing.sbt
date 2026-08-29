val publishableModules = Seq(
  "scalatra-compat",
  "scalatra-common",
  "scalatra-test",
  "scalatra-scalatest",
  "scalatra-specs2",
  "scalatra",
  "scalatra-auth",
  "scalatra-twirl",
  "scalatra-json",
  "scalatra-forms",
  "scalatra-jetty",
  "scalatra-swagger",
  "scalatra-metrics",
  "scalatra-cache",
)

def sonaUploadCommand(servletApi: String, scalaBinaryVersion: String) = {
  val projects     = publishableModules.map(module => s"$module-$servletApi$scalaBinaryVersion")
  val publishTasks = projects.map(project => s"$project/publishSigned")

  (Seq("clean") ++ publishTasks :+ "sonaUpload").mkString(";")
}

addCommandAlias("sonaUploadJavax213", sonaUploadCommand("javax", "2_13"))
addCommandAlias("sonaUploadJavax3", sonaUploadCommand("javax", "3"))
addCommandAlias("sonaUploadJakarta213", sonaUploadCommand("jakarta", "2_13"))
addCommandAlias("sonaUploadJakarta3", sonaUploadCommand("jakarta", "3"))

ThisBuild / publishTo := {
  val centralSnapshots = "https://central.sonatype.com/repository/maven-snapshots/"
  if (isSnapshot.value) Some("central-snapshots" at centralSnapshots)
  else localStaging.value
}
