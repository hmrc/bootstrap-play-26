import play.sbt.PlayImport.{filters, guice}
import sbt._

object AppDependencies {

  import play.core.PlayVersion

  val compile = Seq(
    guice,
    filters,
    "uk.gov.hmrc"           %% "crypto"              % "4.4.0",
    "uk.gov.hmrc"           %% "http-verbs"          % "7.4.0",
    "uk.gov.hmrc"           %% "http-verbs-play-26"  % "0.3.0",
    "uk.gov.hmrc"           %% "play-auditing"       % "3.3.0",
    "uk.gov.hmrc"           %% "auth-client"         % "2.6.0",
    "uk.gov.hmrc"           %% "logback-json-logger" % "3.1.0",
    "com.typesafe.play"     %% "play"                % PlayVersion.current,
    "io.dropwizard.metrics" % "metrics-graphite"     % "3.2.5",
    "com.kenshoo"           %% "metrics-play"        % "2.6.6_0.6.2",
    "ch.qos.logback"        % "logback-core"         % "1.1.7"
//    "uk.gov.hmrc"           %% "play-health"         % "2.1.0" todo (konrad) needs updating to 2.6
  )

  val test = Seq(
    "com.typesafe.play"      %% "play-test"          % PlayVersion.current % Test,
    "org.scalacheck"         %% "scalacheck"         % "1.14.0"            % Test,
    "org.mockito"            % "mockito-all"         % "1.9.5"             % Test,
    "org.pegdown"            % "pegdown"             % "1.5.0"             % Test,
    "com.github.tomakehurst" % "wiremock"            % "2.18.0"            % Test,
    "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2"             % Test,
    "com.typesafe.play"      %% "play-iteratees"     % "2.6.1"             % Test
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
