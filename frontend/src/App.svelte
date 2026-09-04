<script>
import Router from "svelte-spa-router";
import RepoList from "./routes/RepoList.svelte";
import RepoStats from "./routes/RepoStats.svelte";
import CommitList from "./routes/CommitList.svelte";
import CommitDetail from "./routes/CommitDetail.svelte";
import FileBrowser from "./routes/FileBrowser.svelte";

const routes = {
	"/": RepoList,
	"/:repo": RepoStats,
	"/:repo/commits": CommitList,
	"/:repo/commit/:hash": CommitDetail,
	"/:repo/tree/:hash": FileBrowser,
	"/:repo/tree/:hash/*": FileBrowser,
};
</script>

<header class="card">
  <h1><a href="#/">SkyGit</a></h1>
</header>

<Router {routes} />
