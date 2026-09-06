package skygit.git

import org.eclipse.jgit.errors.RepositoryNotFoundException
import org.eclipse.jgit.lib.{ObjectId, Repository}

object GitUtils {

    def sanitize(name: String): String = {
        val cleaned = name.stripPrefix("/").stripSuffix(".git")

        if cleaned.isEmpty || cleaned.contains("..") || cleaned.startsWith("/") then
            throw new RepositoryNotFoundException(name)

        cleaned
    }

    def resolveHead(repo: Repository): Option[ObjectId] =
        Option(repo.resolve("HEAD"))
            .orElse(Option(repo.resolve("refs/heads/main")))
            .orElse(Option(repo.resolve("refs/heads/master")))
}
