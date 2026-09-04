package skygit.git

import java.io.File

import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder

object RepoRegistry {

    def list(baseDir: File): List[String] = {
        Option(baseDir.listFiles())
            .getOrElse(Array.empty[File])
            .filter(f => f.isDirectory && f.getName.endsWith(".git"))
            .map(_.getName.stripSuffix(".git"))
            .sorted
            .toList
    }

    def resolve(baseDir: File, name: String): Option[Repository] = {
        try {
            val dir = new File(baseDir, s"${GitPath.sanitize(name)}.git")

            if !dir.exists() then None
            else
                Some(
                    new FileRepositoryBuilder()
                        .setGitDir(dir)
                        .setMustExist(true)
                        .build()
                )
        } catch case _: Exception => None
    }
}
