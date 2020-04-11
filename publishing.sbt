publishTo in ThisBuild := {
  if (version.value.trim.endsWith("SNAPSHOT")) None
  else Some(Opts.resolver.sonatypeStaging)
}
