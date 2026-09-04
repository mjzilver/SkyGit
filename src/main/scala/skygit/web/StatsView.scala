package skygit.web

import upickle.default.*

import skygit.git.{Stats, StatsAnalysis}

case class LanguageStatsView(
    language: String,
    netLines: Int
) derives ReadWriter

case class AuthorStatsView(
    name: String,
    email: String,
    netLines: Int,
    commitCount: Int,
    percentage: Long
) derives ReadWriter

case class RepoStatsView(
    repoName: String,
    totalCommits: Int,
    totalAuthors: Int,
    added: Int,
    deleted: Int,
    net: Int,
    firstCommitDate: String,
    lastCommitDate: String,
    ageSeconds: Int,
    topLanguages: List[LanguageStatsView],
    topAuthors: List[AuthorStatsView]
) derives ReadWriter

object StatsView {

    def build(stats: Stats): RepoStatsView = {
        val topLanguages =
            StatsAnalysis
                .topLanguages(stats)
                .map { case (lang, net) => LanguageStatsView(lang, net) }

        val topAuthors =
            StatsAnalysis
                .topContributors(stats)
                .map { case (author, percentage) =>
                    AuthorStatsView(
                        name = author.name,
                        email = author.email,
                        netLines = author.netLines,
                        commitCount = author.commits.size,
                        percentage = percentage
                    )
                }

        RepoStatsView(
            repoName = stats.repoName,
            totalCommits = stats.commits.length,
            totalAuthors = stats.authors.size,
            added = stats.commits.map(_.loc.added).sum,
            deleted = stats.commits.map(_.loc.deleted).sum,
            net = StatsAnalysis.totalNetLines(stats),
            firstCommitDate = StatsAnalysis
                .firstCommit(stats)
                .map(commit => StatsAnalysis.formatTimestamp(commit.timestamp))
                .getOrElse("N/A"),
            lastCommitDate = StatsAnalysis
                .lastCommit(stats)
                .map(commit => StatsAnalysis.formatTimestamp(commit.timestamp))
                .getOrElse("N/A"),
            ageSeconds = StatsAnalysis.ageSeconds(stats).getOrElse(0),
            topLanguages = topLanguages,
            topAuthors = topAuthors
        )
    }
}
