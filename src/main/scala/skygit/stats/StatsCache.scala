package skygit.stats

import org.eclipse.jgit.lib.Repository
import scala.collection.concurrent.TrieMap
import skygit.config.LanguageConfig
import skygit.git.{GitStats, Stats}
import skygit.git.GitUtils

class StatsCache(languageConfig: LanguageConfig) {

    private val cache = TrieMap.empty[String, (String, Stats)]

    def getOrCompute(repoName: String, repo: Repository): Stats = {
        val headSha = GitUtils.resolveHead(repo).map(_.getName).getOrElse("") 

        cache.get(repoName) match
            case Some((sha, stats)) if sha == headSha =>
                stats

            case _ =>
                val gitStats = new GitStats(repo, languageConfig)
                val commits = gitStats.loadCommits()
                val stats = gitStats.buildStats(repoName, commits)

                cache.update(repoName, (headSha, stats))
                stats
    }
}
