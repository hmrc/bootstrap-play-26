import sbt._

object AppDependencies {

  private val playVersion = "2.6.15"

  val compile = Seq(
    "ch.qos.logback"        % "logback-core"         % "1.1.7",
    "com.kenshoo"           %% "metrics-play"        % "2.6.6_0.6.2",
    "com.typesafe.play"     %% "filters-helpers"     % playVersion,
    "com.typesafe.play"     %% "play"                % playVersion,
    "com.typesafe.play"     %% "play-guice"          % playVersion,
    "io.dropwizard.metrics" % "metrics-graphite"     % "3.2.5",
    "uk.gov.hmrc"           %% "auth-client"         % "2.10.0-play-26",
    "uk.gov.hmrc"           %% "crypto"              % "5.0.0",
    "uk.gov.hmrc"           %% "http-verbs"          % "8.0.0",
    "uk.gov.hmrc"           %% "http-verbs-play-26"  % "0.9.0",
    "uk.gov.hmrc"           %% "logback-json-logger" % "4.0.0",
    "uk.gov.hmrc"           %% "play-auditing"       % "3.10.0-play-26",
    "uk.gov.hmrc"           %% "play-health"         % "3.6.0-play-26",
    "uk.gov.hmrc"           %% "time"                % "3.1.0"
  )

  val test = Seq(
    "com.github.tomakehurst" % "wiremock"            % "2.18.0"    % Test,
    "com.typesafe.play"      %% "play-test"          % playVersion % Test,
    "org.mockito"            % "mockito-all"         % "1.9.5"     % Test,
    "org.pegdown"            % "pegdown"             % "1.5.0"     % Test,
    "org.scalacheck"         %% "scalacheck"         % "1.14.0"    % Test,
    "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2"     % Test
  )

  // Fixes a transitive dependency clash between wiremock and scalatestplus-play
  val overrides: Set[ModuleID] = {
    val jettyFromWiremockVersion = "9.2.24.v20180105"
    Set(
      "org.eclipse.jetty"           % "jetty-client"       % jettyFromWiremockVersion,
      "org.eclipse.jetty"           % "jetty-continuation" % jettyFromWiremockVersion,
      "org.eclipse.jetty"           % "jetty-http"         % jettyFromWiremockVersion,
      "org.eclipse.jetty"           % "jetty-io"           % jettyFromWiremockVersion,
      "org.eclipse.jetty"           % "jetty-security"     % jettyFromWiremockVersion,
      "org.eclipse.jetty"           % "jetty-server"       % jettyFromWiremockVersion,
      "org.eclipse.jetty"           % "jetty-servlet"      % jettyFromWiremockVersion,
      "org.eclipse.jetty"           % "jetty-servlets"     % jettyFromWiremockVersion,
      "org.eclipse.jetty"           % "jetty-util"         % jettyFromWiremockVersion,
      "org.eclipse.jetty"           % "jetty-webapp"       % jettyFromWiremockVersion,
      "org.eclipse.jetty"           % "jetty-xml"          % jettyFromWiremockVersion,
      "org.eclipse.jetty.websocket" % "websocket-api"      % jettyFromWiremockVersion,
      "org.eclipse.jetty.websocket" % "websocket-client"   % jettyFromWiremockVersion,
      "org.eclipse.jetty.websocket" % "websocket-common"   % jettyFromWiremockVersion
    )
  }
}
