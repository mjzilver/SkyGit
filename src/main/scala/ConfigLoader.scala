import scala.io.Source
import java.nio.file.{Files, Path}
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

object ConfigLoader {
    private val configFilePath: String = "config/languages.json"

    def load(): LanguageConfig = {
        val json = Files.readString(Path.of(configFilePath))
        read[LanguageConfig](json)
    }
}
