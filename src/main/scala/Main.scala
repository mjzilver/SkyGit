import java.io.File

import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.transport.Daemon

def openRepository(repoPath: File): Repository = {
    val gitDir = File(repoPath, ".git")

    new FileRepositoryBuilder()
        .setGitDir(gitDir)
        .setWorkTree(repoPath)
        .setMustExist(true)
        .build()
}

def printStats(repoPath: File): Unit = {
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

def mirror(repoPath: File, destination: String): Unit = {
    val repo = openRepository(repoPath)

    try
        new GitMirror(repo, repoPath.getName).mirrorTo(destination)
    finally repo.close()
}

def startServer(baseDir: File, port: Int): Unit = {
    val server = new GitServer(baseDir, port)

    try {
        server.start()
        Thread.currentThread().join()
    } finally {
        server.close()
    }
}

def main(args: Array[String]): Unit = {
    args.toList match {
        case "mirror" :: repoArg :: destination :: Nil =>
            mirror(File(repoArg).getCanonicalFile, destination)

        case "server" :: baseDirArg :: Nil =>
            startServer(File(baseDirArg).getCanonicalFile, Daemon.DEFAULT_PORT)

        case "server" :: baseDirArg :: portArg :: Nil =>
            startServer(File(baseDirArg).getCanonicalFile, portArg.toInt)

        case repoArg :: Nil =>
            printStats(File(repoArg).getCanonicalFile)

        case _ =>
            println("Usage:")
            println("  skygit <repository>                      Print repository stats")
            println(
                "  skygit mirror <repository> <destination> Push repository to a mirror location"
            )
            println(
                "  skygit server <baseDir> [port]            Run a local git server storing repos in baseDir"
            )
    }
}
