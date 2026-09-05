package skygit.web

import cask.*
import upickle.default.*
import java.nio.file.{Files, Paths}

import java.io.File
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevWalk

import skygit.config.LanguageConfig
import skygit.git.{DiffService, FileBrowserService, RepoRegistry}
import skygit.stats.StatsCache

class WebServer(
    baseDir: File,
    serverPort: Int,
    languageConfig: LanguageConfig,
    pipelineService: PipelineService,
) extends cask.MainRoutes
    with AutoCloseable {

    private val statsCache = new StatsCache(languageConfig)

    override def host: String = "0.0.0.0"
    override def port: Int = serverPort

    private case class AbortException(
        code: Int,
        message: String
    ) extends Exception(message)

    private def abortable(
        body: => String
    ): cask.Response[String] =
        try cask.Response(body)
        catch
            case AbortException(code, message) =>
                cask.Response(message, statusCode = code)

    @cask.get("/")
    def index(): cask.Response.Raw =
        serveStaticFile("frontend/dist/index.html")

    @cask.staticFiles("/assets")
    def frontendAssets(): String =
        new File("frontend/dist/assets").getAbsolutePath

    private def serveStaticFile(path: String): cask.Response.Raw = {
        val contentType = Option(Files.probeContentType(Paths.get(path)))
        cask.StaticFile(path, contentType.map("Content-Type" -> _).toSeq)
    }

    @cask.get("/api/repos")
    def repos(): String =
        write(RepoRegistry.list(baseDir))

    @cask.get("/api/repos/:repoName/stats")
    def stats(
        repoName: String
    ): cask.Response[String] =
        abortable {
            withRepo(repoName) { repo =>
                write(
                    StatsView.build(
                        statsCache.getOrCompute(repoName, repo)
                    )
                )
            }
        }

    @cask.get("/api/repos/:repoName/commits")
    def commits(
        repoName: String,
        page: Int = 0,
        pageSize: Int = 20
    ): cask.Response[String] =
        abortable {
            withRepo(repoName) { repo =>
                val stats =
                    statsCache.getOrCompute(repoName, repo)

                val slice =
                    stats.commits.slice(
                        page * pageSize,
                        page * pageSize + pageSize
                    )

                write(
                    CommitViews.summaryList(
                        slice,
                        stats.commits.length
                    )
                )
            }
        }

    @cask.get("/api/repos/:repoName/commits/:hash")
    def commit(
        repoName: String,
        hash: String
    ): cask.Response[String] =
        abortable {
            withRepo(repoName) { repo =>
                statsCache
                    .getOrCompute(repoName, repo)
                    .commits
                    .find(_.hash == hash) match

                    case Some(commit) =>
                        write(
                            CommitViews.detail(commit)
                        )

                    case None =>
                        throw AbortException(
                            404,
                            "Commit not found"
                        )
            }
        }

    @cask.get("/api/repos/:repoName/commits/:hash/diff")
    def diff(
        repoName: String,
        hash: String,
        path: Option[String] = None
    ): cask.Response[String] =
        abortable {
            withRevCommit(repoName, hash) { (repo, revCommit) =>
                val diffService =
                    new DiffService(repo)

                path match
                    case Some(filePath) =>
                        diffService.fileDiff(
                            revCommit,
                            filePath
                        )

                    case None =>
                        diffService.commitDiff(
                            revCommit
                        )
            }
        }

    @cask.get("/api/repos/:repoName/tree/:hash")
    def tree(
        repoName: String,
        hash: String,
        path: String = ""
    ): cask.Response[String] =
        abortable {
            withRevCommit(repoName, hash) { (repo, revCommit) =>
                write(
                    new FileBrowserService(repo)
                        .listTree(
                            revCommit,
                            path
                        )
                )
            }
        }

    @cask.get("/api/repos/:repoName/blob/:hash")
    def blob(
        repoName: String,
        hash: String,
        path: Option[String] = None
    ): cask.Response[String] =
        abortable {
            path match
                case None =>
                    throw AbortException(
                        400,
                        "Missing path parameter"
                    )

                case Some(filePath) =>
                    withRevCommit(repoName, hash) { (repo, revCommit) =>
                        new FileBrowserService(repo)
                            .readBlob(
                                revCommit,
                                filePath
                            ) match

                            case Some(content) =>
                                content

                            case None =>
                                throw AbortException(
                                    404,
                                    "File not found"
                                )
                    }
        }

    @cask.post("/api/pipeline")
    def pipelines(): cask.Response[String] =
        abortable {
            write(
                pipelineService.startPipeline()
            )
        }


    @cask.get("/api/pipeline/{id}/logs")
    def pipelineLogs(id: String): cask.Response[String] =
        abortable {
            write(
                pipelineService.getPipelineLogs(id)
            )
        }


    @cask.post("/api/pipeline/{id}/cancel")
    def cancelPipeline(id: String): cask.Response[String] =
        abortable {
            write(
                pipelineService.cancelPipeline(id)
            )
        }

    private def withRepo[T](
        repoName: String
    )(
        f: Repository => T
    ): T =
        RepoRegistry.resolve(baseDir, repoName) match

            case Some(repo) =>
                try
                    f(repo)
                finally
                    repo.close()

            case None =>
                throw AbortException(
                    404,
                    "Repository not found"
                )

    private def withRevCommit[T](
        repoName: String,
        hash: String
    )(
        f: (Repository, RevCommit) => T
    ): T =
        withRepo(repoName) { repo =>
            resolveCommit(repo, hash) match

                case Some(revCommit) =>
                    f(repo, revCommit)

                case None =>
                    throw AbortException(
                        404,
                        "Commit not found"
                    )
        }

    private def resolveCommit(
        repo: Repository,
        hash: String
    ): Option[RevCommit] =
        try
            val walk = new RevWalk(repo)

            try
                Option(repo.resolve(hash))
                    .map(walk.parseCommit)
            finally
                walk.close()

        catch
            case _: Exception =>
                None

    def start(): Unit =
        initialize()
        main(Array.empty)
        println(s"SkyGit web server bound to http://$host:$port/")

    def stop(): Unit =
        ()

    override def close(): Unit =
        stop()
}
