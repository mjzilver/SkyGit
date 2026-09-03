import java.io.File
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.revwalk.RevCommit
import scala.jdk.CollectionConverters.*

case class Commit(
  hash: String,
  author: String,
  message: String,
  timestamp: Int,
)

case class Stats(
  commits: List[Commit],
  linesByAuthor: Map[String, Int],
)

private def toCommit(commit: RevCommit): Commit =
  Commit(
    hash = commit.getName,
    author = commit.getAuthorIdent.getName,
    message = commit.getShortMessage,
    timestamp = commit.getCommitTime,
)

def main(args: Array[String]): Unit = {
  if args.length < 1 then
    println("Usage: skygit <repository>")
    return

  val repoPath = File(args(0)).getCanonicalFile
  val gitDir = File(repoPath, ".git")

  println(s"Repository: $repoPath")
  println(s"Git directory: $gitDir")

  val repo = new FileRepositoryBuilder()
    .setGitDir(gitDir)
    .setWorkTree(repoPath)
    .setMustExist(true)
    .build()

  try
    println(s"Repository: ${repo.getDirectory}")

    val git = new Git(repo)

    val commits = git
      .log()
      .call()
      .asScala
      .map(toCommit)
      .toList

    commits.foreach { commit =>
      println(s"${commit.hash.take(8)} ${commit.message}")
    }

  finally repo.close()
}
