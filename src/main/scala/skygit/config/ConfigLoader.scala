package skygit.config

import java.io.File
import java.nio.file.{Files, Path}
import scala.io.Source
import upickle.default.*

case class Language(
    name: String,
    extensions: List[String]
) derives ReadWriter

case class FilenameMapping(
    name: String,
    filenames: List[String]
) derives ReadWriter

case class LanguageConfig(
    languages: List[Language],
    filenames: List[FilenameMapping]
) derives ReadWriter

case class ServerConfig(
    filePath: String,
    gitPort: Int,
    webPort: Int
) derives ReadWriter

object ConfigLoader {
    val configFilePath: String = "config/languages.json"
    val serverConfigFilePath: String = "config/server.json"

    private def genericLoad[T: ReadWriter](filePath: String): Option[T] = {
        if (Files.exists(Path.of(filePath))) {
            val json = Files.readString(Path.of(filePath))
            try
                Some(read[T](json))
            catch {
                case _: Exception => None
            }
        } else {
            None
        }
    }

    def loadLanguages(): Option[LanguageConfig] = {
        genericLoad[LanguageConfig](configFilePath)
    }

    def loadServerConfig(): Option[ServerConfig] = {
        genericLoad[ServerConfig](serverConfigFilePath)
    }
}
