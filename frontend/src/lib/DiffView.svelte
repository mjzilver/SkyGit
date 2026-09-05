<script>
import { html } from "diff2html";
import "diff2html/bundles/css/diff2html.min.css";

/** @type {{ diff: string }} */
let { diff } = $props();

/** @type {string} */
let renderedHtml = $derived(
	diff
		? html(diff, {
				drawFileList: false,
				matching: "lines",
				outputFormat: "line-by-line",
			})
		: "",
);
</script>

{#if diff}
	<div class="diff-view d2h-dark-color-scheme">
		{@html renderedHtml}
	</div>
{:else}
	<p class="muted">No changes.</p>
{/if}
