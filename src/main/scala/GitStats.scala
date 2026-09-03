import org.eclipse.jgit.api.Git
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.api.errors.NoHeadException
import org.eclipse.jgit.lib.PersonIdent
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.treewalk.CanonicalTreeParser
import org.eclipse.jgit.treewalk.EmptyTreeIterator
import org.eclipse.jgit.util.io.DisabledOutputStream

import scala.jdk.CollectionConverters.*

case class LineStats(
    added: Int,
    deleted: Int
)

case class Commit(
    hash: String,
    author: PersonIdent,
    message: String,
    timestamp: Int,
    revCommit: RevCommit,
    loc: LineStats
)

case class AuthorStats(
    name: String,
    email: String,
    commits: Map[String, LineStats]
) {
    def netLines: Int =
        commits.values.map(stats => stats.added - stats.deleted).sum
    def displayName: String = s"$name <$email>"
}

case class Stats(
    repoName: String,
    commits: List[Commit],
    authors: Map[String, AuthorStats]
)

class GitStats(repo: Repository) {

    private val formatter =
        new DiffFormatter(DisabledOutputStream.INSTANCE)

    formatter.setRepository(repo)

    def loadCommits(): List[Commit] = {
        val git = new Git(repo)

        try
            git
                .log()
                .call()
                .asScala
                .map(toCommit)
                .toList
        catch case _: NoHeadException => List.empty
        finally git.close()
    }

    private def toCommit(commit: RevCommit): Commit = {
        Commit(
            hash = commit.getName,
            author = commit.getAuthorIdent,
            message = commit.getShortMessage,
            timestamp = commit.getCommitTime,
            revCommit = commit,
            loc = calcLoc(commit)
        )
    }

    def calcLoc(commit: RevCommit): LineStats = {
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
                finally reader.close()

        countEdits(diffs.asScala.toList)
    }

    private def countEdits(diffs: List[DiffEntry]): LineStats = {
        diffs.foldLeft(LineStats(0, 0)) { case (stats, diff) =>
            val header = formatter.toFileHeader(diff)

            header.toEditList.asScala.foldLeft(stats) { case (stats, edit) =>
                LineStats(
                    added = stats.added +
                        edit.getEndB - edit.getBeginB,
                    deleted = stats.deleted +
                        edit.getEndA - edit.getBeginA
                )
            }
        }
    }

    def calculateStats(
        repoName: String,
        commits: List[Commit]
    ): Stats = {
        Stats(
            repoName = repoName,
            commits = commits,
            authors = calculateAuthors(commits)
        )
    }

    private def calculateAuthors(
        commits: List[Commit]
    ): Map[String, AuthorStats] = {
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
    }
}
