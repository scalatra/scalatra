scalacOptions ++= Seq("-unchecked", "-deprecation")

resolvers ++= Seq(
  "less is" at "http://repo.lessis.me"
)

addSbtPlugin("org.scalatra.sbt" % "scalatra-sbt" % "0.2.0")

addSbtPlugin("me.lessis" % "ls-sbt" % "0.1.2")

addSbtPlugin("com.github.mpeltonen" % "sbt-idea" % "1.2.0")

addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "0.1.5")
