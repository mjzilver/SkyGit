import { getStats } from "../api.js";

/** @typedef {import("../api.js").RepoStatsView} RepoStatsView */
/** @typedef {import("../api.js").RepositoryInfo} RepositoryInfo */

/** @type {Record<string, RepoStatsView>} */
let statsByRepo = $state({});

/** @type {Record<string, boolean>} */
let loadingByRepo = $state({});

/** @type {Record<string, string>} */
let errorByRepo = $state({});

/** @type {Map<string, Promise<RepoStatsView>>} */
const pending = new Map();

/**
 * @param {string} repo
 * @returns {Promise<RepoStatsView>}
 */
export function getRepoStats(repo) {
	if (statsByRepo[repo]) {
		return Promise.resolve(statsByRepo[repo]);
	}

	const existing = pending.get(repo);

	if (existing) {
		return existing;
	}

	loadingByRepo[repo] = true;
	delete errorByRepo[repo];

	const promise = getStats(repo)
		.then((stats) => {
			statsByRepo[repo] = stats;
			return stats;
		})
		.catch((error) => {
			errorByRepo[repo] = error.message;
			throw error;
		})
		.finally(() => {
			loadingByRepo[repo] = false;
			pending.delete(repo);
		});

	pending.set(repo, promise);

	return promise;
}

/**
 * @param {RepositoryInfo[]} repos
 * @returns {void}
 */
export function preloadRepoStats(repos) {
	for (const repo of repos) {
		getRepoStats(repo.name).catch((error) => {
			console.error(`Failed to preload stats for repo: ${repo.name}`, error);
		});
	}
}

/**
 * @param {string} repo
 * @returns {RepoStatsView | null}
 */
export function getStatsForRepo(repo) {
	return statsByRepo[repo] ?? null;
}

/**
 * @param {string} repo
 * @returns {boolean}
 */
export function isStatsLoading(repo) {
	return loadingByRepo[repo] ?? false;
}

/**
 * @param {string} repo
 * @returns {string | null}
 */
export function getStatsError(repo) {
	return errorByRepo[repo] ?? null;
}
