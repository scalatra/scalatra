scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature")

addSbtPlugin("org.xerial.sbt"       % "sbt-sonatype"         % "3.9.13")
addSbtPlugin("org.scalariform"      % "sbt-scalariform"      % "1.8.3")
addSbtPlugin("com.timushev.sbt"     % "sbt-updates"          % "0.6.4")
addSbtPlugin("com.github.sbt"         % "sbt-pgp"              % "2.2.0")
addSbtPlugin("com.github.sbt"       % "sbt-unidoc"           % "0.5.0")
