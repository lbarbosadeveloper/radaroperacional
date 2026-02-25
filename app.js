// public/app.js

// ============================
// ✅ KEYWORDS EDITÁVEL (com persistência)
// ============================
const DEFAULT_KEYWORDS = ["Lamsa", "Avenida Brasil", "Trânsito Rio de Janeiro", "Cet Rio Lamsa"];
const KW_STORAGE_KEY = "radar_keywords_v1";

// ============================
// ✅ LIMITES / JANELA
// ============================
const MAX_RESULTS_PER_KEYWORD = 10;
const MAX_TOTAL_ITEMS_CAP = 60;
const MAX_AGE_HOURS = 48;

// ============================
// Sites permitidos (vai pro backend /search)
// ============================
const SITE_FILTER = [
  // Já existentes
  "g1.globo.com",
  "oglobo.globo.com",
  "diariodorio.com",
  "r7.com",
  "band.uol.com.br",
  "cnnbrasil.com.br",
  "odia.ig.com.br",
  "mobilidaderio.com.br",
  "x.com",

  // Nacionais adicionados
  "uol.com.br",
  "estadao.com.br",
  "folha.uol.com.br",
  "valoreconomico.com.br",
  "metropoles.com",

  // Regionais RJ adicionados
  "extra.globo.com",
  "temporealrj.com",

  // Órgãos oficiais
  "prefeitura.rio"
];

// ============================
// ✅ BLOCKLIST
// ============================
const BLOCKLIST_HOSTS = ["wikipedia.org", "lamsa.com.br"];
const BLOCKLIST_PATH_CONTAINS = [];

// ============================
// ✅ Fallbacks (só quando vier 0 resultados)
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
  location.hostname === "localhost" || location.hostname === "127.0.0.1"
    ? "http://localhost:3000"
    : PROD_API;

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
// ===== Refresh automático do Waze (Live reforçado) =====
// ============================
function refreshWazeIframe() {
  const iframe = document.querySelector(".mapEl");
  if (!iframe) return;

  const url = new URL(iframe.src);
  url.searchParams.set("_t", String(Date.now())); // evita cache
  iframe.src = url.toString();
}

