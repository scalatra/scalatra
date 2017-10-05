import com.typesafe.sbt.pgp.PgpKeys
import scala.xml._
import java.net.URL
import Dependencies._
import UnidocKeys._

lazy val scalatraSettings = Seq(
  organization := "org.scalatra",
  crossScalaVersions := Seq("2.12.3", "2.11.11"),
  scalaVersion := crossScalaVersions.value.head,
  scalacOptions ++= Seq(
    "-target:jvm-1.8",
    "-unchecked",
    "-deprecation",
    /*"-Yinline-warnings",*/
    "-Xcheckinit",
    "-encoding", "utf8",
    "-feature",
    "-Ywarn-unused-import",
    "-language:higherKinds",
    "-language:postfixOps",
    "-language:implicitConversions",
    "-language:reflectiveCalls",
    "-language:existentials"
  ),
  manifestSetting,
  resolvers ++= Seq(
    Opts.resolver.sonatypeSnapshots,
    Opts.resolver.sonatypeReleases,
    "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"
  ),
  dependencyOverrides := Set(
    "org.scala-lang" % "scala-library"  % scalaVersion.value,
    "org.scala-lang" % "scala-reflect"  % scalaVersion.value,
    "org.scala-lang" % "scala-compiler" % scalaVersion.value
  )
) ++ mavenCentralFrouFrou ++ Seq(Compile, Test).flatMap(c =>
  scalacOptions in (c, console) --= Seq("-Ywarn-unused-import")
)

lazy val scalatraProject = Project(
  id = "scalatra-project",
  base = file("."),
  settings = scalatraSettings ++ unidocSettings ++ doNotPublish ++ Seq(
    description := "A tiny, Sinatra-like web framework for Scala"
  ),
  aggregate = Seq(
    scalatraCore,
    scalatraAuth,
    scalatraForms,
    scalatraCommands,
    scalatraScalate,
    scalatraJson,
    scalatraAtmosphere,
    scalatraTest,
    scalatraScalatest,
    scalatraSpecs2,
    scalatraSwagger,
    scalatraJetty,
    scalatraCommon,
    scalatraSpring,
    scalatraMetrics,
    scalatraCache,
    scalatraCacheGuava
  )
)

lazy val scalatraCommon = Project(
  id = "scalatra-common",
  base = file("common"),
  settings = scalatraSettings ++ Seq(
    libraryDependencies ++= Seq(servletApi % "provided,test")
  )
)

lazy val scalatraCore = Project(
  id = "scalatra",
  base = file("core"),
  settings = scalatraSettings ++ Seq(
    libraryDependencies ++= Seq(
      servletApi % "provided;test",
      slf4jApi,
      grizzledSlf4j,
      jUniversalChardet,
      mimeUtil,
      commonsLang3,
      parserCombinators,
      xml,
      akkaActor % "test",
      akkaTestkit % "test"
    ),
    description := "The core Scalatra framework"
  )
) dependsOn(
  scalatraSpecs2 % "test->compile",
  scalatraScalatest % "test->compile",
  scalatraCommon % "compile;test->test"
)

lazy val scalatraAuth = Project(
  id = "scalatra-auth",
  base = file("auth"),
  settings = scalatraSettings ++ Seq(
    description := "Scalatra authentication module"
  )
) dependsOn(scalatraCore % "compile;test->test;provided->provided")

lazy val scalatraAtmosphere = Project(
  id = "scalatra-atmosphere",
  base = file("atmosphere"),
  settings = scalatraSettings ++ Seq(
    libraryDependencies ++= Seq(
      atmosphereRuntime,
      atmosphereRedis,
      atmosphereCompatJbossweb,
      atmosphereCompatTomcat,
      atmosphereCompatTomcat7,
      atmosphereClient % "test",
      jettyWebsocket % "test",
      akkaActor,
      akkaTestkit % "test"
    ),
    description := "Atmosphere integration for scalatra"
  )
) dependsOn(scalatraJson % "compile;test->test;provided->provided")

lazy val scalatraScalate = Project(
  id = "scalatra-scalate",
  base = file("scalate"),
  settings = scalatraSettings ++ Seq(
    libraryDependencies += scalate,
    description := "Scalate integration with Scalatra"
  )
) dependsOn(scalatraCore % "compile;test->test;provided->provided")

