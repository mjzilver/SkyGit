package skygit.git

import java.util
import java.util.ArrayList
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.diff.{DiffEntry, DiffFormatter}
import org.eclipse.jgit.lib.{Constants, ObjectId, PersonIdent, Repository}
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.treewalk.{CanonicalTreeParser, EmptyTreeIterator}
import org.eclipse.jgit.util.io.DisabledOutputStream
import scala.jdk.CollectionConverters.*
import skygit.config.LanguageConfig

case class LineStats(
    added: Int,
    deleted: Int
) {
    def net: Int = added - deleted
}

case class FileChange(
    path: String,
    language: Option[String],
    stats: LineStats
)

case class Commit(
    hash: String,
    author: PersonIdent,
    message: String,
    timestamp: Int,
    revCommit: RevCommit,
    files: List[FileChange]
) {
    def loc: LineStats =
        files
            .map(_.stats)
            .foldLeft(LineStats(0, 0)) { (a, b) =>
                LineStats(
                    a.added + b.added,
                    a.deleted + b.deleted
                )
            }
}

case class AuthorStats(
    name: String,
    email: String,
    commits: Map[String, LineStats]
) {
    def netLines: Int =
        commits.values.map(stats => stats.added - stats.deleted).sum

    def displayName: String =
        s"$name <$email>"
}

case class BranchStats(
    name: String,
    hash: String
)

case class Stats(
    repoName: String,
    commits: List[Commit],
    authors: Map[String, AuthorStats],
    branches: List[BranchStats],
    headHash: String
)

class GitStats(
    repo: Repository,
    languageConfig: LanguageConfig
) {

    def resolveHead(): Option[ObjectId] =
        Option(repo.resolve("HEAD"))
            .orElse(Option(repo.resolve("refs/heads/main")))
            .orElse(Option(repo.resolve("refs/heads/master")))

    def loadCommits(): List[Commit] = {
        val git = new Git(repo)

        try
            resolveHead() match
                case Some(head) =>
                    git
                        .log()
                        .add(head)
                        .call()
                        .asScala
                        .map(toCommit)
                        .toList

                case None =>
                    println(s"[GitStats] No usable HEAD/main/master found for ${repo.getDirectory}")
                    List.empty

        catch
            case e: Exception =>
                println(
                    s"[GitStats] Failed to load commits for ${repo.getDirectory}: " +
                        s"${e.getClass.getSimpleName}: ${e.getMessage}"
                )
                e.printStackTrace()
                List.empty
        finally git.close()
    }

    private def toCommit(commit: RevCommit): Commit =
        Commit(
            hash = commit.getName,
            author = commit.getAuthorIdent,
            message = commit.getShortMessage,
            timestamp = commit.getCommitTime,
            revCommit = commit,
            files = calculateFileChanges(commit)
        )

    private def createFormatter(): DiffFormatter = {
        val formatter =
            new DiffFormatter(DisabledOutputStream.INSTANCE)

        formatter.setRepository(repo)
        formatter
    }

    private def calculateFileChanges(commit: RevCommit): List[FileChange] = {
        val formatter = createFormatter()
        try
            val diffs =
                if commit.getParentCount > 0 then
                    formatter.scan(
                        commit.getParent(0).getTree,
                        commit.getTree
                    )
                else
                    val reader = repo.newObjectReader()

                    try
                        val tree = new CanonicalTreeParser()
                        tree.reset(reader, commit.getTree)

                        formatter.scan(
                            new EmptyTreeIterator(),
                            tree
                        )
                    catch
                        case e: Exception =>
                            println(
                                s"[GitStats] Failed to calculate diffs for commit ${commit.getName}: ${e.getMessage}"
                            )
                            new util.ArrayList()
                    finally reader.close()

            diffs.asScala.toList.filterNot(diff => isIgnoredFile(filePath(diff))).map { diff =>
                FileChange(
                    path = filePath(diff),
                    language = detectLanguage(filePath(diff)),
                    stats = calculateLineStats(diff)
                )
            }
        finally formatter.close()
    }

    private def filePath(diff: DiffEntry): String =
        diff.getChangeType match
            case DiffEntry.ChangeType.DELETE =>
                diff.getOldPath

            case _ =>
                diff.getNewPath

    private def detectLanguage(
        filePath: String
    ): Option[String] = {

        val filename =
            filePath.split('/').last

        languageConfig.filenames
            .find(_.filenames.contains(filename))
            .map(_.name)
            .orElse {
                val extension =
                    filename
                        .lastIndexOf('.') match
                        case -1    => None
                        case index => Some(filename.substring(index + 1).toLowerCase)

                extension.flatMap { ext =>
                    languageConfig.languages
                        .find(_.extensions.exists(_.equalsIgnoreCase(ext)))
                        .map(_.name)
                }
            }
    }

    private val ignoredStatsFiles = Set(
        "package-lock.json",
        "yarn.lock",
        "pnpm-lock.yaml",
        "bun.lockb",
        "Cargo.lock",
        "composer.lock",
        "packages.lock.json",
        "project.assets.json",
        "go.sum"
    )

    private val ignoredStatsDirectories = Set(
        "vendor",
        "third_party",
        "third-party",
        "external"
    )

    private def isIgnoredFile(path: String): Boolean = {
        val parts = path.split('/')

        ignoredStatsFiles.contains(parts.last) ||
        parts.exists(ignoredStatsDirectories.contains)
    }

    private def calculateLineStats(diff: DiffEntry): LineStats = {
        val header = createFormatter().toFileHeader(diff)

        header.toEditList.asScala.foldLeft(LineStats(0, 0)) { case (stats, edit) =>
            LineStats(
                added = stats.added + edit.getEndB - edit.getBeginB,
                deleted = stats.deleted + edit.getEndA - edit.getBeginA
            )
        }
    }

    def buildStats(
        repoName: String,
        commits: List[Commit]
    ): Stats =
        Stats(
            repoName = repoName,
            commits = commits,
            authors = calculateAuthorStats(commits),
            branches = getBranchStats,
            headHash = getHeadCommitHash
        )

    private def getHeadCommitHash: String =
        Option(repo.resolve(Constants.HEAD)).map(_.getName).getOrElse("")

    private def calculateAuthorStats(
        commits: List[Commit]
    ): Map[String, AuthorStats] =
        commits
            .groupBy(_.author.getEmailAddress)
            .map { case (email, authorCommits) =>
                val author = authorCommits.head.author

                val commitStats =
                    authorCommits
                        .map(commit => commit.hash -> commit.loc)
                        .toMap

                email -> AuthorStats(
                    name = author.getName,
                    email = email,
                    commits = commitStats
                )
            }

    private def getBranchStats: List[BranchStats] =
        repo.getRefDatabase
            .getRefsByPrefix(Constants.R_HEADS)
            .asScala
            .map(ref => BranchStats(Repository.shortenRefName(ref.getName), ref.getObjectId.name()))
            .toList
}

