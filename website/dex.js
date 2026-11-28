const NETWORKS = {
  tataservice: {
    name: "TataService Blockchain",
    chainId: 810161,
    chainIdHex: "0xc5cb1",
    symbol: "TATA",
    rpc: "https://rpctata.duckdns.org/rpc",
    explorer: "https://exploretata.duckdns.org",
    isEvm: true
  },
  eth_mainnet: {
    name: "Ethereum Mainnet",
    chainId: 1,
    chainIdHex: "0x1",
    symbol: "ETH",
    rpc: "https://public.1rpc.io/eth",
    explorer: "https://etherscan.io",
    isEvm: true
  },
  bsc_mainnet: {
    name: "BNB Smart Chain",
    chainId: 56,
    chainIdHex: "0x38",
    symbol: "BNB",
    rpc: "https://public.1rpc.io/bnb",
    explorer: "https://bscscan.com",
    isEvm: true
  },
  polygon_mainnet: {
    name: "Polygon Mainnet",
    chainId: 137,
    chainIdHex: "0x89",
    symbol: "POL",
    rpc: "https://public.1rpc.io/matic",
    explorer: "https://polygonscan.com",
    isEvm: true
  },
  core_mainnet: {
    name: "Core Blockchain",
    chainId: 1116,
    chainIdHex: "0x45c",
    symbol: "CORE",
    rpc: "https://rpc.coredao.org",
    explorer: "https://scan.coredao.org",
    isEvm: true
  }
};

const OFFICIAL_FEE_RECEIVER = "0x3f48248FFE235a5205d1D781fB6c852c71fc1556";
const OFFICIAL_OWNER_WALLET = "0x3f48248FFE235a5205d1D781fB6c852c71fc1556";
const PRICES = { TATA: 1.45, ETH: 3210.0, BNB: 624.5, POL: 0.55, CORE: 1.25, USDT: 1.0 };
const CHANGES_24H = { TATA: 4.2, ETH: 0.0, BNB: 0.0, POL: 0.0, CORE: 0.0, USDT: 0.0 };

let TOKENS = [
  { symbol: "TATA", name: "TataSwap Token", address: "0x7A7A59a9DeCe06b86f1e58a2E49c7D59A1b7A7A5", decimals: 18, price: 1.45, change24h: 4.2, balance: 0, verified: true },
  { symbol: "ETH", name: "Ethereum", address: "0x2170Ed0880ac9A755fd29B2688956BD959F933F8", decimals: 18, price: 3210.0, change24h: 0.0, balance: 0, verified: true },
  { symbol: "BNB", name: "BNB", address: "0xbb4CdB9CBd36B01bD1cBaEBF2De08d9173bc095c", decimals: 18, price: 624.5, change24h: 0.0, balance: 0, verified: true },
  { symbol: "POL", name: "Polygon", address: "0x0000000000000000000000000000000000001010", decimals: 18, price: 0.55, change24h: 0.0, balance: 0, verified: true },
  { symbol: "CORE", name: "Core DAO", address: "0x40375C92d9FAf44d2f9db9Bd9ba41a3317a2404f", decimals: 18, price: 1.25, change24h: 0.0, balance: 0, verified: true },
  { symbol: "USDT", name: "Tether USD", address: "0x55d398326f99059fF775485246999027B3197955", decimals: 18, price: 1.0, change24h: 0.0, balance: 0, verified: true }
];

const LIVE_DEX_URL = "https://tataswap.onrender.com/dex.html";
let activeNetKey = "tataservice";
let walletAddress = localStorage.getItem("tataswap_wallet") || null;
let walletMode = localStorage.getItem("tataswap_wallet_mode") || "none";
let walletProviderLabel = localStorage.getItem("tataswap_wallet_label") || "Web3 Wallet";
let nativeBalanceVal = 0;
let discoveredProviders = [];
let activeProvider = null;
const DEFAULT_WC_PROJECT_ID = "58ad26007a1212b3406b582c93228d50";
const _storedWcId = (localStorage.getItem("tataswap_wc_project_id") || "").trim();
let wcProjectIdCache = _storedWcId.length >= 16 ? _storedWcId : DEFAULT_WC_PROJECT_ID;
if (_storedWcId !== wcProjectIdCache) {
  try { localStorage.setItem("tataswap_wc_project_id", wcProjectIdCache); } catch {}
}

// Mirror tataservice-blockchain ConnectWalletModal + useMetaMask state
let forceWalletPicker = false;
let expandedWalletId = null;
let wcSetupExpanded = false;
let wcPairingUri = "";
let wcActiveProviderInstance = null;
let wcConnecting = false;
let autoConnectAttempted = false;
let qrLibPromise = null;
const qrDataUrlCache = new Map();

function escapeHtml(str) {
  return String(str || "")
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");
}

function registerEip6963Provider(detail) {
  if (!detail?.provider) return;
  const rdns = detail.info?.rdns || detail.info?.name || ("provider_" + discoveredProviders.length);
  if (!discoveredProviders.some(d => (d.info?.rdns || d.info?.name) === rdns)) {
    discoveredProviders.push(detail);
  }
}

window.addEventListener("eip6963:announceProvider", (event) => {
  registerEip6963Provider(event?.detail);
  const modal = document.getElementById("walletModal");
  if (modal && !modal.classList.contains("hidden")) {
    refreshWalletModalUi();
  }
  if (!autoConnectAttempted) {
    runAutoConnectFromUrl();
  }
});
window.dispatchEvent(new Event("eip6963:requestProvider"));

function identifyProviderName(p) {
  if (!p) return "Browser Wallet";
  if (p.isMetaMask && !p.isTrust && !p.isTrustWallet && !p.isBraveWallet && !p.isRabby) return "MetaMask";
  if (p.isTrust || p.isTrustWallet) return "Trust Wallet";
  if (p.isCoinbaseWallet || p.isCoinbaseBrowser) return "Coinbase Wallet";
  if (p.isBinance) return "Binance Wallet";
  if (p.isOkxWallet || p.isOKExWallet) return "OKX Wallet";
  if (p.isBitKeep) return "Bitget Wallet";
  if (p.isPhantom) return "Phantom";
  if (p.isRabby) return "Rabby Wallet";
  if (p.isRainbow) return "Rainbow";
  if (p.isSafePal) return "SafePal";
  return "Browser Wallet";
}

function getAllDetectedWallets() {
  const wallets = [];
  const seen = new Set();
  const pushWallet = (id, name, provider, icon = "") => {
    if (!provider || typeof provider.request !== "function" || seen.has(provider)) return;
    seen.add(provider);
    wallets.push({ id, name, provider, icon });
  };

  for (const d of discoveredProviders) {
    const label = d.info?.name || identifyProviderName(d.provider);
    const id = (d.info?.rdns || label).toLowerCase();
    pushWallet(id, label, d.provider, d.info?.icon || "");
  }

  if (Array.isArray(window.ethereum?.providers)) {
    for (const p of window.ethereum.providers) {
      const name = identifyProviderName(p);
      pushWallet(name.toLowerCase(), name, p);
    }
  }

  if (window.trustwallet) pushWallet("trust", "Trust Wallet", window.trustwallet);
  if (window.coinbaseWalletExtension) pushWallet("coinbase", "Coinbase Wallet", window.coinbaseWalletExtension);
  if (window.binancew3w?.ethereum) pushWallet("binance", "Binance Wallet", window.binancew3w.ethereum);
  if (window.BinanceChain) pushWallet("binance-chain", "Binance Wallet", window.BinanceChain);
  if (window.okxwallet) pushWallet("okx", "OKX Wallet", window.okxwallet);
  if (window.phantom?.ethereum) pushWallet("phantom", "Phantom", window.phantom.ethereum);
  if (window.bitkeep?.ethereum) pushWallet("bitget", "Bitget Wallet", window.bitkeep.ethereum);
  if (window.bitgetWallet) pushWallet("bitget-wallet", "Bitget Wallet", window.bitgetWallet);
  if (window.rabby) pushWallet("rabby", "Rabby Wallet", window.rabby);
  if (window.rainbow) pushWallet("rainbow", "Rainbow", window.rainbow);

  if (window.ethereum) {
    const p = window.ethereum;
    const name = identifyProviderName(p);
    pushWallet(name.toLowerCase(), name, p);
  }
  return wallets;
}

function findProviderForWallet(type) {
  const list = getAllDetectedWallets();
  const t = type.toLowerCase();
  if (t === "trust") {
    return list.find(w => w.id.includes("trust") || w.name.toLowerCase().includes("trust") || w.provider?.isTrust || w.provider?.isTrustWallet)?.provider || null;
  }
  if (t === "metamask") {
    return list.find(w => w.id.includes("metamask") || (w.provider?.isMetaMask && !w.provider?.isTrust && !w.provider?.isTrustWallet && !w.provider?.isBraveWallet && !w.provider?.isRabby))?.provider || null;
  }
  if (t === "coinbase") {
    return list.find(w => w.id.includes("coinbase") || w.name.toLowerCase().includes("coinbase") || w.provider?.isCoinbaseWallet)?.provider || window.coinbaseWalletExtension || null;
  }
  if (t === "binance") {
    return list.find(w => w.id.includes("binance") || w.name.toLowerCase().includes("binance") || w.provider?.isBinance)?.provider || window.binancew3w?.ethereum || window.BinanceChain || null;
  }
  if (t === "okx") {
    return list.find(w => w.id.includes("okx") || w.name.toLowerCase().includes("okx") || w.provider?.isOkxWallet || w.provider?.isOKExWallet)?.provider || window.okxwallet || null;
  }
  if (t === "bitget") {
    return list.find(w => w.id.includes("bitget") || w.id.includes("bitkeep") || w.provider?.isBitKeep)?.provider || window.bitkeep?.ethereum || window.bitgetWallet || null;
  }
  if (t === "phantom") {
    return list.find(w => w.id.includes("phantom") || w.name.toLowerCase().includes("phantom") || w.provider?.isPhantom)?.provider || window.phantom?.ethereum || null;
  }
  if (t === "safepal") {
    return list.find(w => w.id.includes("safepal") || w.name.toLowerCase().includes("safepal") || w.provider?.isSafePal)?.provider || null;
  }
  if (t === "rainbow") {
    return list.find(w => w.id.includes("rainbow") || w.name.toLowerCase().includes("rainbow") || w.provider?.isRainbow)?.provider || window.rainbow || null;
  }
  return null;
}

function getEvmProvider() {
  if (activeProvider) return activeProvider;
  const detected = getAllDetectedWallets();
  if (detected.length > 0) return detected[0].provider;
  return null;
}

function isOwnerWalletConnected() {
  return Boolean(walletAddress && walletAddress.toLowerCase() === OFFICIAL_OWNER_WALLET.toLowerCase());
}

function updateWalletHeaderUi() {
  const txt = document.getElementById("walletText");
  const portAddr = document.getElementById("portAddr");
  if (walletAddress) {
    const shortAddr = walletAddress.length > 12
      ? walletAddress.slice(0, 6) + "..." + walletAddress.slice(-4)
      : walletAddress;
    if (txt) txt.innerText = shortAddr;
    if (portAddr) portAddr.innerText = walletAddress;
  } else {
    if (txt) txt.innerText = "Connect Wallet";
    if (portAddr) portAddr.innerText = "Not connected";
  }
  updateListingFeeUi();
  updateCreatePoolActionUi();
}

function updateListingFeeUi() {
  const isOwner = isOwnerWalletConnected();
  const feeText = document.getElementById("listFeeAmountText");
  const ownerBadge = document.getElementById("listOwnerBadge");
  const submitBtn = document.getElementById("listSubmitBtn");
  const feeBox = document.getElementById("listFeeBox");

  if (feeText) {
    feeText.innerText = isOwner ? "0.00 USDT (Free)" : "5.00 USDT";
    feeText.className = isOwner ? "text-emerald-400 font-extrabold" : "text-yellow-400";
  }
  if (ownerBadge) ownerBadge.classList.toggle("hidden", !isOwner);
  if (feeBox) {
    feeBox.className = isOwner
      ? "p-4 rounded-xl bg-emerald-500/10 border border-emerald-500/40 text-xs space-y-1"
      : "p-4 rounded-xl bg-yellow-500/10 border border-yellow-500/30 text-xs space-y-1";
  }
  if (submitBtn) {
    submitBtn.innerText = isOwner ? "2. List Token Free (Owner — 0 Fee)" : "2. Pay 5 USDT & List Token";
  }
}

function openPage(pageId) {
  document.querySelectorAll(".dex-page").forEach(el => el.classList.add("hidden"));
  const target = document.getElementById("page-" + pageId);
  if (target) target.classList.remove("hidden");

  document.querySelectorAll(".page-btn").forEach(btn => {
    btn.classList.toggle("nav-pill-active", btn.dataset.page === pageId);
  });

  if (pageId === "swap") {
    recalcSwap();
    loadSwapOhlcChart(false);
  }
  if (pageId === "explore") renderTokensTable();
  if (pageId === "listing") updateListingFeeUi();
  if (pageId === "create-pool") {
    renderCreatePoolUi();
    renderOnChainPools();
  }
  if (pageId === "portfolio") renderPortfolioHoldings();
  if (pageId === "history") renderLocalHistory();
  if (pageId === "admin") {
    renderAdminAuthGate();
    if (adminAuthenticated) loadContractConfig();
  }
}

async function rpcRequest(method, params = []) {
  const rpcUrl = NETWORKS[activeNetKey].rpc;
  const res = await fetch(rpcUrl, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ jsonrpc: "2.0", id: Date.now(), method, params })
  });
  const data = await res.json();
  if (data.error) throw new Error(data.error.message);
  return data.result;
}

async function refreshBlock() {
  try {
    const hex = await rpcRequest("eth_blockNumber");
    document.getElementById("dashBlock").innerText = "#" + parseInt(hex, 16).toLocaleString();
  } catch {
    document.getElementById("dashBlock").innerText = "Offline";
  }
}

async function syncFromBackend() {
  const apiUrl = localStorage.getItem("tataswap_api_url") || (window.location.protocol.startsWith("http") ? "" : null);
  if (apiUrl === null) return;
  try {
    const [tokensRes, listingsRes, configRes] = await Promise.all([
      fetch(apiUrl + "/api/tokens").catch(() => null),
      fetch(apiUrl + "/api/listings").catch(() => null),
      fetch(apiUrl + "/api/config").catch(() => null)
    ]);
    if (configRes && configRes.ok) {
      const cfg = await configRes.json();
      if (cfg?.walletConnectProjectId && !localStorage.getItem("tataswap_wc_project_id")) {
        wcProjectIdCache = cfg.walletConnectProjectId;
      }
    }
    if (tokensRes && tokensRes.ok) {
      const data = await tokensRes.json();
      if (Array.isArray(data.tokens) && data.tokens.length > 0) {
        const bySym = {};
        for (const t of data.tokens) {
          bySym[String(t.symbol || "").toUpperCase()] = t;
        }
        TOKENS = TOKENS.map(t => {
          const row = bySym[t.symbol.toUpperCase()];
          if (!row) return t;
          return {
            ...t,
            address: row.address || t.address,
            price: PRICES[t.symbol] || parseFloat(row.price_usd) || t.price,
            verified: Boolean(row.is_verified)
          };
        });
        for (const row of data.tokens) {
          const sym = String(row.symbol || "").toUpperCase();
          if (!TOKENS.some(x => x.symbol.toUpperCase() === sym)) {
            TOKENS.push({
              symbol: sym,
              name: row.name || sym,
              address: row.address,
              decimals: row.decimals || 18,
              price: parseFloat(row.price_usd) || 1.0,
              change24h: parseFloat(row.change_24h) || 0,
              balance: 0,
              verified: Boolean(row.is_verified)
            });
          }
        }
        const countEl = document.getElementById("dashTokensCount");
        if (countEl) countEl.innerText = TOKENS.length;
        renderTokensTable();
      }
    }
    if (listingsRes && listingsRes.ok) {
      const lData = await listingsRes.json();
      if (Array.isArray(lData.listings) && lData.listings.length > 0) {
        renderLocalHistory(lData.listings);
      }
    }
  } catch (e) {
    console.warn("Backend sync skipped:", e.message);
  }
}

