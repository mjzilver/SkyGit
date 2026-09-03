import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.treewalk.CanonicalTreeParser
import org.eclipse.jgit.treewalk.EmptyTreeIterator

import scala.jdk.CollectionConverters.*

case class LineStats(
    added: Int,
    deleted: Int
)

case class Commit(
    hash: String,
    author: String,
    message: String,
    timestamp: Int,
    revCommit: RevCommit,
    loc: LineStats
)

case class Stats(
    repoName: String,
    commits: List[Commit],
    linesByAuthor: Map[String, Int]
)

class GitStats(repo: Repository) {

  private val formatter =
    new DiffFormatter(org.eclipse.jgit.util.io.DisabledOutputStream.INSTANCE)

  formatter.setRepository(repo)

  def loadCommits(): List[Commit] =
    val git = new org.eclipse.jgit.api.Git(repo)

    try
      git
        .log()
        .call()
        .asScala
        .map(toCommit)
        .toList
    finally
      git.close()

  private def toCommit(commit: RevCommit): Commit =
    Commit(
      hash = commit.getName,
      author = commit.getAuthorIdent.getName,
      message = commit.getShortMessage,
      timestamp = commit.getCommitTime,
      revCommit = commit,
      loc = calcLoc(commit)
    )

  def calcLoc(commit: RevCommit): LineStats =
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

  private def countEdits(diffs: List[DiffEntry]): LineStats =
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

  def calculateStats(repoName: String, commits: List[Commit]): Stats =
    Stats(
      repoName = repoName,
      commits = commits,
      linesByAuthor = calculateLinesByAuthor(commits)
    )

  private def calculateLinesByAuthor(
      commits: List[Commit]
  ): Map[String, Int] =
    commits
      .groupBy(_.author)
      .map { case (author, commits) =>
        val netLines = commits.map { commit =>
          commit.loc.added - commit.loc.deleted
        }.sum

        author -> netLines
      }
}
