const form = document.getElementById("searchForm");
const qEl = document.getElementById("q");
const partialEl = document.getElementById("partial");
const limitEl = document.getElementById("limit");
const statusEl = document.getElementById("status");
const resultsEl = document.getElementById("results");

async function runSearch(query, partial, limit) {
  const params = new URLSearchParams();
  params.set("q", query);
  params.set("partial", String(!!partial));
  if (Number.isFinite(limit) && limit > 0) {
    params.set("limit", String(limit));
  }

  const res = await fetch(`/api/search?${params.toString()}`, {
    headers: { Accept: "application/json" },
  });

  if (!res.ok) {
    const text = await res.text().catch(() => "");
    throw new Error(`HTTP ${res.status}: ${text || res.statusText}`);
  }

  return await res.json();
}

form.addEventListener("submit", async (e) => {
  e.preventDefault();

  const query = (qEl.value || "").trim();
  const partial = partialEl.checked;
  const limit = parseInt(limitEl.value, 10);

  resultsEl.textContent = "";
  statusEl.textContent = "Searching…";

  try {
    const data = await runSearch(query, partial, Number.isFinite(limit) ? limit : 0);
    statusEl.textContent = `Found ${data.results?.length ?? 0} result(s).`;
    resultsEl.textContent = JSON.stringify(data, null, 2);
  } catch (err) {
    statusEl.textContent = "Search failed.";
    resultsEl.textContent = String(err?.message || err);
  }
});

