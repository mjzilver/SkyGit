val scala3Version = "3.8.4"

lazy val root = project
    .in(file("."))
    .settings(
        name := "SkyGit",
        version := "0.1.0-SNAPSHOT",

        scalaVersion := scala3Version,

        libraryDependencies += "org.scalameta" %% "munit" % "1.3.4" % Test,
        libraryDependencies += "org.eclipse.jgit" % "org.eclipse.jgit" % "7.7.1.202607240634-r",
        libraryDependencies += "org.jmdns" % "jmdns" % "3.6.3"
    )
