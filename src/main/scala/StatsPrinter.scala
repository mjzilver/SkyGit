import java.text.SimpleDateFormat
import java.sql.Date
class StatsPrinter {

    def print(stats: Stats): Unit = {
        val header = s"Repository: ${stats.repoName}"

        println("-" * (header.length + 2))
        println(header)
        println("-" * (header.length + 2))
        println()

        printStatLine(
            "Total commits",
            stats.commits.length.toString
        )

        printStatLine(
            "Total lines",
            stats.authors.values.map(_.netLines).sum.toString
        )

        printStatLine(
            "Total added lines",
            stats.commits.map(_.loc.added).sum.toString
        )

        printStatLine(
            "Total deleted lines",
            stats.commits.map(_.loc.deleted).sum.toString
        )

        printStatLine(
            "Total authors",
            stats.authors.size.toString
        )

        printStatLine(
            "Top languages (lines)",
            calculateTopLanguages(stats)
        )

        println()

        printCommitDates(stats)

        println()
        printTopContributors(stats)
    }

    private def calculateTopLanguages(stats: Stats): String = {
        stats.commits
            .flatMap(_.files)
            .flatMap(file => file.language.map(_ -> file.stats.net))
            .groupBy(_._1)
            .view
            .mapValues(_.map(_._2).sum)
            .toList
            .sortBy(-_._2)
            .take(3)
            .map { case (lang, net) => s"$lang ($net)" }
            .mkString(", ")
    }

    private def printCommitDates(stats: Stats): Unit = {
        val firstCommit = stats.commits.lastOption
        val lastCommit = stats.commits.headOption

        val age =
            for
                first <- firstCommit
                last <- lastCommit
            yield last.timestamp - first.timestamp

        printStatLine(
            "First commit",
            firstCommit
                .map(commit => timestampToDate(commit.timestamp))
                .getOrElse("N/A")
        )

        printStatLine(
            "Last commit",
            lastCommit
                .map(commit => timestampToDate(commit.timestamp))
                .getOrElse("N/A")
        )

        printStatLine(
            "Age",
            age
                .map(timestampToDuration)
                .getOrElse("N/A")
        )
    }

    private def printTopContributors(stats: Stats): Unit = {
        println("Top contributors:")

        val total = stats.authors.values.map(_.netLines).sum

        stats.authors.toList
            .sortBy(-_._2.netLines)
            .take(5)
            .foreach { case (email, authorStats) =>
                val percentage =
                    if (total > 0) (authorStats.netLines.toDouble / total * 100).round
                    else 0
                printStatLine(
                    authorStats.displayName,
                    s"${authorStats.netLines} ($percentage%)"
                )
            }
    }

    private def printStatLine(
        key: String,
        value: String
    ): Unit = {
        println(f"$key%-30s: $value")
    }

    private def timestampToDate(timestamp: Int): String = {
        val date = new Date(timestamp.toLong * 1000)
        val format = new SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss"
        )

        format.format(date)
    }

    private def timestampToDuration(seconds: Int): String = {
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

        sb.toString()
    }
}
