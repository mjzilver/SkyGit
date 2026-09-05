/**
 * @typedef {Object} RepositoryInfo
 * @property {string} name
 */

/**
 * @typedef {Object} BranchStatsView
 * @property {string} name
 * @property {string} hash
 */

/**
 * @typedef {Object} LanguageStatsView
 * @property {string} language
 * @property {number} netLines
 */

/**
 * @typedef {Object} AuthorStatsView
 * @property {string} name
 * @property {string} email
 * @property {number} netLines
 * @property {number} commitCount
 * @property {number} percentage
 */

/**
 * @typedef {Object} RepoStatsView
 * @property {string} repoName
 * @property {number} totalCommits
 * @property {number} totalAuthors
 * @property {number} added
 * @property {number} deleted
 * @property {number} net
 * @property {string} firstCommitDate
 * @property {string} lastCommitDate
 * @property {number} ageSeconds
 * @property {LanguageStatsView[]} topLanguages
 * @property {AuthorStatsView[]} topAuthors
 * @property {BranchStatsView[]} branches
 * @property {string} headHash
 */

/**
 * @typedef {Object} CommitSummaryDto
 * @property {string} hash
 * @property {string} author
 * @property {string} email
 * @property {string} message
 * @property {number} timestamp
 */

/**
 * @typedef {Object} CommitListView
 * @property {CommitSummaryDto[]} commits
 * @property {number} total
 */

/**
 * @typedef {Object} FileChangeDto
 * @property {string} path
 * @property {string | null} language
 * @property {number} added
 * @property {number} deleted
 */

/**
 * @typedef {Object} CommitDetailDto
 * @property {string} hash
 * @property {string} author
 * @property {string} email
 * @property {string} message
 * @property {number} timestamp
 * @property {FileChangeDto[]} files
 */

/**
 * @typedef {Object} TreeEntry
 * @property {string} name
 * @property {string} path
 * @property {boolean} isDir
 */

const base = "/api";

/**
 * @template T
 * @param {string} path
 * @returns {Promise<T>}
 */
async function getJson(path) {
	const res = await fetch(`${base}${path}`);
	if (!res.ok) throw new Error(`${res.status} ${res.statusText}`);
	return res.json();
}

/**
 * @param {string} path
 * @returns {Promise<string>}
 */
async function getText(path) {
	const res = await fetch(`${base}${path}`);
	if (!res.ok) throw new Error(`${res.status} ${res.statusText}`);
	return res.text();
}

/** @returns {Promise<RepositoryInfo[]>} */
export function getRepos() {
	return getJson("/repos");
}

/**
 * @param {string} repo
 * @returns {Promise<RepoStatsView>}
 */
export function getStats(repo) {
	return getJson(`/repos/${encodeURIComponent(repo)}/stats`);
}

/**
 * @param {string} repo
 * @param {number} [page=0]
 * @param {number} [pageSize=20]
 * @returns {Promise<CommitListView>}
 */
export function getCommits(repo, page = 0, pageSize = 20) {
	return getJson(
		`/repos/${encodeURIComponent(repo)}/commits?page=${page}&pageSize=${pageSize}`,
	);
}

/**
 * @param {string} repo
 * @param {string} hash
 * @returns {Promise<CommitDetailDto>}
 */
export function getCommit(repo, hash) {
	return getJson(
		`/repos/${encodeURIComponent(repo)}/commits/${encodeURIComponent(hash)}`,
	);
}

/**
 * @param {string} repo
 * @param {string} hash
 * @param {string} [path]
 * @returns {Promise<string>}
 */
export function getDiff(repo, hash, path) {
	const query = path ? `?path=${encodeURIComponent(path)}` : "";
	return getText(
		`/repos/${encodeURIComponent(repo)}/commits/${encodeURIComponent(hash)}/diff${query}`,
	);
}

/**
 * @param {string} repo
 * @param {string} hash
 * @param {string} [path=""]
 * @returns {Promise<TreeEntry[]>}
 */
export function getTree(repo, hash, path = "") {
	const query = path ? `?path=${encodeURIComponent(path)}` : "";
	return getJson(
		`/repos/${encodeURIComponent(repo)}/tree/${encodeURIComponent(hash)}${query}`,
	);
}

/**
 * @param {string} repo
 * @param {string} hash
 * @param {string} path
 * @returns {Promise<string>}
 */
export function getBlob(repo, hash, path) {
	return getText(
		`/repos/${encodeURIComponent(repo)}/blob/${encodeURIComponent(hash)}?path=${encodeURIComponent(path)}`,
	);
}
