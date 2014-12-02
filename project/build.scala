import com.typesafe.sbt.pgp.PgpKeys
import sbt._
import Keys._
import scala.xml._
import java.net.URL
import org.scalatra.sbt.ScalatraPlugin.scalatraWithWarOverlays
import com.typesafe.tools.mima.core._
import com.typesafe.tools.mima.core.ProblemFilters._
import com.typesafe.tools.mima.plugin.MimaPlugin.mimaDefaultSettings
import com.typesafe.tools.mima.plugin.MimaKeys.{binaryIssueFilters, previousArtifact}

object ScalatraBuild extends Build {
  import Dependencies._

  lazy val scalatraSettings =
    mimaDefaultSettings ++ Seq(
    organization := "org.scalatra",
    crossScalaVersions := Seq("2.11.4", "2.10.4"),
    scalaVersion <<= (crossScalaVersions) { versions => versions.head },
    scalacOptions ++= Seq("-target:jvm-1.7", "-unchecked", "-deprecation", "-Yinline-warnings", "-Xcheckinit", "-encoding", "utf8", "-feature"),
    scalacOptions ++= Seq("-language:higherKinds", "-language:postfixOps", "-language:implicitConversions", "-language:reflectiveCalls", "-language:existentials"),
    javacOptions ++= Seq("-target", "1.7", "-source", "1.7", "-Xlint:deprecation"),
    manifestSetting,
    resolvers ++= Seq(
      Opts.resolver.sonatypeSnapshots, 
      Opts.resolver.sonatypeReleases,
      "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"
    ),
    dependencyOverrides := Set(
      "org.scala-lang" %  "scala-library"  % scalaVersion.value,
      "org.scala-lang" %  "scala-reflect"  % scalaVersion.value,
      "org.scala-lang" %  "scala-compiler" % scalaVersion.value
    ),
    previousArtifact <<= (name, scalaVersion) { (name, sv) =>
      val cross = name + "_" + CrossVersion.binaryScalaVersion(sv)
      Some("org.scalatra" % cross % "2.2.2")
    }
  ) ++ net.virtualvoid.sbt.graph.Plugin.graphSettings ++ mavenCentralFrouFrou

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
      scalatraCommon, scalatraSwaggerExt, scalatraSpring)
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
          grizzledSlf4j(sv),
          rl,
          jUniversalChardet,
          mimeUtil,
          jodaTime,
          jodaConvert,
          akkaActor(sv) % "test"
        )
        if (sv.startsWith("2.10")) defau else defau ++ Seq(parserCombinators, xml)
      }),
      libraryDependencies <++= scalaVersion(sv => Seq(akkaTestkit(sv) % "test")),
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
      libraryDependencies <++= scalaVersion(sv => Seq(akkaActor(sv), akkaTestkit(sv) % "test")),
      libraryDependencies ++= Seq(atmosphereRuntime, atmosphereRedis, atmosphereCompatJbossweb, atmosphereCompatTomcat, atmosphereCompatTomcat7, atmosphereClient % "test", jettyWebsocket % "test"),
      description := "Atmosphere integration for scalatra"
    )
  ) dependsOn(scalatraJson % "compile;test->test;provided->provided")

  lazy val scalatraScalate = Project(
    id = "scalatra-scalate",
    base = file("scalate"),
    settings = scalatraSettings ++ Seq(
      libraryDependencies <+= scalaVersion(scalate),
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
        grizzledSlf4j(sv),
        jettyWebapp,
        servletApi,
        mockitoAll,
        commonsLang3,
        specs2(sv) % "test",
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
      libraryDependencies <++= scalaVersion(sv => Seq(scalatest(sv), junit, testng % "optional", guice % "optional")),
      description := "ScalaTest support for the Scalatra test framework"
    )
  ) dependsOn(scalatraTest % "compile;test->test;provided->provided")

  lazy val scalatraSpecs2 = Project(
    id = "scalatra-specs2",
    base = file("specs2"),
    settings = scalatraSettings ++ Seq(
      libraryDependencies <+= scalaVersion(specs2),
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
      libraryDependencies <++= scalaVersion(sv => Seq(grizzledSlf4j(sv), logbackClassic % "provided")),
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
      libraryDependencies += metricsScala,
      description := "Scalatra integration with Metrics"
    )
  ) dependsOn(scalatraCore % "compile;test->test;provided->provided")

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
    // Sort by artifact ID.
    lazy val parserCombinators          = "org.scala-lang.modules"   %% "scala-parser-combinators" % "1.0.2"
    lazy val xml                        = "org.scala-lang.modules"   %% "scala-xml"                % "1.0.2"
    lazy val akkaActor: MM         = sv => "com.typesafe.akka"       %%  "akka-actor"         % akkaVersion(sv)
    lazy val akkaTestkit: MM       = sv => "com.typesafe.akka"       %%  "akka-testkit"       % akkaVersion(sv)
    lazy val atmosphereRuntime          =  "org.atmosphere"          % "atmosphere-runtime"  % atmosphereVersion
    lazy val atmosphereJQuery           =  "org.atmosphere.client"   % "jquery"              % "2.2.5" artifacts(Artifact("jquery", "war", "war"))
    lazy val atmosphereClient           =  "org.atmosphere"          % "wasync"              % "1.3.2"
    lazy val atmosphereRedis            =  "org.atmosphere"          % "atmosphere-redis"    % "2.1.3"
    lazy val atmosphereCompatJbossweb   =  "org.atmosphere"          % "atmosphere-compat-jbossweb" % atmosphereCompatVersion
    lazy val atmosphereCompatTomcat     =  "org.atmosphere"          % "atmosphere-compat-tomcat"   % atmosphereCompatVersion
    lazy val atmosphereCompatTomcat7    =  "org.atmosphere"          % "atmosphere-compat-tomcat7"  % atmosphereCompatVersion
    lazy val base64                     =  "net.iharder"             %  "base64"             % "2.3.8"
    lazy val commonsFileupload          =  "commons-fileupload"      %  "commons-fileupload" % "1.3.1"
    lazy val commonsIo                  =  "commons-io"              %  "commons-io"         % "2.4"
    lazy val commonsLang3               =  "org.apache.commons"      %  "commons-lang3"      % "3.3.2"
    lazy val grizzledSlf4j: MM     = sv => "org.clapper"             % "grizzled-slf4j"      % grizzledSlf4jVersion(sv) cross crossMapped("2.9.3" -> "2.9.2")
    lazy val guice                      =  "com.google.inject"       %  "guice"              % "3.0"
    lazy val httpclient                 =  "org.apache.httpcomponents" % "httpclient"        % httpcomponentsVersion
    lazy val httpmime                   =  "org.apache.httpcomponents" % "httpmime"          % httpcomponentsVersion
    lazy val jettyServer                =  "org.eclipse.jetty"       %  "jetty-server"       % jettyVersion
    lazy val jettyPlus                  =  "org.eclipse.jetty"       %  "jetty-plus"         % jettyVersion
    lazy val jettyServlet               =  "org.eclipse.jetty"       %  "jetty-servlet"      % jettyVersion
    lazy val jettyWebsocket             =  "org.eclipse.jetty.websocket" %"websocket-server" % jettyVersion
    lazy val jettyWebapp                =  "org.eclipse.jetty"       %  "jetty-webapp"       % jettyVersion
    lazy val jodaConvert                =  "org.joda"                %  "joda-convert"       % "1.7"
    lazy val jodaTime                   =  "joda-time"               %  "joda-time"          % "2.5"
    lazy val json4sCore                 =  "org.json4s"              %% "json4s-core"        % json4sVersion
    lazy val json4sExt                  =  "org.json4s"              %% "json4s-ext"         % json4sVersion
    lazy val json4sJackson              =  "org.json4s"              %% "json4s-jackson"     % json4sVersion
    lazy val json4sNative               =  "org.json4s"              %% "json4s-native"      % json4sVersion
    lazy val junit                      =  "junit"                   %  "junit"              % "4.11"
    lazy val jUniversalChardet          =  "com.googlecode.juniversalchardet" % "juniversalchardet" % "1.0.3"
    lazy val logbackClassic             =  "ch.qos.logback"          %  "logback-classic"    % "1.1.2"
    lazy val mimeUtil                   =  "eu.medsea.mimeutil"      % "mime-util"           % "2.1.3" exclude("org.slf4j", "slf4j-log4j12") exclude("log4j", "log4j")
    lazy val mockitoAll                 =  "org.mockito"             %  "mockito-all"        % "1.9.5"
    lazy val rl                         =  "org.scalatra.rl"         %% "rl"                 % "0.4.10"
    lazy val scalajCollection           =  "org.scalaj"              %% "scalaj-collection"  % "1.2"
    lazy val scalate: MM           = sv => "org.scalatra.scalate"    %  scalateArtifact(sv)  % scalateVersion(sv)
    lazy val scalatest: MM         = sv => "org.scalatest"           %% "scalatest"          % scalatestVersion(sv)
    lazy val scalaz                     =  "org.scalaz"              %% "scalaz-core"        % "7.1.0"
    lazy val servletApi                 =  "javax.servlet"           % "javax.servlet-api"   % "3.1.0"
    lazy val springWeb                  =  "org.springframework"     % "spring-web"          % "4.1.1.RELEASE"
    lazy val slf4jApi                   =  "org.slf4j"               % "slf4j-api"           % "1.7.7"
    lazy val slf4jSimple                =  "org.slf4j"               % "slf4j-simple"        % "1.7.7"
    lazy val specs: MM             = sv => "org.scala-tools.testing" %  "specs"              % specsVersion(sv) cross specsCross
    lazy val specs2: MM            = sv => "org.specs2"              %% "specs2"             % specs2Version(sv)
