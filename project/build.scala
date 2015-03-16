import com.typesafe.sbt.pgp.PgpKeys
import sbt._
import Keys._
import scala.xml._
import java.net.URL
import org.scalatra.sbt.ScalatraPlugin.scalatraWithWarOverlays
import com.typesafe.sbt.SbtScalariform.scalariformSettings
import com.typesafe.tools.mima.core._
import com.typesafe.tools.mima.core.ProblemFilters._
import com.typesafe.tools.mima.plugin.MimaPlugin.mimaDefaultSettings
import com.typesafe.tools.mima.plugin.MimaKeys.{binaryIssueFilters, previousArtifact}

object ScalatraBuild extends Build {
  import Dependencies._

  lazy val scalatraSettings =
    mimaDefaultSettings ++ Seq(
    organization := "org.scalatra",
    crossScalaVersions := Seq("2.11.6", "2.10.5"),
    scalaVersion <<= (crossScalaVersions) { versions => versions.head },
    scalacOptions ++= Seq("-target:jvm-1.7", "-unchecked", "-deprecation", "-Yinline-warnings", "-Xcheckinit", "-encoding", "utf8", "-feature"),
    scalacOptions ++= Seq("-language:higherKinds", "-language:postfixOps", "-language:implicitConversions", "-language:reflectiveCalls", "-language:existentials"),
    javacOptions  ++= Seq("-target", "1.7", "-source", "1.7", "-Xlint:deprecation"),
    manifestSetting,
    resolvers ++= Seq(
      Opts.resolver.sonatypeSnapshots,
      Opts.resolver.sonatypeReleases,
      "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
      "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases" // specs2 2.4.3 or higher requires this
    ),
    dependencyOverrides := Set(
      "org.scala-lang" %  "scala-library"  % scalaVersion.value,
      "org.scala-lang" %  "scala-reflect"  % scalaVersion.value,
      "org.scala-lang" %  "scala-compiler" % scalaVersion.value
    ),
    previousArtifact <<= (name, scalaVersion) { (name, sv) =>
      val cross = name + "_" + CrossVersion.binaryScalaVersion(sv)
      Some("org.scalatra" % cross % "2.3.0")
    }
  ) ++ net.virtualvoid.sbt.graph.Plugin.graphSettings ++ mavenCentralFrouFrou ++ scalariformSettings

