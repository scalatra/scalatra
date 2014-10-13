scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature")

resolvers ++= Seq(
  "less is" at "http://repo.lessis.me", // TODO remove
  "coda" at "http://repo.codahale.com"
)

addSbtPlugin("org.scalatra.sbt" % "scalatra-sbt" % "0.3.5")

// TODO Unused for 2.3.0. Should we remove this?
addSbtPlugin("me.lessis" % "ls-sbt" % "0.1.3")

addSbtPlugin("com.github.mpeltonen" % "sbt-idea" % "1.6.0")

addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "0.1.6")

addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.1.6")

addSbtPlugin("com.typesafe.sbt" % "sbt-pgp" % "0.8.3")

addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.7.4")

