<script>
import { onMount } from "svelte";
import { getRepos } from "../api.js";

let repos = $state([]);
let error = $state(null);

onMount(async () => {
	try {
		repos = await getRepos();
	} catch (e) {
		error = e.message;
	}
});
</script>

<h2>Repositories</h2>

{#if error}
  <p class="muted">Failed to load repositories: {error}</p>
{:else if repos.length === 0}
  <p class="muted">No repositories found.</p>
{:else}
  <ul class="list">
    {#each repos as repo}
      <li><a href="#/{repo}">{repo}</a></li>
    {/each}
  </ul>
{/if}
