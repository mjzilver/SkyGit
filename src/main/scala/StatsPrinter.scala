class StatsPrinter {

  def print(stats: Stats): Unit =
    val header = s"Repository: ${stats.repoName}"

    println(header)
    println("-" * header.length)
    println()

    printStatLine(
      "Total commits",
      stats.commits.length.toString
    )

    printStatLine(
      "Total lines",
      stats.linesByAuthor.values.sum.toString
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
      stats.linesByAuthor.size.toString
    )

    println()

    printCommitDates(stats)

    println()
    printTopContributors(stats)

  private def printCommitDates(stats: Stats): Unit =
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

  private def printTopContributors(stats: Stats): Unit =
    println("Top contributors:")

    stats.linesByAuthor.toList
      .sortBy(-_._2)
      .take(3)
      .foreach { case (author, lines) =>
        printStatLine(author, lines.toString)
      }

  private def printStatLine(
      key: String,
      value: String
  ): Unit =
    println(f"$key%-20s: $value")

  private def timestampToDate(timestamp: Int): String =
    val date = new java.util.Date(timestamp.toLong * 1000)
    val format = new java.text.SimpleDateFormat(
      "yyyy-MM-dd HH:mm:ss"
    )

    format.format(date)

  private def timestampToDuration(seconds: Int): String =
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    s"${days}d ${hours % 24}h ${minutes % 60}m ${seconds % 60}s"
}
