import java.io.File

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.transport.PushResult

import scala.jdk.CollectionConverters.*

/** Pushes a repository's branches and tags to a mirror destination, creating it if needed. */
class GitMirror(repo: Repository) {

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
        if destination.contains("://") then destination
        else
            val dir = new File(destination).getCanonicalFile

            if !new File(dir, "HEAD").exists() then
                Git.init()
                    .setDirectory(dir)
                    .setBare(true)
                    .call()
                    .close()

            dir.getAbsolutePath
    }

    private def printPushResult(result: PushResult): Unit = {
        result.getRemoteUpdates.asScala.foreach { update =>
            println(s"${update.getSrcRef} -> ${update.getRemoteName}: ${update.getStatus}")
        }
    }
}
