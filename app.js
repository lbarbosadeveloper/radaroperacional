// ==========================================
// ✅ CONFIGURAÇÕES GERAIS
// ==========================================
const DEFAULT_KEYWORDS = ["Lamsa", "Avenida Brasil", "Trânsito Rio de Janeiro", "Cet Rio Lamsa"];
const KW_STORAGE_KEY = "radar_keywords_v1";

const MAX_RESULTS_PER_KEYWORD = 3; // Permite até 3 notícias por termo
const MAX_TODAY_ITEMS = 15;        // Aumentado para dar visibilidade a mais termos
const MAX_AGE_HOURS = 48; 

const SITE_FILTER = ["g1.globo.com", "oglobo.globo.com", "diariodorio.com", "r7.com"];
const BLOCKLIST_HOSTS = ["wikipedia.org", "lamsa.com.br"];

const PROD_API = "https://radaroperacional-api.onrender.com";
const API_BASE = location.hostname === "localhost" || location.hostname === "127.0.0.1" ? "http://localhost:3000" : PROD_API;

// ==========================================
// ✅ ESTADO GLOBAL
// ==========================================
let todayItems = [];
let isScanning = false;
let pendingRescan = false;
let keywords = loadKeywords();
let kwStates = new Map();
let marqueeRAF = null;

// ==========================================
// ✅ UTILS & DEDUPE
// ==========================================
function normalizeUrl(url) {
    if (!url) return "";
    try {
        const o = new URL(url.startsWith('http') ? url : 'https://' + url);
        o.hostname = o.hostname.replace(/^www\./, "").toLowerCase();
        ["utm_source", "utm_medium", "utm_campaign", "gclid"].forEach(p => o.searchParams.delete(p));
        o.hash = "";
        return o.toString();
    } catch { return ""; }
}

function makeDedupeKey(item) {
    // Chave baseada na URL ou Título para identificar a MESMA notícia
    const url = normalizeUrl(item.url);
    const title = String(item.title || "").trim().toLowerCase();
    return url ? `U:${url}` : `T:${title}`;
}

function countItemsForKeyword(kw) {
    return todayItems.filter(x => x.keywords && x.keywords.includes(kw)).length;
}

// ==========================================
// ✅ CORE: ADICIONAR NOTÍCIA (A mágica acontece aqui)
// ==========================================
function pushToday(item) {
    if (!withinHours(item, MAX_AGE_HOURS)) return;

    const key = makeDedupeKey(item);
    const existingIdx = todayItems.findIndex(x => makeDedupeKey(x) === key);

    if (existingIdx !== -1) {
        // Se a notícia já existe, apenas "carimbamos" com a nova keyword
        if (!todayItems[existingIdx].keywords.includes(item.keyword)) {
            todayItems[existingIdx].keywords.push(item.keyword);
        }
        return;
    }

    // Se for notícia nova, verificamos se o termo atual já saturou
    if (countItemsForKeyword(item.keyword) >= MAX_RESULTS_PER_KEYWORD) return;

    // Criamos o item com um array de keywords
    item.keywords = [item.keyword];
    
    // Adicionamos ao array global
    todayItems.push(item);

    // Mantém o limite global para não sobrecarregar o DOM
    if (todayItems.length > MAX_TODAY_ITEMS) {
        todayItems.shift();
    }
}

