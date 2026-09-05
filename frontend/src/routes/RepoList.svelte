<script>
import { onMount } from "svelte";
import { getRepos } from "../api.js";
import { preloadRepoStats, getStatsForRepo } from "../lib/StatsCache.svelte.js";

// type definition for Repo imported from api.js
/** @typedef {import("../api.js").RepositoryInfo} RepositoryInfo */

/** @typedef {{
 *   key: string,
 *   label: string,
 *   source: "repo" | "stats"
 * }} Column */

/** @type {RepositoryInfo[]} */
let repos = $state([]);
/** @type {string | null} */
let error = $state(null);
/** @type {string} */
let sortColumn = $state("name");
/** @type {"asc" | "desc"} */
let sortDirection = $state("asc");

/** @type {Column[]} */
const columns = [
	{ key: "name", label: "Repository", source: "repo" },
	{ key: "totalCommits", label: "Commits", source: "stats" },
	{ key: "totalAuthors", label: "Authors", source: "stats" },
	{ key: "net", label: "Net lines", source: "stats" },
	{ key: "lastCommitDate", label: "Last commit", source: "stats" },
];

onMount(async () => {
	try {
		repos = await getRepos();
		preloadRepoStats(repos);
	} catch (e) {
		error = e instanceof Error ? e.message : String(e);
	}
});

/**  @param {string} column */
function sortBy(column) {
	if (sortColumn === column) {
		sortDirection = sortDirection === "asc" ? "desc" : "asc";
	} else {
		sortColumn = column;
		sortDirection = "asc";
	}
}

/**
 * @param {RepositoryInfo} repo
 * @param {Column} column
 * @returns {string | number | null}
 */
function getSortValue(repo, column) {
	if (column.source === "repo") {
		return repo.name;
	}

	const stats = getStatsForRepo(repo.name);

	if (!stats) {
		return null;
	}

	switch (column.key) {
		case "totalCommits":
			return stats.totalCommits;
		case "totalAuthors":
			return stats.totalAuthors;
		case "net":
			return stats.net;
		case "lastCommitDate":
			return stats.lastCommitDate;
	}
	return null;
}

/**
 * @param {string | number | null} value
 * @returns {boolean}
 */
function isNA(value) {
	return value == null || value === "N/A";
}

/**
 * @param {string | number | null} a
 * @param {string | number | null} b
 * @returns {number}
 */
function compareValues(a, b) {
	const aNA = isNA(a);
	const bNA = isNA(b);

	// N/A always goes last.
	if (aNA && bNA) return 0;
	if (aNA) return 1;
	if (bNA) return -1;

	if (typeof a === "string" && typeof b === "string") {
		return a.localeCompare(b, undefined, {
			numeric: true,
			sensitivity: "base",
		});
	}

	if (typeof a === "number" && typeof b === "number") {
		return a - b;
	}

	return String(a).localeCompare(String(b), undefined, {
		numeric: true,
		sensitivity: "base",
	});
}

let sortedRepos = $derived.by(() => {
	const column = columns.find((column) => column.key === sortColumn);

	if (!column) {
		return repos;
	}

	return [...repos].sort((a, b) => {
		const av = getSortValue(a, column);
		const bv = getSortValue(b, column);

		const result = compareValues(av, bv);

		if (isNA(av) || isNA(bv)) {
			return result;
		}

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

					<th>Languages</th>
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
		              .slice(0, 3)
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