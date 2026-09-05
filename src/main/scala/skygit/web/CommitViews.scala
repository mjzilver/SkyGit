package skygit.web

import skygit.git.Commit
import upickle.default.*

case class CommitSummaryDto(
    hash: String,
    author: String,
    email: String,
    message: String,
    timestamp: Int
) derives ReadWriter

case class CommitListView(
    commits: List[CommitSummaryDto],
    total: Int
) derives ReadWriter

case class FileChangeDto(
    path: String,
    language: Option[String],
    added: Int,
    deleted: Int
) derives ReadWriter

case class CommitDetailDto(
    hash: String,
    author: String,
    email: String,
    message: String,
    timestamp: Int,
    files: List[FileChangeDto]
) derives ReadWriter

object CommitViews {

    def summaryList(commits: List[Commit], total: Int): CommitListView =
        CommitListView(commits.map(summary), total)

    def summary(commit: Commit): CommitSummaryDto =
        CommitSummaryDto(
            hash = commit.hash,
            author = commit.author.getName,
            email = commit.author.getEmailAddress,
            message = commit.message,
            timestamp = commit.timestamp
        )

    def detail(commit: Commit): CommitDetailDto =
        CommitDetailDto(
            hash = commit.hash,
            author = commit.author.getName,
            email = commit.author.getEmailAddress,
            message = commit.message,
            timestamp = commit.timestamp,
            files = commit.files.map(file =>
                FileChangeDto(
                    path = file.path,
                    language = file.language,
                    added = file.stats.added,
                    deleted = file.stats.deleted
                )
            )
        )
}
