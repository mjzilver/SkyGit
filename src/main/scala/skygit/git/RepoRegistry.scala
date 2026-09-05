package skygit.git

import java.io.File
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import upickle.default.ReadWriter
import scala.jdk.CollectionConverters._

case class RepositoryInfo(name: String) derives ReadWriter

object RepoRegistry {

    def getLastCommitTime(folder: File): Long = {
        val repo =
            new FileRepositoryBuilder()
                .setGitDir(folder)
                .setMustExist(true)
                .build()

        try {
            val head =
                Option(repo.resolve("HEAD"))
                    .orElse(Option(repo.resolve("refs/heads/main")))
                    .orElse(Option(repo.resolve("refs/heads/master")))

            head match
                case Some(objectId) =>
                    repo.parseCommit(objectId).getCommitTime.toLong * 1000

                case None =>
                    println(s"No commits found in repository at ${folder.getAbsolutePath}")
                    0L
        } finally {
            repo.close()
        }
    }

    def list(baseDir: File): List[RepositoryInfo] = {
        Option(baseDir.listFiles())
            .getOrElse(Array.empty[File])
            .filter(f =>
                f.isDirectory && f.getName
                    .endsWith(".git") && f.getName.stripSuffix(".git").nonEmpty
            )
            .map(f =>
                RepositoryInfo(
                    f.getName.stripSuffix(".git")
                )
            )
            .sortBy(repo => repo.name)
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