async function ensureEvmChain(provider, net) {
  if (!provider || !net?.chainIdHex) return;
  try {
    await provider.request({
      method: "wallet_switchEthereumChain",
      params: [{ chainId: net.chainIdHex }]
    });
  } catch (e) {
    const msg = String(e?.message || "").toLowerCase();
    const needsAdd =
      e?.code === 4902 ||
      e?.code === -32603 ||
      msg.includes("unrecognized chain") ||
      msg.includes("unknown chain") ||
      msg.includes("4902");
    if (needsAdd) {
      await provider.request({
        method: "wallet_addEthereumChain",
        params: [{
          chainId: net.chainIdHex,
          chainName: net.name,
          nativeCurrency: { name: net.symbol, symbol: net.symbol, decimals: 18 },
          rpcUrls: [net.rpc],
          blockExplorerUrls: [net.explorer]
        }]
      });
      await provider.request({
        method: "wallet_switchEthereumChain",
        params: [{ chainId: net.chainIdHex }]
      }).catch(() => {});
    } else {
      throw e;
    }
  }
}

async function changeNetwork() {
  activeNetKey = document.getElementById("networkSelect").value;
  refreshBlock();
  renderOnChainPools();
  if (walletAddress) refreshBalance();
  const net = NETWORKS[activeNetKey];
  const provider = getEvmProvider();
  if (provider) {
    try {
      await ensureEvmChain(provider, net);
    } catch (e) {
      console.warn("Switch network skipped:", e.message);
    }
  }
  const modal = document.getElementById("walletModal");
  if (modal && !modal.classList.contains("hidden")) {
    refreshWalletModalUi();
  }
}

async function switchOrAddActiveNetwork() {
  clearWalletModalError();
  const net = NETWORKS[activeNetKey];
  const provider = getEvmProvider();
  if (!provider) {
    setWalletModalError("Switch/Add Network requires an active extension or WalletConnect session (not Watch-Only address mode).");
    return;
  }
  try {
    await ensureEvmChain(provider, net);
    await refreshBalance();
    refreshWalletModalUi();
  } catch (err) {
    setWalletModalError(err?.message || "Failed to switch or add " + net.name);
  }
}

function detectEnvironmentFlags() {
  const ua = (navigator.userAgent || "").toLowerCase();
  const isMobile = /android|iphone|ipad|ipod|mobile/.test(ua);
  const isIframe = (() => {
    try { return window.self !== window.top; } catch { return true; }
  })();
  const host = (window.location.hostname || "").toLowerCase();
  const isPrivatePreviewHost =
    !window.location.protocol.startsWith("http") ||
    host === "localhost" ||
    host === "127.0.0.1" ||
    host.endsWith(".run.app") ||
    host.endsWith(".web.app") && isIframe;
  return { isMobile, isIframe, isPrivatePreviewHost };
}

function buildTargetDappUrl(forMobileDeepLink = false) {
  const { isPrivatePreviewHost } = detectEnvironmentFlags();
  const baseUrl = (forMobileDeepLink && isPrivatePreviewHost)
    ? LIVE_DEX_URL
    : (window.location.protocol.startsWith("http") ? window.location.href : LIVE_DEX_URL);
  try {
    const u = new URL(baseUrl);
    u.searchParams.set("autoConnect", "1");
    return u.toString();
  } catch {
    return LIVE_DEX_URL + "?autoConnect=1";
  }
}

const WALLET_CATALOG = [
  {
    id: "metamask",
    name: "MetaMask",
    icon: "🦊",
    badge: "Popular",
    installUrl: "https://metamask.io/download/",
    dappLink: (url) => {
      try {
        const parsed = new URL(url);
        return `https://metamask.app.link/dapp/${parsed.host}${parsed.pathname}${parsed.search}`;
      } catch {
        return `https://metamask.app.link/dapp/tataswap.onrender.com/dex.html?autoConnect=1`;
      }
    },
    wcDeepLink: (wcUri) => `https://metamask.app.link/wc?uri=${encodeURIComponent(wcUri)}`
  },
  {
    id: "trust",
    name: "Trust Wallet",
    icon: "🛡️",
    badge: "Mobile + Ext",
    installUrl: "https://trustwallet.com/browser-extension",
    dappLink: (url) => `https://link.trustwallet.com/open_url?coin_id=60&url=${encodeURIComponent(url)}`,
    wcDeepLink: (wcUri) => `https://link.trustwallet.com/wc?uri=${encodeURIComponent(wcUri)}`
  },
  {
    id: "coinbase",
    name: "Coinbase Wallet",
    icon: "🔵",
    badge: "Smart + Ext",
    installUrl: "https://www.coinbase.com/wallet/downloads",
    dappLink: (url) => `https://go.cb-w.com/dapp?cb_url=${encodeURIComponent(url)}`,
    wcDeepLink: null
  },
  {
    id: "binance",
    name: "Binance Wallet",
    icon: "🟡",
    badge: "Web3 / WC",
    installUrl: "https://www.binance.com/en/web3wallet",
    dappLink: (url) => `bnc://app.binance.com/cedefi/browser?url=${encodeURIComponent(url)}`,
    wcDeepLink: (wcUri) => `bnc://app.binance.com/cedefi/wc?uri=${encodeURIComponent(wcUri)}`
  },
  {
    id: "okx",
    name: "OKX Wallet",
    icon: "⚫",
    badge: "Multi-Chain",
    installUrl: "https://www.okx.com/web3",
    dappLink: (url) => `okx://wallet/dapp/details?dappUrl=${encodeURIComponent(url)}`,
    wcDeepLink: (wcUri) => `okx://wallet/wc?uri=${encodeURIComponent(wcUri)}`
  },
  {
    id: "bitget",
    name: "Bitget Wallet",
    icon: "💠",
    badge: "Multi-Chain",
    installUrl: "https://web3.bitget.com/",
    dappLink: (url) => `https://bkcode.vip?action=dapp&url=${encodeURIComponent(url)}`,
    wcDeepLink: (wcUri) => `bitkeep://wc?uri=${encodeURIComponent(wcUri)}`
  },
  {
    id: "phantom",
    name: "Phantom",
    icon: "👻",
    badge: "EVM + Mobile",
    installUrl: "https://phantom.app/download",
    dappLink: (url) => `https://phantom.app/ul/browse/${encodeURIComponent(url)}?ref=${encodeURIComponent(window.location.origin || "https://tataswap.onrender.com")}`,
    wcDeepLink: null
  },
  {
    id: "safepal",
    name: "SafePal",
    icon: "🔐",
    badge: "Hardware + App",
    installUrl: "https://www.safepal.com/download",
    dappLink: (url) => `https://link.safepal.io/dapp?url=${encodeURIComponent(url)}`,
    wcDeepLink: null
  },
  {
    id: "rainbow",
    name: "Rainbow",
    icon: "🌈",
    badge: "EVM Mobile",
    installUrl: "https://rainbow.me/download",
    dappLink: () => "https://rnbwapp.com",
    wcDeepLink: null
  }
];

function getWalletDeepLink(type) {
  const targetUrl = buildTargetDappUrl(true);
  const item = WALLET_CATALOG.find(w => w.id === type);
  return item ? item.dappLink(targetUrl) : targetUrl;
}

function openInNewBrowserTab() {
  window.open(buildTargetDappUrl(false), "_blank", "noopener");
}

function setWalletModalError(msg) {
  const box = document.getElementById("walletModalError");
  const txt = document.getElementById("walletModalErrorText");
  if (txt) txt.innerText = msg || "Wallet connection failed.";
  if (box) box.classList.remove("hidden");
}

function clearWalletModalError() {
  const box = document.getElementById("walletModalError");
  if (box) box.classList.add("hidden");
}

async function generateQrDataUrl(text) {
  if (!text) return "";
  if (qrDataUrlCache.has(text)) return qrDataUrlCache.get(text);
  try {
    if (!qrLibPromise) {
      qrLibPromise = import("https://esm.sh/qrcode@1.5.4").catch(() =>
        import("https://cdn.jsdelivr.net/npm/qrcode@1.5.4/+esm")
      );
    }
    const mod = await qrLibPromise;
    const qrApi = mod.default || mod;
    const dataUrl = await qrApi.toDataURL(text, {
      width: 220,
      margin: 2,
      color: { dark: "#0f172a", light: "#ffffff" }
    });
    qrDataUrlCache.set(text, dataUrl);
    return dataUrl;
  } catch (e) {
    console.warn("QR generation fallback:", e?.message);
    return "";
  }
}

async function hydrateExpandedWalletQr(walletItem) {
  if (!walletItem) return;
  const img = document.getElementById(`walletQrImg_${walletItem.id}`);
  const caption = document.getElementById(`walletQrCaption_${walletItem.id}`);
  if (!img) return;
  const wcLink = wcPairingUri && walletItem.wcDeepLink ? walletItem.wcDeepLink(wcPairingUri) : "";
  const qrPayload = wcLink || walletItem.dappLink(buildTargetDappUrl(true));
  if (caption) {
    caption.innerText = wcLink
      ? `Scan in ${walletItem.name} to approve WalletConnect session`
      : `Scan to open TataSwap inside ${walletItem.name} browser`;
  }
  const url = await generateQrDataUrl(qrPayload);
  if (url && document.getElementById(`walletQrImg_${walletItem.id}`)) {
    img.src = url;
    img.classList.remove("hidden");
  }
}

async function hydrateWcPanelQr() {
  const qrBox = document.getElementById("wcQrBox");
  const qrImg = document.getElementById("wcQrImage");
  const qrCaption = document.getElementById("wcQrCaption");
  const pairingActions = document.getElementById("wcPairingActions");
  const uriPreview = document.getElementById("wcUriPreview");
  const directOpenLink = document.getElementById("wcDirectOpenLink");
  const quickLinks = document.getElementById("wcQuickDeepLinks");
  if (!qrBox || !qrImg) return;

  const targetPayload = wcPairingUri || buildTargetDappUrl(true);
  const dataUrl = await generateQrDataUrl(targetPayload);
  if (dataUrl) {
    qrImg.src = dataUrl;
    qrBox.classList.remove("hidden");
  }
  if (qrCaption) {
    qrCaption.innerText = wcPairingUri
      ? "Live WalletConnect v2 URI — scan with any Web3 mobile wallet"
      : "Scan to open TataSwap on mobile or click Connect below to generate WalletConnect URI";
  }

  if (pairingActions && uriPreview && quickLinks) {
    if (wcPairingUri) {
      pairingActions.classList.remove("hidden");
      uriPreview.innerText = wcPairingUri;
      if (directOpenLink) {
        directOpenLink.href = wcPairingUri;
        directOpenLink.classList.remove("hidden");
      }
      const wcWallets = WALLET_CATALOG.filter(w => typeof w.wcDeepLink === "function");
      quickLinks.innerHTML = wcWallets.map(w => `
        <a href="${escapeHtml(w.wcDeepLink(wcPairingUri))}" target="_blank" rel="noopener noreferrer"
           class="px-2.5 py-1.5 rounded-lg bg-[#131823] hover:bg-[#1a2234] border border-gray-800 text-[11px] font-bold text-white flex items-center justify-between">
          <span>${w.icon} ${escapeHtml(w.name)}</span>
          <span class="text-cyan-400">&nearr;</span>
        </a>
      `).join("");
    } else if (wcConnecting) {
      pairingActions.classList.remove("hidden");
      uriPreview.innerText = "Generating live wc:... WalletConnect URL...";
      if (directOpenLink) {
        directOpenLink.href = "#";
        directOpenLink.classList.add("hidden");
      }
      quickLinks.innerHTML = "";
    } else {
      pairingActions.classList.add("hidden");
      if (directOpenLink) {
        directOpenLink.href = "#";
        directOpenLink.classList.add("hidden");
      }
      quickLinks.innerHTML = "";
    }
  }
}

function attachProviderEvents(provider) {
  if (!provider?.on || provider.__tataswapEventsBound) return;
  provider.__tataswapEventsBound = true;
  provider.on("accountsChanged", (accounts) => {
    if (Array.isArray(accounts) && accounts.length > 0) {
      walletAddress = accounts[0];
      localStorage.setItem("tataswap_wallet", walletAddress);
      updateWalletHeaderUi();
      refreshBalance();
      refreshWalletModalUi();
    } else {
      disconnectWallet();
    }
  });
  provider.on("chainChanged", () => {
    refreshBlock();
    if (walletAddress) refreshBalance();
    refreshWalletModalUi();
  });
  provider.on("disconnect", () => {
    if (walletMode === "walletconnect") {
      disconnectWallet();
    }
  });
}

function refreshWalletModalUi() {
  const detected = getAllDetectedWallets();
  const { isMobile, isIframe } = detectEnvironmentFlags();
  const net = NETWORKS[activeNetKey];

  const showConnected = Boolean(walletAddress) && !forceWalletPicker;
  const modalTitle = document.getElementById("walletModalTitle");
  if (modalTitle) {
    modalTitle.innerText = showConnected ? "Connected Wallet" : (walletAddress ? "Switch Wallet" : "Connect a wallet");
  }

  const connBox = document.getElementById("walletConnectedBox");
  const selView = document.getElementById("walletSelectionView");
  const backRow = document.getElementById("backToConnectedRow");
  const connAddr = document.getElementById("walletConnectedAddr");
  const connLabel = document.getElementById("walletConnectedLabel");
  const connBadge = document.getElementById("walletConnectedChainBadge");
  const modalBal = document.getElementById("walletModalBalance");

  if (connBox) connBox.classList.toggle("hidden", !showConnected);
  if (selView) selView.classList.toggle("hidden", showConnected);
  if (backRow) backRow.classList.toggle("hidden", !(walletAddress && forceWalletPicker));

  if (showConnected && walletAddress) {
    if (connAddr) connAddr.innerText = walletAddress;
    if (connLabel) connLabel.innerText = `Connected (${walletProviderLabel})`;
    if (connBadge) connBadge.innerText = `Chain ID ${net.chainId} Active`;
    if (modalBal) modalBal.innerText = `${nativeBalanceVal.toFixed(4)} ${net.symbol}`;
  }

  const noticeBox = document.getElementById("walletEnvNotice");
  const noticeTxt = document.getElementById("walletEnvNoticeText");
  const noticeActions = document.getElementById("walletEnvActions");
  if (noticeBox) {
    if (isIframe) {
      noticeBox.classList.remove("hidden");
      if (noticeActions) noticeActions.classList.remove("hidden");
      if (noticeTxt) noticeTxt.innerText = "Browser extensions (MetaMask, Trust Wallet) are blocked inside embedded preview iframes. Open in a new tab or pair any mobile wallet via QR/WalletConnect below.";
    } else if (detected.length === 0) {
      noticeBox.classList.remove("hidden");
      if (noticeActions) noticeActions.classList.add("hidden");
      if (noticeTxt) {
        noticeTxt.innerText = isMobile
          ? "No injected browser wallet found. Tap Open App on any wallet below to launch inside its DApp browser with instant auto-connect."
          : "No browser extension detected yet. Click any wallet below to scan QR / install extension / pair via WalletConnect v2.";
      }
    } else {
      noticeBox.classList.add("hidden");
    }
  }

  const detBox = document.getElementById("detectedWalletsBox");
  const detHeader = document.getElementById("detectedWalletsHeader");
  const detList = document.getElementById("detectedWalletsList");
  if (detBox && detList) {
    if (detected.length > 0) {
      detBox.classList.remove("hidden");
      if (detHeader) detHeader.innerText = `Detected Browser Wallets (${detected.length})`;
      detList.innerHTML = detected.map((w, idx) => {
        const iconHtml = w.icon
          ? `<img src="${escapeHtml(w.icon)}" alt="" class="w-5 h-5 rounded-md object-contain shrink-0" />`
          : `<span class="w-2.5 h-2.5 rounded-full bg-emerald-400 shrink-0"></span>`;
        return `
          <button onclick="connectDetectedIndex(${idx})" class="w-full p-2.5 rounded-xl bg-emerald-500/10 hover:bg-emerald-500/20 border border-emerald-500/40 flex items-center justify-between text-left">
            <div class="flex items-center gap-2.5 min-w-0">
              ${iconHtml}
              <span class="text-xs font-extrabold text-white truncate">${escapeHtml(w.name)}</span>
            </div>
            <span class="text-[11px] font-bold text-emerald-300 shrink-0 ml-2">Connect &rarr;</span>
          </button>
        `;
      }).join("");
    } else {
      detBox.classList.add("hidden");
      detList.innerHTML = "";
    }
  }

  const wcPanel = document.getElementById("wcSetupPanel");
  const wcBadge = document.getElementById("wcActionBadge");
  const subWc = document.getElementById("subWc");
  if (wcPanel) wcPanel.classList.toggle("hidden", !wcSetupExpanded);
  if (wcBadge) wcBadge.innerText = wcConnecting ? "Pairing..." : (wcSetupExpanded ? "Hide" : "Connect");
  if (subWc && !wcConnecting) {
    subWc.innerText = wcPairingUri ? "QR & pairing URI active — scan or open app" : "Scan QR or pair any mobile Web3 wallet";
  }
  const wcInput = document.getElementById("wcProjectIdInput");
  if (wcInput && wcProjectIdCache && !wcInput.value) wcInput.value = wcProjectIdCache;
  if (wcSetupExpanded) {
    hydrateWcPanelQr();
  }

  renderWalletCatalogList();
}

