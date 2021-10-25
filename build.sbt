import com.jsuereth.sbtpgp.PgpKeys
import scala.xml._
import java.net.URL
import Dependencies._

val unusedOptions = Seq("-Ywarn-unused:imports")

lazy val scalatraSettings = Seq(
  organization := "org.scalatra",
  Test / fork := true,
  Test / baseDirectory := (ThisBuild / baseDirectory).value,
  crossScalaVersions := Seq("2.12.15", "2.13.6", "3.1.1-RC1"),
  scalaVersion := crossScalaVersions.value.head,
  libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % "always",
  allDependencies := {
    val values = allDependencies.value
    // workaround for
    // "Modules were resolved with conflicting cross-version suffixes"
    // "   org.scala-lang.modules:scala-xml _3.0.0-RC1, _2.13"
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((3, _)) =>
        values.map(_.exclude("org.scala-lang.modules", "scala-xml_2.13"))
      case _ =>
        values
    }
  },
  Test / testOptions ++= {
    if (scalaBinaryVersion.value == "3") {
      Seq(
        Tests.Exclude(Set(
          "org.scalatra.test.specs2.ScalatraSpecSpec",
          "org.scalatra.swagger.ModelSpec",
          "org.scalatra.swagger.SwaggerSpec2",
          "org.scalatra.swagger.ModelCollectionSpec",
        )),
      )
    } else {
      Nil
    }
  },
  scalacOptions ++= {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, _)) =>
        unusedOptions ++ Seq(
          "-target:jvm-1.8",
          "-Xlint",
          "-Xcheckinit",
        )
      case _ =>
        Seq(
          "-source",
          "3.0-migration",
        )
    }
  },
  scalacOptions ++= Seq(
    "-unchecked",
    "-deprecation",
    /*"-Yinline-warnings",*/
    "-encoding", "utf8",
    "-feature",
    "-language:higherKinds",
    "-language:implicitConversions",
    "-language:existentials"
  ),
  manifestSetting,
) ++ mavenCentralFrouFrou ++ Seq(Compile, Test).flatMap(c =>
  c / console / scalacOptions --= unusedOptions
)

lazy val scalatraProject = Project(
  id = "scalatra-project",
  base = file(".")).settings(
    scalatraSettings ++ Seq(
    name := "scalatra-unidoc",
    ScalaUnidoc / unidoc / unidocProjectFilter := {
      if (scalaBinaryVersion.value == "3") {
        // TODO enable
        inAnyProject -- inProjects(scalatraSpecs2)
      } else {
        (ScalaUnidoc / unidoc / unidocProjectFilter).value
      }
    },
    artifacts := Classpaths.artifactDefs(Seq(Compile / packageDoc, Compile / makePom)).value,
    packagedArtifacts := Classpaths.packaged(Seq(Compile / packageDoc, Compile / makePom)).value,
    description := "A tiny, Sinatra-like web framework for Scala",
    shellPrompt := { state =>
      s"sbt:${Project.extract(state).currentProject.id}" + Def.withColor("> ", Option(scala.Console.CYAN))
    }
  ) ++ Defaults.packageTaskSettings(
    Compile / packageDoc, (Compile / unidoc).map(_.flatMap(Path.allSubpaths))
  )).aggregate(
    scalatraCore,
    scalatraAuth,
    scalatraForms,
    scalatraTwirl,
    scalatraJson,
    scalatraTest,
    scalatraScalatest,
    scalatraSpecs2,
    scalatraSwagger,
    scalatraJetty,
    scalatraCommon,
    scalatraMetrics,
    scalatraCache,
  ).enablePlugins(ScalaUnidocPlugin)

lazy val scalatraCommon = Project(
  id = "scalatra-common",
  base = file("common")).settings(
    scalatraSettings ++ Seq(
    libraryDependencies ++= Seq(servletApi % "provided,test")
  )
)