  lazy val scalatraProject = Project(
    id = "scalatra-project",
    base = file("."),
    settings = scalatraSettings ++ Unidoc.unidocSettings ++ doNotPublish ++ Seq(
      description := "A tiny, Sinatra-like web framework for Scala",
      Unidoc.unidocExclude := Seq("scalatra-example"),
      previousArtifact := None
    ),
    aggregate = Seq(scalatraCore, scalatraAuth, scalatraFileupload, scalatraCommands,
      scalatraScalate, scalatraJson, scalatraSlf4j, scalatraAtmosphere,
      scalatraTest, scalatraScalatest, scalatraSpecs2,
      scalatraExample, scalatraSwagger, scalatraJetty,
      scalatraCommon, scalatraSwaggerExt, scalatraSpring,
      scalatraMetrics, scalatraCache, scalatraCacheGuava)
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
      libraryDependencies <++= scalaVersion(sv => {
        val defau = Seq(servletApi % "provided;test",
          slf4jApi,
          grizzledSlf4j,
          rl,
          jUniversalChardet,
          mimeUtil,
          jodaTime,
          jodaConvert,
          akkaActor % "test"
        )
        if (sv.startsWith("2.10")) defau else defau ++ Seq(parserCombinators, xml)
      }),
      libraryDependencies ++= Seq(akkaTestkit % "test"),
      description := "The core Scalatra framework",
      binaryIssueFilters ++= Seq(
        exclude[MissingTypesProblem]("org.scalatra.HaltException"),
        exclude[MissingTypesProblem]("org.scalatra.PassException"),
        exclude[MissingMethodProblem]("org.scalatra.i18n.I18nSupport.provideMessages")
      )
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
      libraryDependencies ++= Seq(base64),
      description := "Scalatra authentication module"
    )
  ) dependsOn(scalatraCore % "compile;test->test;provided->provided")


  lazy val scalatraFileupload = Project(
    id = "scalatra-fileupload",
    base = file("fileupload"),
    settings = scalatraSettings ++ Seq(
      libraryDependencies ++= Seq(commonsFileupload, commonsIo),
      description := "Commons-Fileupload integration with Scalatra"
    )
  ) dependsOn(scalatraCore % "compile;test->test;provided->provided")

  lazy val scalatraAtmosphere = Project(
    id = "scalatra-atmosphere",
    base = file("atmosphere"),
    settings = scalatraSettings ++ Seq(
      libraryDependencies ++= Seq(akkaActor, akkaTestkit % "test"),
      libraryDependencies ++= Seq(atmosphereRuntime, atmosphereRedis, atmosphereCompatJbossweb, atmosphereCompatTomcat, atmosphereCompatTomcat7, atmosphereClient % "test", jettyWebsocket % "test"),
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
      libraryDependencies ++= Seq(json4sJackson % "provided", json4sNative % "provided", json4sCore)
    )
  ) dependsOn(scalatraCore % "compile;test->test;provided->provided")

  lazy val scalatraCommands = Project(
    id = "scalatra-commands",
    base = file("commands"),
    settings = scalatraSettings ++ Seq(
      libraryDependencies ++= Seq(
        "commons-validator"       % "commons-validator"  % "1.4.0",
        "io.backchat.inflector"  %% "scala-inflector"    % "1.3.5"
      ),
      libraryDependencies ++= Seq(scalaz, jodaTime, jodaConvert),
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
  ) dependsOn(
    scalatraJson % "compile;test->test;provided->provided")

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
      libraryDependencies <++= scalaVersion(sv => Seq(
        grizzledSlf4j,
        jettyWebapp,
        servletApi,
        mockitoAll,
        commonsLang3,
        specs2 % "test",
        httpclient,
        httpmime,
        jodaTime % "provided",
        jodaConvert % "provided"
      )),
      description := "The abstract Scalatra test framework"
    )
  ) dependsOn(scalatraCommon % "compile;test->test;provided->provided")

  lazy val scalatraScalatest = Project(
    id = "scalatra-scalatest",
    base = file("scalatest"),
    settings = scalatraSettings ++ Seq(
      libraryDependencies ++= Seq(scalatest, junit, testng % "optional", guice % "optional"),
      description := "ScalaTest support for the Scalatra test framework"
    )
  ) dependsOn(scalatraTest % "compile;test->test;provided->provided")

  lazy val scalatraSpecs2 = Project(
    id = "scalatra-specs2",
    base = file("specs2"),
    settings = scalatraSettings ++ Seq(
      libraryDependencies += specs2,
      description := "Specs2 support for the Scalatra test framework"
    )
  ) dependsOn(scalatraTest % "compile;test->test;provided->provided")

  lazy val scalatraSwagger = Project(
    id = "scalatra-swagger",
    base = file("swagger"),
    settings = scalatraSettings ++ Seq(
      libraryDependencies <++= (scalaVersion) { sv =>
        val com = Seq(json4sExt, logbackClassic % "provided")
        if (sv.startsWith("2.10")) com else  parserCombinators +: com
      },
      description := "Scalatra integration with Swagger"
    )
  ) dependsOn(scalatraCore % "compile;test->test;provided->provided", scalatraJson % "compile;test->test;provided->provided")

  lazy val scalatraSwaggerExt = Project(
    id = "scalatra-swagger-ext",
    base = file("swagger-ext"),
    settings = scalatraSettings ++ Seq(
      description := "Deeper Swagger integration for scalatra"
    )
  ) dependsOn(scalatraSwagger % "compile;test->test;provided->provided", scalatraCommands % "compile;test->test;provided->provided", scalatraAuth % "compile;test->test")

  lazy val scalatraSlf4j = Project(
    id = "scalatra-slf4j",
    base = file("slf4j"),
    settings = scalatraSettings ++ Seq(
      libraryDependencies ++= Seq(grizzledSlf4j, logbackClassic % "provided"),
      description := "Scalatra integration with SLF4J and Logback"
    )
  ) dependsOn(scalatraCore % "compile;test->test;provided->provided")

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
      libraryDependencies ++= Seq(metricsScala, metricsServlets, metricsServlet),
      description := "Scalatra integration with Metrics"
    )
  ) dependsOn(scalatraCore % "compile;test->test;provided->provided")

  lazy val scalatraCache = Project(
    id = "scalatra-cache",
    base = file("cache"),
    settings = scalatraSettings ++ Seq(
      libraryDependencies ++= Seq(),
      description := "Scalatra Cache support"
    )
  ) dependsOn(scalatraCore % "compile;test->test;provided->provided")

  lazy val scalatraCacheGuava = Project(
    id = "scalatra-cache-guava",
    base = file("cache-guava"),
    settings = scalatraSettings ++ Seq(
      libraryDependencies ++= Seq(googleGuava, googleFindBugs),
      description := "Scalatra Cache integration with Google Guava"
    )
  ) dependsOn(scalatraCore % "compile;test->test;provided->provided", scalatraCache % "compile;test->test;provided->provided")

  lazy val scalatraExample = Project(
     id = "scalatra-example",
     base = file("example"),
     settings = scalatraSettings ++ doNotPublish ++ scalatraWithWarOverlays ++ Seq(
       libraryDependencies += servletApi % "container;test;provided",
       libraryDependencies += jettyWebsocket % "container;test;provided",
       libraryDependencies += jettyServer % "container;test;provided",
       libraryDependencies += jettyPlus % "container;test",
       libraryDependencies ++= Seq(jettyWebapp % "container;test", slf4jSimple),
       libraryDependencies += json4sJackson,
       libraryDependencies += atmosphereJQuery,
       description := "Scalatra example project",
       previousArtifact := None
     )
  ) dependsOn(
     scalatraCore % "compile;test->test;provided->provided", scalatraScalate,
     scalatraAuth, scalatraFileupload, scalatraJetty, scalatraCommands, scalatraAtmosphere
  )

  object Dependencies {
    lazy val parserCombinators        =  "org.scala-lang.modules"  %% "scala-parser-combinators"   % "1.0.2"
    lazy val xml                      =  "org.scala-lang.modules"  %% "scala-xml"                  % "1.0.3"
    lazy val akkaActor                =  "com.typesafe.akka"       %% "akka-actor"                 % akkaVersion
    lazy val akkaTestkit              =  "com.typesafe.akka"       %% "akka-testkit"               % akkaVersion
    lazy val atmosphereRuntime        =  "org.atmosphere"          %  "atmosphere-runtime"         % "2.1.5"
    lazy val atmosphereJQuery         =  "org.atmosphere.client"   %  "jquery"                     % "2.2.6" artifacts(Artifact("jquery", "war", "war"))
    lazy val atmosphereClient         =  "org.atmosphere"          %  "wasync"                     % "1.4.3"
    lazy val atmosphereRedis          =  "org.atmosphere"          %  "atmosphere-redis"           % "2.2.1"
    lazy val atmosphereCompatJbossweb =  "org.atmosphere"          %  "atmosphere-compat-jbossweb" % atmosphereCompatVersion
    lazy val atmosphereCompatTomcat   =  "org.atmosphere"          %  "atmosphere-compat-tomcat"   % atmosphereCompatVersion
    lazy val atmosphereCompatTomcat7  =  "org.atmosphere"          %  "atmosphere-compat-tomcat7"  % atmosphereCompatVersion
    lazy val base64                   =  "net.iharder"             %  "base64"                     % "2.3.8"
    lazy val commonsFileupload        =  "commons-fileupload"      %  "commons-fileupload"         % "1.3.1"
    lazy val commonsIo                =  "commons-io"              %  "commons-io"                 % "2.4"
    lazy val commonsLang3             =  "org.apache.commons"      %  "commons-lang3"              % "3.3.2"
    lazy val grizzledSlf4j            =  "org.clapper"             %% "grizzled-slf4j"             % grizzledSlf4jVersion
    lazy val guice                    =  "com.google.inject"       %  "guice"                      % "3.0"
    lazy val httpclient               =  "org.apache.httpcomponents" % "httpclient"                % httpcomponentsVersion
    lazy val httpmime                 =  "org.apache.httpcomponents" % "httpmime"                  % httpcomponentsVersion
    lazy val jettyServer              =  "org.eclipse.jetty"       %  "jetty-server"               % jettyVersion
    lazy val jettyPlus                =  "org.eclipse.jetty"       %  "jetty-plus"                 % jettyVersion
    lazy val jettyServlet             =  "org.eclipse.jetty"       %  "jetty-servlet"              % jettyVersion
    lazy val jettyWebsocket           =  "org.eclipse.jetty.websocket" %"websocket-server"         % jettyVersion
    lazy val jettyWebapp              =  "org.eclipse.jetty"       %  "jetty-webapp"               % jettyVersion
    lazy val jodaConvert              =  "org.joda"                %  "joda-convert"               % "1.7"
    lazy val jodaTime                 =  "joda-time"               %  "joda-time"                  % "2.6"
    lazy val json4sCore               =  "org.json4s"              %% "json4s-core"                % json4sVersion
    lazy val json4sExt                =  "org.json4s"              %% "json4s-ext"                 % json4sVersion
    lazy val json4sJackson            =  "org.json4s"              %% "json4s-jackson"             % json4sVersion
    lazy val json4sNative             =  "org.json4s"              %% "json4s-native"              % json4sVersion
    lazy val junit                    =  "junit"                   %  "junit"                      % "4.11"
    lazy val jUniversalChardet        =  "com.googlecode.juniversalchardet" % "juniversalchardet"  % "1.0.3"
    lazy val logbackClassic           =  "ch.qos.logback"          %  "logback-classic"            % "1.1.2"
    lazy val mimeUtil                 =  "eu.medsea.mimeutil"      %  "mime-util"                  % "2.1.3" exclude("org.slf4j", "slf4j-log4j12") exclude("log4j", "log4j")
    lazy val mockitoAll               =  "org.mockito"             %  "mockito-all"                % "1.10.15"
    lazy val rl                       =  "org.scalatra.rl"         %% "rl"                         % "0.4.10"
    lazy val scalajCollection         =  "org.scalaj"              %% "scalaj-collection"          % "1.2"
    lazy val scalate                  =  "org.scalatra.scalate"    %% "scalate-core"               % scalateVersion
    lazy val scalatest                =  "org.scalatest"           %% "scalatest"                  % scalatestVersion
    lazy val scalaz                   =  "org.scalaz"              %% "scalaz-core"                % "7.1.0"
    lazy val servletApi               =  "javax.servlet"           %  "javax.servlet-api"          % "3.1.0"
    lazy val springWeb                =  "org.springframework"     %  "spring-web"                 % "4.1.3.RELEASE"
    lazy val slf4jApi                 =  "org.slf4j"               %  "slf4j-api"                  % "1.7.7"
    lazy val slf4jSimple              =  "org.slf4j"               %  "slf4j-simple"               % "1.7.7"
    lazy val specs2                   =  "org.specs2"              %% "specs2"                     % specs2Version
    lazy val testJettyServlet         =  "org.eclipse.jetty"       %  "test-jetty-servlet"         % jettyVersion
    lazy val testng                   =  "org.testng"              %  "testng"                     % "6.8.8"
    lazy val metricsScala             =  "nl.grons"                %% "metrics-scala"              % "3.3.0_a2.3"
    lazy val metricsServlets          =  "io.dropwizard.metrics"   %  "metrics-servlets"           % "3.1.0"
    lazy val metricsServlet           =  "io.dropwizard.metrics"   %  "metrics-servlet"            % "3.1.0"
    lazy val googleGuava              =  "com.google.guava"        % "guava"                       % "18.0"
    lazy val googleFindBugs           = "com.google.code.findbugs" % "jsr305"                      % "1.3.9"

    private val akkaVersion             = "2.3.7"
    private val grizzledSlf4jVersion    = "1.0.2"
    private val atmosphereCompatVersion = "2.0.1"
    private val httpcomponentsVersion   = "4.3.6"
    private val jettyVersion            = "9.2.6.v20141205"
    private val json4sVersion           = "3.2.11"
    private val scalateVersion          = "1.7.1"
    private val scalatestVersion        = "2.2.2"
    private val specs2Version           = "2.4.15"
  }

  lazy val manifestSetting = packageOptions <+= (name, version, organization) map {
    (title, version, vendor) =>
      Package.ManifestAttributes(
        "Created-By" -> "Simple Build Tool",
        "Built-By" -> System.getProperty("user.name"),
        "Build-Jdk" -> System.getProperty("java.version"),
        "Specification-Title" -> title,
        "Specification-Version" -> version,
        "Specification-Vendor" -> vendor,
        "Implementation-Title" -> title,
        "Implementation-Version" -> version,
        "Implementation-Vendor-Id" -> vendor,
        "Implementation-Vendor" -> vendor
      )
  }

  // Things we care about primarily because Maven Central demands them
  lazy val mavenCentralFrouFrou = Seq(
    homepage := Some(new URL("http://www.scalatra.org/")),
    startYear := Some(2009),
    licenses := Seq(("BSD", new URL("http://github.com/scalatra/scalatra/raw/HEAD/LICENSE"))),
    pomExtra <<= (pomExtra, name, description) {(pom, name, desc) => pom ++ Group(
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
      </developers>
    )}
  )

  lazy val doNotPublish = Seq(publish := {}, publishLocal := {}, PgpKeys.publishSigned := {}, PgpKeys.publishLocalSigned := {})

}
