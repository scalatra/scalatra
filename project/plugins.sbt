scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature")

resolvers ++= Seq("coda" at "http://repo.codahale.com")

addSbtPlugin("org.scalatra.sbt"     % "scalatra-sbt"         % "0.3.5")
addSbtPlugin("com.github.mpeltonen" % "sbt-idea"             % "1.6.0")
addSbtPlugin("com.typesafe"         % "sbt-mima-plugin"      % "0.1.6")
addSbtPlugin("com.timushev.sbt"     % "sbt-updates"          % "0.1.7")
addSbtPlugin("com.jsuereth"         % "sbt-pgp"              % "1.0.0")
addSbtPlugin("net.virtual-void"     % "sbt-dependency-graph" % "0.7.4")