lazy val scalatraCore = Project(
  id = "scalatra",
  base = file("core")).settings(scalatraSettings ++ Seq(
    libraryDependencies ++= Seq(
      servletApi % "provided;test",
      slf4jApi,
      jUniversalChardet,
      commonsText,
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
  base = file("auth")).settings(
    scalatraSettings ++ Seq(
    description := "Scalatra authentication module"
  )
) dependsOn(scalatraCore % "compile;test->test;provided->provided")

lazy val scalatraTwirl = Project(
  id = "scalatra-twirl",
  base = file("twirl")).settings(
  scalatraSettings ++ Seq(
    libraryDependencies += twirlApi,
    description := "Twirl integration with Scalatra"
  )
) dependsOn(scalatraCore  % "compile;test->test;provided->provided")

lazy val scalatraJson = Project(
  id = "scalatra-json",
  base = file("json")).settings(
    scalatraSettings ++ Seq(
    description := "JSON support for Scalatra",
    libraryDependencies ++= Seq(
      json4sJackson % "provided",
      json4sNative % "provided",
      json4sCore,
      json4sXml
    )
  )
) dependsOn(scalatraCore % "compile;test->test;provided->provided")

lazy val scalatraForms = Project(
  id = "scalatra-forms",
  base = file("forms")).settings(
    scalatraSettings ++ Seq(
    description := "Data binding and validation for Scalatra"
  )
) dependsOn(scalatraCore % "compile;test->test;provided->provided")

lazy val scalatraJetty = Project(
  id = "scalatra-jetty",
  base = file("jetty")).settings(
    scalatraSettings ++ Seq(
    libraryDependencies ++= Seq(
      servletApi,
      jettyServlet
    ),
    description := "Embedded Jetty server for Scalatra apps"
  )
) dependsOn(scalatraCore % "compile;test->test;provided->provided")

lazy val scalatraTest = Project(
  id = "scalatra-test",
  base = file("test")).settings(
    scalatraSettings ++ Seq(
    libraryDependencies ++= Seq(
      jettyWebapp,
      servletApi,
      mockitoAll,
      httpclient,
      httpmime
    ) ++ specs2.map(_ % "test"),
    description := "The abstract Scalatra test framework"
  )
) dependsOn(scalatraCommon % "compile;test->test;provided->provided")

lazy val scalatraScalatest = Project(
  id = "scalatra-scalatest",
  base = file("scalatest")).settings(
    scalatraSettings ++ Seq(
    libraryDependencies ++= scalatest,
    libraryDependencies ++= Seq(
      scalatestJunit,
      junit
    ),
    description := "ScalaTest support for the Scalatra test framework"
  )
) dependsOn(scalatraTest % "compile;test->test;provided->provided")

lazy val scalatraSpecs2 = Project(
  id = "scalatra-specs2",
  base = file("specs2")).settings(
    scalatraSettings ++ Seq(
    libraryDependencies ++= specs2,
    Test / scalacOptions ++= {
      if (scalaBinaryVersion.value == "3") {
        Seq("-Xignore-scala2-macros")
      } else {
        Nil
      }
    },
    description := "Specs2 support for the Scalatra test framework"
  )
) dependsOn(scalatraTest % "compile;test->test;provided->provided")

lazy val scalatraSwagger = Project(
  id = "scalatra-swagger",
  base = file("swagger")).settings(
    scalatraSettings ++ Seq(
    libraryDependencies ++= Seq(
      parserCombinators,
      logbackClassic % "provided"
    ),
    description := "Scalatra integration with Swagger"
  )
) dependsOn(
  scalatraCore % "compile;test->test;provided->provided",
  scalatraJson % "compile;test->test;provided->provided",
  scalatraAuth % "compile;test->test"
)

lazy val scalatraMetrics = Project(
  id = "scalatra-metrics",
  base = file("metrics")).settings(
    scalatraSettings ++ Seq(
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
  base = file("cache")).settings(
    scalatraSettings ++ Seq(
    libraryDependencies ++= Seq(
      googleGuava
    ),
    description := "Scalatra Cache support"
  )
) dependsOn(scalatraCore % "compile;test->test;provided->provided")

//lazy val scalatraCacheGuava = Project(
//  id = "scalatra-cache-guava",
//  base = file("cache-guava")).settings(
//    scalatraSettings ++ Seq(
//    libraryDependencies ++= Seq(
//      googleGuava,
//      googleFindBugs
//    ),
//    description := "Scalatra Cache integration with Google Guava"
//  )
//) dependsOn(
//  scalatraCore % "compile;test->test;provided->provided",
//  scalatraCache % "compile;test->test;provided->provided"
//)

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
