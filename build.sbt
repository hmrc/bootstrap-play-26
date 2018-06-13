val appName = "bootstrap-play-26"

lazy val library = Project(appName, file("."))
  .enablePlugins(SbtAutoBuildPlugin, SbtGitVersioning)
  .settings(
    scalaVersion        := "2.11.11",
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
    dependencyOverrides ++= AppDependencies.overrides,
    crossScalaVersions  := Seq("2.11.11"),
    fork in Test        := true,
    scalacOptions       ++= Seq("-deprecation"),
    resolvers           :=
      Seq(
        Resolver.bintrayRepo("hmrc", "releases"),
        Resolver.bintrayRepo("hmrc", "snapshots"),
        Resolver.bintrayRepo("hmrc", "release-candidates"),
        Resolver.typesafeRepo("releases"),
        Resolver.jcenterRepo
      )
  )
  .disablePlugins(sbt.plugins.JUnitXmlReportPlugin)
