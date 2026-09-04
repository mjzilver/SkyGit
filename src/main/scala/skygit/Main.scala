package skygit

import java.io.File

import skygit.cli.Cli
import skygit.config.ConfigLoader
import skygit.git.GitMirror

object Main {
    def main(args: Array[String]): Unit =
        Cli.run(args.toList)
}
