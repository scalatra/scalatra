scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature")

addSbtPlugin("org.xerial.sbt"       % "sbt-sonatype"         % "3.9.5")
addSbtPlugin("org.scalariform"      % "sbt-scalariform"      % "1.8.3")
addSbtPlugin("com.timushev.sbt"     % "sbt-updates"          % "0.5.1")
addSbtPlugin("com.jsuereth"         % "sbt-pgp"              % "2.0.2")
addSbtPlugin("com.eed3si9n"         % "sbt-unidoc"           % "0.4.3")