//    lazy val swaggerAnnotations         =  "com.wordnik"             % "swagger-annotations" % swaggerVersion       cross swaggerCross exclude("log4j", "log4j") exclude("org.slf4j", "slf4j-log4j12")
//    lazy val swaggerCore                =  "com.wordnik"             % "swagger-core"        % swaggerVersion       cross swaggerCross exclude("log4j", "log4j") exclude("org.slf4j", "slf4j-log4j12")
    lazy val testJettyServlet           =  "org.eclipse.jetty"       %  "test-jetty-servlet" % jettyVersion
    lazy val testng                     =  "org.testng"              %  "testng"             % "6.8.8"
    lazy val metricsScala               =  "nl.grons"                %% "metrics-scala"      % "3.3.0_a2.3"

    type MM = String => ModuleID

    def crossMapped(mappings: (String, String)*): CrossVersion =
      CrossVersion.binaryMapped(Map(mappings: _*) orElse { case v => CrossVersion.binaryScalaVersion(v) })

    def defaultOrMapped(default: String, alternatives: (String, String)*): String => String =
      Map(alternatives: _*).withDefaultValue(default)

    private val akkaVersion: String => String =
      defaultOrMapped("2.3.6", "2.9.3" -> "2.0.5", "2.9.2" -> "2.0.5", "2.9.1" -> "2.0.2")

    private val grizzledSlf4jVersion: String => String = {
      case sv if sv startsWith "2.9."   => "0.6.10"
      case _                            => "1.0.2"
    }

    private val atmosphereVersion = "2.1.5"

    private val atmosphereCompatVersion = "2.0.1"

    private val httpcomponentsVersion = "4.3.5"

    private val jettyVersion = "9.2.3.v20140905"

    private val json4sVersion = "3.2.10"

    private val scalateArtifact: String => String = {
      case sv if sv startsWith "2.8."   => "scalate-core"
      case "2.9.0-1"                    => "scalate-core"
      case sv if sv startsWith "2.9."   => "scalate-core_2.9"
      case sv if sv startsWith "2.10."  => "scalate-core_2.10"
      case sv if sv startsWith "2.11."  => "scalate-core_2.11"
    }
    private val scalateVersion: String => String = {
      case "2.8.1"                      => "1.5.2-scala_2.8.1"
      case "2.8.2"                      => "1.5.3-scala_2.8.2"
      case "2.9.0-1"                    => "1.5.1"
      case "2.9.1"                      => "1.6.1"
      case "2.9.2"                      => "1.6.1"
      case _                            => "1.7.0"
    }

    private val scalatestVersion: String => String =
      defaultOrMapped("2.2.0")

    private val specsCross = crossMapped("2.8.2" -> "2.8.1", "2.9.2" -> "2.9.1")
    private val specsVersion: String => String =
      defaultOrMapped("1.6.9", "2.9.0-1" -> "1.6.8", "2.8.0" -> "1.6.8", "2.8.1" -> "1.6.8", "2.8.2" -> "1.6.8")

    private val specs2Version: String => String = {
      case sv if sv startsWith "2.8."   => "1.5"
      case "2.9.0-1"                    => "1.8.2"
      case sv if sv startsWith "2.9."   => "1.12.4.1"
      case _                            => "2.4.2"
    }

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
