scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature")

addSbtPlugin("org.xerial.sbt"       % "sbt-sonatype"         % "3.9.8")
addSbtPlugin("org.scalariform"      % "sbt-scalariform"      % "1.8.3")
addSbtPlugin("com.timushev.sbt"     % "sbt-updates"          % "0.6.0")
addSbtPlugin("com.github.sbt"         % "sbt-pgp"              % "2.1.2")
addSbtPlugin("com.eed3si9n"         % "sbt-unidoc"           % "0.4.3")