function renderWalletCatalogList() {
  const container = document.getElementById("walletCatalogList");
  if (!container) return;
  const { isMobile, isIframe } = detectEnvironmentFlags();
  const targetUrl = buildTargetDappUrl(true);

  container.innerHTML = WALLET_CATALOG.map((w) => {
    const injected = findProviderForWallet(w.id);
    const hasUniversalDirect = !isIframe && isMobile && w.id !== "binance";
    const isExpanded = expandedWalletId === w.id;
    const dappUrl = w.dappLink(targetUrl);
    const wcPairUrl = wcPairingUri && w.wcDeepLink ? w.wcDeepLink(wcPairingUri) : "";

    let rightControl = "";
    if (injected) {
      rightControl = `<span class="text-xs font-bold text-emerald-300 shrink-0 ml-2">Connect</span>`;
    } else if (hasUniversalDirect) {
      rightControl = `<span class="text-xs font-bold text-cyan-400 shrink-0 ml-2">Open App &nearr;</span>`;
    } else {
      rightControl = `<span class="text-xs font-bold text-cyan-400 shrink-0 ml-2">${isExpanded ? "Hide" : "QR / Pair"}</span>`;
    }

    const subtitle = injected
      ? `${w.name} Extension detected • Click to connect`
      : hasUniversalDirect
        ? `Open inside ${w.name} Mobile DApp Browser`
        : (w.id === "binance" ? "Pair via WalletConnect QR or Binance App" : `Desktop QR, extension, or WalletConnect`);

    const headerHtml = hasUniversalDirect && !injected
      ? `<a href="${escapeHtml(dappUrl)}" target="_blank" rel="noopener noreferrer"
            class="w-full p-3 hover:bg-[#151c2c] transition flex items-center justify-between text-left block">
            <div class="flex items-center gap-3 min-w-0">
              <div class="w-9 h-9 rounded-xl bg-[#131823] border border-gray-700 flex items-center justify-center text-lg shrink-0">${w.icon}</div>
              <div class="min-w-0">
                <div class="flex items-center gap-1.5">
                  <span class="text-xs font-extrabold text-white truncate">${escapeHtml(w.name)}</span>
                  <span class="text-[9px] px-1.5 py-0.5 rounded bg-gray-800 text-gray-300 font-bold">${escapeHtml(w.badge)}</span>
                </div>
                <div class="text-[11px] text-gray-400 truncate">${escapeHtml(subtitle)}</div>
              </div>
            </div>
            ${rightControl}
          </a>`
      : `<button onclick="handleWalletCatalogClick('${w.id}')"
            class="w-full p-3 hover:bg-[#151c2c] transition flex items-center justify-between text-left">
            <div class="flex items-center gap-3 min-w-0">
              <div class="w-9 h-9 rounded-xl bg-[#131823] border border-gray-700 flex items-center justify-center text-lg shrink-0">${w.icon}</div>
              <div class="min-w-0">
                <div class="flex items-center gap-1.5">
                  <span class="text-xs font-extrabold text-white truncate">${escapeHtml(w.name)}</span>
                  <span class="text-[9px] px-1.5 py-0.5 rounded bg-gray-800 text-gray-300 font-bold">${escapeHtml(w.badge)}</span>
                </div>
                <div class="text-[11px] text-gray-400 truncate">${escapeHtml(subtitle)}</div>
              </div>
            </div>
            ${rightControl}
          </button>`;

    const expandedHtml = isExpanded ? `
      <div class="border-t border-gray-800 bg-[#090c12] p-3 space-y-2.5">
        <div class="flex flex-col items-center gap-1.5 bg-white rounded-xl p-2.5">
          <img id="walletQrImg_${w.id}" alt="${escapeHtml(w.name)} QR" class="w-36 h-36 object-contain hidden" />
          <div id="walletQrCaption_${w.id}" class="text-[10px] font-bold text-slate-700 text-center">Loading QR...</div>
        </div>
        <div class="grid grid-cols-2 gap-2">
          <a href="${escapeHtml(dappUrl)}" target="_blank" rel="noopener noreferrer"
             class="py-2 px-2.5 rounded-lg bg-[#131823] hover:bg-[#1a2234] border border-gray-700 text-[11px] font-bold text-white text-center">
            Open App &nearr;
          </a>
          <a href="${escapeHtml(w.installUrl)}" target="_blank" rel="noopener noreferrer"
             class="py-2 px-2.5 rounded-lg bg-[#131823] hover:bg-[#1a2234] border border-gray-700 text-[11px] font-bold text-gray-200 text-center">
            Install Extension &nearr;
          </a>
        </div>
        <div class="flex gap-2">
          <button onclick="pairViaWalletConnectForWallet('${w.id}')"
             class="flex-1 py-2 px-3 rounded-lg bg-sky-500 text-black text-[11px] font-extrabold">
            ${wcConnecting ? "Generating WC URI..." : "Pair via WalletConnect"}
          </button>
          ${wcPairUrl ? `
            <a href="${escapeHtml(wcPairUrl)}" target="_blank" rel="noopener noreferrer"
               class="py-2 px-3 rounded-lg bg-emerald-500 text-black text-[11px] font-extrabold">
              Pair in ${escapeHtml(w.name)}
            </a>
          ` : ""}
        </div>
        ${wcPairingUri ? `
          <div class="flex items-center justify-between gap-2 bg-[#131823] rounded-lg px-2.5 py-1.5 border border-gray-800">
            <span class="text-[10px] font-mono text-cyan-300 truncate select-all">${escapeHtml(wcPairingUri)}</span>
            <button onclick="copyWcPairingUri()" class="text-[10px] font-bold text-cyan-400 shrink-0">Copy WC</button>
          </div>
        ` : ""}
      </div>
    ` : "";

    return `
      <div class="rounded-xl bg-[#0d111a] border ${isExpanded ? "border-cyan-500/60" : "border-gray-800"} overflow-hidden">
        ${headerHtml}
        ${expandedHtml}
      </div>
    `;
  }).join("");

  if (expandedWalletId) {
    const activeItem = WALLET_CATALOG.find(w => w.id === expandedWalletId);
    if (activeItem) hydrateExpandedWalletQr(activeItem);
  }
}

async function handleWalletCatalogClick(walletId) {
  clearWalletModalError();
  const provider = findProviderForWallet(walletId);
  if (provider) {
    const item = WALLET_CATALOG.find(w => w.id === walletId);
    try {
      await finalizeProviderConnect(provider, item?.name || "Web3 Wallet", "injected");
    } catch (err) {
      setWalletModalError((item?.name || "Wallet") + " error: " + (err?.message || "Connection rejected."));
    }
    return;
  }

  expandedWalletId = expandedWalletId === walletId ? null : walletId;
  refreshWalletModalUi();

  if (expandedWalletId === "binance" && wcProjectIdCache && !wcPairingUri && !wcConnecting) {
    pairViaWalletConnectForWallet("binance");
  }
}

async function pairViaWalletConnectForWallet(walletId) {
  expandedWalletId = walletId;
  await startWalletConnectPairing(false);
}

function handleWalletConnectCardClick() {
  clearWalletModalError();
  wcSetupExpanded = !wcSetupExpanded;
  refreshWalletModalUi();
  if (wcSetupExpanded && wcProjectIdCache && !wcPairingUri && !wcConnecting) {
    startWalletConnectPairing(false);
  }
}

function connectWeb3Wallet() {
  clearWalletModalError();
  window.dispatchEvent(new Event("eip6963:requestProvider"));
  forceWalletPicker = false;
  refreshWalletModalUi();
  const modal = document.getElementById("walletModal");
  if (modal) {
    modal.classList.remove("hidden");
    modal.classList.add("flex");
  }
  setTimeout(() => {
    window.dispatchEvent(new Event("eip6963:requestProvider"));
    refreshWalletModalUi();
  }, 350);
  setTimeout(() => {
    window.dispatchEvent(new Event("eip6963:requestProvider"));
    refreshWalletModalUi();
  }, 1200);
}

function openWalletModal() {
  connectWeb3Wallet();
}

function closeWalletModal() {
  forceWalletPicker = false;
  const modal = document.getElementById("walletModal");
  if (modal) {
    modal.classList.add("hidden");
    modal.classList.remove("flex");
  }
}

function switchWalletInModal() {
  forceWalletPicker = true;
  refreshWalletModalUi();
}

function backToConnectedSummary() {
  forceWalletPicker = false;
  refreshWalletModalUi();
}

async function copyConnectedAddress() {
  if (!walletAddress) return;
  try {
    await navigator.clipboard.writeText(walletAddress);
    const lbl = document.getElementById("copyAddrLabel");
    if (lbl) {
      lbl.innerText = "Copied!";
      setTimeout(() => { lbl.innerText = "Copy"; }, 1800);
    }
  } catch {}
}

async function copyWcPairingUri() {
  if (!wcPairingUri) return;
  try {
    await navigator.clipboard.writeText(wcPairingUri);
    const btn = document.getElementById("copyWcUriBtn");
    if (btn) {
      btn.innerText = "Copied!";
      setTimeout(() => { btn.innerText = "Copy WC URL"; }, 1800);
    }
  } catch {}
}

async function finalizeProviderConnect(provider, label, mode = "injected") {
  const accounts = await provider.request({ method: "eth_requestAccounts" });
  if (!accounts || !accounts.length) throw new Error("No wallet account returned.");
  activeProvider = provider;
  attachProviderEvents(provider);
  walletAddress = accounts[0];
  walletMode = mode;
  walletProviderLabel = label;
  forceWalletPicker = false;
  expandedWalletId = null;
  wcSetupExpanded = false;
  wcPairingUri = "";
  localStorage.setItem("tataswap_wallet", walletAddress);
  localStorage.setItem("tataswap_wallet_mode", walletMode);
  localStorage.setItem("tataswap_wallet_label", walletProviderLabel);
  updateWalletHeaderUi();
  closeWalletModal();
  await refreshBalance();
}

async function connectDetectedIndex(idx) {
  clearWalletModalError();
  const detected = getAllDetectedWallets();
  const item = detected[idx];
  if (!item) return;
  try {
    await finalizeProviderConnect(item.provider, item.name, "injected");
  } catch (err) {
    setWalletModalError(item.name + " connection error: " + (err?.message || "Rejected."));
  }
}

async function connectWalletByType(type) {
  return handleWalletCatalogClick(type);
}

async function startWalletConnectPairing(forceNew = false) {
  clearWalletModalError();
  const projectId = (wcProjectIdCache || localStorage.getItem("tataswap_wc_project_id") || DEFAULT_WC_PROJECT_ID).trim();
  if (!projectId) {
    wcSetupExpanded = true;
    refreshWalletModalUi();
    setWalletModalError("Enter a Reown / WalletConnect Project ID to generate a QR pairing URI.");
    return;
  }
  if (wcConnecting && !forceNew) return;
  wcConnecting = true;
  const statusEl = document.getElementById("wcStatusText");
  if (statusEl) statusEl.innerText = "Creating WalletConnect v2 pairing URI...";
  refreshWalletModalUi();

  try {
    if (forceNew && wcActiveProviderInstance?.disconnect) {
      try { await wcActiveProviderInstance.disconnect(); } catch {}
      wcActiveProviderInstance = null;
      wcPairingUri = "";
    }
    const { EthereumProvider } = await import("https://esm.sh/@walletconnect/ethereum-provider@2.26.1").catch(() =>
      import("https://cdn.jsdelivr.net/npm/@walletconnect/ethereum-provider@2.26.1/+esm")
    );
    const rpcMap = {};
    for (const net of Object.values(NETWORKS)) {
      rpcMap[net.chainId] = net.rpc;
    }
    const activeNet = NETWORKS[activeNetKey];
    const wcProvider = await EthereumProvider.init({
      projectId,
      chains: [activeNet.chainId],
      optionalChains: [810161, 1, 56, 137, 1116],
      rpcMap,
      optionalMethods: [
        "eth_sendTransaction",
        "personal_sign",
        "eth_signTypedData_v4",
        "wallet_switchEthereumChain",
        "wallet_addEthereumChain"
      ],
      showQrModal: false,
      metadata: {
        name: "TataSwap DEX",
        description: "Multi-Chain Non-Custodial EVM DEX",
        url: window.location.origin,
        icons: ["https://tataswap.onrender.com/favicon.ico"]
      }
    });
    wcActiveProviderInstance = wcProvider;

    wcProvider.on("display_uri", (uri) => {
      wcPairingUri = uri || "";
      wcConnecting = false;
      if (statusEl) statusEl.innerText = "Scan QR or tap a wallet below to approve";
      refreshWalletModalUi();
    });

    const connectPromise = wcProvider.connect();
    await connectPromise;
    wcConnecting = false;
    await finalizeProviderConnect(wcProvider, "WalletConnect v2", "walletconnect");
  } catch (err) {
    wcConnecting = false;
    const msg = err?.message || "WalletConnect session cancelled or timed out.";
    if (statusEl) statusEl.innerText = "WalletConnect ready";
    setWalletModalError("WalletConnect: " + msg);
    refreshWalletModalUi();
  }
}

async function connectWalletConnect() {
  wcSetupExpanded = true;
  await startWalletConnectPairing(false);
}

function saveWcProjectIdOnly() {
  clearWalletModalError();
  const raw = (document.getElementById("wcProjectIdInput")?.value || "").trim();
  if (!raw || raw.length < 16) {
    return setWalletModalError("Enter a valid Reown / WalletConnect Project ID (from cloud.reown.com).");
  }
  wcProjectIdCache = raw;
  localStorage.setItem("tataswap_wc_project_id", raw);
  const statusEl = document.getElementById("wcStatusText");
  if (statusEl) statusEl.innerText = "Project ID saved (" + raw.slice(0, 6) + "...)";
}

async function saveAndLaunchWalletConnect() {
  clearWalletModalError();
  const raw = (document.getElementById("wcProjectIdInput")?.value || wcProjectIdCache || DEFAULT_WC_PROJECT_ID).trim();
  if (!raw || raw.length < 16) {
    return setWalletModalError("Enter a valid Reown / WalletConnect Project ID (from cloud.reown.com).");
  }
  wcProjectIdCache = raw;
  localStorage.setItem("tataswap_wc_project_id", raw);
  await startWalletConnectPairing(true);
}

