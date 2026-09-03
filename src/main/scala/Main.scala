import java.io.File

import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder

def openRepository(repoPath: File): Repository = {
    val gitDir = File(repoPath, ".git")

    new FileRepositoryBuilder()
        .setGitDir(gitDir)
        .setWorkTree(repoPath)
        .setMustExist(true)
        .build()
}

def main(args: Array[String]): Unit = {
    if args.length < 1 then
        println("Usage: skygit <repository>")
        return

    val repoPath = File(args(0)).getCanonicalFile
    val repo = openRepository(repoPath)

    try
        val gitStats = new GitStats(repo)
        val printer = new StatsPrinter()

        val commits = gitStats.loadCommits()
        val stats = gitStats.calculateStats(
            repoName = repoPath.getName,
            commits = commits
        )

        printer.print(stats)

    finally repo.close()
}
