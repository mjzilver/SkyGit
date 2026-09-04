package skygit

import java.io.File

import org.eclipse.jgit.transport.Daemon

import skygit.config.ConfigLoader
import skygit.discovery.MdnsAdvertiser
import skygit.git.GitServer
import skygit.web.WebServer

object Server {

    def start(
        baseDir: File,
        gitPort: Int = Daemon.DEFAULT_PORT,
        webPort: Int = 8080
    ): Unit = {
        try {
            val languageConfig = ConfigLoader
                .loadLanguages()
                .getOrElse(throw new RuntimeException("Failed to load language configuration"))

            val gitServer = new GitServer(baseDir, gitPort)
            val webServer = new WebServer(baseDir, webPort, languageConfig)
            val mdnsAdvertiser = new MdnsAdvertiser(gitPort, webPort)

            gitServer.start()
            webServer.start()
            mdnsAdvertiser.start()
            Thread.currentThread().join()
        } catch {
            case e: Exception => println(s"Failed to start server: ${e.getMessage}")
        }
    }
}