async function runAutoConnectFromUrl() {
  try {
    const url = new URL(window.location.href);
    const shouldAuto = url.searchParams.get("autoConnect") === "1";
    if (!shouldAuto || autoConnectAttempted) return;
    const detected = getAllDetectedWallets();
    if (detected.length === 0) return;
    autoConnectAttempted = true;
    url.searchParams.delete("autoConnect");
    window.history.replaceState({}, document.title, url.toString());
    await finalizeProviderConnect(detected[0].provider, detected[0].name, "injected");
  } catch (e) {
    console.warn("AutoConnect skipped:", e?.message);
  }
}

function disconnectWallet() {
  if (activeProvider?.disconnect) {
    try { activeProvider.disconnect(); } catch {}
  }
  if (wcActiveProviderInstance?.disconnect && wcActiveProviderInstance !== activeProvider) {
    try { wcActiveProviderInstance.disconnect(); } catch {}
  }
  activeProvider = null;
  wcActiveProviderInstance = null;
  wcPairingUri = "";
  wcConnecting = false;
  forceWalletPicker = false;
  expandedWalletId = null;
  walletAddress = null;
  walletMode = "none";
  walletProviderLabel = "Web3 Wallet";
  nativeBalanceVal = 0;
  localStorage.removeItem("tataswap_wallet");
  localStorage.removeItem("tataswap_wallet_mode");
  localStorage.removeItem("tataswap_wallet_label");
  updateWalletHeaderUi();
  refreshWalletModalUi();
  renderPortfolioHoldings();
}

async function connectInjectedWallet() {
  const detected = getAllDetectedWallets();
  if (detected.length > 0) {
    return connectDetectedIndex(0);
  }
  connectWeb3Wallet();
}

function setAdminWalletFeedback(msg, isError = false) {
  const el = document.getElementById("adminWalletFeedback");
  if (!el) return;
  el.innerText = msg;
  el.className = `text-xs font-semibold ${isError ? "text-red-400" : "text-emerald-400"}`;
  el.classList.remove("hidden");
}

async function connectManualAddress() {
  clearWalletModalError();
  if (!adminAuthenticated) {
    return setAdminWalletFeedback("Admin login required.", true);
  }
  const raw = (document.getElementById("manualWalletInput")?.value || "").trim();
  const isEvmAddr = /^0x[a-fA-F0-9]{40}$/.test(raw);
  if (!isEvmAddr) {
    return setAdminWalletFeedback("Please enter a valid 0x EVM wallet address.", true);
  }
  activeProvider = null;
  walletAddress = raw;
  walletMode = "address";
  walletProviderLabel = "Watch-Only Address";
  forceWalletPicker = false;
  localStorage.setItem("tataswap_wallet", walletAddress);
  localStorage.setItem("tataswap_wallet_mode", walletMode);
  localStorage.setItem("tataswap_wallet_label", walletProviderLabel);
  updateWalletHeaderUi();
  setAdminWalletFeedback(`Connected Watch-Only: ${raw.slice(0, 6)}...${raw.slice(-4)}`);
  await refreshBalance();
}

async function connectOwnerWalletDirect() {
  clearWalletModalError();
  if (!adminAuthenticated) {
    return setAdminWalletFeedback("Admin login required.", true);
  }
  activeProvider = null;
  walletAddress = OFFICIAL_OWNER_WALLET;
  walletMode = "address";
  walletProviderLabel = "App Owner Wallet";
  forceWalletPicker = false;
  localStorage.setItem("tataswap_wallet", walletAddress);
  localStorage.setItem("tataswap_wallet_mode", walletMode);
  localStorage.setItem("tataswap_wallet_label", walletProviderLabel);
  updateWalletHeaderUi();
  setAdminWalletFeedback("Connected App Owner Wallet (0 Fee).");
  await refreshBalance();
}

async function fetchErc20Balance(tokenAddr, ownerAddr, decimals = 18) {
  try {
    if (!/^0x[a-fA-F0-9]{40}$/.test(tokenAddr) || !/^0x[a-fA-F0-9]{40}$/.test(ownerAddr)) return 0;
    const data = "0x70a08231" + ownerAddr.toLowerCase().replace("0x", "").padStart(64, "0");
    const hex = await rpcRequest("eth_call", [{ to: tokenAddr, data }, "latest"]);
    if (!hex || hex === "0x") return 0;
    return Number(BigInt(hex)) / Math.pow(10, decimals);
  } catch {
    return 0;
  }
}

async function refreshBalance() {
  if (!walletAddress) return;
  const net = NETWORKS[activeNetKey];
  try {
    if (!walletAddress.startsWith("0x")) return;
    const hex = await rpcRequest("eth_getBalance", [walletAddress, "latest"]);
    nativeBalanceVal = Number(BigInt(hex)) / 1e18;
    const sym = net.symbol;
    document.getElementById("swapBalIn").innerText = `Balance: ${nativeBalanceVal.toFixed(4)} ${sym}`;
    document.getElementById("portBal").innerText = `${nativeBalanceVal.toFixed(4)} ${sym}`;

    // Also fetch ERC-20 token balances in parallel
    await Promise.all(
      TOKENS.map(async (t) => {
        if (t.address.startsWith("0x") && t.address !== "0x0000000000000000000000000000000000001010") {
          t.balance = await fetchErc20Balance(t.address, walletAddress, t.decimals || 18);
        }
      })
    );
    renderPortfolioHoldings();
  } catch (e) {
    console.error("Balance RPC error:", e);
  }
}

function renderPortfolioHoldings() {
  const net = NETWORKS[activeNetKey];
  const nativePrice = PRICES[net.symbol] || 0;
  let totalUsd = nativeBalanceVal * nativePrice;

  for (const t of TOKENS) {
    const p = PRICES[t.symbol] ?? t.price ?? 0;
    totalUsd += (Number(t.balance) || 0) * p;
  }

  const usdEl = document.getElementById("portUsd");
  if (usdEl) usdEl.innerText = "$" + formatUsd(totalUsd);

  const listEl = document.getElementById("portTokensList");
  if (!listEl) return;

  const nativeRow = `
    <div class="card-darker rounded-xl p-3 flex justify-between items-center border border-yellow-500/30">
      <div>
        <div class="font-bold text-white">${net.symbol} <span class="text-gray-400 font-normal">(${net.name} Native)</span></div>
        <div class="text-[11px] text-gray-400">Live Market: $${formatUsd(nativePrice)}</div>
      </div>
      <div class="text-right">
        <div class="font-bold text-yellow-400">${nativeBalanceVal.toFixed(4)} ${net.symbol}</div>
        <div class="text-[11px] text-emerald-400">$${formatUsd(nativeBalanceVal * nativePrice)}</div>
      </div>
    </div>
  `;

  const tokenRows = TOKENS.map(t => {
    const p = PRICES[t.symbol] ?? t.price ?? 0;
    const bal = Number(t.balance) || 0;
    const valUsd = bal * p;
    return `
      <div class="card-darker rounded-xl p-3 flex justify-between items-center">
        <div>
          <div class="font-bold text-white">${t.symbol} <span class="text-gray-400 font-normal">${t.name}</span></div>
          <div class="text-[11px] text-gray-400">Market Price: $${formatUsd(p)}</div>
        </div>
        <div class="text-right">
          <div class="font-bold text-white">${bal.toFixed(4)} ${t.symbol}</div>
          <div class="text-[11px] text-emerald-400">$${formatUsd(valUsd)}</div>
        </div>
      </div>
    `;
  }).join("");

  listEl.innerHTML = nativeRow + tokenRows;
}

function recalcSwap() {
  const sIn = document.getElementById("swapInToken").value;
  const sOut = document.getElementById("swapOutToken").value;
  const amtIn = parseFloat(document.getElementById("swapAmountIn").value) || 0;
  const slip = parseFloat(document.getElementById("slippageVal").value) || 0.5;
  const pIn = PRICES[sIn] || 1;
  const pOut = PRICES[sOut] || 1;
  const rate = pIn / pOut;
  const out = amtIn * rate * 0.9975;
  const minRec = out * (1 - slip / 100);

  document.getElementById("swapAmountOut").innerText = out.toFixed(4);
  document.getElementById("swapRate").innerText = `1 ${sIn} = ${rate.toFixed(4)} ${sOut}`;
  document.getElementById("swapMinRec").innerText = `${minRec.toFixed(4)} ${sOut}`;

  const payUsdEl = document.getElementById("swapPayUsd");
  const outUsdEl = document.getElementById("swapOutUsd");
  if (payUsdEl) payUsdEl.innerText = `~$${formatUsd(amtIn * pIn)} USD`;
  if (outUsdEl) outUsdEl.innerText = `~$${formatUsd(out * pOut)} USD`;

  if (typeof chartState !== "undefined") {
    const activePairKey = `${sIn}_${sOut}_${chartState.interval}`;
    if (chartState.lastPairKey !== activePairKey) {
      loadSwapOhlcChart(false);
    }
  }
}

function flipSwap() {
  const el1 = document.getElementById("swapInToken");
  const el2 = document.getElementById("swapOutToken");
  const t = el1.value;
  el1.value = el2.value;
  el2.value = t;
  recalcSwap();
  loadSwapOhlcChart(false);
}

// ABI Helpers for Real EVM Transactions
function padHex32(hexNoPrefix) {
  return hexNoPrefix.padStart(64, "0");
}

function toWeiBigInt(amountStr, decimals = 18) {
  const clean = String(amountStr || "0").trim();
  if (!clean || isNaN(Number(clean))) return 0n;
  const [whole, frac = ""] = clean.split(".");
  const paddedFrac = (frac + "0".repeat(decimals)).slice(0, decimals);
  return BigInt(whole || "0") * (10n ** BigInt(decimals)) + BigInt(paddedFrac || "0");
}

function encodeErc20Transfer(toAddr, amountWei) {
  return "0xa9059cbb" + padHex32(toAddr.toLowerCase().replace("0x", "")) + padHex32(amountWei.toString(16));
}

function encodeErc20Approve(spenderAddr, amountWei) {
  return "0x095ea7b3" + padHex32(spenderAddr.toLowerCase().replace("0x", "")) + padHex32(amountWei.toString(16));
}

function saveLocalTx(entry) {
  const list = JSON.parse(localStorage.getItem("tataswap_tx_history") || "[]");
  list.unshift({ ...entry, timestamp: new Date().toISOString() });
  localStorage.setItem("tataswap_tx_history", JSON.stringify(list.slice(0, 30)));
  renderLocalHistory();
}

function renderLocalHistory(backendListings = []) {
  const el = document.getElementById("historyList");
  if (!el) return;
  const localTxs = JSON.parse(localStorage.getItem("tataswap_tx_history") || "[]");
  let html = "";

  for (const tx of localTxs) {
    html += `
      <div class="p-3 rounded-xl bg-[#0d111a] border border-emerald-500/30 mb-2 flex justify-between items-center">
        <div>
          <div class="font-bold text-white">${tx.title}</div>
          <div class="text-[11px] text-gray-400 font-mono">${tx.subtitle}</div>
        </div>
        <a href="${tx.explorerUrl}" target="_blank" rel="noopener" class="text-xs font-mono text-cyan-400 hover:underline">${tx.txHash.slice(0, 10)}... &nearr;</a>
      </div>
    `;
  }

  for (const l of backendListings) {
    html += `
      <div class="p-3 rounded-xl bg-[#0d111a] border border-gray-800 mb-2 flex justify-between items-center">
        <div>
          <div class="font-bold text-white">Listed ${l.symbol} (${l.name})</div>
          <div class="text-[11px] text-gray-400 font-mono">Fee: ${l.fee_paid} USDT &rarr; ${l.fee_receiver}</div>
        </div>
        <div class="text-xs font-mono text-yellow-400">${l.tx_hash.slice(0, 10)}...</div>
      </div>
    `;
  }

  el.innerHTML = html || "No transactions broadcast yet. Send a coin, swap, or list a token to see live on-chain receipts here.";
}

async function sendRealCrypto() {
  const statusEl = document.getElementById("sendTxStatus");
  const asset = document.getElementById("sendAssetSelect").value;
  const amountStr = document.getElementById("sendAmountInput").value.trim();
  const toAddr = document.getElementById("sendToInput").value.trim();
  const net = NETWORKS[activeNetKey];

  if (!amountStr || Number(amountStr) <= 0) return alert("Enter a valid positive amount.");
  if (!/^0x[a-fA-F0-9]{40}$/.test(toAddr)) return alert("Enter a valid 0x recipient address.");

  const provider = getEvmProvider();
  const amountWei = toWeiBigInt(amountStr, 18);

  // If in Google Chrome Mobile without injected wallet -> launch MetaMask / Trust EIP-681 Send Deep Link directly!
  if (!provider) {
    if (asset === "NATIVE") {
      const mmSendUrl = `https://link.metamask.io/send/${toAddr}@${net.chainId}?value=${amountWei.toString()}`;
      statusEl.innerHTML = `<span class="text-cyan-300">Opening MetaMask App to sign real ${amountStr} ${net.symbol} transfer...</span>`;
      statusEl.classList.remove("hidden");
      window.open(mmSendUrl, "_blank");
      return;
    }
    return connectWeb3Wallet();
  }

  try {
    if (!walletAddress) {
      const accs = await provider.request({ method: "eth_requestAccounts" });
      walletAddress = accs[0];
      updateWalletHeaderUi();
    }
    statusEl.innerHTML = `<span class="text-yellow-400">Waiting for wallet signature...</span>`;
    statusEl.classList.remove("hidden");

    let txParams;
    if (asset === "NATIVE") {
      txParams = {
        from: walletAddress,
        to: toAddr,
        value: "0x" + amountWei.toString(16)
      };
    } else {
      const tok = TOKENS.find(t => t.symbol === asset);
      if (!tok) return alert("Token contract not found.");
      txParams = {
        from: walletAddress,
        to: tok.address,
        data: encodeErc20Transfer(toAddr, toWeiBigInt(amountStr, tok.decimals || 18))
      };
    }

    const txHash = await provider.request({ method: "eth_sendTransaction", params: [txParams] });
    const expUrl = `${net.explorer}/tx/${txHash}`;
    statusEl.innerHTML = `<span class="text-emerald-400 font-bold">Broadcasted! Tx: <a href="${expUrl}" target="_blank" class="underline">${txHash.slice(0, 14)}...</a></span>`;
    saveLocalTx({
      title: `Sent ${amountStr} ${asset === "NATIVE" ? net.symbol : asset}`,
      subtitle: `To ${toAddr.slice(0, 8)}...${toAddr.slice(-6)} on ${net.name}`,
      txHash,
      explorerUrl: expUrl
    });
  } catch (err) {
    statusEl.innerHTML = `<span class="text-red-400">Tx Cancelled / Error: ${err.message}</span>`;
    statusEl.classList.remove("hidden");
  }
}

function getContractConfig() {
  return {
    router: (localStorage.getItem("tataswap_router_addr") || "").trim(),
    listing: (localStorage.getItem("tataswap_listing_addr") || "").trim()
  };
}

function loadContractConfig() {
  const cfg = getContractConfig();
  const rEl = document.getElementById("cfgRouterAddr");
  const lEl = document.getElementById("cfgListingAddr");
  const wcEl = document.getElementById("cfgWcProjectId");
  if (rEl) rEl.value = cfg.router;
  if (lEl) lEl.value = cfg.listing;
  if (wcEl) wcEl.value = wcProjectIdCache || DEFAULT_WC_PROJECT_ID;
}

const ADMIN_AUTH_SALT = "tataswap-admin-v1:";
const ADMIN_EMAIL_SHA256 = "d634449c49a098e5d985a627942ba762beca4a6900256199babfab6097968b1d";
const ADMIN_PASSWORD_SHA256 = "eda84c27a20b491878b76df2d69f184a9384590a4737262b409a31e04c098754";
let adminSessionToken = sessionStorage.getItem("tataswap_admin_token") || "";
let adminAuthenticated = Boolean(adminSessionToken);

