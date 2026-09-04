const base = "/api";

async function getJson(path) {
	const res = await fetch(`${base}${path}`);
	if (!res.ok) throw new Error(`${res.status} ${res.statusText}`);
	return res.json();
}

async function getText(path) {
	const res = await fetch(`${base}${path}`);
	if (!res.ok) throw new Error(`${res.status} ${res.statusText}`);
	return res.text();
}

export function getRepos() {
	return getJson("/repos");
}

export function getStats(repo) {
	return getJson(`/repos/${encodeURIComponent(repo)}/stats`);
}

export function getCommits(repo, page = 0, pageSize = 20) {
	return getJson(
		`/repos/${encodeURIComponent(repo)}/commits?page=${page}&pageSize=${pageSize}`,
	);
}

export function getCommit(repo, hash) {
	return getJson(
		`/repos/${encodeURIComponent(repo)}/commits/${encodeURIComponent(hash)}`,
	);
}

export function getDiff(repo, hash, path) {
	const query = path ? `?path=${encodeURIComponent(path)}` : "";
	return getText(
		`/repos/${encodeURIComponent(repo)}/commits/${encodeURIComponent(hash)}/diff${query}`,
	);
}

export function getTree(repo, hash, path = "") {
	const query = path ? `?path=${encodeURIComponent(path)}` : "";
	return getJson(
		`/repos/${encodeURIComponent(repo)}/tree/${encodeURIComponent(hash)}${query}`,
	);
}

export function getBlob(repo, hash, path) {
	return getText(
		`/repos/${encodeURIComponent(repo)}/blob/${encodeURIComponent(hash)}?path=${encodeURIComponent(path)}`,
	);
}
