import java.io.File

import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.transport.Daemon

def openRepository(repoPath: File): Option[Repository] = {
    try {
        val gitDir = File(repoPath, ".git")

        Some(
            new FileRepositoryBuilder()
                .setGitDir(gitDir)
                .setWorkTree(repoPath)
                .setMustExist(true)
                .build()
        )
    } catch {
        case _: Exception => None
    }
}

def printStats(repoPath: File): Unit = {
    openRepository(repoPath) match
        case None =>
            println(s"Could not open repository: ${repoPath.getAbsolutePath}")

        case Some(repo) =>
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
    openRepository(repoPath) match
        case None =>
            println(s"Could not open repository: ${repoPath.getAbsolutePath}")

        case Some(repo) =>
            try
                new GitMirror(repo, repoPath.getName).mirrorTo(destination)
            finally repo.close()
}

def startServer(baseDir: File, port: Int = Daemon.DEFAULT_PORT): Unit = {
    try {
        val server = new GitServer(baseDir, port)

        server.start()
        Thread.currentThread().join()
    } catch {
        case e: Exception => println(s"Failed to start server: ${e.getMessage}")
    }
}

def main(args: Array[String]): Unit = {
    args.toList match {
        case "mirror" :: repoArg :: destination :: Nil =>
            mirror(File(repoArg).getCanonicalFile, destination)

        case "server" :: baseDirArg :: Nil =>
            startServer(File(baseDirArg).getCanonicalFile)

        case "server" :: baseDirArg :: portArg :: Nil =>
            portArg.toIntOption match
                case Some(port) =>
                    startServer(File(baseDirArg).getCanonicalFile, port)
                case None =>
                    println(s"Invalid port: $portArg")

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