// ==========================================
// ✅ WEB SEARCH & SCAN
// ==========================================
async function runScan() {
    if (isScanning) { pendingRescan = true; return; }
    isScanning = true;
    setStatus("scanning…");

    // Limpamos para garantir que novos termos apareçam
    todayItems = [];
    kwStates = new Map();

    for (let i = 0; i < keywords.length; i++) {
        const k = keywords[i];
        setStatus(`buscando: ${k}`);

        try {
            const date = new Date().toLocaleDateString("en-CA", { timeZone: "America/Sao_Paulo" });
            const sites = SITE_FILTER.length ? `&sites=${encodeURIComponent(SITE_FILTER.join(","))}` : "";
            const res = await fetch(`${API_BASE}/search?q=${encodeURIComponent(k)}&date=${encodeURIComponent(date)}${sites}`);
            
            if (res.ok) {
                const data = await res.json();
                const results = data.results || [];
                setKwState(i, results.length > 0 ? "ok" : "bad");

                results.forEach(r => {
                    const rawUrl = r.url || r.link || "";
                    if (!rawUrl || isBlockedUrl(rawUrl)) return;

                    pushToday({
                        keyword: k,
                        title: r.title || "",
                        snippet: r.snippet || r.description || "",
                        source: r.source || "Fonte",
                        url: rawUrl,
                        publishedAt: r.publishedAt || r.date || null
                    });
                });
            }
        } catch (e) {
            console.error("Erro busca:", e);
            setKwState(i, "bad");
        }
        await new Promise(r => setTimeout(r, 300)); // Delay para evitar bloqueio da API
    }

    renderResults();
    setStatus("Online");
    isScanning = false;
    if (pendingRescan) { pendingRescan = false; runScan(); }
}

// ==========================================
// ✅ RENDERIZAÇÃO
// ==========================================
function renderResults() {
    stopMarquee();
    const container = document.getElementById("results");
    if (!container) return;

    if (todayItems.length === 0) {
        container.innerHTML = `<div class="hint">Nenhum resultado para os termos atuais.</div>`;
        return;
    }

    // Ordenar: as mais recentes primeiro
    todayItems.sort((a, b) => {
        const da = new Date(a.publishedAt || 0);
        const db = new Date(b.publishedAt || 0);
        return db - da;
    });

    container.innerHTML = todayItems.map(r => {
        const openUrl = r.url || "#";
        const kwsHtml = r.keywords.map(k => `<span class="chipCard">${escapeHtml(k)}</span>`).join("");
        const iconSrc = `https://www.google.com/s2/favicons?domain=${encodeURIComponent(r.url)}&sz=64`;

        return `
            <article class="news-card">
                <header class="news-top">
                    <div class="news-chips">
                        ${kwsHtml}
                        <span class="chipCard chipCard-url">
                            <img class="chipIcon" src="${iconSrc}" alt="" />
                            <span class="chipLabel">${escapeHtml(r.source)}</span>
                        </span>
                    </div>
                </header>
                <h3 class="news-title">${escapeHtml(r.title)}</h3>
                <p class="news-snippet">${escapeHtml(r.snippet.slice(0, 140))}...</p>
                <a class="news-link" href="${openUrl}" target="_blank">abrir fonte ↗</a>
            </article>
        `;
    }).join("");

    setupInfiniteMarquee({ speedPxPerSec: 50 });
}

// ==========================================
// ✅ INICIALIZAÇÃO E EVENTOS (MANTER O RESTANTE)
// ==========================================
document.addEventListener("DOMContentLoaded", () => {
    // Vincular botões de busca
    document.getElementById("kwAdd")?.addEventListener("click", addFromInput);
    document.getElementById("kwInput")?.addEventListener("keydown", e => e.key === "Enter" && addFromInput());
    document.getElementById("kwClear")?.addEventListener("click", () => { keywords = []; saveKeywords([]); renderKeywords(); runScan(); });
    document.getElementById("kwReset")?.addEventListener("click", () => { keywords = [...DEFAULT_KEYWORDS]; saveKeywords(keywords); renderKeywords(); runScan(); });

    renderKeywords();
    loadWeather();
    runScan();
    
    setInterval(runScan, 5 * 60 * 1000); // Auto-refresh a cada 5 min
    setInterval(loadWeather, 5 * 60 * 1000);
});

// Funções auxiliares mantidas (loadKeywords, saveKeywords, setStatus, setKwState, etc.) 
// devem permanecer conforme seu código original para funcionamento da UI.
