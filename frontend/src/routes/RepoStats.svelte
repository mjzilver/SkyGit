<script>
	import {
		getRepoStats,
		getStatsForRepo,
		isStatsLoading,
		getStatsError,
	} from "../lib/StatsCache.svelte.js";

	let { params } = $props();
	let repo = $derived(params.repo);

	$effect(() => {
		getRepoStats(repo).catch(() => {});
	});

	let stats = $derived(getStatsForRepo(repo));
	let loading = $derived(isStatsLoading(repo));
	let error = $derived(getStatsError(repo));
</script>
<div class="breadcrumbs"><a href="#/">Repositories</a> / {repo}</div>

<h2>{repo}</h2>

<p>
  <a href="#/{repo}/commits">Browse commits</a> ·
  <a href="#/{repo}/tree/HEAD">Browse files</a>
</p>

{#if error}
  <p class="muted">Failed to load stats: {error}</p>
{:else if loading}
  <p class="muted">Loading…</p>
{:else}
  <div class="stat-grid card">
    <div class="stat">
      <div class="value">{stats.totalCommits}</div>
      <div class="label">Commits</div>
    </div>
    {#if stats.branches.length > 1}
    <div class="stat">
      <div class="value">{stats.branches.length}</div>
      <div class="label">Branches</div>
    </div>
    {/if}
    <div class="stat">
      <div class="value">{stats.totalAuthors}</div>
      <div class="label">Authors</div>
    </div>
    <div class="stat">
      <div class="value">{stats.net}</div>
      <div class="label">Net lines</div>
    </div>
    <div class="stat">
      <div class="value file-added">+{stats.added}</div>
      <div class="label">Added</div>
    </div>
    <div class="stat">
      <div class="value file-removed">-{stats.deleted}</div>
      <div class="label">Deleted</div>
    </div>
  </div>

  <div class="card">
    <p class="muted">First commit: {stats.firstCommitDate}</p>
    <p class="muted">Last commit: {stats.lastCommitDate}</p>
  </div>

  <div class="card">
    <h3>Top languages</h3>
    <ul class="list">
      {#each stats.topLanguages as lang}
        <li>{lang.language} — {lang.netLines} lines</li>
      {/each}
    </ul>
  </div>

  <div class="card">
    <h3>Top contributors</h3>
    <ul class="list">
      {#each stats.topAuthors as author}
        <li>
          {author.name} &lt;{author.email}&gt; — {author.netLines} lines ({author.percentage}%)
        </li>
      {/each}
    </ul>
  </div>
{/if}
