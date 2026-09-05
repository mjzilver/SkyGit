import { getStats } from "../api.js";

let statsByRepo = $state({});
let loadingByRepo = $state({});
let errorByRepo = $state({});

const pending = new Map();

export function getRepoStats(repo) {
	if (statsByRepo[repo]) {
		return Promise.resolve(statsByRepo[repo]);
	}

	if (pending.has(repo)) {
		return pending.get(repo);
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

export function preloadRepoStats(repos) {
	for (const repo of repos) {
		getRepoStats(repo.name).catch((error) => {
			console.error(`Failed to preload stats for repo: ${repo.name}`, error);
		});
	}
}

export function getStatsForRepo(repo) {
	return statsByRepo[repo] ?? null;
}

export function isStatsLoading(repo) {
	return loadingByRepo[repo] ?? false;
}

export function getStatsError(repo) {
	return errorByRepo[repo] ?? null;
}
