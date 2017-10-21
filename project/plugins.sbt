resolvers += Opts.resolver.sonatypeReleases
scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature")

addSbtPlugin("org.xerial.sbt"       % "sbt-sonatype"         % "2.0")
addSbtPlugin("org.scalariform"      % "sbt-scalariform"      % "1.8.1")
addSbtPlugin("com.timushev.sbt"     % "sbt-updates"          % "0.3.1")
addSbtPlugin("com.jsuereth"         % "sbt-pgp"              % "1.1.0")
addSbtPlugin("net.virtual-void"     % "sbt-dependency-graph" % "0.8.2")
addSbtPlugin("com.eed3si9n"         % "sbt-unidoc"           % "0.4.1")
