<script>
  import hljs from "highlight.js";
  import "highlight.js/styles/github-dark.css";
  import { push } from "svelte-spa-router";
  import { getTree, getBlob, getStats } from "../api.js";

  let { params } = $props();

  let repo = $derived(params.repo);
  let hash = $derived(params.hash);
  let path = $derived(params.wild ?? "");

  let branches = $state([]);
  let headHash = $state("");
  let entries = $state([]);
  let blob = $state(null);
  let highlighted = $state("");
  let error = $state(null);

  $effect(() => {
    blob = null;
    error = null;
    getTree(repo, hash, path)
      .then((e) => (entries = e))
      .catch((err) => (error = err.message));
  });

  $effect(() => {
    getStats(repo)
      .then((stats) => {
        branches = stats.branches || [];
        headHash = stats.headHash || "";
      })
      .catch((err) => (error = err.message));
  });

  let currentHash = $derived.by(() => {
    if (!hash || hash.toUpperCase() === "HEAD") return headHash;
    const matchByName = branches.find((b) => b.name === hash);
    if (matchByName) return matchByName.hash;
    return hash;
  });

  let selectedBranchHash = $derived(
    branches.find((b) => b.hash === currentHash)?.hash ?? "",
  );

  function onBranchChange(event) {
    const newBranchHash = event.target.value;
    if (newBranchHash && newBranchHash !== hash) {
      push(`/${repo}/tree/${newBranchHash}`);
    }
  }

  function onCommitChange(event) {
    const newHash = event.target.value.trim();
    if (newHash && newHash !== hash) {
      push(`/${repo}/tree/${newHash}`);
    }
  }

  function onCommitKeyDown(event) {
    if (event.key === "Enter") {
      onCommitChange(event);
    }
  }

  function openDir(entry) {
    push(`/${repo}/tree/${hash}/${entry.path}`);
  }

  function openFile(entry) {
    getBlob(repo, hash, entry.path)
      .then((content) => {
        blob = { path: entry.path, content };
        highlighted = hljs.highlightAuto(content).value;
      })
      .catch((err) => (error = err.message));
  }

  function goUp() {
    const parts = path.split("/").filter(Boolean);
    parts.pop();
    const parentPath = parts.join("/");
    push(`/${repo}/tree/${hash}${parentPath ? `/${parentPath}` : ""}`);
  }
</script>

<div class="breadcrumbs">
  <a href="#/">Repositories</a> / <a href="#/{repo}">{repo}</a> / tree
</div>

<h2>Files</h2>

<p>
  Branch:
  <select
    class="muted"
    value={selectedBranchHash}
    onchange={onBranchChange}
    aria-label="Branch"
  >
    {#if !selectedBranchHash}
      <option value="" disabled selected>Select branch</option>
    {/if}
    {#each branches as branch}
      <option value={branch.hash}>{branch.name}</option>
    {/each}
  </select>

  Commit:
  <input
    value={hash}
    onkeydown={onCommitKeyDown}
    onchange={onCommitChange}
    placeholder="HEAD"
  />
  <span class="muted">path: /{path}</span>
</p>

{#if error}
  <p class="muted">{error}</p>
{/if}

<div class="card">
  {#if path}
    <button onclick={goUp}>.. (up)</button>
  {/if}
  <ul class="list">
    {#each entries as entry}
      <li>
        {#if entry.isDir}
          <button onclick={() => openDir(entry)}>📁 {entry.name}</button>
        {:else}
          <button onclick={() => openFile(entry)}>📄 {entry.name}</button>
        {/if}
      </li>
    {/each}
  </ul>
</div>

{#if blob}
  <div class="card">
    <h3>{blob.path}</h3>
    <pre class="blob"><code>{@html highlighted}</code></pre>
  </div>
{/if}
