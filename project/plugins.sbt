scalacOptions ++= Seq(
  "-unchecked",
  "-deprecation",
  "-feature",
  "-language:implicitConversions"
)

addSbtPlugin("com.github.sbt" % "sbt-pgp"         % "2.3.1")
addSbtPlugin("com.github.sbt" % "sbt-unidoc"      % "0.6.1")
addSbtPlugin("com.typesafe"   % "sbt-mima-plugin" % "1.1.5")
addSbtPlugin("org.scalameta"  % "sbt-scalafmt"    % "2.6.1")