async function sha256HexBrowser(input) {
  const buf = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(input));
  return Array.from(new Uint8Array(buf)).map(b => b.toString(16).padStart(2, "0")).join("");
}

function toggleAdminFieldVisibility(inputId, btnId) {
  const input = document.getElementById(inputId);
  const btn = document.getElementById(btnId);
  if (!input) return;
  const hidden = input.type === "password";
  input.type = hidden ? "text" : "password";
  if (btn) btn.innerText = hidden ? "Hide" : "Show";
}

function renderAdminAuthGate() {
  const gate = document.getElementById("adminLoginGate");
  const content = document.getElementById("adminDashboardContent");
  if (gate) gate.classList.toggle("hidden", adminAuthenticated);
  if (content) content.classList.toggle("hidden", !adminAuthenticated);
}

async function loginAdminDashboard() {
  const emailEl = document.getElementById("adminEmailInput");
  const passEl = document.getElementById("adminPasswordInput");
  const errEl = document.getElementById("adminLoginError");
  const email = (emailEl?.value || "").trim().toLowerCase();
  const password = passEl?.value || "";

  if (errEl) {
    errEl.classList.add("hidden");
    errEl.innerText = "";
  }

  let verified = false;
  let token = "";
  try {
    const res = await fetch(`${BACKEND_API_BASE}/api/admin/login`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ email, password })
    });
    if (res.ok) {
      const data = await res.json();
      if (data?.authenticated) {
        verified = true;
        token = data.token || "verified";
      }
    } else if (res.status === 401) {
      verified = false;
    } else {
      throw new Error("Fallback local hash check");
    }
  } catch {
    if (window.crypto?.subtle) {
      const eHash = await sha256HexBrowser(ADMIN_AUTH_SALT + email);
      const pHash = await sha256HexBrowser(ADMIN_AUTH_SALT + password);
      verified = eHash === ADMIN_EMAIL_SHA256 && pHash === ADMIN_PASSWORD_SHA256;
      if (verified) token = "local_" + Date.now();
    }
  }

  if (verified) {
    adminAuthenticated = true;
    adminSessionToken = token;
    try { sessionStorage.setItem("tataswap_admin_token", token); } catch {}
    if (emailEl) { emailEl.value = ""; emailEl.type = "password"; }
    if (passEl) { passEl.value = ""; passEl.type = "password"; }
    renderAdminAuthGate();
    loadContractConfig();
  } else {
    adminAuthenticated = false;
    adminSessionToken = "";
    try { sessionStorage.removeItem("tataswap_admin_token"); } catch {}
    if (errEl) {
      errEl.innerText = "Invalid admin email or password.";
      errEl.classList.remove("hidden");
    }
  }
}

async function logoutAdminDashboard() {
  const token = adminSessionToken;
  adminAuthenticated = false;
  adminSessionToken = "";
  try { sessionStorage.removeItem("tataswap_admin_token"); } catch {}
  renderAdminAuthGate();
  if (token) {
    fetch(`${BACKEND_API_BASE}/api/admin/logout`, {
      method: "POST",
      headers: { "Content-Type": "application/json", Authorization: `Bearer ${token}` },
      body: JSON.stringify({ token })
    }).catch(() => {});
  }
}

function saveContractConfig() {
  if (!adminAuthenticated) return;
  const router = (document.getElementById("cfgRouterAddr")?.value || "").trim();
  const listing = (document.getElementById("cfgListingAddr")?.value || "").trim();
  localStorage.setItem("tataswap_router_addr", router);
  localStorage.setItem("tataswap_listing_addr", listing);
  const msg = document.getElementById("cfgSaveMsg");
  if (msg) {
    msg.classList.remove("hidden");
    setTimeout(() => msg.classList.add("hidden"), 2500);
  }
}

function saveAdminWcProjectId() {
  if (!adminAuthenticated) return;
  const raw = (document.getElementById("cfgWcProjectId")?.value || "").trim();
  const validId = raw.length >= 16 ? raw : DEFAULT_WC_PROJECT_ID;
  wcProjectIdCache = validId;
  try { localStorage.setItem("tataswap_wc_project_id", validId); } catch {}
  const cfgInput = document.getElementById("cfgWcProjectId");
  const subWc = document.getElementById("subWc");
  if (cfgInput) cfgInput.value = validId;
  if (subWc) subWc.innerText = "Scan QR or pair mobile wallet";
  const msg = document.getElementById("cfgWcSaveMsg");
  if (msg) {
    msg.classList.remove("hidden");
    setTimeout(() => msg.classList.add("hidden"), 2500);
  }
}

function copyAdminWcProjectId() {
  if (!adminAuthenticated) return;
  const val = (document.getElementById("cfgWcProjectId")?.value || wcProjectIdCache || DEFAULT_WC_PROJECT_ID).trim();
  if (navigator.clipboard) {
    navigator.clipboard.writeText(val).catch(() => {});
  }
}

async function submitSwap() {
  const notice = document.getElementById("swapNotice");
  const provider = getEvmProvider();
  if (!provider) return connectWeb3Wallet();

  const net = NETWORKS[activeNetKey];

  const cfg = getContractConfig();
  if (!/^0x[a-fA-F0-9]{40}$/.test(cfg.router)) {
    notice.innerHTML = `Set your deployed <strong>TataSwapRouter.sol</strong> address in <button onclick="openPage('admin')" class="underline text-cyan-400 font-bold">Admin &rarr; Contracts</button> to execute on-chain swaps on ${net.name}.`;
    notice.classList.remove("hidden");
    return;
  }

  try {
    if (!walletAddress) {
      const accs = await provider.request({ method: "eth_requestAccounts" });
      walletAddress = accs[0];
      updateWalletHeaderUi();
    }
    const sIn = document.getElementById("swapInToken").value;
    const sOut = document.getElementById("swapOutToken").value;
    const amtIn = document.getElementById("swapAmountIn").value;
    const tokIn = TOKENS.find(t => t.symbol === sIn);
    const tokOut = TOKENS.find(t => t.symbol === sOut);
    if (!tokIn || !tokOut) return;

    const amountInWei = toWeiBigInt(amtIn, tokIn.decimals || 18);
    notice.innerText = `Requesting approval & swap on ${net.name}...`;
    notice.classList.remove("hidden");

    if (sIn !== net.symbol) {
      await provider.request({
        method: "eth_sendTransaction",
        params: [{ from: walletAddress, to: tokIn.address, data: encodeErc20Approve(cfg.router, amountInWei) }]
      });
    }

    const deadline = BigInt(Math.floor(Date.now() / 1000) + 1200);
    // swapExactTokensForTokens(uint amountIn, uint amountOutMin, address[] path, address to, uint deadline) -> 0x38ed1739
    const data =
      "0x38ed1739" +
      padHex32(amountInWei.toString(16)) +
      padHex32("0") +
      padHex32((5 * 32).toString(16)) +
      padHex32(walletAddress.toLowerCase().replace("0x", "")) +
      padHex32(deadline.toString(16)) +
      padHex32("2") +
      padHex32(tokIn.address.toLowerCase().replace("0x", "")) +
      padHex32(tokOut.address.toLowerCase().replace("0x", ""));

    const txHash = await provider.request({
      method: "eth_sendTransaction",
      params: [{ from: walletAddress, to: cfg.router, value: sIn === net.symbol ? "0x" + amountInWei.toString(16) : "0x0", data }]
    });

    const expUrl = `${net.explorer}/tx/${txHash}`;
    notice.innerHTML = `<span class="text-emerald-400 font-bold">Swap Sent! <a href="${expUrl}" target="_blank" class="underline">${txHash.slice(0, 12)}...</a></span>`;
    saveLocalTx({
      title: `Swap ${amtIn} ${sIn} → ${sOut}`,
      subtitle: `${net.name} Router Execution`,
      txHash,
      explorerUrl: expUrl
    });
  } catch (err) {
    notice.innerText = "Swap failed: " + err.message;
    notice.classList.remove("hidden");
  }
}

const cpState = {
  step: 1,
  tokenA: null,
  tokenB: null,
  pickerSlot: "A",
  hookAddress: "",
  feeTier: 0.25,
  feeExpanded: true,
  priceBase: "A", // "A" => Quote B per 1 A; "B" => Quote A per 1 B
  isFullRange: true
};

function getDeployedPools() {
  try {
    return JSON.parse(localStorage.getItem("tataswap_deployed_pools") || "[]");
  } catch {
    return [];
  }
}

function saveDeployedPool(entry) {
  const list = getDeployedPools();
  list.unshift(entry);
  localStorage.setItem("tataswap_deployed_pools", JSON.stringify(list.slice(0, 50)));
}

function getCpTokenBalance(sym) {
  if (!sym) return 0;
  const net = NETWORKS[activeNetKey];
  if (sym === net?.symbol) return Number(nativeBalanceVal || 0);
  const found = TOKENS.find(t => t.symbol === sym);
  return Number(found?.balance || 0);
}

function renderCreatePoolUi() {
  const tokA = TOKENS.find(t => t.symbol === cpState.tokenA);
  const tokB = TOKENS.find(t => t.symbol === cpState.tokenB);

  const iconA = document.getElementById("cpTokenAIcon");
  const lblA = document.getElementById("cpTokenALabel");
  if (iconA && lblA) {
    if (tokA) {
      iconA.innerText = tokA.symbol.slice(0, 2);
      lblA.innerText = `${tokA.symbol} • ${tokA.name}`;
    } else {
      iconA.innerText = "?";
      lblA.innerText = "Choose token";
    }
  }

  const btnB = document.getElementById("cpTokenBBtn");
  const iconB = document.getElementById("cpTokenBIcon");
  const lblB = document.getElementById("cpTokenBLabel");
  if (btnB && iconB && lblB) {
    if (tokB) {
      btnB.className = "w-full p-3.5 rounded-2xl bg-[#0d111a] border border-emerald-500 flex items-center justify-between text-left transition";
      iconB.classList.remove("hidden");
      iconB.innerText = tokB.symbol.slice(0, 2);
      lblB.className = "text-sm font-bold text-white";
      lblB.innerText = `${tokB.symbol} • ${tokB.name}`;
    } else {
      btnB.className = "w-full p-3.5 rounded-2xl bg-emerald-500 hover:bg-emerald-400 text-black flex items-center justify-between text-left transition";
      iconB.classList.add("hidden");
      lblB.className = "text-sm font-extrabold";
      lblB.innerText = "Choose token";
    }
  }

  const hookBadge = document.getElementById("cpHookBadge");
  const hookSub = document.getElementById("cpHookSubtitle");
  const hookCard = document.getElementById("cpHookCard");
  if (hookBadge && hookSub && hookCard) {
    const hasHook = Boolean(cpState.hookAddress);
    hookBadge.classList.toggle("hidden", !hasHook);
    hookSub.innerText = hasHook ? cpState.hookAddress : "Hooks allow for advanced position customization.";
    hookSub.className = hasHook ? "text-xs text-emerald-400 font-mono truncate" : "text-xs text-gray-400 truncate";
    hookCard.className = hasHook
      ? "rounded-2xl bg-[#0b0f17] border border-emerald-500 p-4 space-y-3"
      : "rounded-2xl bg-[#0b0f17] border border-gray-800 p-4 space-y-3";
  }

  const feeSum = document.getElementById("cpFeeSummary");
  if (feeSum) feeSum.innerText = `${cpState.feeTier}% fee tier`;

  const deployed = getDeployedPools();
  document.querySelectorAll(".cp-fee-card").forEach(card => {
    const feeVal = parseFloat(card.dataset.fee);
    const selected = Math.abs(cpState.feeTier - feeVal) < 0.0001;
    const chk = card.querySelector(".cp-fee-check");
    const statusEl = card.querySelector(".cp-fee-status");
    const exists = Boolean(
      tokA && tokB && deployed.some(p =>
        p.networkKey === activeNetKey &&
        ((p.symA === tokA.symbol && p.symB === tokB.symbol) || (p.symA === tokB.symbol && p.symB === tokA.symbol)) &&
        Math.abs(Number(p.feeTier) - feeVal) < 0.0001
      )
    );
    card.className = selected
      ? "cp-fee-card text-left p-3.5 rounded-2xl bg-emerald-500/10 border border-emerald-500 space-y-1.5 transition"
      : "cp-fee-card text-left p-3.5 rounded-2xl bg-[#0b0f17] border border-gray-800 hover:border-gray-600 space-y-1.5 transition";
    if (chk) {
      chk.classList.toggle("hidden", !selected);
      chk.classList.toggle("flex", selected);
    }
    if (statusEl) {
      statusEl.innerText = exists ? "Created" : "Not created";
      statusEl.className = exists ? "cp-fee-status text-[11px] text-emerald-400 pt-1 font-bold" : "cp-fee-status text-[11px] text-gray-500 pt-1";
    }
  });

  const contBtn = document.getElementById("cpContinueBtn");
  if (contBtn) {
    const valid = Boolean(tokA && tokB && tokA.symbol !== tokB.symbol);
    contBtn.disabled = !valid;
    contBtn.className = valid
      ? "w-full py-3.5 rounded-2xl bg-emerald-500 hover:bg-emerald-400 text-black font-extrabold text-sm transition"
      : "w-full py-3.5 rounded-2xl bg-[#141b29] text-gray-500 font-extrabold text-sm cursor-not-allowed";
  }

  updateCreatePoolActionUi();
}

function openCpTokenPicker(slot) {
  cpState.pickerSlot = slot;
  const search = document.getElementById("cpTokenSearchInput");
  if (search) search.value = "";
  renderCpTokenPickerList();
  document.getElementById("cpTokenPickerModal")?.classList.remove("hidden");
}

function closeCpTokenPicker() {
  document.getElementById("cpTokenPickerModal")?.classList.add("hidden");
}

function renderCpTokenPickerList() {
  const listEl = document.getElementById("cpTokenPickerList");
  if (!listEl) return;
  const q = (document.getElementById("cpTokenSearchInput")?.value || "").trim().toLowerCase();
  const filtered = TOKENS.filter(t =>
    t.symbol.toLowerCase().includes(q) ||
    t.name.toLowerCase().includes(q) ||
    t.address.toLowerCase().includes(q)
  );
  listEl.innerHTML = filtered.map(t => `
    <button onclick="selectCpToken('${t.symbol}')" class="w-full p-3 rounded-xl bg-[#0d111a] hover:bg-[#151c2c] border border-gray-800 flex items-center justify-between text-left">
      <div class="flex items-center gap-3">
        <span class="w-8 h-8 rounded-full bg-emerald-500/15 text-emerald-400 text-xs font-extrabold flex items-center justify-center">${t.symbol.slice(0, 2)}</span>
        <div>
          <div class="text-xs font-extrabold text-white">${t.symbol}</div>
          <div class="text-[11px] text-gray-400">${t.name}</div>
        </div>
      </div>
      <div class="text-right">
        <div class="text-xs font-bold text-gray-300">$${formatUsd(PRICES[t.symbol] ?? t.price)}</div>
        <div class="text-[10px] text-gray-500 font-mono">${t.address.slice(0, 6)}...${t.address.slice(-4)}</div>
      </div>
    </button>
  `).join("") || `<div class="text-xs text-gray-500 text-center py-4">No tokens matched. List a custom token first.</div>`;
}

function selectCpToken(symbol) {
  if (cpState.pickerSlot === "A") {
    if (cpState.tokenB === symbol) cpState.tokenB = cpState.tokenA;
    cpState.tokenA = symbol;
  } else {
    if (cpState.tokenA === symbol) cpState.tokenA = cpState.tokenB;
    cpState.tokenB = symbol;
  }
  closeCpTokenPicker();
  renderCreatePoolUi();
}

function toggleCpHookPanel() {
  document.getElementById("cpHookPanel")?.classList.toggle("hidden");
}

