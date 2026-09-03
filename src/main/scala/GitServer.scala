import java.io.File
import java.net.InetSocketAddress

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.errors.RepositoryNotFoundException
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.transport.Daemon
import org.eclipse.jgit.transport.DaemonClient
import org.eclipse.jgit.transport.resolver.RepositoryResolver

class GitServer(baseDir: File, port: Int = Daemon.DEFAULT_PORT) {

    private val daemon = new Daemon(new InetSocketAddress(port))

    daemon.setRepositoryResolver(resolver)
    daemon.getService("receive-pack").setEnabled(true)

    private def resolver: RepositoryResolver[DaemonClient] =
        (_, name) => openOrCreate(sanitize(name))

    private def sanitize(name: String): String = {
        val cleaned = name.stripPrefix("/").stripSuffix(".git")

        if cleaned.isEmpty || cleaned.contains("..") || cleaned.startsWith("/") then
            throw new RepositoryNotFoundException(name)

        cleaned
    }

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
            .build()
    }

    def start(): Unit = {
        baseDir.mkdirs()
        daemon.start()
        println(s"SkyGit server listening on git://localhost:${daemon.getAddress.getPort}/")
        println(s"Repositories stored in: ${baseDir.getAbsolutePath}")
    }

    def stop(): Unit = daemon.stop()
}
