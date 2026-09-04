<script>
import { getCommit, getDiff } from "../api.js";
import DiffView from "../lib/DiffView.svelte";

let { params } = $props();
let repo = $derived(params.repo);
let hash = $derived(params.hash);

let commit = $state(null);
let diff = $state("");
let selectedPath = $state(null);
let error = $state(null);

$effect(() => {
	getCommit(repo, hash)
		.then((c) => (commit = c))
		.catch((e) => (error = e.message));
});

$effect(() => {
	getDiff(repo, hash, selectedPath)
		.then((d) => (diff = d))
		.catch((e) => (error = e.message));
});

function formatDate(timestamp) {
	return new Date(timestamp * 1000).toLocaleString();
}
</script>

<div class="breadcrumbs">
  <a href="#/">Repositories</a> / <a href="#/{repo}">{repo}</a> /
  <a href="#/{repo}/commits">commits</a> / {hash.substring(0, 8)}
</div>

{#if error}
  <p class="muted">Failed to load commit: {error}</p>
{:else if !commit}
  <p class="muted">Loading…</p>
{:else}
  <div class="card">
    <h2>{commit.message}</h2>
    <p class="muted">
      {commit.author} &lt;{commit.email}&gt; — {formatDate(commit.timestamp)}
    </p>
    <p class="muted">{commit.hash}</p>
  </div>

  <div class="card">
    <h3>Changed files</h3>
    <ul class="list">
      <li>
        <button
          onclick={() => (selectedPath = null)}
          disabled={selectedPath === null}
        >
          All files
        </button>
      </li>
      {#each commit.files as file}
        <li>
          <button
            onclick={() => (selectedPath = file.path)}
            disabled={selectedPath === file.path}
          >
            {file.path}
          </button>
          <span class="file-added">+{file.added}</span>
          <span class="file-removed">-{file.deleted}</span>
        </li>
      {/each}
    </ul>
  </div>

  <div class="card">
    <DiffView {diff} />
  </div>
{/if}
