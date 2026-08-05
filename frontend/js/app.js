/* Library Management System — shared page behaviour (static prototype).
   In the Spring Boot build, search and filtering become server-side
   queries; this exists so the prototype feels alive in a browser. */

// ---------- Catalog: live search + category chips ----------
const grid = document.getElementById("grid");
if (grid) {
  const search = document.getElementById("search");
  const chips = document.querySelectorAll("#chips .chip");
  const noResults = document.getElementById("no-results");
  const cards = Array.from(grid.querySelectorAll(".book-card"));
  let activeCat = "all";

  function applyFilters() {
    const q = (search.value || "").trim().toLowerCase();
    let visible = 0;
    for (const card of cards) {
      const matchesCat = activeCat === "all" || card.dataset.cat === activeCat;
      const matchesText = !q || card.dataset.search.includes(q);
      const show = matchesCat && matchesText;
      card.style.display = show ? "" : "none";
      if (show) visible++;
    }
    noResults.style.display = visible === 0 ? "" : "none";
  }

  search.addEventListener("input", applyFilters);
  chips.forEach((chip) =>
    chip.addEventListener("click", () => {
      chips.forEach((c) => c.classList.remove("active"));
      chip.classList.add("active");
      activeCat = chip.dataset.cat;
      applyFilters();
    })
  );
}

// ---------- Account: tab switching ----------
const tabs = document.querySelectorAll(".tabs .tab");
if (tabs.length) {
  tabs.forEach((tab) =>
    tab.addEventListener("click", () => {
      tabs.forEach((t) => t.classList.remove("active"));
      document.querySelectorAll(".tabpane").forEach((p) => p.classList.remove("active"));
      tab.classList.add("active");
      document.getElementById("pane-" + tab.dataset.pane).classList.add("active");
    })
  );
}

// ---------- Book detail: join-waitlist demo ----------
const joinBtn = document.getElementById("join-waitlist");
if (joinBtn) {
  joinBtn.addEventListener("click", () => {
    joinBtn.textContent = "You're in line — position 5";
    joinBtn.classList.add("disabled");
    joinBtn.setAttribute("disabled", "disabled");
  });
}
