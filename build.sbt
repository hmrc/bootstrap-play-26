val appName = "bootstrap-play-26"

lazy val library = Project(appName, file("."))
  .enablePlugins(SbtAutoBuildPlugin, SbtGitVersioning, SbtArtifactory)
  .settings(
    majorVersion := 0,
    makePublicallyAvailableOnBintray := true
  )
  .settings(
    scalaVersion        := "2.11.12",
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
    dependencyOverrides ++= AppDependencies.overrides,
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
