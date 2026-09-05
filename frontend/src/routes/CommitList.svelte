<script>
/** @typedef {import("../api.js").CommitSummaryDto} CommitSummaryDto */

import { getCommits } from "../api.js";

let { params } = $props();
let repo = $derived(params.repo);
const pageSize = 20;

let page = $state(0);

/** @type {CommitSummaryDto[]} */
let commits = $state([]);
let total = $state(0);
let error = $state(null);

$effect(() => {
	getCommits(repo, page, pageSize)
		.then((result) => {
			commits = result.commits;
			total = result.total;
		})
		.catch((e) => (error = e.message));
});

/**
 * @param {number} timestamp
 */
function formatDate(timestamp) {
	return new Date(timestamp * 1000).toLocaleString();
}
</script>

<div class="breadcrumbs">
  <a href="#/">Repositories</a> / <a href="#/{repo}">{repo}</a> / commits
</div>

<h2>Commits</h2>

{#if error}
  <p class="muted">Failed to load commits: {error}</p>
{:else}
  <table>
    <thead>
      <tr>
        <th>When</th>
        <th>Author</th>
        <th>Message</th>
      </tr>
    </thead>
    <tbody>
      {#each commits as commit}
        <tr>
          <td class="muted">{formatDate(commit.timestamp)}</td>
          <td>{commit.author}</td>
          <td><a href="#/{repo}/commit/{commit.hash}">{commit.message}</a></td>
        </tr>
      {/each}
    </tbody>
  </table>

  <p>
    <button disabled={page === 0} onclick={() => (page -= 1)}>Previous</button>
    Page {page + 1} of {Math.max(1, Math.ceil(total / pageSize))}
    <button
      disabled={(page + 1) * pageSize >= total}
      onclick={() => (page += 1)}>Next</button
    >
  </p>
{/if}
