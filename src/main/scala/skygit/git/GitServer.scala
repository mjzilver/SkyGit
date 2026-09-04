package skygit.git

import java.io.File
import java.net.InetSocketAddress

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.transport.Daemon
import org.eclipse.jgit.transport.DaemonClient
import org.eclipse.jgit.transport.resolver.RepositoryResolver

class GitServer(
    baseDir: File,
    port: Int = Daemon.DEFAULT_PORT
) extends AutoCloseable {

    private val daemon = new Daemon(new InetSocketAddress(port))

    def start(): Unit = {
        baseDir.mkdirs()

        daemon.setRepositoryResolver(repositoryResolver)
        daemon.getService("receive-pack").setEnabled(true)
        daemon.getService("upload-pack").setEnabled(true)

        daemon.start()

        println(
            s"SkyGit server listening on git://skygit.local:${daemon.getAddress.getPort}/"
        )
        println(s"Repositories stored in: ${baseDir.getAbsolutePath}")
    }

    def stop(): Unit = {
        daemon.stop()
    }

    override def close(): Unit =
        stop()

    private val repositoryResolver: RepositoryResolver[DaemonClient] =
        (_, name) => openOrCreate(GitPath.sanitize(name))

    private def openOrCreate(name: String): Repository = {
        val dir = new File(baseDir, s"$name.git")

        if !dir.exists() then
            Git.init()
                .setDirectory(dir)
                .setBare(true)
                .call()
                .close()

        new FileRepositoryBuilder()
            .setGitDir(dir)
            .setMustExist(true)
            .build()
    }
}