lazy val scalatraJson = Project(
  id = "scalatra-json",
  base = file("json"),
  settings = scalatraSettings ++ Seq(
    description := "JSON support for Scalatra",
    libraryDependencies ++= Seq(
      json4sJackson % "provided",
      json4sNative % "provided",
      json4sCore
    )
  )
) dependsOn(scalatraCore % "compile;test->test;provided->provided")

lazy val scalatraForms = Project(
  id = "scalatra-forms",
  base = file("forms"),
  settings = scalatraSettings ++ Seq(
    description := "Data binding and validation for Scalatra"
  )
) dependsOn(scalatraCore % "compile;test->test;provided->provided")

lazy val scalatraCommands = Project(
  id = "scalatra-commands",
  base = file("commands"),
  settings = scalatraSettings ++ Seq(
    libraryDependencies ++= Seq(
      "commons-validator" % "commons-validator" % "1.6"
    ),
    libraryDependencies ++= Seq(
      scalaz,
      jodaTime,
      jodaConvert
    ),
    initialCommands :=
      """
        |import scalaz._
        |import Scalaz._
        |import org.scalatra._
        |import org.scalatra.util._
        |import conversion._
        |import commands._
        |import BindingSyntax._
      """.stripMargin,
    description := "Data binding and validation with scalaz for Scalatra"
  )
) dependsOn(scalatraJson % "compile;test->test;provided->provided")

lazy val scalatraJetty = Project(
  id = "scalatra-jetty",
  base = file("jetty"),
  settings = scalatraSettings ++ Seq(
    libraryDependencies ++= Seq(
      servletApi,
      jettyServlet
    ),
    description := "Embedded Jetty server for Scalatra apps"
  )
) dependsOn(scalatraCore % "compile;test->test;provided->provided")

lazy val scalatraTest = Project(
  id = "scalatra-test",
  base = file("test"),
  settings = scalatraSettings ++ Seq(
    libraryDependencies ++= Seq(
      grizzledSlf4j,
      jettyWebapp,
      servletApi,
      mockitoAll,
      commonsLang3,
      httpclient,
      httpmime,
      jodaTime % "provided",
      jodaConvert % "provided"
    ) ++ specs2.map(_ % "test"),
    description := "The abstract Scalatra test framework"
  )
) dependsOn(scalatraCommon % "compile;test->test;provided->provided")

lazy val scalatraScalatest = Project(
  id = "scalatra-scalatest",
  base = file("scalatest"),
  settings = scalatraSettings ++ Seq(
    libraryDependencies ++= Seq(
      scalatest,
      junit,
      testng % "optional",
      guice % "optional"
    ),
    description := "ScalaTest support for the Scalatra test framework"
  )
) dependsOn(scalatraTest % "compile;test->test;provided->provided")

lazy val scalatraSpecs2 = Project(
  id = "scalatra-specs2",
  base = file("specs2"),
  settings = scalatraSettings ++ Seq(
    libraryDependencies ++= specs2,
    description := "Specs2 support for the Scalatra test framework"
  )
) dependsOn(scalatraTest % "compile;test->test;provided->provided")

lazy val scalatraSwagger = Project(
  id = "scalatra-swagger",
  base = file("swagger"),
  settings = scalatraSettings ++ Seq(
    libraryDependencies ++= Seq(
      json4sExt,
      parserCombinators,
      logbackClassic % "provided"
    ),
    description := "Scalatra integration with Swagger"
  )
) dependsOn(
  scalatraCore % "compile;test->test;provided->provided",
  scalatraJson % "compile;test->test;provided->provided",
  scalatraCommands % "compile;test->test;provided->provided",
  scalatraAuth % "compile;test->test"
)

lazy val scalatraSpring = Project(
  id = "scalatra-spring",
  base = file("spring"),
  settings = scalatraSettings ++ Seq(
    libraryDependencies += springWeb,
    description := "Scalatra integration with Spring Framework"
  )
) dependsOn(scalatraCore % "compile;test->test;provided->provided")

lazy val scalatraMetrics = Project(
  id = "scalatra-metrics",
  base = file("metrics"),
  settings = scalatraSettings ++ Seq(
    libraryDependencies ++= Seq(
      metricsScala,
      metricsServlets,
      metricsServlet
    ),
    description := "Scalatra integration with Metrics"
  )
) dependsOn(scalatraCore % "compile;test->test;provided->provided")

