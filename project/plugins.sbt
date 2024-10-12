scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature")

addSbtPlugin("org.xerial.sbt"       % "sbt-sonatype"         % "3.12.2")
addSbtPlugin("org.scalariform"      % "sbt-scalariform"      % "1.8.3")
addSbtPlugin("com.github.sbt"       % "sbt-pgp"              % "2.3.0")
addSbtPlugin("com.github.sbt"       % "sbt-unidoc"           % "0.5.0")
addSbtPlugin("com.eed3si9n"         % "sbt-projectmatrix"    % "0.10.0")
addSbtPlugin("com.typesafe"         % "sbt-mima-plugin"      % "1.1.4")
