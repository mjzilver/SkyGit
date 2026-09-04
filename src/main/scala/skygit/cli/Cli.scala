package skygit.cli

import java.io.File

import skygit.config.ServerConfig
import skygit.git.GitMirror
import skygit.config.ConfigLoader
import skygit.Server

object Cli {

    def run(args: List[String]): Unit = {
        args match
            case "--help" :: Nil =>
                Cli.printUsage()

            case "mirror" :: repoArg :: destination :: Nil =>
                GitMirror.mirror(File(repoArg).getCanonicalFile, destination)

            case "server" :: rest =>
                ConfigLoader.loadServerConfig() match
                    case Some(config) =>
                        Cli.parseServerOptions(rest, config) match
                            case Some(serverConfig) =>
                                Server.start(
                                    File(serverConfig.filePath).getCanonicalFile,
                                    serverConfig.gitPort,
                                    serverConfig.webPort
                                )
                            case None =>
                                Cli.printUsage()
                    case None =>
                        println(
                            s"Server configuration not found at ${ConfigLoader.serverConfigFilePath}."
                        )

            case _ =>
                Cli.printUsage()
    }

    def parseServerOptions(
        args: List[String],
        config: ServerConfig
    ): Option[ServerConfig] = {
        args match
            case Nil =>
                Some(config)

            case "--filedir" :: value :: rest =>
                parseServerOptions(
                    rest,
                    config.copy(filePath = File(value).getCanonicalFile.getAbsolutePath)
                )

            case "--git-port" :: value :: rest =>
                value.toIntOption match
                    case Some(port) =>
                        parseServerOptions(
                            rest,
                            config.copy(gitPort = port)
                        )
                    case None =>
                        println(s"Invalid git port: $value")
                        None

            case "--web-port" :: value :: rest =>
                value.toIntOption match
                    case Some(port) =>
                        parseServerOptions(
                            rest,
                            config.copy(webPort = port)
                        )
                    case None =>
                        println(s"Invalid web port: $value")
                        None

            case _ =>
                println(s"Unknown server option: ${args.head}")
                None
    }

    private def printUsageLine(key: String, description: String): Unit =
        println(f"  $key%-45s $description")

    def printUsage(): Unit =
        println("Usage:")
        printUsageLine(
            "skygit mirror <repository> <destination>",
            "Push repository to a mirror location"
        )
        printUsageLine("skygit server [options]", "Run the git server and web UI")
        println()
        println("Server options:")
        printUsageLine("--filedir <dir>", "Repository directory")
        printUsageLine("--git-port <port>", "Git server port")
        printUsageLine("--web-port <port>", "Web UI port")
        println()
        println("Options:")
        printUsageLine("--help", "Show this help message")
}