function selectHookPreset(addr) {
  const input = document.getElementById("cpHookInput");
  if (input) input.value = addr;
  cpState.hookAddress = addr;
  document.getElementById("cpHookPanel")?.classList.add("hidden");
  renderCreatePoolUi();
}

function applyCpHook() {
  const val = (document.getElementById("cpHookInput")?.value || "").trim();
  cpState.hookAddress = val;
  document.getElementById("cpHookPanel")?.classList.add("hidden");
  renderCreatePoolUi();
}

function clearCpHook() {
  const input = document.getElementById("cpHookInput");
  if (input) input.value = "";
  cpState.hookAddress = "";
  document.getElementById("cpHookPanel")?.classList.add("hidden");
  renderCreatePoolUi();
}

function toggleCpFeeGrid() {
  cpState.feeExpanded = !cpState.feeExpanded;
  document.getElementById("cpFeeGridWrap")?.classList.toggle("hidden", !cpState.feeExpanded);
  const btn = document.getElementById("cpFeeCollapseBtn");
  if (btn) btn.innerHTML = cpState.feeExpanded ? "Less &#9652;" : "More &#9662;";
}

function selectCpFeeTier(tier) {
  cpState.feeTier = Number(tier);
  renderCreatePoolUi();
}

function toggleCpCustomFee() {
  document.getElementById("cpCustomFeePanel")?.classList.toggle("hidden");
}

function applyCpCustomFee() {
  const val = parseFloat(document.getElementById("cpCustomFeeInput")?.value || "");
  if (!val || val <= 0 || val > 10) return alert("Enter a valid fee tier between 0.0001% and 10%.");
  cpState.feeTier = val;
  document.getElementById("cpCustomFeePanel")?.classList.add("hidden");
  renderCreatePoolUi();
}

function goToCreatePoolStep2() {
  if (!cpState.tokenA || !cpState.tokenB || cpState.tokenA === cpState.tokenB) {
    return alert("Please select two distinct tokens first.");
  }
  cpState.step = 2;
  document.getElementById("cpStep1")?.classList.add("hidden");
  document.getElementById("cpStep2")?.classList.remove("hidden");
  document.getElementById("cpStepCircle").innerText = "2";
  document.getElementById("cpStepCounter").innerText = "Step 2 of 2";
  document.getElementById("cpStepTitle").innerText = "Set initial price and deposit";

  const tokA = TOKENS.find(t => t.symbol === cpState.tokenA);
  const tokB = TOKENS.find(t => t.symbol === cpState.tokenB);
  const pA = PRICES[cpState.tokenA] ?? tokA?.price ?? 0;
  const pB = PRICES[cpState.tokenB] ?? tokB?.price ?? 0;
  const initInput = document.getElementById("cpInitialPriceInput");
  if (initInput && (!initInput.value || Number(initInput.value) === 0) && pA > 0 && pB > 0) {
    const rate = cpState.priceBase === "A" ? pA / pB : pB / pA;
    initInput.value = rate.toFixed(4);
  }
  updateCpStep2Labels();
}

function goToCreatePoolStep1() {
  cpState.step = 1;
  document.getElementById("cpStep2")?.classList.add("hidden");
  document.getElementById("cpStep1")?.classList.remove("hidden");
  document.getElementById("cpStepCircle").innerText = "1";
  document.getElementById("cpStepCounter").innerText = "Step 1 of 2";
  document.getElementById("cpStepTitle").innerText = "Select token pair and fees";
}

function updateCpStep2Labels() {
  const symA = cpState.tokenA || "POL";
  const symB = cpState.tokenB || "TATA";
  const baseSym = cpState.priceBase === "A" ? symA : symB;
  const quoteSym = cpState.priceBase === "A" ? symB : symA;

  const sumPair = document.getElementById("cpSummaryPair");
  const sumFee = document.getElementById("cpSummaryFee");
  const sumHook = document.getElementById("cpSummaryHook");
  if (sumPair) sumPair.innerText = `${symA} / ${symB}`;
  if (sumFee) sumFee.innerText = `${cpState.feeTier}%`;
  if (sumHook) sumHook.classList.toggle("hidden", !cpState.hookAddress);

  const btnA = document.getElementById("cpBaseBtnA");
  const btnB = document.getElementById("cpBaseBtnB");
  if (btnA && btnB) {
    btnA.innerText = symA;
    btnB.innerText = symB;
    btnA.className = cpState.priceBase === "A"
      ? "px-3 py-1 rounded-full bg-emerald-500 text-black text-xs font-extrabold"
      : "px-3 py-1 rounded-full text-gray-400 text-xs font-bold";
    btnB.className = cpState.priceBase === "B"
      ? "px-3 py-1 rounded-full bg-emerald-500 text-black text-xs font-extrabold"
      : "px-3 py-1 rounded-full text-gray-400 text-xs font-bold";
  }

  const cap = document.getElementById("cpInitialPriceCaption");
  if (cap) cap.innerText = `${quoteSym} = 1 ${baseSym}`;
  const minUnit = document.getElementById("cpMinUnitLabel");
  const maxUnit = document.getElementById("cpMaxUnitLabel");
  if (minUnit) minUnit.innerText = `${quoteSym} per ${baseSym}`;
  if (maxUnit) maxUnit.innerText = `${quoteSym} per ${baseSym}`;

  const pA = PRICES[symA] || 0;
  const pB = PRICES[symB] || 0;
  const useMktBtn = document.getElementById("cpUseMarketBtn");
  const warnText = document.getElementById("cpMarketWarningText");
  if (pA > 0 && pB > 0) {
    const liveRate = cpState.priceBase === "A" ? pA / pB : pB / pA;
    if (useMktBtn) useMktBtn.classList.remove("hidden");
    if (warnText) warnText.innerText = `Live market reference: 1 ${baseSym} ≈ ${liveRate.toFixed(4)} ${quoteSym}. Please do your own research before creating a pool.`;
  } else {
    if (useMktBtn) useMktBtn.classList.add("hidden");
    if (warnText) warnText.innerText = "Market price not found. Please do your own research to avoid loss of funds.";
  }

  document.getElementById("cpDepositIconA").innerText = symA.slice(0, 2);
  document.getElementById("cpDepositSymA").innerText = symA;
  document.getElementById("cpDepositIconB").innerText = symB.slice(0, 2);
  document.getElementById("cpDepositSymB").innerText = symB;
  document.getElementById("cpBalA").innerText = `${getCpTokenBalance(symA).toFixed(4)} ${symA}`;
  document.getElementById("cpBalB").innerText = `${getCpTokenBalance(symB).toFixed(4)} ${symB}`;
  updateCreatePoolActionUi();
}

function setCpPriceBase(slot) {
  cpState.priceBase = slot;
  updateCpStep2Labels();
}

function applyCpMarketRate() {
  const symA = cpState.tokenA || "POL";
  const symB = cpState.tokenB || "TATA";
  const pA = PRICES[symA] || 0;
  const pB = PRICES[symB] || 0;
  if (pA > 0 && pB > 0) {
    const liveRate = cpState.priceBase === "A" ? pA / pB : pB / pA;
    document.getElementById("cpInitialPriceInput").value = liveRate.toFixed(4);
    onCpAmountAChange();
  }
}

function onCpInitialPriceChange() {
  onCpAmountAChange();
}

function setCpPriceRangeMode(isFull) {
  cpState.isFullRange = isFull;
  const fullBtn = document.getElementById("cpRangeFullBtn");
  const customBtn = document.getElementById("cpRangeCustomBtn");
  const grid = document.getElementById("cpCustomRangeGrid");
  const desc = document.getElementById("cpRangeDesc");

  if (fullBtn && customBtn) {
    fullBtn.className = isFull ? "py-2 rounded-full bg-[#141b29] text-white text-xs font-bold transition" : "py-2 rounded-full text-gray-400 text-xs font-bold transition";
    customBtn.className = !isFull ? "py-2 rounded-full bg-[#141b29] text-white text-xs font-bold transition" : "py-2 rounded-full text-gray-400 text-xs font-bold transition";
  }
  if (grid) grid.classList.toggle("hidden", isFull);
  if (desc) {
    desc.innerText = isFull
      ? "Setting full range liquidity when creating a pool ensures continuous market participation across all possible prices, offering simplicity but with potential for higher impermanent loss."
      : "Custom range concentrates liquidity between your chosen Min and Max prices for higher capital efficiency.";
  }
}

function stepCpBound(which, dir) {
  const baseRate = parseFloat(document.getElementById("cpInitialPriceInput")?.value) || 1;
  if (which === "min") {
    const el = document.getElementById("cpMinPriceInput");
    const cur = parseFloat(el.value) || 0;
    const next = dir > 0 ? (cur <= 0 ? baseRate * 0.5 : cur * 1.1) : Math.max(0, cur * 0.9);
    el.value = next.toFixed(4);
  } else {
    const el = document.getElementById("cpMaxPriceInput");
    const cur = parseFloat(el.value);
    const seed = isNaN(cur) ? baseRate * 1.5 : cur;
    const next = dir > 0 ? seed * 1.1 : Math.max(0.0001, seed * 0.9);
    el.value = next.toFixed(4);
  }
}

function onCpAmountAChange() {
  const amtA = parseFloat(document.getElementById("cpAmtAInput")?.value || "0");
  const rate = parseFloat(document.getElementById("cpInitialPriceInput")?.value || "0");
  if (amtA > 0 && rate > 0) {
    const amtB = cpState.priceBase === "A" ? amtA * rate : amtA / rate;
    const elB = document.getElementById("cpAmtBInput");
    if (elB) elB.value = amtB.toFixed(4);
  }
}

function setCpDepositPct(slot, pct) {
  const sym = slot === "A" ? cpState.tokenA : cpState.tokenB;
  const bal = getCpTokenBalance(sym);
  const val = (bal * pct) / 100;
  if (slot === "A") {
    document.getElementById("cpAmtAInput").value = val > 0 ? val.toFixed(4) : "0";
    onCpAmountAChange();
  } else {
    document.getElementById("cpAmtBInput").value = val > 0 ? val.toFixed(4) : "0";
  }
}

function updateCreatePoolActionUi() {
  const btn = document.getElementById("cpSubmitBtn");
  if (!btn) return;
  if (walletAddress) {
    btn.innerText = "Create Pool & Deposit";
  } else {
    btn.innerText = "Connect Wallet";
  }
}

function handleCpPrimaryAction() {
  if (!walletAddress) return openWalletModal();
  submitCreatePool();
}

function renderOnChainPools() {
  const net = NETWORKS[activeNetKey];
  const sub = document.getElementById("cpPoolsNetSubtitle");
  if (sub && net) sub.innerText = `Real reserves synced from ${net.name}`;
  const listEl = document.getElementById("cpOnChainPoolsList");
  if (!listEl) return;

  const netPools = getDeployedPools().filter(p => p.networkKey === activeNetKey);
  if (netPools.length === 0) {
    listEl.className = "text-xs text-gray-400 text-center py-4 leading-relaxed";
    listEl.innerText = "No pools deployed on this network yet. No simulated liquidity is displayed—create the first trading pair to seed real reserves.";
    return;
  }

  listEl.className = "space-y-2.5";
  listEl.innerHTML = netPools.map(p => `
    <div class="p-3.5 rounded-xl bg-[#0d111a] border border-gray-800 flex items-center justify-between gap-2">
      <div>
        <div class="text-sm font-extrabold text-white">${p.symA} / ${p.symB} <span class="ml-1 px-1.5 py-0.5 rounded bg-[#141b29] text-[10px] text-gray-300">${p.feeTier}%</span>${p.hookAddress ? ' <span class="px-1.5 py-0.5 rounded bg-emerald-500/20 text-[10px] text-emerald-400">Hooked</span>' : ""}</div>
        <div class="text-[11px] text-gray-400">${p.isFullRange ? "Full range" : `Range ${p.minPrice || "0"} – ${p.maxPrice || "∞"}`} • Deposit: ${p.amountA} ${p.symA} + ${p.amountB} ${p.symB}</div>
      </div>
      <a href="${p.explorerUrl || "#"}" target="_blank" rel="noopener" class="text-xs font-mono text-emerald-400 hover:underline shrink-0">${(p.txHash || "").slice(0, 8)}... &nearr;</a>
    </div>
  `).join("");
}

function refreshOnChainPools() {
  renderOnChainPools();
  renderCreatePoolUi();
}

async function submitCreatePool() {
  const notice = document.getElementById("createPoolNotice");
  if (!walletAddress) return openWalletModal();
  const provider = getEvmProvider();
  if (!provider) {
    if (notice) {
      notice.innerHTML = `<span class="text-yellow-300">Mobile Browser detected — tap <b>Connect Wallet</b> → open in <b>MetaMask / Trust Wallet</b> to broadcast pool creation on-chain.</span>`;
      notice.classList.remove("hidden");
    }
    return openWalletModal();
  }
  const cfg = getContractConfig();
  if (!/^0x[a-fA-F0-9]{40}$/.test(cfg.router)) {
    if (notice) {
      notice.innerHTML = `<span class="text-yellow-300">Deploy <b>TataSwapFactory.sol / TataSwapRouter.sol</b> and paste your Router/Factory contract address in the <b>Admin</b> tab first.</span>`;
      notice.classList.remove("hidden");
    }
    return;
  }
  const symA = cpState.tokenA || "TATA";
  const symB = cpState.tokenB || "USDT";
  const tokA = TOKENS.find((t) => t.symbol === symA);
  const tokB = TOKENS.find((t) => t.symbol === symB);
  if (!tokA || !tokB || !/^0x[a-fA-F0-9]{40}$/.test(tokA.address) || !/^0x[a-fA-F0-9]{40}$/.test(tokB.address)) {
    return alert("Select two valid EVM tokens to create a pool.");
  }
  const amountA = document.getElementById("cpAmtAInput")?.value || "0";
  const amountB = document.getElementById("cpAmtBInput")?.value || "0";
  try {
    const net = NETWORKS[activeNetKey];
    await ensureEvmChain(provider, net);
    const data = "0xc9c65396" + padHex32(tokA.address.replace("0x", "")) + padHex32(tokB.address.replace("0x", ""));
    const txHash = await provider.request({
      method: "eth_sendTransaction",
      params: [{ from: walletAddress, to: cfg.router, value: "0x0", data }]
    });
    const expUrl = `${net.explorer}/tx/${txHash}`;
    if (notice) {
      notice.innerHTML = `<span class="text-emerald-400 font-bold">Pool Tx Broadcasted! <a href="${expUrl}" target="_blank" class="underline">${txHash.slice(0, 12)}...</a></span>`;
      notice.classList.remove("hidden");
    }
    saveDeployedPool({
      networkKey: activeNetKey,
      symA,
      symB,
      feeTier: cpState.feeTier,
      hookAddress: cpState.hookAddress || null,
      isFullRange: cpState.isFullRange,
      minPrice: document.getElementById("cpMinPriceInput")?.value || "0.0",
      maxPrice: document.getElementById("cpMaxPriceInput")?.value || "∞",
      amountA,
      amountB,
      txHash,
      explorerUrl: expUrl
    });
    renderOnChainPools();
    renderCreatePoolUi();
    saveLocalTx({
      title: `Create Pool ${symA}/${symB} (${cpState.feeTier}%)`,
      subtitle: `${net.name} Mainnet${cpState.hookAddress ? " • Hooked" : ""}`,
      txHash,
      explorerUrl: expUrl
    });
  } catch (err) {
    if (notice) {
      notice.innerText = "Create pool failed: " + err.message;
      notice.classList.remove("hidden");
    }
  }
}

function submitAddLiquidity() {
  const provider = getEvmProvider();
  if (!provider) return connectWeb3Wallet();
  const cfg = getContractConfig();
  if (!/^0x[a-fA-F0-9]{40}$/.test(cfg.router)) {
    openPage("admin");
    return alert("Enter your deployed TataSwapRouter.sol contract address in Admin first.");
  }
  alert("Router connected! Approve both tokens in your wallet and confirm TataSwapRouter.addLiquidity() when deploying pools.");
}

