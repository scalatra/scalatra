scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature")

addSbtPlugin("org.xerial.sbt"       % "sbt-sonatype"         % "3.9.11")
addSbtPlugin("org.scalariform"      % "sbt-scalariform"      % "1.8.3")
addSbtPlugin("com.timushev.sbt"     % "sbt-updates"          % "0.6.2")
addSbtPlugin("com.github.sbt"         % "sbt-pgp"              % "2.1.2")
addSbtPlugin("com.github.sbt"       % "sbt-unidoc"           % "0.5.0")
