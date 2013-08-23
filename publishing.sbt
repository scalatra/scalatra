publishTo in ThisBuild <<= (version) { version: String =>
  val artifactory = "https://ci.aws.wordnik.com/artifactory/m2-"
  if (version.trim.endsWith("SNAPSHOT"))
    Some("snapshots" at artifactory + "snapshots")
  else
    Some("releases"  at artifactory + "releases")
}