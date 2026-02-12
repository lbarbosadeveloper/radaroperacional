// public/app.js

// ============================
// ✅ Logo clicável: recarrega
// ============================
document.addEventListener("DOMContentLoaded", () => {
  const logo = document.getElementById("lamsaLogo");
  if (!logo) return;

  logo.style.cursor = "pointer";
  logo.addEventListener("click", () => window.location.reload());
  logo.addEventListener("keydown", (e) => {
    if (e.key === "Enter" || e.key === " ") {
      e.preventDefault();
      window.location.reload();
    }
  });
});

// ============================
// ✅ KEYWORDS EDITÁVEL (com persistência)
// ============================
const DEFAULT_KEYWORDS = ["Lamsa", "Avenida Brasil", "Trânsito Rio de Janeiro", "Cet Rio Lamsa"];
const KW_STORAGE_KEY = "radar_keywords_v1";

// ============================
// ✅ LIMITES / JANELA
// ============================
const MAX_RESULTS_PER_KEYWORD = 2;

// total fica dinâmico: keywords.length * MAX_RESULTS_PER_KEYWORD (com teto)
const MAX_TOTAL_ITEMS_CAP = 60;

// últimas 48h (se quiser mais “vida”, sobe pra 72/168)
const MAX_AGE_HOURS = 48;

// ============================
// Sites permitidos (vai pro backend /search)
// ============================
const SITE_FILTER = ["g1.globo.com", "oglobo.globo.com", "diariodorio.com", "r7.com"];

// ============================
// ✅ BLOCKLIST
// ============================
const BLOCKLIST_HOSTS = ["wikipedia.org", "lamsa.com.br"];
const BLOCKLIST_PATH_CONTAINS = [];

// ============================
// ✅ Fallbacks (só quando vier 0 resultados)
// - Isso ajuda quando “LAMSA” / “CET” não aparecem literal nas manchetes.
// ============================
const KW_FALLBACKS = {
  Lamsa: ["LAMSA", "Linha Amarela", "concessionária Linha Amarela"],
  "Cet Rio Lamsa": ["CET-Rio Linha Amarela", "CET Rio Linha Amarela", "CET-Rio"],
  "Trânsito Rio de Janeiro": ["trânsito RJ", "CET-Rio trânsito", "trânsito Linha Amarela", "engarrafamento RJ"],
  "Avenida Brasil": ["Avenida Brasil trânsito", "acidente Avenida Brasil", "engarrafamento Avenida Brasil"],
};

// ============================
// ✅ API_BASE (local vs GitHub Pages)
// ============================
const PROD_API = "https://radaroperacional-api.onrender.com"; // <- TROQUE AQUI

const API_BASE =
  location.hostname === "localhost" || location.hostname === "127.0.0.1" ? "http://localhost:3000" : PROD_API;

// ============================
// ✅ GOOGLE MAPS (Dark) — apenas front
// ============================
const MAPS = {
  enabled: true,
  elementId: "map",
  center: { lat: -22.8749, lng: -43.3096 },
  zoom: 14,
};

let __gmapsLoaded = false;
let __gmapsLoading = null;
let __mapInstance = null;

function getGoogleMapsKey() {
  const meta = document.querySelector('meta[name="google-maps-key"]');
  const fromMeta = meta?.getAttribute("content")?.trim();
  if (fromMeta) return fromMeta;

  const fromGlobal = String(window.GOOGLE_MAPS_KEY || "").trim();
  if (fromGlobal) return fromGlobal;

  return "";
}

function loadGoogleMapsScript(key) {
  if (__gmapsLoaded) return Promise.resolve();
  if (__gmapsLoading) return __gmapsLoading;

  __gmapsLoading = new Promise((resolve, reject) => {
    const s = document.createElement("script");
    s.async = true;
    s.defer = true;

    window.__initMapCallback = () => {
      __gmapsLoaded = true;
      resolve();
    };

    s.src = `https://maps.googleapis.com/maps/api/js?key=${encodeURIComponent(key)}&callback=__initMapCallback`;
    s.onerror = () => reject(new Error("Falha ao carregar Google Maps API."));
    document.head.appendChild(s);
  });

  return __gmapsLoading;
}

