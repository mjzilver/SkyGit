package skygit.git

import java.io.File
import java.net.URI
import java.util.concurrent.Executors
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.transport.PushResult
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.*
import scala.jdk.CollectionConverters.*
import ExecutionContext.Implicits.global

object GitMirror {

    def openRepository(repoPath: File): Option[Repository] =
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

    def mirror(destination: String, args: String*): Unit =
        try
            val futures = args.map { repoPath =>
                Future {
                    mirrorRepo(new File(repoPath).getCanonicalFile, destination)
                }
            }

            Await.result(Future.sequence(futures), Duration.Inf)
        catch
            case e: Exception =>
                println(s"Error mirroring repositories: ${e.getMessage}")

    def mirrorRepo(repoPath: File, destination: String): Unit =
        openRepository(repoPath) match
            case None =>
                println(s"Could not open repository: ${repoPath.getAbsolutePath}")

            case Some(repo) =>
                try
                    new GitMirror(repo, repoPath.getName).mirrorTo(destination)
                finally repo.close()
}

class GitMirror(repo: Repository, repoName: String) {

    private def mirrorTo(destination: String): Unit = {
        val target = resolveDestination(destination)
        val git = Git.wrap(repo)

        try
            git
                .push()
                .setRemote(target)
                .setPushAll()
                .setPushTags()
                .call()
                .asScala
                .foreach(printPushResult)
        finally
            git.close()
    }

    private def resolveDestination(destination: String): String =
        if destination.contains("://") then
            if hasExplicitPath(destination) then destination
            else s"${destination.stripSuffix("/")}/$repoName"
        else
            val dir = new File(destination).getCanonicalFile
            val target =
                if isBareRepo(dir) || dir.getName.endsWith(".git") then dir
                else new File(dir, s"$repoName.git")

            if !isBareRepo(target) then
                Git.init()
                    .setDirectory(target)
                    .setBare(true)
                    .call()
                    .close()

            target.getAbsolutePath

    private def hasExplicitPath(destination: String): Boolean = {
        val path = Option(new URI(destination).getPath).getOrElse("")
        path.nonEmpty && path != "/"
    }

    private def isBareRepo(dir: File): Boolean = new File(dir, "HEAD").exists()

    private def printPushResult(result: PushResult): Unit =
        result.getRemoteUpdates.asScala.foreach { update =>
            println(
                s"$repoName mirrored: ${update.getSrcRef} -> ${update.getRemoteName}: ${update.getStatus}"
            )
        }
}
