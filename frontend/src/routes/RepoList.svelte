<script>
	import { onMount } from "svelte";
	import { getRepos } from "../api.js";
	import {
		preloadRepoStats,
		getStatsForRepo,
	} from "../lib/StatsCache.svelte.js";

	let repos = $state([]);
	let error = $state(null);

	let sortColumn = $state("name");
	let sortDirection = $state("asc");

	const columns = [
		{ key: "name", label: "Repository", source: "repo" },
		{ key: "totalCommits", label: "Commits", source: "stats" },
		{ key: "totalAuthors", label: "Authors", source: "stats" },
		{ key: "net", label: "Net lines", source: "stats" },
		{ key: "lastCommitDate", label: "Last commit", source: "stats" },
		{ key: "topLanguages", label: "Languages", source: "stats" },
	];

	onMount(async () => {
		try {
			repos = await getRepos();

			preloadRepoStats(repos);
		} catch (e) {
			error = e.message;
		}
	});

	function sortBy(column) {
		if (sortColumn === column) {
			sortDirection = sortDirection === "asc" ? "desc" : "asc";
		} else {
			sortColumn = column;
			sortDirection = "asc";
		}
	}

	function getSortValue(repo, column) {
		if (column.source === "repo") {
			return repo[column.key];
		}

		const stats = getStatsForRepo(repo.name);
		return stats?.[column.key];
	}

	function isNA(value) {
		return value == null || value === "N/A";
	}

	function compareValues(a, b) {
		const aNA = isNA(a);
		const bNA = isNA(b);

		if (aNA && bNA) return 0;
		if (aNA) return 1;
		if (bNA) return -1;

		if (typeof a === "string" && typeof b === "string") {
			return a.localeCompare(b);
		}

		return a - b;
	}

	let sortedRepos = $derived.by(() => {
		return [...repos].sort((a, b) => {
			const av = getSortValue(a, sortColumn);
			const bv = getSortValue(b, sortColumn);

			const result = compareValues(av, bv);

			return sortDirection === "asc" ? result : -result;
		});
	});
</script>

<h2>Repositories</h2>

{#if error}
	<p class="muted">Failed to load repositories: {error}</p>
{:else if repos.length === 0}
	<p class="muted">No repositories found.</p>
{:else}
	<div class="card table-container">
		<table>
			<thead>
				<tr>
					{#each columns as column}
						<th>
							<button onclick={() => sortBy(column.key)}>
								{column.label}
								{#if sortColumn === column.key}
									{sortDirection === "asc" ? " ↑" : " ↓"}
								{/if}
							</button>
						</th>
					{/each}
				</tr>
			</thead>

			<tbody>
				{#each sortedRepos as repo}
					{@const stats = getStatsForRepo(repo.name)}

					<tr>
						<td>
							<a href="#/{repo.name}">{repo.name}</a>
						</td>

						<td>
							{stats?.totalCommits ?? "N/A"}
						</td>

						<td>
							{stats?.totalAuthors ?? "N/A"}
						</td>

						<td>
							{stats?.net ?? "N/A"}
						</td>

						<td>
							{stats?.lastCommitDate ?? "N/A"}
						</td>

						<td>
							{#if stats?.topLanguages?.length}
								{stats.topLanguages
									.map((language) => language.language)
									.join(", ")}
							{:else}
								N/A
							{/if}
						</td>
					</tr>
				{/each}
			</tbody>
		</table>
	</div>
{/if}