function getDarkMapStyle() {
  return [
    { elementType: "geometry", stylers: [{ color: "#0b1220" }] },
    { elementType: "labels.icon", stylers: [{ visibility: "off" }] },
    { elementType: "labels.text.fill", stylers: [{ color: "#b9d2ea" }] },
    { elementType: "labels.text.stroke", stylers: [{ color: "#0b1220" }] },
    { featureType: "administrative", elementType: "geometry", stylers: [{ color: "#22324c" }] },
    { featureType: "poi", elementType: "geometry", stylers: [{ color: "#0e1a2f" }] },
    { featureType: "poi", elementType: "labels.text.fill", stylers: [{ color: "#89a8c6" }] },
    { featureType: "road", elementType: "geometry", stylers: [{ color: "#18253a" }] },
    { featureType: "road", elementType: "geometry.stroke", stylers: [{ color: "#0b1220" }] },
    { featureType: "road", elementType: "labels.text.fill", stylers: [{ color: "#9fb3c8" }] },
    { featureType: "transit", elementType: "geometry", stylers: [{ color: "#132136" }] },
    { featureType: "water", elementType: "geometry", stylers: [{ color: "#07101a" }] },
    { featureType: "water", elementType: "labels.text.fill", stylers: [{ color: "#7aa5c9" }] },
  ];
}

async function initGoogleMapIfPossible() {
  if (!MAPS.enabled) return;

  const el = document.getElementById(MAPS.elementId);
  if (!el) return;

  const key = getGoogleMapsKey();
  if (!key) {
    console.warn("[Maps] Sem Google Maps API key.");
    if (!el.dataset.mapPlaceholder) {
      el.dataset.mapPlaceholder = "1";
      el.innerHTML = `<div style="
        width:100%;height:100%;
        display:flex;align-items:center;justify-content:center;
        font: 500 12px system-ui, -apple-system, Segoe UI, Roboto, Arial, sans-serif;
        color: rgba(235,245,255,.65);
        background: rgba(10,16,26,.55);
        border: 1px solid rgba(170,220,255,.12);
        border-radius: 16px;
      ">Google Maps: falta configurar API KEY</div>`;
    }
    return;
  }

  await loadGoogleMapsScript(key);

  if (!__mapInstance) {
    // eslint-disable-next-line no-undef
    __mapInstance = new google.maps.Map(el, {
      center: MAPS.center,
      zoom: MAPS.zoom,
      styles: getDarkMapStyle(),
      disableDefaultUI: true,
      zoomControl: true,
      gestureHandling: "greedy",
      clickableIcons: false,
    });

    // eslint-disable-next-line no-undef
    new google.maps.Marker({
      position: MAPS.center,
      map: __mapInstance,
      title: "Centro",
    });
  }

  setTimeout(() => {
    try {
      // eslint-disable-next-line no-undef
      google.maps.event.trigger(__mapInstance, "resize");
      __mapInstance.setCenter(MAPS.center);
    } catch {}
  }, 150);
}

// ============================
// Utils
// ============================
function sleep(ms) {
  return new Promise((r) => setTimeout(r, ms));
}

