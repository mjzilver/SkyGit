<script>
import hljs from "highlight.js";
import "highlight.js/styles/github-dark.css";
import { getTree, getBlob } from "../api.js";

let { params } = $props();

let repo = $derived(params.repo);
let hash = $derived(params.hash);
let path = $derived(params.wild ?? "");

let entries = $state([]);
let blob = $state(null);
let highlighted = $state("");
let error = $state(null);

$effect(() => {
	blob = null;
	getTree(repo, hash, path)
		.then((e) => (entries = e))
		.catch((err) => (error = err.message));
});

function openDir(entry) {
	path = entry.path;
	blob = null;
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
	path = parts.join("/");
	blob = null;
}
</script>

<div class="breadcrumbs">
  <a href="#/">Repositories</a> / <a href="#/{repo}">{repo}</a> / tree
</div>

<h2>Files</h2>

<p>
  Commit:
  <input bind:value={hash} placeholder="HEAD" />
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
