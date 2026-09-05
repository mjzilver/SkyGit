val scala3Version = "3.8.4"

lazy val root = project
    .in(file("."))
    .settings(
        name := "SkyGit",
        version := "0.1.0",

        scalaVersion := scala3Version,

        libraryDependencies += "org.scalameta" %% "munit" % "1.3.6" % Test,
        libraryDependencies += "org.eclipse.jgit" % "org.eclipse.jgit" % "7.7.1.202607240634-r",
        libraryDependencies += "org.jmdns" % "jmdns" % "3.6.3",
        libraryDependencies += "com.lihaoyi" %% "upickle" % "4.4.3",
        libraryDependencies += "com.lihaoyi" %% "cask" % "0.11.3",
        libraryDependencies += "org.slf4j" % "slf4j-simple" % "2.0.19",

        assembly / assemblyMergeStrategy := {
            case PathList("META-INF", "MANIFEST.MF") => MergeStrategy.discard
            case PathList("META-INF", _*)            => MergeStrategy.discard
            case _                                   => MergeStrategy.first
        },
        assembly / mainClass := Some("skygit.Main"),
        assembly / assemblyOutputPath := target.value / s"skygit-${version.value}.jar"
    )
