package skygit.git

import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.treewalk.TreeWalk
import org.eclipse.jgit.treewalk.filter.PathFilter
import upickle.default.*
import scala.collection.mutable.ListBuffer

case class TreeEntry(
    name: String,
    path: String,
    isDir: Boolean
) derives ReadWriter

class FileBrowserService(repo: Repository) {

    def listTree(commit: RevCommit, path: String): List[TreeEntry] = {
        val reader = repo.newObjectReader()

        try {
            val walk = new TreeWalk(repo, reader)
            walk.addTree(commit.getTree)
            walk.setRecursive(false)

            if path.nonEmpty then
                walk.setFilter(PathFilter.create(path))

                var enteredSubtree = false
                while !enteredSubtree && walk.next() do
                    if walk.isSubtree then
                        val current = walk.getPathString
                        if current == path then
                            walk.enterSubtree()
                            enteredSubtree = true
                        else if path.startsWith(current + "/") then walk.enterSubtree()

                if !enteredSubtree then return List.empty

            val entries = ListBuffer.empty[TreeEntry]
            while walk.next() do
                entries += TreeEntry(
                    name = walk.getNameString,
                    path = walk.getPathString,
                    isDir = walk.isSubtree
                )

            entries.toList.sortBy(entry => (!entry.isDir, entry.name.toLowerCase))
        } finally reader.close()
    }

    def readBlob(commit: RevCommit, path: String): Option[String] = {
        val walk = TreeWalk.forPath(repo, path, commit.getTree)

        Option(walk).filterNot(_.isSubtree).map { w =>
            try new String(repo.open(w.getObjectId(0)).getBytes, "UTF-8")
            finally w.close()
        }
    }
}