// ============================
// App
// ============================
document.addEventListener("DOMContentLoaded", () => {
  // ✅ Logo clicável: recarrega
  const logo = document.getElementById("lamsaLogo");
  if (logo) {
    logo.style.cursor = "pointer";
    logo.addEventListener("click", () => window.location.reload());
    logo.addEventListener("keydown", (e) => {
      if (e.key === "Enter" || e.key === " ") {
        e.preventDefault();
        window.location.reload();
      }
    });
  }

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

    // ⚠️ wTemp existe no HTML, mas NÃO vamos usar mais.
    wTemp: document.getElementById("wTemp"),

    wWind: document.getElementById("wWind"),
    wHum: document.getElementById("wHum"),
    wFeels: document.getElementById("wFeels"),
    wPlace: document.getElementById("wPlace"),
    wUpdated: document.getElementById("wUpdated"),

    // ✅ novos IDs do badge
    wCond: document.getElementById("wCond"),
    wDay: document.getElementById("wDay"),
    wMin: document.getElementById("wMin"),
    wMax: document.getElementById("wMax"),
    weatherMini: document.getElementById("weatherMini"),
  };

  if (!els.results || !els.kwChips) {
    console.error("IDs obrigatórios não encontrados no HTML (kwChips/results).");
    return;
  }

  // ============================
  // ✅ Estágio (imagem + dots mini-selo)
  // ============================
  const STAGE_STORAGE_KEY = "radar_stage_v1";

  const stageImages = {
    1: "./assets/estagio-1.png",
    2: "./assets/estagio-2.png",
    3: "./assets/estagio-3.png",
    4: "./assets/estagio-4.png",
    5: "./assets/estagio-5.png",
  };

  // ✅ descrições (simples, editável)
  const ESTAGIOS_DESC = {
    1: "Não há mudanças na rotina da cidade, nem foram identificados fatores de risco que possam impactar a cidade nas próximas horas.",
    2: "Há previsão de mudança na rotina da cidade nas próximas horas, ou já há impactos que exigem ações de resposta imediatas.",
    3: "Há impactos na rotina de parte da cidade, que exigem ações de resposta integradas.",
    4: "Há impactos na rotina de grande parte da cidade, que exigem ações de resposta complexas e coordenadas.",
    5: "Há impactos na rotina da cidade que superam a capacidade de resposta e exigem recursos extraordinários.",
  };

  const rgbMap = {
    1: "46,204,113",
    2: "241,196,15",
    3: "243,156,18",
    4: "231,76,60",
    5: "142,68,173",
  };

  function renderStageInfo(n) {
    const el = document.getElementById("stageInfoText");
    if (!el) return;
    el.textContent = ESTAGIOS_DESC[n] || "—";
  }

  // 🔥 coloca as imagens nos botões (uma vez)
  document.querySelectorAll(".stageDots .dot").forEach((btn) => {
    const s = Number(btn.dataset.stage);
    btn.style.setProperty("--stageImg", `url(${stageImages[s]})`);
  });

  function setEstagio(n, { persist = true } = {}) {
    n = Math.max(1, Math.min(5, Number(n) || 1));

    // número (se existir)
    const elNum = document.getElementById("stageNumber");
    if (elNum) elNum.textContent = n;

    // imagem central
    const badge = document.getElementById("stageBadge");
    if (badge) badge.src = stageImages[n] || stageImages[1];

    // ✅ texto ao lado
    renderStageInfo(n);

    // dots
    document.querySelectorAll(".stageDots .dot").forEach((btn) => {
      const s = Number(btn.dataset.stage);
      btn.classList.toggle("on", s <= n);
      btn.classList.toggle("active", s === n);
    });

    // CSS var
    document.documentElement.style.setProperty("--stageRGB", rgbMap[n] || rgbMap[1]);

    if (persist) localStorage.setItem(STAGE_STORAGE_KEY, String(n));
  }

  // clique nos dots (se quiser manter clicável, deixa)
  document.querySelectorAll(".stageDots .dot").forEach((btn) => {
    btn.addEventListener("click", () => setEstagio(btn.dataset.stage, { persist: true }));
  });

  async function loadCorEstagio() {
    try {
      const r = await fetch(`${API_BASE}/cor/estagio`, { cache: "no-store" });
      if (!r.ok) throw new Error();
      const j = await r.json();
      if (j?.estagio) setEstagio(j.estagio, { persist: true });
    } catch {
      // silêncio
    }
  }

  // ============================
  // Status + relógio
  // ============================
  function maxTodayItemsNow() {
    const dynamic = keywords.length * MAX_RESULTS_PER_KEYWORD;
    return Math.min(MAX_TOTAL_ITEMS_CAP, Math.max(20, dynamic));
  }

  function setStatus(txt) {
    if (els.statusText) els.statusText.textContent = txt;
    const dot = document.querySelector(".pill .dot"); // ✅ só o dot do status
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
        Array.isArray(r.keywords) && r.keywords.length ? r.keywords : [r.keyword || "Linha Amarela"];

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
          ${live ? `<span class="badge-live">AO VIVO</span>` : ``}
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

  async function searchWebWithFallbacks(originalKw) {
    const tryList = [originalKw, ...(KW_FALLBACKS[originalKw] || [])];

    for (let t = 0; t < tryList.length; t++) {
      const q = tryList[t];
      try {
        const data = await searchWeb(q);
        const results = Array.isArray(data?.results) ? data.results : [];
        if (results.length > 0) return results;
      } catch {}
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
            keyword: k,
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

  // ============================
  // ===== Clima (Open-Meteo) =====
  // ✅ Mantém SOMENTE o badge (min/max) e texto
  // ============================

  function weatherCodeToText(code) {
    const c = Number(code);
    if (Number.isNaN(c)) return "—";

    // Mapeamento oficial Open-Meteo (resumo)
    if (c === 0) return "Céu limpo";
    if (c === 1) return "Poucas nuvens";
    if (c === 2) return "Parcialmente nublado";
    if (c === 3) return "Nublado";

    if (c === 45 || c === 48) return "Neblina";

    if (c === 51 || c === 53 || c === 55) return "Garoa";
    if (c === 56 || c === 57) return "Garoa congelante";

    if (c === 61 || c === 63 || c === 65) return "Chuva";
    if (c === 66 || c === 67) return "Chuva congelante";

    if (c === 71 || c === 73 || c === 75) return "Neve";
    if (c === 77) return "Grãos de neve";

    if (c === 80 || c === 81 || c === 82) return "Pancadas de chuva";

    if (c === 85 || c === 86) return "Pancadas de neve";

    if (c === 95) return "Trovoadas";
    if (c === 96 || c === 99) return "Trovoadas com granizo";

    return "—";
  }

  function weatherCodeToEmoji(code) {
    const c = Number(code);
    if (Number.isNaN(c)) return "☁️";

    if (c === 0) return "☀️";
    if (c === 1) return "🌤️";
    if (c === 2) return "⛅";
    if (c === 3) return "☁️";
    if (c === 45 || c === 48) return "🌫️";
    if ([51,53,55,56,57].includes(c)) return "🌦️";
    if ([61,63,65,66,67,80,81,82].includes(c)) return "🌧️";
    if ([71,73,75,77,85,86].includes(c)) return "❄️";
    if ([95,96,99].includes(c)) return "⛈️";
    return "☁️";
  }

  async function loadWeather() {
    try {
      const lat = -22.8749;
      const lon = -43.3096;

      const url =
        `https://api.open-meteo.com/v1/forecast` +
        `?latitude=${lat}&longitude=${lon}` +
        `&current=relative_humidity_2m,apparent_temperature,wind_speed_10m` +
        `&daily=temperature_2m_min,temperature_2m_max,weather_code` +
        `&timezone=America/Sao_Paulo`;

      const res = await fetch(url, { cache: "no-store" });
      if (!res.ok) throw new Error("Falha no Open-Meteo");

      const j = await res.json();

      // Local fixo (como você já fazia)
      if (els.wPlace) els.wPlace.textContent = "Água Santa • RJ";

      // Condição do dia (via daily weather_code[0])
      const code = j?.daily?.weather_code?.[0];
      const cond = weatherCodeToText(code);
      if (els.wCond) els.wCond.textContent = cond;

      // ✅ Min/Max do dia (daily)
      const min = j?.daily?.temperature_2m_min?.[0];
      const max = j?.daily?.temperature_2m_max?.[0];

      if (els.wMin && min != null) els.wMin.textContent = `MAX${Math.round(min)}°C`;
      if (els.wMax && max != null) els.wMax.textContent = `MIN${Math.round(max)}°C`;

      // Dia
      if (els.wDay) els.wDay.textContent = "HOJE";

      // Emoji do clima (pega o segundo emoji do badge, se existir)
      const emojiSpans = els.weatherMini?.querySelectorAll(".wmEmoji");
      const weatherEmoji = weatherCodeToEmoji(code);
      if (emojiSpans && emojiSpans.length >= 2) emojiSpans[1].textContent = weatherEmoji;

      // 🔕 NÃO preencher temperatura grande (27°)
      if (els.wTemp) els.wTemp.textContent = ""; // pode manter vazio (ou remove do HTML)

      // Extras (se existirem no HTML)
      const c = j?.current;
      if (els.wFeels && c?.apparent_temperature != null) els.wFeels.textContent = `${Math.round(c.apparent_temperature)}°`;

      if (els.wUpdated)
        els.wUpdated.textContent = new Date().toLocaleTimeString("pt-BR", { timeZone: "America/Sao_Paulo" });

    } catch (e) {
      console.warn("[weather]", e?.message || e);

      if (els.wPlace) els.wPlace.textContent = "Água Santa • RJ";
      if (els.wCond) els.wCond.textContent = "—";
      if (els.wMin) els.wMin.textContent = "--°C";
      if (els.wMax) els.wMax.textContent = "--°C";
      if (els.wDay) els.wDay.textContent = "HOJE";

      if (els.wTemp) els.wTemp.textContent = "";

      if (els.wFeels) els.wFeels.textContent = "—";
      if (els.wUpdated)
        els.wUpdated.textContent = new Date().toLocaleTimeString("pt-BR", { timeZone: "America/Sao_Paulo" });
    }
  }

  // ============================
  // Init
  // ============================
  renderKeywords();
  renderResults();
  setStatus("idle");

  // Waze refresh
  setTimeout(refreshWazeIframe, 2000);
  setInterval(refreshWazeIframe, 3 * 60 * 1000);

  // Clima
  loadWeather();
  setInterval(loadWeather, 5 * 60 * 1000);

  // Estágio
  const savedStage = Number(localStorage.getItem(STAGE_STORAGE_KEY) || 2);
  setEstagio(savedStage, { persist: false });
  loadCorEstagio();
  setInterval(loadCorEstagio, 2 * 60 * 1000);

  // Maps
  initGoogleMapIfPossible();
  setTimeout(initGoogleMapIfPossible, 1000);

  // Notícias
  runScan();
  setInterval(runScan, 5 * 60 * 1000);

  // Ctrl + Shift + K: mostra/oculta o painel de buscas (modo admin)
  document.addEventListener("keydown", (e) => {
    const isShortcut = e.ctrlKey && e.shiftKey && (e.key === "k" || e.key === "K");
    if (!isShortcut) return;

    e.preventDefault();

    const panel = document.querySelector(".keywordsPanel");
    if (!panel) return;

    panel.classList.toggle("is-open");
    document.body.classList.toggle("kw-open", panel.classList.contains("is-open"));
  });

  // Resize Maps
  window.addEventListener("resize", () => {
    if (__mapInstance) {
      try {
        // eslint-disable-next-line no-undef
        google.maps.event.trigger(__mapInstance, "resize");
        __mapInstance.setCenter(MAPS.center);
      } catch {}
    }
  });

  // ✅ travar os botões de estágio (não clicar)
  document.querySelectorAll(".stageDots .dot").forEach((btn) => {
    btn.disabled = true;
    btn.style.cursor = "default";
  });
});
