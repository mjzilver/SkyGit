package skygit.git

import org.eclipse.jgit.errors.RepositoryNotFoundException

object GitPath {

    def sanitize(name: String): String = {
        val cleaned = name.stripPrefix("/").stripSuffix(".git")

        if cleaned.isEmpty || cleaned.contains("..") || cleaned.startsWith("/") then
            throw new RepositoryNotFoundException(name)

        cleaned
    }
}
