package skygit.git

import java.io.ByteArrayOutputStream
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.{RevCommit, RevWalk}
import org.eclipse.jgit.treewalk.filter.PathFilter
import org.eclipse.jgit.treewalk.{CanonicalTreeParser, EmptyTreeIterator}

class DiffService(repo: Repository) {

    def commitDiff(commit: RevCommit): String =
        format(commit, pathFilter = None)

    def fileDiff(commit: RevCommit, path: String): String =
        format(commit, pathFilter = Some(path))

    private def format(commit: RevCommit, pathFilter: Option[String]): String = {
        val out = new ByteArrayOutputStream()
        val formatter = new DiffFormatter(out)

        formatter.setRepository(repo)
        pathFilter.foreach(path => formatter.setPathFilter(PathFilter.create(path)))

        try
            if commit.getParentCount > 0 then
                val walk = new RevWalk(repo)

                try
                    val parent = walk.parseCommit(commit.getParent(0).getId)
                    formatter.format(parent.getTree, commit.getTree)
                finally walk.close()
            else
                val reader = repo.newObjectReader()

                try
                    val tree = new CanonicalTreeParser()
                    tree.reset(reader, commit.getTree)

                    formatter.format(new EmptyTreeIterator(), tree)
                finally reader.close()
        finally formatter.close()

        out.toString("UTF-8")
    }
}