lazy val scalatraCache = Project(
  id = "scalatra-cache",
  base = file("cache"),
  settings = scalatraSettings ++ Seq(
    libraryDependencies ++= Seq(
      jodaTime,
      jodaConvert
    ),
    description := "Scalatra Cache support"
  )
) dependsOn(scalatraCore % "compile;test->test;provided->provided")

lazy val scalatraCacheGuava = Project(
  id = "scalatra-cache-guava",
  base = file("cache-guava"),
  settings = scalatraSettings ++ Seq(
    libraryDependencies ++= Seq(
      googleGuava,
      googleFindBugs
    ),
    description := "Scalatra Cache integration with Google Guava"
  )
) dependsOn(
  scalatraCore % "compile;test->test;provided->provided",
  scalatraCache % "compile;test->test;provided->provided"
)

lazy val manifestSetting = packageOptions += {
  Package.ManifestAttributes(
    "Created-By" -> "Simple Build Tool",
    "Built-By" -> System.getProperty("user.name"),
    "Build-Jdk" -> System.getProperty("java.version"),
    "Specification-Title" -> name.value,
    "Specification-Version" -> version.value,
    "Specification-Vendor" -> organization.value,
    "Implementation-Title" -> name.value,
    "Implementation-Version" -> version.value,
    "Implementation-Vendor-Id" -> organization.value,
    "Implementation-Vendor" -> organization.value
  )
}

// Things we care about primarily because Maven Central demands them
lazy val mavenCentralFrouFrou = Seq(
  homepage := Some(new URL("http://www.scalatra.org/")),
  startYear := Some(2009),
  licenses := Seq(("BSD", new URL("http://github.com/scalatra/scalatra/raw/HEAD/LICENSE"))),
  pomExtra := pomExtra.value ++ Group(
    <scm>
      <url>http://github.com/scalatra/scalatra</url>
      <connection>scm:git:git://github.com/scalatra/scalatra.git</connection>
    </scm>
    <developers>
      <developer>
        <id>riffraff</id>
        <name>Gabriele Renzi</name>
        <url>http://www.riffraff.info</url>
      </developer>
      <developer>
        <id>alandipert</id>
        <name>Alan Dipert</name>
        <url>http://alan.dipert.org</url>
      </developer>
      <developer>
        <id>rossabaker</id>
        <name>Ross A. Baker</name>
        <url>http://www.rossabaker.com/</url>
      </developer>
      <developer>
        <id>chirino</id>
        <name>Hiram Chirino</name>
        <url>http://hiramchirino.com/blog/</url>
      </developer>
      <developer>
        <id>casualjim</id>
        <name>Ivan Porto Carrero</name>
        <url>http://flanders.co.nz/</url>
      </developer>
      <developer>
        <id>jlarmstrong</id>
        <name>Jared Armstrong</name>
        <url>http://www.jaredarmstrong.name/</url>
      </developer>
      <developer>
        <id>mnylen</id>
        <name>Mikko Nylen</name>
        <url>https://github.com/mnylen/</url>
      </developer>
      <developer>
        <id>dozed</id>
        <name>Stefan Ollinger</name>
        <url>http://github.com/dozed/</url>
      </developer>
      <developer>
        <id>sdb</id>
        <name>Stefan De Boey</name>
        <url>http://github.com/sdb/</url>
      </developer>
      <developer>
        <id>ymasory</id>
        <name>Yuvi Masory</name>
        <url>http://github.com/ymasory/</url>
      </developer>
      <developer>
        <id>jfarcand</id>
        <name>Jean-François Arcand</name>
        <url>http://github.com/jfarcand/</url>
      </developer>
      <developer>
        <id>ceedubs</id>
        <name>Cody Alen</name>
        <url>http://github.com/ceedubs/</url>
      </developer>
      <developer>
        <id>BowlingX</id>
        <name>David Heidrich</name>
        <url>http://github.com/BowlingX/</url>
      </developer>
      <developer>
        <id>ayush</id>
        <name>Ayush Gupta</name>
        <url>hhttps://github.com/ayush</url>
      </developer>
      <developer>
        <id>seratch</id>
        <name>Kazuhiro Sera</name>
        <url>hhttps://github.com/seratch</url>
      </developer>
    </developers>
  )
)

lazy val doNotPublish = Seq(publish := {}, publishLocal := {}, PgpKeys.publishSigned := {}, PgpKeys.publishLocalSigned := {})

