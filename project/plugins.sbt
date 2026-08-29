scalacOptions ++= Seq(
  "-unchecked",
  "-deprecation",
  "-feature",
  "-language:implicitConversions"
)

addSbtPlugin("com.github.sbt" % "sbt-pgp"         % "2.3.2")
addSbtPlugin("com.eed3si9n"   % "sbt-salad-days"  % "0.2.0")
addSbtPlugin("com.github.sbt" % "sbt-unidoc"      % "0.6.1")
addSbtPlugin("com.typesafe"   % "sbt-mima-plugin" % "1.1.6")
addSbtPlugin("org.scalameta"  % "sbt-scalafmt"    % "2.6.2")
