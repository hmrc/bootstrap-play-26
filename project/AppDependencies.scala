import play.sbt.PlayImport.filters
import sbt._

object AppDependencies {

  import play.core.PlayVersion

  val compile = Seq(
    filters,
    "uk.gov.hmrc"           %% "crypto"              % "4.4.0",
    "uk.gov.hmrc"           %% "http-verbs"          % "7.3.0",
    "uk.gov.hmrc"           %% "http-verbs-play-25"  % "0.10.0",
    "uk.gov.hmrc"           %% "play-auditing"       % "3.3.0",
    "uk.gov.hmrc"           %% "auth-client"         % "2.6.0",
    "uk.gov.hmrc"           %% "play-health"         % "2.1.0",
    "uk.gov.hmrc"           %% "play-config"         % "5.0.0",
    "uk.gov.hmrc"           %% "logback-json-logger" % "3.1.0",
    "com.typesafe.play"     %% "play"                % "2.5.12",
    "io.dropwizard.metrics" % "metrics-graphite"     % "3.2.5",
    "de.threedimensions"    %% "metrics-play"        % "2.5.13",
    "ch.qos.logback"        % "logback-core"         % "1.1.7"
  )

  val test = Seq(
    "com.typesafe.play"      %% "play-test"          % PlayVersion.current % Test,
    "org.scalacheck"         % "scalacheck_2.11"     % "1.12.5"            % Test,
    "org.mockito"            % "mockito-all"         % "1.9.5"             % Test,
    "org.pegdown"            % "pegdown"             % "1.5.0"             % Test,
    "com.github.tomakehurst" % "wiremock"            % "2.7.1"             % Test,
    "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1"             % Test,
    "uk.gov.hmrc"            %% "hmrctest"           % "2.4.0"             % Test
  )

}
