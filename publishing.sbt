publishTo in ThisBuild := {
  if (version.value.trim.endsWith("SNAPSHOT")) Some(Opts.resolver.sonatypeSnapshots)
  else Some(Opts.resolver.sonatypeStaging)
}
