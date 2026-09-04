package skygit.git

import java.io.File
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import upickle.default.ReadWriter

case class RepositoryInfo(name: String, lastModified: Long) derives ReadWriter

object RepoRegistry {

    def getLastCommitTime(folder: File): Long = {
        val repo = new FileRepositoryBuilder().setGitDir(folder).setMustExist(true).build()
        val head = repo.resolve("HEAD")
        if (head == null) then 0L
        else repo.parseCommit(head).getCommitTime.toLong * 1000
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
                    f.getName.stripSuffix(".git"),
                    getLastCommitTime(f)
                )
            )
            .sortBy(repo => -repo.lastModified)
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
