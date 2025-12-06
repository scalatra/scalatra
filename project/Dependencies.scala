import sbt.*
import sbt.Keys.*

object Dependencies {
  lazy val parserCombinators   = "org.scala-lang.modules"           %% "scala-parser-combinators" % "2.4.0"
  lazy val xml                 = "org.scala-lang.modules"           %% "scala-xml"                % "2.4.0"
  lazy val collectionCompact   = "org.scala-lang.modules"           %% "scala-collection-compat"  % "2.14.0"
  lazy val commonsText         = "org.apache.commons"                % "commons-text"             % "1.14.0"
  lazy val httpclient          = "org.apache.httpcomponents.client5" % "httpclient5"              % "5.5.1"
  lazy val jettyServletJavax   = "org.eclipse.jetty"                 % "jetty-servlet"            % "10.0.21"
  lazy val jettyWebappJavax    = "org.eclipse.jetty"                 % "jetty-webapp"             % "10.0.26"
  lazy val jettyServletJakarta = "org.eclipse.jetty.ee10"            % "jetty-ee10-servlet"       % "12.0.9"
  lazy val jettyWebappJakarta  = "org.eclipse.jetty.ee10"            % "jetty-ee10-webapp"        % "12.1.5"
  lazy val json4sCore          = "org.json4s"                       %% "json4s-core"              % json4sVersion
  lazy val json4sJackson       = "org.json4s"                       %% "json4s-jackson"           % json4sVersion
  lazy val json4sNative        = "org.json4s"                       %% "json4s-native"            % json4sVersion
  lazy val json4sXml           = "org.json4s"                       %% "json4s-xml"               % json4sVersion
  lazy val scalatestJunit      = "org.scalatestplus"                %% "junit-4-13"               % "3.2.19.1"
  lazy val jUniversalChardet   = "com.github.albfernandez"           % "juniversalchardet"        % "2.4.0"
  lazy val mockitoAll          = "org.mockito"                       % "mockito-core"             % "5.20.0"
  lazy val logbackClassic      = "ch.qos.logback"                    % "logback-classic"          % "1.5.21"
  lazy val scalatest           = Seq(
    "funspec",
    "wordspec",
    "flatspec",
    "freespec",
    "featurespec",
    "funsuite",
    "shouldmatchers",
    "mustmatchers",
  ).map(x => "org.scalatest" %% s"scalatest-$x" % scalatestVersion)
  lazy val servletApiJavax   = "javax.servlet"   % "javax.servlet-api"   % "4.0.1"
  lazy val servletApiJakarta = "jakarta.servlet" % "jakarta.servlet-api" % "6.0.0"
  lazy val slf4jApi          = "org.slf4j"       % "slf4j-api"           % "2.0.17"
  lazy val specs2            = Seq(
    "org.specs2" %% "specs2-core",
    "org.specs2" %% "specs2-matcher-extra"
  ).map(_ % specs2Version)
  lazy val metricsScala           = "nl.grons"                %% "metrics4-scala"           % "4.3.7"
  lazy val metricsServletsJavax   = "io.dropwizard.metrics"    % "metrics-servlets"         % "4.2.37"
  lazy val metricsServletJavax    = "io.dropwizard.metrics"    % "metrics-servlet"          % "4.2.37"
  lazy val metricsServletsJakarta = "io.dropwizard.metrics"    % "metrics-jakarta-servlets" % "4.2.37"
  lazy val metricsServletJakarta  = "io.dropwizard.metrics"    % "metrics-jakarta-servlet"  % "4.2.37"
  lazy val googleGuava            = "com.google.guava"         % "guava"                    % "33.4.7-jre"
  lazy val twirlApi               = "org.playframework.twirl" %% "twirl-api"                % "2.0.9"

  private val json4sVersion    = "4.0.7"
  private val specs2Version    = "4.23.0"
  private val scalatestVersion = "3.2.19"
}
