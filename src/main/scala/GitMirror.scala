import java.io.File
import java.net.URI

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.transport.PushResult

import scala.jdk.CollectionConverters.*

class GitMirror(repo: Repository, repoName: String) {

    def mirrorTo(destination: String): Unit = {
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

    private def resolveDestination(destination: String): String = {
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
    }

    private def hasExplicitPath(destination: String): Boolean = {
        val path = Option(new URI(destination).getPath).getOrElse("")
        path.nonEmpty && path != "/"
    }

    private def isBareRepo(dir: File): Boolean = new File(dir, "HEAD").exists()

    private def printPushResult(result: PushResult): Unit = {
        result.getRemoteUpdates.asScala.foreach { update =>
            println(s"${update.getSrcRef} -> ${update.getRemoteName}: ${update.getStatus}")
        }
    }
}