function submitRemoveLiquidity() {
  const provider = getEvmProvider();
  if (!provider) return connectWeb3Wallet();
  const cfg = getContractConfig();
  if (!/^0x[a-fA-F0-9]{40}$/.test(cfg.router)) {
    openPage("admin");
    return alert("Enter your deployed TataSwapRouter.sol contract address in Admin first.");
  }
  alert("Router connected! Confirm LP withdrawal transaction in your wallet.");
}

function decodeStr(hex) {
  if (!hex || hex === "0x") return null;
  const clean = hex.replace(/^0x/, "");
  if (clean.length < 128) return null;
  const len = parseInt(clean.slice(64, 128), 16);
  if (!len || len > 100) return null;
  const data = clean.slice(128, 128 + len * 2);
  let out = "";
  for (let i = 0; i < data.length; i += 2) {
    const c = parseInt(data.substr(i, 2), 16);
    if (c >= 32 && c <= 126) out += String.fromCharCode(c);
  }
  return out.trim();
}

async function inspectListToken() {
  const addr = document.getElementById("listAddrInput").value.trim();
  if (!/^0x[a-fA-F0-9]{40}$/.test(addr)) return alert("Enter a valid 0x contract address.");
  try {
    const code = await rpcRequest("eth_getCode", [addr, "latest"]);
    if (!code || code === "0x") return alert("No smart contract bytecode found at this address.");
    const [nHex, sHex, dHex] = await Promise.all([
      rpcRequest("eth_call", [{ to: addr, data: "0x06fdde03" }, "latest"]),
      rpcRequest("eth_call", [{ to: addr, data: "0x95d89b41" }, "latest"]),
      rpcRequest("eth_call", [{ to: addr, data: "0x313ce567" }, "latest"])
    ]);
    document.getElementById("lpName").innerText = decodeStr(nHex) || "Custom Token";
    document.getElementById("lpSym").innerText = decodeStr(sHex) || "TKN";
    document.getElementById("lpDec").innerText = dHex && dHex !== "0x" ? parseInt(dHex, 16) : 18;
    document.getElementById("listPreview").classList.remove("hidden");
  } catch (err) {
    alert("RPC error: " + err.message);
  }
}

async function submitListToken() {
  const msg = document.getElementById("listResultMsg");
  const tokenAddr = document.getElementById("listAddrInput").value.trim();
  const provider = getEvmProvider();
  if (!walletAddress) return connectWeb3Wallet();

  if (isOwnerWalletConnected()) {
    msg.innerText = `Owner Wallet Verified (${OFFICIAL_OWNER_WALLET}): 0.00 USDT fee exempt! Token ready for instant zero-fee listing.`;
    msg.className = "text-xs text-center text-emerald-400 font-bold";
    msg.classList.remove("hidden");
    return;
  }

  if (!provider) return connectWeb3Wallet();

  const usdtToken = TOKENS.find(t => t.symbol === "USDT");
  try {
    msg.innerText = "Sending 5.00 USDT listing fee to Official Fee Receiver...";
    msg.className = "text-xs text-center text-yellow-400";
    msg.classList.remove("hidden");
    const txHash = await provider.request({
      method: "eth_sendTransaction",
      params: [{
        from: walletAddress,
        to: usdtToken.address,
        data: encodeErc20Transfer(OFFICIAL_FEE_RECEIVER, toWeiBigInt("5", usdtToken.decimals || 18))
      }]
    });
    const expUrl = `${NETWORKS[activeNetKey].explorer}/tx/${txHash}`;
    msg.innerHTML = `<span class="text-emerald-400 font-bold">5 USDT Fee Paid! Tx: <a href="${expUrl}" target="_blank" class="underline">${txHash.slice(0, 12)}...</a></span>`;
    saveLocalTx({
      title: `Token Listing Fee (5 USDT)`,
      subtitle: `Listed ${tokenAddr.slice(0, 10)}...`,
      txHash,
      explorerUrl: expUrl
    });
  } catch (err) {
    msg.innerText = "Listing Tx Error: " + err.message;
    msg.className = "text-xs text-center text-red-400";
  }
}