function escapeHtml(str) {
  return String(str ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function todayISO() {
  return new Date().toLocaleDateString("en-CA", { timeZone: "America/Sao_Paulo" });
}

function getHost(url) {
  try {
    return new URL(url).hostname.replace(/^www\./, "").toLowerCase();
  } catch {
    try {
      return new URL("https://" + url).hostname.replace(/^www\./, "").toLowerCase();
    } catch {
      return "";
    }
  }
}

function normalizeUrl(url) {
  if (!url) return "";
  let u = String(url).trim();
  if (!u) return "";
  if (!/^https?:\/\//i.test(u)) u = "https://" + u;

  try {
    const o = new URL(u);
    o.hostname = o.hostname.replace(/^www\./, "").toLowerCase();
    ["utm_source", "utm_medium", "utm_campaign", "utm_term", "utm_content", "gclid", "fbclid", "igshid"].forEach((p) =>
      o.searchParams.delete(p)
    );
    o.hash = "";
    return o.toString();
  } catch {
    return "";
  }
}

function isBlockedUrl(url) {
  const host = getHost(url);
  const full = String(url || "").toLowerCase();

  const hostBlocked = BLOCKLIST_HOSTS.some((b) => {
    const bb = String(b).replace(/^www\./, "").toLowerCase();
    return host === bb || host.endsWith("." + bb);
  });
  if (hostBlocked) return true;

  return BLOCKLIST_PATH_CONTAINS.some((part) => full.includes(String(part).toLowerCase()));
}

function getItemDateObj(r) {
  const raw = r?.publishedAt || r?.published_at || r?.date || r?.datetime || r?.time || null;
  if (!raw) return null;
  const d = new Date(raw);
  if (Number.isNaN(d.getTime())) return null;
  return d;
}

function withinHours(item, maxHours) {
  const d = getItemDateObj(item);
  if (!d) return true;
  const diffH = (Date.now() - d.getTime()) / (1000 * 60 * 60);
  return diffH >= 0 && diffH <= maxHours;
}

function isLiveItem(item, hours = 3) {
  if (item?.isLive === true) return true;
  const d = getItemDateObj(item);
  if (!d) return false;
  const diffH = (Date.now() - d.getTime()) / (1000 * 60 * 60);
  return diffH >= 0 && diffH <= hours;
}

// ✅ DEDUPE “REAL”: por URL (ou título+fonte), SEM keyword.
// Assim, a mesma notícia pode carregar várias keywords no MESMO card.
function makeDedupeKey(item) {
  const url = normalizeUrl(item?.url || "");
  const title = String(item?.title || "").trim().toLowerCase();
  const source = String(item?.source || "").trim().toLowerCase();
  return url ? `U:${url}` : `TS:${source}__${title}`;
}

// ============================
// ✅ KEYWORDS helpers
// ============================
function normalizeKw(s) {
  return String(s || "").trim().replace(/\s+/g, " ");
}

function parseMany(raw) {
  return String(raw || "")
    .split(/[,;\n]+/g)
    .map(normalizeKw)
    .filter(Boolean);
}

function loadKeywords() {
  try {
    const saved = localStorage.getItem(KW_STORAGE_KEY);
    if (!saved) return [...DEFAULT_KEYWORDS];
    const arr = JSON.parse(saved);
    if (!Array.isArray(arr)) return [...DEFAULT_KEYWORDS];
    const clean = arr.map(normalizeKw).filter(Boolean);
    return clean.length ? clean : [...DEFAULT_KEYWORDS];
  } catch {
    return [...DEFAULT_KEYWORDS];
  }
}

function saveKeywords(list) {
  localStorage.setItem(KW_STORAGE_KEY, JSON.stringify(list));
}

function uniquePush(list, item) {
  const lower = item.toLowerCase();
  if (list.some((k) => k.toLowerCase() === lower)) return false;
  list.push(item);
  return true;
}

function cleanSnippetFront(s) {
  return String(s || "")
    .replace(/<[^>]*>/g, " ")
    .replace(/https?:\/\/\S+/gi, " ")
    .replace(/\s+/g, " ")
    .trim();
}

function sourceToDomain(sourceText = "") {
  const s = String(sourceText || "").toLowerCase();
  if (s.includes("g1")) return "g1.globo.com";
  if (s.includes("o globo") || s.includes("oglobo")) return "oglobo.globo.com";
  if (s.includes("diário do rio") || s.includes("diariodorio")) return "diariodorio.com";
  if (s.includes("r7")) return "r7.com";
  return "";
}

function faviconFromDomain(domain, sourceText, fallbackUrl) {
  const d =
    String(domain || "").trim() || sourceToDomain(sourceText) || getHost(fallbackUrl) || "news.google.com";
  return `https://www.google.com/s2/favicons?domain=${encodeURIComponent(d)}&sz=64`;
}

// ============================
// Estado
// ============================
let todayItems = [];
let isScanning = false;
let pendingRescan = false;

let keywords = loadKeywords();
let kwStates = new Map();

// ============================
// Marquee infinito (rAF)
// ============================
let marqueeRAF = null;

function stopMarquee() {
  if (marqueeRAF) cancelAnimationFrame(marqueeRAF);
  marqueeRAF = null;
}

function setupInfiniteMarquee({ speedPxPerSec = 55, minCards = 24 } = {}) {
  const carousel = document.getElementById("carousel");
  const track = document.getElementById("results");
  const viewport = carousel?.querySelector(".car-viewport");
  if (!carousel || !track || !viewport) return;

  carousel.classList.add("is-marquee");
  stopMarquee();
  track.style.transform = "translate3d(0,0,0)";
  track.dataset.marqueeReady = "0";

  const baseCards = Array.from(track.querySelectorAll(".news-card"));
  if (baseCards.length === 0) return;

  const fragFill = document.createDocumentFragment();
  let currentCount = baseCards.length;
  while (currentCount < minCards) {
    for (const c of baseCards) {
      if (currentCount >= minCards) break;
      fragFill.appendChild(c.cloneNode(true));
      currentCount++;
    }
  }
  track.appendChild(fragFill);

  const nowCards = Array.from(track.querySelectorAll(".news-card"));
  const fragDup = document.createDocumentFragment();
  nowCards.forEach((c) => fragDup.appendChild(c.cloneNode(true)));
  track.appendChild(fragDup);

  let offset = 0;
  let last = performance.now();
  let paused = false;

  function halfWidth() {
    return track.scrollWidth / 2;
  }

  function tick(now) {
    const dt = (now - last) / 1000;
    last = now;

    if (!paused) {
      offset -= speedPxPerSec * dt;
      const half = halfWidth();
      if (half > 0 && Math.abs(offset) >= half) offset += half;
      track.style.transform = `translate3d(${offset}px,0,0)`;
    }
    marqueeRAF = requestAnimationFrame(tick);
  }

  marqueeRAF = requestAnimationFrame(tick);
  track.dataset.marqueeReady = "1";

  if (!viewport.dataset.hoverPauseBound) {
    viewport.addEventListener("mouseenter", () => {
      paused = true;
      carousel.classList.add("user-paused");
    });
    viewport.addEventListener("mouseleave", () => {
      paused = false;
      last = performance.now();
      carousel.classList.remove("user-paused");
    });
    viewport.dataset.hoverPauseBound = "1";
  }
}

// ============================
// App
// ============================
document.addEventListener("DOMContentLoaded", () => {
  const els = {
    kwInput: document.getElementById("kwInput"),
    kwAdd: document.getElementById("kwAdd"),
    kwClear: document.getElementById("kwClear"),
    kwReset: document.getElementById("kwReset"),
    kwChips: document.getElementById("kwChips"),
    kwCount: document.getElementById("kwCount"),
    kwCopy: document.getElementById("kwCopy"),

    resultsMeta: document.getElementById("resultsMeta"),
    refreshBtn: document.getElementById("refreshBtn"),

    results: document.getElementById("results"),
    statusText: document.getElementById("statusText"),
    clock: document.getElementById("clock"),

    wTemp: document.getElementById("wTemp"),
    wWind: document.getElementById("wWind"),
    wHum: document.getElementById("wHum"),
    wFeels: document.getElementById("wFeels"),
    wPlace: document.getElementById("wPlace"),
    wUpdated: document.getElementById("wUpdated"),
  };

  if (!els.results || !els.kwChips) {
    console.error("IDs obrigatórios não encontrados no HTML (kwChips/results).");
    return;
  }

  // total dinâmico: garante “justiça” quando você adiciona keywords
  function maxTodayItemsNow() {
    const dynamic = keywords.length * MAX_RESULTS_PER_KEYWORD;
    return Math.min(MAX_TOTAL_ITEMS_CAP, Math.max(20, dynamic));
  }

  function setStatus(txt) {
    if (els.statusText) els.statusText.textContent = txt;
    const dot = document.querySelector(".dot");
    if (!dot) return;

    if (txt.includes("scanning")) dot.style.background = "rgba(125,245,255,.95)";
    else if (txt.includes("Online")) dot.style.background = "rgba(120,255,190,.9)";
    else dot.style.background = "rgba(235,245,255,.55)";
  }

  function tickClock() {
    if (!els.clock) return;
    els.clock.textContent = new Date().toLocaleTimeString("pt-BR", { timeZone: "America/Sao_Paulo" });
  }
  tickClock();
  setInterval(tickClock, 1000);

  // ===== Keywords UI =====
  function setCount() {
    const n = keywords.length;
    if (els.kwCount) els.kwCount.textContent = `${n} palavra${n === 1 ? "" : "s"}-chave`;
  }

  function setKwState(idx, state) {
    kwStates.set(idx, state);
    const chip = els.kwChips.querySelector(`.kw-chip[data-idx="${idx}"]`);
    if (!chip) return;
    chip.classList.remove("ok", "bad");
    if (state === "ok") chip.classList.add("ok");
    if (state === "bad") chip.classList.add("bad");
  }

  function renderKeywords() {
    els.kwChips.innerHTML = "";

    keywords.forEach((k, idx) => {
      const chip = document.createElement("div");
      chip.className = "kw-chip";
      chip.dataset.idx = String(idx);

      const dot = document.createElement("span");
      dot.className = "kw-dot";

      const text = document.createElement("span");
      text.className = "kw-text";
      text.textContent = k;

      const x = document.createElement("button");
      x.className = "kw-x";
      x.type = "button";
      x.title = "Remover";
      x.innerHTML = "&times;";
      x.addEventListener("click", () => {
        keywords.splice(idx, 1);
        saveKeywords(keywords);
        kwStates = new Map();
        renderKeywords();
        runScan();
      });

      chip.appendChild(dot);
      chip.appendChild(text);
      chip.appendChild(x);
      els.kwChips.appendChild(chip);

      setKwState(idx, kwStates.get(idx) || "neutral");
    });

    setCount();
  }

  function addFromInput() {
    const raw = els.kwInput.value;
    const items = parseMany(raw);
    if (!items.length) return;

    let changed = false;
    items.forEach((it) => {
      changed = uniquePush(keywords, it) || changed;
    });

    if (changed) {
      saveKeywords(keywords);
      kwStates = new Map();
      renderKeywords();
      runScan();
    }

    els.kwInput.value = "";
    els.kwInput.focus();
  }

  els.kwAdd?.addEventListener("click", addFromInput);
  els.kwInput?.addEventListener("keydown", (e) => {
    if (e.key === "Enter") {
      e.preventDefault();
      addFromInput();
    }
  });

  els.kwClear?.addEventListener("click", () => {
    keywords = [];
    saveKeywords(keywords);
    kwStates = new Map();
    renderKeywords();
    runScan();
  });

  els.kwReset?.addEventListener("click", () => {
    keywords = [...DEFAULT_KEYWORDS];
    saveKeywords(keywords);
    kwStates = new Map();
    renderKeywords();
    runScan();
  });

  els.kwCopy?.addEventListener("click", async () => {
    const text = keywords.join(", ");
    try {
      await navigator.clipboard.writeText(text);
      els.kwCopy.textContent = "Copiado!";
      setTimeout(() => (els.kwCopy.textContent = "Copiar lista"), 900);
    } catch {
      prompt("Copie aqui:", text);
    }
  });

  els.refreshBtn?.addEventListener("click", () => runScan());

  // ===== Results =====
  function renderResults() {
    stopMarquee();

    if (todayItems.length === 0) {
      els.results.innerHTML = `<div class="hint">Sem notícias recentes ainda.</div>`;
      return;
    }

    els.results.innerHTML = "";

    todayItems.forEach((r) => {
      const card = document.createElement("article");
      card.className = "news-card";

      const openUrl = r.publisherUrl || r.url || "#";

      const kws =
        Array.isArray(r.keywords) && r.keywords.length
          ? r.keywords
          : [r.keyword || "Linha Amarela"];

      const kwChipsHtml = kws
        .filter(Boolean)
        .slice(0, 4)
        .map((k) => `<span class="chipCard">${escapeHtml(k)}</span>`)
        .join("");

      const chip2Text = String(r.source || "").trim() || (r.publisherDomain ? r.publisherDomain : "") || "Fonte";
      const chip2 = escapeHtml(chip2Text);

      const iconSrc = faviconFromDomain(r.publisherDomain, r.source, openUrl);

      const live = isLiveItem(r, 3);
      const title = escapeHtml(r.title || "(sem título)");
      const snippet = escapeHtml(cleanSnippetFront(r.snippet || ""));

      card.innerHTML = `
        <header class="news-top">
          <div class="news-chips">
            ${kwChipsHtml}

            <span class="chipCard chipCard-url">
              <img class="chipIcon" src="${escapeHtml(iconSrc)}" alt="" loading="lazy" />
              <span class="chipLabel">${chip2}</span>
            </span>
          </div>
          ${live ? `<span class="badge-live">LIVE</span>` : ``}
        </header>

        <h3 class="news-title">${title}</h3>
        ${snippet ? `<p class="news-snippet">${snippet}</p>` : ``}

        <a class="news-link" href="${escapeHtml(openUrl)}" target="_blank" rel="noopener noreferrer">
          abrir fonte <span aria-hidden="true">↗</span>
        </a>
      `;

      els.results.appendChild(card);
    });

    setupInfiniteMarquee({ speedPxPerSec: 55, minCards: 24 });
  }

  async function searchWeb(keyword) {
    const date = todayISO();
    const sites = SITE_FILTER.length ? `&sites=${encodeURIComponent(SITE_FILTER.join(","))}` : "";
    const url = `${API_BASE}/search?q=${encodeURIComponent(keyword)}&date=${encodeURIComponent(date)}${sites}`;

    const res = await fetch(url);
    if (!res.ok) {
      const txt = await res.text().catch(() => "");
      console.warn("Falha /search:", res.status, txt.slice(0, 200));
      throw new Error(`Erro HTTP ${res.status}`);
    }
    return res.json();
  }

  // ✅ tenta keyword + fallbacks (só se vier 0)
  async function searchWebWithFallbacks(originalKw) {
    const tryList = [originalKw, ...(KW_FALLBACKS[originalKw] || [])];

    for (let t = 0; t < tryList.length; t++) {
      const q = tryList[t];
      try {
        const data = await searchWeb(q);
        const results = Array.isArray(data?.results) ? data.results : [];
        if (results.length > 0) return results;
      } catch (e) {
        // se falhou, tenta o próximo fallback
      }
      if (t < tryList.length - 1) await sleep(180);
    }
    return [];
  }

  function countItemsForKeyword(kw) {
    return todayItems.filter((x) => {
      const kws = Array.isArray(x.keywords) && x.keywords.length ? x.keywords : [x.keyword || "—"];
      return kws.includes(kw);
    }).length;
  }

  function pushToday(item) {
    if (!withinHours(item, MAX_AGE_HOURS)) return;

    const norm = normalizeUrl(item.url || "");
    if (norm) item.url = norm;

    const key = makeDedupeKey(item);

    // ✅ se já existe: mescla keywords no MESMO card
    const existingIdx = todayItems.findIndex((x) => makeDedupeKey(x) === key);
    if (existingIdx !== -1) {
      const existing = todayItems[existingIdx];

      const existingKws =
        Array.isArray(existing.keywords) && existing.keywords.length
          ? existing.keywords
          : existing.keyword
            ? [existing.keyword]
            : [];

      const kw = item.keyword || "—";
      if (kw && !existingKws.includes(kw)) existingKws.push(kw);

      existing.keywords = existingKws;
      if (!existing.keyword && kw) existing.keyword = kw;

      if (!existing.publisherUrl && item.publisherUrl) existing.publisherUrl = item.publisherUrl;
      if (!existing.publisherDomain && item.publisherDomain) existing.publisherDomain = item.publisherDomain;

      return;
    }

    const kw = item.keyword || "—";
    const kwCount = countItemsForKeyword(kw);
    if (kwCount >= MAX_RESULTS_PER_KEYWORD) return;

    item.keywords = [kw];

    todayItems.unshift(item);

    const maxTotal = maxTodayItemsNow();
    if (todayItems.length > maxTotal) todayItems.length = maxTotal;
  }

  async function runScan() {
    if (isScanning) {
      pendingRescan = true;
      setStatus("scanning… (atualização na fila)");
      return;
    }

    isScanning = true;
    setStatus("scanning…");

    todayItems = [];
    renderResults();

    kwStates = new Map();
    for (let i = 0; i < keywords.length; i++) kwStates.set(i, "neutral");
    renderKeywords();

    for (let i = 0; i < keywords.length; i++) {
      const k = keywords[i];
      setStatus(`scanning: ${k}`);

      try {
        const results = await searchWebWithFallbacks(k);
        console.log("[scan]", k, "=>", results.length);

        setKwState(i, results.length > 0 ? "ok" : "bad");

        results.forEach((r) => {
          const rawUrl = r.url || r.link || "";
          if (!rawUrl) return;
          if (isBlockedUrl(rawUrl)) return;

          pushToday({
            keyword: k, // ✅ sempre “carimba” a keyword original
            title: r.title || "",
            snippet: r.snippet || r.description || "",
            source: r.source || "Fonte",
            url: rawUrl,
            publishedAt: r.publishedAt || r.published_at || r.date || r.datetime || r.time || null,
            publisherUrl: r.publisherUrl || "",
            publisherDomain: r.publisherDomain || "",
          });
        });
      } catch (e) {
        console.warn("Erro na busca:", k, e?.message || e);
        setKwState(i, "bad");
      }

      await sleep(250);
    }

    renderResults();
    setStatus("Online");
    isScanning = false;

    if (pendingRescan) {
      pendingRescan = false;
      runScan();
    }
  }

  // ===== Clima =====
  async function loadWeather() {
    try {
      const lat = -22.8749;
      const lon = -43.3096;

      const res = await fetch(
        `https://api.open-meteo.com/v1/forecast?latitude=${lat}&longitude=${lon}&current=temperature_2m,relative_humidity_2m,apparent_temperature,wind_speed_10m&timezone=America/Sao_Paulo`
      );
      if (!res.ok) throw new Error();

      const c = (await res.json()).current;

      els.wTemp.textContent = `${Math.round(c.temperature_2m)}°`;
      els.wWind.textContent = `${Math.round(c.wind_speed_10m)} km/h`;
      els.wHum.textContent = `${Math.round(c.relative_humidity_2m)}%`;
      els.wFeels.textContent = `${Math.round(c.apparent_temperature)}°`;
      els.wPlace.textContent = "Água Santa • RJ";
      els.wUpdated.textContent = new Date().toLocaleTimeString("pt-BR", { timeZone: "America/Sao_Paulo" });
    } catch {
      els.wPlace.textContent = "Clima indisponível";
      els.wUpdated.textContent = new Date().toLocaleTimeString("pt-BR", { timeZone: "America/Sao_Paulo" });
    }
  }

// ===== Refresh automático do Waze (Live reforçado) =====
function refreshWazeIframe() {
  const iframe = document.querySelector(".mapEl");
  if (!iframe) return;

  const url = new URL(iframe.src);
  url.searchParams.set("_t", String(Date.now())); // evita cache
  iframe.src = url.toString();
}

// refresh logo ao abrir (2s depois)
setTimeout(refreshWazeIframe, 2000);

// atualiza periodicamente
setInterval(refreshWazeIframe, 3 * 60 * 1000);

  
  // ===== Init =====
  renderKeywords();
  renderResults();
  setStatus("idle");

  loadWeather();
  setInterval(loadWeather, 5 * 60 * 1000);

  initGoogleMapIfPossible();
  setTimeout(initGoogleMapIfPossible, 1000);

  runScan();
  setInterval(runScan, 5 * 60 * 1000);

  window.addEventListener("resize", () => {
    if (__mapInstance) {
      try {
        // eslint-disable-next-line no-undef
        google.maps.event.trigger(__mapInstance, "resize");
        __mapInstance.setCenter(MAPS.center);
      } catch {}
    }
  });
});