object StatsAnalysis {

    def totalNetLines(stats: Stats): Int =
        stats.authors.values.map(_.netLines).sum

    def topLanguages(stats: Stats, limit: Int = 5): List[(String, Int)] =
        stats.commits
            .flatMap(_.files)
            .flatMap(file => file.language.map(_ -> file.stats.net))
            .groupBy(_._1)
            .view
            .mapValues(_.map(_._2).sum)
            .toList
            .sortBy(-_._2)
            .take(limit)

    def topContributors(stats: Stats, limit: Int = 5): List[(AuthorStats, Long)] = {
        val total = totalNetLines(stats)

        stats.authors.values.toList
            .sortBy(-_.netLines)
            .take(limit)
            .map { author =>
                val percentage =
                    if total > 0 then (author.netLines.toDouble / total * 100).round
                    else 0L

                author -> percentage
            }
    }

    def firstCommit(stats: Stats): Option[Commit] = stats.commits.lastOption

    def lastCommit(stats: Stats): Option[Commit] = stats.commits.headOption

    def ageSeconds(stats: Stats): Option[Int] =
        for
            first <- firstCommit(stats)
            last <- lastCommit(stats)
        yield last.timestamp - first.timestamp

    def formatTimestamp(timestamp: Int): String = {
        val date = new java.sql.Date(timestamp.toLong * 1000)
        val format = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

        format.format(date)
    }

    def formatDuration(seconds: Int): String = {
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24
        val year = days / 365

        val sb = new StringBuilder
        if (year > 0) sb.append(s"${year}y ")
        if (days % 365 > 0) sb.append(s"${days % 365}d ")
        if (hours % 24 > 0) sb.append(s"${hours % 24}h ")
        if (minutes % 60 > 0) sb.append(s"${minutes % 60}m ")
        sb.append(s"${seconds % 60}s ")

        sb.toString().trim
    }
}
