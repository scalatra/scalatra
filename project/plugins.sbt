resolvers += Opts.resolver.sonatypeReleases
scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature")

addSbtPlugin("org.xerial.sbt"       % "sbt-sonatype"         % "1.0")
addSbtPlugin("org.scalariform"      % "sbt-scalariform"      % "1.6.0")
addSbtPlugin("org.scalatra.sbt"     % "scalatra-sbt"         % "0.4.0")
addSbtPlugin("com.github.mpeltonen" % "sbt-idea"             % "1.6.0")
addSbtPlugin("com.timushev.sbt"     % "sbt-updates"          % "0.1.10")
addSbtPlugin("com.jsuereth"         % "sbt-pgp"              % "1.0.0")
addSbtPlugin("net.virtual-void"     % "sbt-dependency-graph" % "0.8.2")
addSbtPlugin("com.eed3si9n"         % "sbt-unidoc"           % "0.3.3")