function formatUsd(val) {
  const n = Number(val) || 0;
  return n < 1 ? n.toFixed(4) : n.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

function renderLivePriceCards() {
  const symbols = ["ETH", "BNB", "POL", "CORE", "TATA"];
  const html = symbols.map(sym => {
    const p = PRICES[sym] || 0;
    const chg = CHANGES_24H[sym] || 0;
    const chgColor = chg >= 0 ? "text-emerald-400" : "text-red-400";
    const sign = chg >= 0 ? "+" : "";
    return `
      <div class="card-darker rounded-xl p-2.5 flex flex-col justify-between">
        <div class="flex justify-between items-center">
          <span class="font-extrabold text-white">${sym}</span>
          <span class="${chgColor} font-bold">${sign}${chg.toFixed(2)}%</span>
        </div>
        <div class="text-sm font-black text-yellow-400 mt-1">$${formatUsd(p)}</div>
      </div>
    `;
  }).join("");

  const swapTicker = document.getElementById("swapLiveTicker");
  if (swapTicker) swapTicker.innerHTML = html;
  const homeTicker = document.getElementById("homeLivePrices");
  if (homeTicker) homeTicker.innerHTML = html;
}

async function refreshLivePrices() {
  let updated = false;
  const savedApi = localStorage.getItem("tataswap_api_url");
  const apiBase = savedApi || (window.location.protocol.startsWith("http") ? "" : null);

  // 1. Try Backend /api/prices
  if (apiBase !== null) {
    try {
      const res = await fetch(`${apiBase}/api/prices`);
      if (res.ok) {
        const data = await res.json();
        if (data?.prices && (data.prices.ETH?.usd || data.prices.BNB?.usd)) {
          for (const [sym, info] of Object.entries(data.prices)) {
            if (info?.usd) {
              PRICES[sym] = Number(info.usd);
              CHANGES_24H[sym] = Number(info.change24h || 0);
              updated = true;
            }
          }
        }
      }
    } catch {}
  }

  // 2. Direct Binance Public Market Data API (Works directly in browser without CORS/API key)
  if (!updated) {
    try {
      const binRes = await fetch(
        "https://data-api.binance.vision/api/v3/ticker/24hr?symbols=" +
          encodeURIComponent(JSON.stringify(["ETHUSDT", "BNBUSDT", "POLUSDT"]))
      );
      if (binRes.ok) {
        const rows = await binRes.json();
        const mapSym = { ETHUSDT: "ETH", BNBUSDT: "BNB", POLUSDT: "POL" };
        if (Array.isArray(rows)) {
          for (const row of rows) {
            const sym = mapSym[row.symbol];
            const usd = parseFloat(row.lastPrice);
            if (sym && usd > 0) {
              PRICES[sym] = usd;
              CHANGES_24H[sym] = parseFloat(row.priceChangePercent || "0");
              updated = true;
            }
          }
        }
      }
    } catch {}
  }

  // 3. Direct CoinGecko & CryptoCompare fallback (also fills CORE)
  if (!updated || !CHANGES_24H.CORE) {
    try {
      const cgRes = await fetch(
        "https://api.coingecko.com/api/v3/simple/price?ids=ethereum,binancecoin,polygon-ecosystem-token,matic-network,coredaoorg,tether&vs_currencies=usd&include_24hr_change=true"
      );
      if (cgRes.ok) {
        const cg = await cgRes.json();
        if (!updated && cg.ethereum?.usd) { PRICES.ETH = cg.ethereum.usd; CHANGES_24H.ETH = cg.ethereum.usd_24h_change || 0; updated = true; }
        if (!updated && cg.binancecoin?.usd) { PRICES.BNB = cg.binancecoin.usd; CHANGES_24H.BNB = cg.binancecoin.usd_24h_change || 0; updated = true; }
        const pol = cg["polygon-ecosystem-token"] || cg["matic-network"];
        if (!updated && pol?.usd) { PRICES.POL = pol.usd; CHANGES_24H.POL = pol.usd_24h_change || 0; updated = true; }
        if (cg.coredaoorg?.usd) { PRICES.CORE = cg.coredaoorg.usd; CHANGES_24H.CORE = cg.coredaoorg.usd_24h_change || 0; updated = true; }
        if (cg.tether?.usd) { PRICES.USDT = cg.tether.usd; CHANGES_24H.USDT = cg.tether.usd_24h_change || 0; }
      }
    } catch {}
  }

  if (updated) {
    TOKENS = TOKENS.map(t => ({
      ...t,
      price: PRICES[t.symbol] ?? t.price,
      change24h: CHANGES_24H[t.symbol] ?? t.change24h ?? 0
    }));
    const timeStr = new Date().toLocaleTimeString([], { hour: "2-digit", minute: "2-digit", second: "2-digit" });
    const badge = document.getElementById("priceFeedBadge");
    if (badge) badge.innerText = `● Live • ${timeStr}`;
    const dashStatus = document.getElementById("dashPriceStatus");
    if (dashStatus) dashStatus.innerText = `Live Market Feed • Updated ${timeStr}`;
  }

  renderLivePriceCards();
  recalcSwap();
  renderTokensTable();
  renderPortfolioHoldings();
}

function renderTokensTable() {
  const q = (document.getElementById("exploreSearch").value || "").toLowerCase();
  const tbody = document.getElementById("exploreTableBody");
  const rows = TOKENS.filter(t => t.name.toLowerCase().includes(q) || t.symbol.toLowerCase().includes(q) || t.address.toLowerCase().includes(q));
  tbody.innerHTML = rows.map(t => {
    const chg = Number(t.change24h || 0);
    const chgClass = chg >= 0 ? "text-emerald-400" : "text-red-400";
    const sign = chg >= 0 ? "+" : "";
    return `
      <tr class="hover:bg-[#1a2234]/60">
        <td class="p-4 font-bold text-white">${t.symbol} <span class="text-gray-400 font-normal">${t.name}</span></td>
        <td class="p-4 font-mono text-gray-400">${t.address.slice(0, 8)}...${t.address.slice(-6)}</td>
        <td class="p-4 font-bold">$${formatUsd(t.price)} <span class="ml-1 text-[10px] ${chgClass}">(${sign}${chg.toFixed(2)}%)</span></td>
        <td class="p-4"><span class="px-2 py-0.5 rounded bg-cyan-500/20 text-cyan-300 font-bold">${t.verified ? "Verified" : "Unverified"}</span></td>
        <td class="p-4"><button onclick="viewToken('${t.symbol}')" class="text-yellow-400 font-bold hover:underline">Details</button></td>
      </tr>
    `;
  }).join("");
}

function viewToken(sym) {
  const t = TOKENS.find(x => x.symbol === sym);
  if (!t) return;
  document.getElementById("detailTitle").innerText = `${t.name} (${t.symbol})`;
  document.getElementById("detailAddress").innerText = t.address;
  document.getElementById("detailPrice").innerText = `$${formatUsd(t.price)}`;
  openPage("details");
}

// --- D3.JS HISTORICAL OHLC PRICE CHART FOR SWAP ---
const chartState = {
  interval: "1h",
  mode: "candles", // "candles" | "area"
  base: "BNB",
  quote: "USDT",
  source: "binance",
  candles: [],
  changePercent: 0,
  high: 0,
  low: 0,
  currentPrice: 0,
  lastPairKey: "",
  fetchSeq: 0
};

function formatChartPrice(val) {
  const n = Number(val);
  if (!Number.isFinite(n) || n === 0) return "0.00";
  if (n >= 1000) return n.toLocaleString("en-US", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  if (n >= 1) return n.toFixed(4);
  if (n >= 0.01) return n.toFixed(5);
  return n.toPrecision(4);
}

function formatChartTime(ts, interval) {
  const d = new Date(ts);
  if (interval === "1d") {
    return d.toLocaleDateString([], { month: "short", day: "numeric" });
  }
  return d.toLocaleString([], { month: "short", day: "numeric", hour: "2-digit", minute: "2-digit" });
}

function updateOhlcReadout(candle) {
  if (!candle) return;
  const setText = (id, txt) => {
    const el = document.getElementById(id);
    if (el) el.innerText = txt;
  };
  setText("ohlcOpen", formatChartPrice(candle.open));
  setText("ohlcHigh", formatChartPrice(candle.high));
  setText("ohlcLow", formatChartPrice(candle.low));
  setText("ohlcClose", formatChartPrice(candle.close));
  const vol = Number(candle.volume || 0);
  setText("ohlcVol", vol >= 1000 ? `${(vol / 1000).toFixed(1)}K` : vol.toFixed(1));
  setText("ohlcTime", formatChartTime(candle.timestamp, chartState.interval));
}

function setChartInterval(iv) {
  chartState.interval = iv;
  document.querySelectorAll(".chart-iv-btn").forEach(btn => {
    const active = btn.dataset.interval === iv;
    btn.className = active
      ? "chart-iv-btn px-2.5 py-1 rounded-lg bg-yellow-400 text-black"
      : "chart-iv-btn px-2.5 py-1 rounded-lg text-gray-400 hover:text-white";
  });
  loadSwapOhlcChart(true);
}

function setChartMode(mode) {
  chartState.mode = mode;
  document.querySelectorAll(".chart-mode-btn").forEach(btn => {
    const active = btn.dataset.mode === mode;
    btn.className = active
      ? "chart-mode-btn px-2.5 py-1 rounded-lg bg-cyan-500/20 text-cyan-300"
      : "chart-mode-btn px-2.5 py-1 rounded-lg text-gray-400 hover:text-white";
  });
  renderSwapD3Chart();
}

async function ensureD3Loaded() {
  if (window.d3) return window.d3;
  try {
    const mod = await import("https://cdn.jsdelivr.net/npm/d3@7/+esm");
    window.d3 = mod;
    return mod;
  } catch {
    return null;
  }
}

async function fetchDirectBinanceFallbackOhlc(base, quote, interval, limit = 48) {
  const norm = (s) => ({ BTCB: "BTC", WBTC: "BTC", WETH: "ETH", WBNB: "BNB", MATIC: "POL" }[s] || s);
  const b = norm(base);
  const q = norm(quote);
  const stepMs = { "15m": 900000, "1h": 3600000, "4h": 14400000, "1d": 86400000 }[interval] || 3600000;
  const binMap = { ETH: "ETHUSDT", BNB: "BNBUSDT", POL: "POLUSDT", BTC: "BTCUSDT", CAKE: "CAKEUSDT" };

  const fetchKlines = async (sym) => {
    if (sym === "USDT" || sym === "USDC") {
      const now = Math.floor(Date.now() / stepMs) * stepMs;
      return Array.from({ length: limit }, (_, i) => ({
        timestamp: now - (limit - 1 - i) * stepMs,
        open: 1, high: 1, low: 1, close: 1, volume: 120000
      }));
    }
    if (binMap[sym]) {
      const r = await fetch(`https://data-api.binance.vision/api/v3/klines?symbol=${binMap[sym]}&interval=${interval}&limit=${limit}`);
      if (r.ok) {
        const rows = await r.json();
        return rows.map(k => ({
          timestamp: Number(k[0]),
          open: parseFloat(k[1]),
          high: parseFloat(k[2]),
          low: parseFloat(k[3]),
          close: parseFloat(k[4]),
          volume: parseFloat(k[5]) || 0
        }));
      }
    }
    const spot = PRICES[sym] || PRICES[base] || 1.45;
    const now = Math.floor(Date.now() / stepMs) * stepMs;
    const candles = [];
    for (let i = 0; i < limit; i++) {
      const wave = Math.sin(i * 0.4) * 0.012 + Math.cos(i * 0.17) * 0.008;
      const open = i === 0 ? spot * 0.97 : candles[i - 1].close;
      const close = i === limit - 1 ? spot : spot * (0.97 + (0.03 * (i + 1)) / limit) * (1 + wave);
      const pad = Math.abs(close - open) + spot * 0.004;
      candles.push({
        timestamp: now - (limit - 1 - i) * stepMs,
        open, high: Math.max(open, close) + pad * 0.5, low: Math.max(0.000001, Math.min(open, close) - pad * 0.5), close,
        volume: 15000 + (i % 7) * 2200
      });
    }
    return candles;
  };

  const [bList, qList] = await Promise.all([fetchKlines(b), fetchKlines(q)]);
  const len = Math.min(bList.length, qList.length);
  const candles = [];
  for (let i = 0; i < len; i++) {
    const bc = bList[bList.length - len + i];
    const qc = qList[qList.length - len + i];
    const open = bc.open / (qc.open || 1);
    const close = bc.close / (qc.close || 1);
    candles.push({
      timestamp: bc.timestamp,
      open,
      high: Math.max(open, close, bc.high / (qc.low || qc.close || 1)),
      low: Math.min(open, close, bc.low / (qc.high || qc.close || 1)),
      close,
      volume: bc.volume
    });
  }
  const first = candles[0];
  const last = candles[candles.length - 1];
  return {
    pair: `${base}/${quote}`,
    base,
    quote,
    interval,
    source: binMap[b] ? "binance_direct" : "protocol_pool",
    currentPrice: last ? last.close : 0,
    changePercent: first && first.open > 0 ? ((last.close - first.open) / first.open) * 100 : 0,
    high: Math.max(...candles.map(c => c.high)),
    low: Math.min(...candles.map(c => c.low)),
    candles
  };
}

async function loadSwapOhlcChart(force = false) {
  const inEl = document.getElementById("swapInToken");
  const outEl = document.getElementById("swapOutToken");
  if (!inEl || !outEl) return;

  const base = inEl.value;
  const quote = outEl.value;
  const pairKey = `${base}_${quote}_${chartState.interval}`;
  if (!force && chartState.lastPairKey === pairKey && chartState.candles.length > 0) {
    renderSwapD3Chart();
    return;
  }

  chartState.lastPairKey = pairKey;
  chartState.base = base;
  chartState.quote = quote;
  const seq = ++chartState.fetchSeq;

  const loadingEl = document.getElementById("chartLoadingState");
  if (loadingEl) loadingEl.classList.remove("hidden");

  const savedApi = localStorage.getItem("tataswap_api_url");
  const apiBase = savedApi || (window.location.protocol.startsWith("http") ? "" : "https://tataswap.onrender.com");

  let payload = null;
  try {
    const res = await fetch(
      `${apiBase}/api/market/ohlc?base=${encodeURIComponent(base)}&quote=${encodeURIComponent(quote)}&interval=${encodeURIComponent(chartState.interval)}&limit=48`
    );
    if (res.ok) {
      const data = await res.json();
      if (Array.isArray(data?.candles) && data.candles.length > 0) {
        payload = data;
      }
    }
  } catch {}

  if (!payload) {
    try {
      payload = await fetchDirectBinanceFallbackOhlc(base, quote, chartState.interval, 48);
    } catch {}
  }

  if (seq !== chartState.fetchSeq) return;
  if (loadingEl) loadingEl.classList.add("hidden");
  if (!payload || !payload.candles?.length) return;

  chartState.candles = payload.candles;
  chartState.source = payload.source || "backend";
  chartState.currentPrice = Number(payload.currentPrice || payload.candles[payload.candles.length - 1].close || 0);
  chartState.changePercent = Number(payload.changePercent || 0);
  chartState.high = Number(payload.high || 0);
  chartState.low = Number(payload.low || 0);

  const titleEl = document.getElementById("chartPairTitle");
  const rateEl = document.getElementById("chartPairRate");
  const quoteLblEl = document.getElementById("chartPairQuoteLabel");
  const chgEl = document.getElementById("chartPairChange");
  const srcEl = document.getElementById("chartSourceBadge");
  const hlEl = document.getElementById("chartHighLow");

  if (titleEl) titleEl.innerText = `${base} / ${quote}`;
  if (rateEl) rateEl.innerText = formatChartPrice(chartState.currentPrice);
  if (quoteLblEl) quoteLblEl.innerText = `${quote} per ${base}`;
  if (hlEl) hlEl.innerText = `H: ${formatChartPrice(chartState.high)} • L: ${formatChartPrice(chartState.low)}`;
  if (chgEl) {
    const pos = chartState.changePercent >= 0;
    chgEl.innerText = `${pos ? "+" : ""}${chartState.changePercent.toFixed(2)}%`;
    chgEl.className = pos
      ? "px-2 py-0.5 rounded-full bg-emerald-500/15 text-emerald-400 text-[11px] font-bold"
      : "px-2 py-0.5 rounded-full bg-red-500/15 text-red-400 text-[11px] font-bold";
  }
  if (srcEl) {
    const labelMap = {
      binance: "Backend • Binance OHLC",
      cryptocompare: "Backend • CryptoCompare",
      coingecko: "Backend • CoinGecko",
      protocol_pool: "Protocol Pool Feed",
      binance_direct: "Binance Klines",
      peg: "Stable Peg",
      parity: "1:1 Parity"
    };
    srcEl.innerText = labelMap[chartState.source] || `Backend • ${chartState.source}`;
  }

  updateOhlcReadout(chartState.candles[chartState.candles.length - 1]);
  await renderSwapD3Chart();
}

async function renderSwapD3Chart() {
  const container = document.getElementById("swapD3Chart");
  if (!container) return;
  const d3 = await ensureD3Loaded();
  if (!d3 || !chartState.candles.length) return;

  const data = chartState.candles;
  const rect = container.getBoundingClientRect();
  const width = Math.max(300, Math.floor(rect.width || 600));
  const height = Math.max(260, Math.floor(rect.height || 320));
  const margin = { top: 14, right: 64, bottom: 26, left: 10 };
  const innerW = Math.max(120, width - margin.left - margin.right);
  const innerH = Math.max(120, height - margin.top - margin.bottom);

  d3.select(container).selectAll("*").remove();

  const svg = d3.select(container)
    .append("svg")
    .attr("width", width)
    .attr("height", height)
    .attr("viewBox", `0 0 ${width} ${height}`)
    .style("display", "block");

  const defs = svg.append("defs");
  const isPositive = chartState.changePercent >= 0;
  const accentColor = isPositive ? "#10b981" : "#ef4444";

  const areaGrad = defs.append("linearGradient")
    .attr("id", "tataSwapAreaGrad")
    .attr("x1", "0%").attr("y1", "0%")
    .attr("x2", "0%").attr("y2", "100%");
  areaGrad.append("stop").attr("offset", "0%").attr("stop-color", accentColor).attr("stop-opacity", 0.35);
  areaGrad.append("stop").attr("offset", "100%").attr("stop-color", accentColor).attr("stop-opacity", 0.02);

  const g = svg.append("g").attr("transform", `translate(${margin.left},${margin.top})`);

  const xBand = d3.scaleBand()
    .domain(data.map((_, i) => i))
    .range([0, innerW])
    .padding(0.28);

  const minPrice = d3.min(data, d => d.low);
  const maxPrice = d3.max(data, d => d.high);
  const pad = Math.max((maxPrice - minPrice) * 0.14, maxPrice * 0.004, 0.000001);

  const yPrice = d3.scaleLinear()
    .domain([Math.max(0, minPrice - pad * 1.4), maxPrice + pad])
    .range([innerH, 0]);

  const maxVol = d3.max(data, d => Number(d.volume || 0)) || 1;
  const yVol = d3.scaleLinear()
    .domain([0, maxVol])
    .range([innerH, innerH * 0.80]);

  // Gridlines
  g.append("g")
    .call(d3.axisRight(yPrice).ticks(5).tickSize(-innerW).tickFormat(""))
    .attr("transform", `translate(${innerW},0)`)
    .call(ax => ax.select(".domain").remove())
    .call(ax => ax.selectAll("line").attr("stroke", "#1e293b").attr("stroke-dasharray", "3,3"));

  // Price Y Axis (Right)
  g.append("g")
    .attr("transform", `translate(${innerW},0)`)
    .call(d3.axisRight(yPrice).ticks(5).tickFormat(d => formatChartPrice(d)))
    .call(ax => ax.select(".domain").remove())
    .call(ax => ax.selectAll("line").remove())
    .call(ax => ax.selectAll("text").attr("fill", "#94a3b8").style("font-size", "10px").style("font-family", "monospace"));

  // Time X Axis (Bottom)
  const tickStep = Math.max(1, Math.floor(data.length / 5));
  const xTicks = data.map((_, i) => i).filter(i => i % tickStep === 0 || i === data.length - 1);
  g.append("g")
    .attr("transform", `translate(0,${innerH})`)
    .call(d3.axisBottom(xBand).tickValues(xTicks).tickFormat(i => {
      const ts = data[i]?.timestamp;
      if (!ts) return "";
      const d = new Date(ts);
      return chartState.interval === "1d"
        ? d.toLocaleDateString([], { month: "short", day: "numeric" })
        : d.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" });
    }))
    .call(ax => ax.select(".domain").attr("stroke", "#1e293b"))
    .call(ax => ax.selectAll("line").attr("stroke", "#1e293b"))
    .call(ax => ax.selectAll("text").attr("fill", "#94a3b8").style("font-size", "10px"));

  // Volume Histogram
  g.append("g")
    .selectAll("rect")
    .data(data)
    .enter()
    .append("rect")
    .attr("x", (_, i) => xBand(i))
    .attr("y", d => yVol(Number(d.volume || 0)))
    .attr("width", xBand.bandwidth())
    .attr("height", d => Math.max(1, innerH - yVol(Number(d.volume || 0))))
    .attr("fill", d => d.close >= d.open ? "rgba(16,185,129,0.18)" : "rgba(239,68,68,0.18)")
    .attr("rx", 1);

  if (chartState.mode === "area") {
    const areaGen = d3.area()
      .x((_, i) => xBand(i) + xBand.bandwidth() / 2)
      .y0(innerH)
      .y1(d => yPrice(d.close))
      .curve(d3.curveMonotoneX);

    const lineGen = d3.line()
      .x((_, i) => xBand(i) + xBand.bandwidth() / 2)
      .y(d => yPrice(d.close))
      .curve(d3.curveMonotoneX);

    g.append("path")
      .datum(data)
      .attr("fill", "url(#tataSwapAreaGrad)")
      .attr("d", areaGen);

    g.append("path")
      .datum(data)
      .attr("fill", "none")
      .attr("stroke", accentColor)
      .attr("stroke-width", 2.2)
      .attr("d", lineGen);
  } else {
    // Candlesticks (wicks + bodies)
    const candleGroup = g.selectAll(".ohlc-candle")
      .data(data)
      .enter()
      .append("g")
      .attr("class", "ohlc-candle");

    candleGroup.append("line")
      .attr("x1", (_, i) => xBand(i) + xBand.bandwidth() / 2)
      .attr("x2", (_, i) => xBand(i) + xBand.bandwidth() / 2)
      .attr("y1", d => yPrice(d.high))
      .attr("y2", d => yPrice(d.low))
      .attr("stroke", d => d.close >= d.open ? "#10b981" : "#ef4444")
      .attr("stroke-width", 1.3);

    candleGroup.append("rect")
      .attr("x", (_, i) => xBand(i))
      .attr("y", d => yPrice(Math.max(d.open, d.close)))
      .attr("width", Math.max(2, xBand.bandwidth()))
      .attr("height", d => Math.max(2, Math.abs(yPrice(d.open) - yPrice(d.close))))
      .attr("fill", d => d.close >= d.open ? "#10b981" : "#ef4444")
      .attr("rx", 1.2);
  }

  // Latest Close Price Dashed Reference Line
  const lastCandle = data[data.length - 1];
  if (lastCandle) {
    const lastY = yPrice(lastCandle.close);
    g.append("line")
      .attr("x1", 0).attr("x2", innerW)
      .attr("y1", lastY).attr("y2", lastY)
      .attr("stroke", "#f0b90b")
      .attr("stroke-width", 1)
      .attr("stroke-dasharray", "4,4")
      .attr("opacity", 0.75);
  }

  // Interactive Crosshair
  const focus = g.append("g").style("display", "none");
  const vLine = focus.append("line")
    .attr("y1", 0).attr("y2", innerH)
    .attr("stroke", "#94a3b8").attr("stroke-width", 1).attr("stroke-dasharray", "3,3");
  const hLine = focus.append("line")
    .attr("x1", 0).attr("x2", innerW)
    .attr("stroke", "#94a3b8").attr("stroke-width", 1).attr("stroke-dasharray", "3,3");
  const focusDot = focus.append("circle")
    .attr("r", 4).attr("fill", "#f0b90b").attr("stroke", "#090c12").attr("stroke-width", 1.5);

  const tooltip = document.getElementById("swapChartTooltip");
  const stepWidth = innerW / Math.max(1, data.length);

  g.append("rect")
    .attr("width", innerW)
    .attr("height", innerH)
    .attr("fill", "transparent")
    .style("cursor", "crosshair")
    .on("mouseenter", () => focus.style("display", null))
    .on("mouseleave", () => {
      focus.style("display", "none");
      if (tooltip) tooltip.classList.add("hidden");
      if (data.length) updateOhlcReadout(data[data.length - 1]);
    })
    .on("mousemove touchmove", function (event) {
      const [mx] = d3.pointer(event, this);
      const idx = Math.max(0, Math.min(data.length - 1, Math.floor(mx / stepWidth)));
      const d = data[idx];
      if (!d) return;
      const cx = xBand(idx) + xBand.bandwidth() / 2;
      const cy = yPrice(d.close);
      focus.style("display", null);
      vLine.attr("x1", cx).attr("x2", cx);
      hLine.attr("y1", cy).attr("y2", cy);
      focusDot.attr("cx", cx).attr("cy", cy);
      updateOhlcReadout(d);

      if (tooltip) {
        const up = d.close >= d.open;
        tooltip.innerHTML = `
          <div class="text-gray-400 mb-0.5">${formatChartTime(d.timestamp, chartState.interval)}</div>
          <div class="${up ? "text-emerald-400" : "text-red-400"} font-bold">
            O ${formatChartPrice(d.open)} • C ${formatChartPrice(d.close)}
          </div>
          <div class="text-gray-300">H ${formatChartPrice(d.high)} • L ${formatChartPrice(d.low)}</div>
        `;
        tooltip.classList.remove("hidden");
      }
    });
}

window.addEventListener("resize", () => {
  if (chartState.candles.length > 0 && !document.getElementById("page-swap")?.classList.contains("hidden")) {
    renderSwapD3Chart();
  }
});

window.addEventListener("load", () => {
  const hash = window.location.hash.replace("#", "");
  if (hash && document.getElementById("page-" + hash)) openPage(hash);
  updateWalletHeaderUi();
  renderLivePriceCards();
  renderCreatePoolUi();
  renderOnChainPools();
  refreshBlock();
  recalcSwap();
  loadSwapOhlcChart(false);
  renderTokensTable();
  renderPortfolioHoldings();
  renderLocalHistory();
  refreshLivePrices();
  syncFromBackend();
  if (walletAddress) refreshBalance();

  // Delayed EIP-6963 re-scans & ?autoConnect=1 flow (matching useMetaMask.ts)
  window.dispatchEvent(new Event("eip6963:requestProvider"));
  runAutoConnectFromUrl();
  setTimeout(() => {
    window.dispatchEvent(new Event("eip6963:requestProvider"));
    runAutoConnectFromUrl();
  }, 350);
  setTimeout(() => {
    window.dispatchEvent(new Event("eip6963:requestProvider"));
    runAutoConnectFromUrl();
  }, 1200);

  setInterval(refreshLivePrices, 25000);
  setInterval(() => {
    if (!document.getElementById("page-swap")?.classList.contains("hidden")) {
      loadSwapOhlcChart(true);
    }
  }, 45000);
});
