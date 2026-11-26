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

let activeNetKey = "tataservice";
let walletAddress = localStorage.getItem("tataswap_wallet") || null;
let walletMode = localStorage.getItem("tataswap_wallet_mode") || "none";
let walletProviderLabel = localStorage.getItem("tataswap_wallet_label") || "Web3 Wallet";
let nativeBalanceVal = 0;
let discoveredProviders = [];
let activeProvider = null;
let wcProjectIdCache = (localStorage.getItem("tataswap_wc_project_id") || "").trim();

function registerEip6963Provider(detail) {
  if (!detail?.provider) return;
  const rdns = detail.info?.rdns || detail.info?.name || ("provider_" + discoveredProviders.length);
  if (!discoveredProviders.some(d => (d.info?.rdns || d.info?.name) === rdns)) {
    discoveredProviders.push(detail);
  }
}

window.addEventListener("eip6963:announceProvider", (event) => {
  registerEip6963Provider(event?.detail);
});
window.dispatchEvent(new Event("eip6963:requestProvider"));

function getAllDetectedWallets() {
  const wallets = [];
  const seen = new Set();
  const pushWallet = (id, name, provider) => {
    if (!provider || seen.has(provider)) return;
    seen.add(provider);
    wallets.push({ id, name, provider });
  };

  for (const d of discoveredProviders) {
    const label = d.info?.name || "Browser Wallet";
    const id = (d.info?.rdns || label).toLowerCase();
    pushWallet(id, label, d.provider);
  }

  if (Array.isArray(window.ethereum?.providers)) {
    for (const p of window.ethereum.providers) {
      const name = p.isMetaMask ? "MetaMask" : p.isTrust || p.isTrustWallet ? "Trust Wallet" : p.isCoinbaseWallet ? "Coinbase Wallet" : p.isOkxWallet ? "OKX Wallet" : p.isBitKeep ? "Bitget Wallet" : p.isBinance ? "Binance Wallet" : "Browser Wallet";
      pushWallet(name.toLowerCase(), name, p);
    }
  }

  if (window.trustwallet) pushWallet("trust", "Trust Wallet", window.trustwallet);
  if (window.coinbaseWalletExtension) pushWallet("coinbase", "Coinbase Wallet", window.coinbaseWalletExtension);
  if (window.BinanceChain) pushWallet("binance", "Binance Wallet", window.BinanceChain);
  if (window.okxwallet) pushWallet("okx", "OKX Wallet", window.okxwallet);
  if (window.bitkeep?.ethereum) pushWallet("bitget", "Bitget Wallet", window.bitkeep.ethereum);

  if (window.ethereum) {
    const p = window.ethereum;
    const name = p.isMetaMask ? "MetaMask" : p.isTrust || p.isTrustWallet ? "Trust Wallet" : p.isCoinbaseWallet ? "Coinbase Wallet" : p.isOkxWallet ? "OKX Wallet" : p.isBitKeep ? "Bitget Wallet" : p.isBinance ? "Binance Wallet" : "Browser Wallet";
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
    return list.find(w => (w.id.includes("metamask") || (w.provider?.isMetaMask && !w.provider?.isTrust && !w.provider?.isBraveWallet)))?.provider || null;
  }
  if (t === "coinbase") {
    return list.find(w => w.id.includes("coinbase") || w.name.toLowerCase().includes("coinbase") || w.provider?.isCoinbaseWallet)?.provider || window.coinbaseWalletExtension || null;
  }
  if (t === "binance") {
    return list.find(w => w.id.includes("binance") || w.name.toLowerCase().includes("binance") || w.provider?.isBinance)?.provider || window.BinanceChain || null;
  }
  if (t === "okx") {
    return list.find(w => w.id.includes("okx") || w.name.toLowerCase().includes("okx") || w.provider?.isOkxWallet)?.provider || window.okxwallet || null;
  }
  if (t === "bitget") {
    return list.find(w => w.id.includes("bitget") || w.id.includes("bitkeep") || w.provider?.isBitKeep)?.provider || window.bitkeep?.ethereum || null;
  }
  if (t === "rainbow") {
    return list.find(w => w.id.includes("rainbow") || w.provider?.isRainbow)?.provider || null;
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

  if (pageId === "explore") renderTokensTable();
  if (pageId === "listing") updateListingFeeUi();
  if (pageId === "create-pool") {
    renderCreatePoolUi();
    renderOnChainPools();
  }
  if (pageId === "portfolio") renderPortfolioHoldings();
  if (pageId === "history") renderLocalHistory();
  if (pageId === "admin") loadContractConfig();
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
      if (cfg?.walletConnectProjectId && !wcProjectIdCache) {
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
    if (e.code === 4902) {
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
}

function getWalletDeepLink(type) {
  const currentUrl = window.location.href;
  const encodedUrl = encodeURIComponent(currentUrl);
  const hostPath = window.location.host + window.location.pathname;
  switch (type) {
    case "metamask":
      return `https://link.metamask.io/dapp/${hostPath}`;
    case "trust":
      return `https://link.trustwallet.com/open_url?coin_id=20000714&url=${encodedUrl}`;
    case "coinbase":
      return `https://go.cb-w.com/dapp?cb_url=${encodedUrl}`;
    case "binance":
      return `bnc://app.binance.com/cedefi/browser?url=${encodedUrl}`;
    case "okx":
      return `https://www.okx.com/download?deeplink=${encodeURIComponent("okx://wallet/dapp/url?dappUrl=" + encodedUrl)}`;
    case "bitget":
      return `https://bkcode.vip?action=dapp&url=${encodedUrl}`;
    case "safepal":
      return `https://link.safepal.io/dapp?url=${encodedUrl}`;
    case "rainbow":
      return "https://rnbwapp.com";
    default:
      return currentUrl;
  }
}

function openInNewBrowserTab() {
  window.open(window.location.href, "_blank", "noopener");
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
    } else {
      disconnectWallet();
    }
  });
  provider.on("chainChanged", () => {
    refreshBlock();
    if (walletAddress) refreshBalance();
  });
}

function refreshWalletModalUi() {
  const detected = getAllDetectedWallets();
  const isIframe = window.self !== window.top;

  const connBox = document.getElementById("walletConnectedBox");
  const connAddr = document.getElementById("walletConnectedAddr");
  const connLabel = document.getElementById("walletConnectedLabel");
  if (connBox) {
    connBox.classList.toggle("hidden", !walletAddress);
    if (connAddr && walletAddress) connAddr.innerText = walletAddress;
    if (connLabel) connLabel.innerText = `Connected (${walletProviderLabel})`;
  }

  const noticeBox = document.getElementById("walletEnvNotice");
  const noticeTxt = document.getElementById("walletEnvNoticeText");
  if (noticeBox) {
    if (isIframe) {
      noticeBox.classList.remove("hidden");
      if (noticeTxt) noticeTxt.innerText = "Browser extensions (MetaMask, Trust Wallet) are blocked inside embedded preview iframes.";
    } else if (detected.length === 0) {
      noticeBox.classList.remove("hidden");
      if (noticeTxt) noticeTxt.innerText = "No browser extension detected in Google Chrome. Tap any wallet app below or WalletConnect to link directly.";
    } else {
      noticeBox.classList.add("hidden");
    }
  }

  const detBox = document.getElementById("detectedWalletsBox");
  const detList = document.getElementById("detectedWalletsList");
  if (detBox && detList) {
    if (detected.length > 0) {
      detBox.classList.remove("hidden");
      detList.innerHTML = detected.map((w, idx) => `
        <button onclick="connectDetectedIndex(${idx})" class="w-full p-2.5 rounded-xl bg-emerald-500/10 hover:bg-emerald-500/20 border border-emerald-500/40 flex items-center justify-between text-left">
          <div class="flex items-center gap-2.5">
            <span class="w-2.5 h-2.5 rounded-full bg-emerald-400"></span>
            <span class="text-xs font-extrabold text-white">${w.name}</span>
          </div>
          <span class="text-[11px] font-bold text-emerald-300">Connect Extension &rarr;</span>
        </button>
      `).join("");
    } else {
      detBox.classList.add("hidden");
      detList.innerHTML = "";
    }
  }

  const subTrust = document.getElementById("subTrust");
  const subMetaMask = document.getElementById("subMetaMask");
  const subCoinbase = document.getElementById("subCoinbase");
  const subBinance = document.getElementById("subBinance");
  if (subTrust) subTrust.innerText = findProviderForWallet("trust") ? "Trust Wallet Extension detected • Click to connect" : "Tap to open in Trust Wallet DApp Browser";
  if (subMetaMask) subMetaMask.innerText = findProviderForWallet("metamask") ? "MetaMask Extension detected • Click to connect" : "Tap to open in MetaMask Mobile Browser";
  if (subCoinbase) subCoinbase.innerText = findProviderForWallet("coinbase") ? "Coinbase Extension detected • Click to connect" : "Tap to open in Coinbase Wallet App";
  if (subBinance) subBinance.innerText = findProviderForWallet("binance") ? "Binance Extension detected • Click to connect" : "BNB Smart Chain Web3 wallet";

  const wcInput = document.getElementById("wcProjectIdInput");
  if (wcInput && wcProjectIdCache && !wcInput.value) wcInput.value = wcProjectIdCache;
}

function connectWeb3Wallet() {
  window.dispatchEvent(new Event("eip6963:requestProvider"));
  refreshWalletModalUi();
  const modal = document.getElementById("walletModal");
  if (modal) {
    modal.classList.remove("hidden");
    modal.classList.add("flex");
  }
}

function openWalletModal() {
  connectWeb3Wallet();
}

function closeWalletModal() {
  const modal = document.getElementById("walletModal");
  if (modal) {
    modal.classList.add("hidden");
    modal.classList.remove("flex");
  }
}

async function finalizeProviderConnect(provider, label) {
  const accounts = await provider.request({ method: "eth_requestAccounts" });
  if (!accounts || !accounts.length) throw new Error("No wallet account returned.");
  activeProvider = provider;
  attachProviderEvents(provider);
  walletAddress = accounts[0];
  walletMode = "injected";
  walletProviderLabel = label;
  localStorage.setItem("tataswap_wallet", walletAddress);
  localStorage.setItem("tataswap_wallet_mode", walletMode);
  localStorage.setItem("tataswap_wallet_label", walletProviderLabel);
  updateWalletHeaderUi();
  closeWalletModal();
  await refreshBalance();
}

async function connectDetectedIndex(idx) {
  const detected = getAllDetectedWallets();
  const item = detected[idx];
  if (!item) return;
  try {
    await finalizeProviderConnect(item.provider, item.name);
  } catch (err) {
    alert(item.name + " connection error: " + err.message);
  }
}

async function connectWalletByType(type) {
  const provider = findProviderForWallet(type);
  const labelMap = {
    trust: "Trust Wallet",
    metamask: "MetaMask",
    coinbase: "Coinbase Wallet",
    binance: "Binance Wallet",
    okx: "OKX Wallet",
    bitget: "Bitget Wallet",
    safepal: "SafePal Wallet",
    rainbow: "Rainbow Wallet"
  };
  if (provider) {
    try {
      await finalizeProviderConnect(provider, labelMap[type] || "Web3 Wallet");
      return;
    } catch (err) {
      alert((labelMap[type] || "Wallet") + " error: " + err.message);
      return;
    }
  }
  const deepUrl = getWalletDeepLink(type);
  window.open(deepUrl, "_blank", "noopener");
}

async function connectWalletConnect() {
  const projectId = (wcProjectIdCache || localStorage.getItem("tataswap_wc_project_id") || "").trim();
  if (!projectId) {
    const panel = document.getElementById("wcExtraPanel");
    if (panel) panel.classList.toggle("hidden");
    return;
  }
  try {
    const subWc = document.getElementById("subWc");
    if (subWc) subWc.innerText = "Loading WalletConnect v2 modal...";
    const { EthereumProvider } = await import("https://esm.sh/@walletconnect/ethereum-provider@2.17.0");
    const net = NETWORKS[activeNetKey];
    const wcProvider = await EthereumProvider.init({
      projectId,
      chains: [net.chainId],
      optionalChains: [810161, 1, 56, 137, 1116],
      showQrModal: true,
      metadata: {
        name: "TataSwap DEX",
        description: "Multi-Chain Non-Custodial EVM DEX",
        url: window.location.origin,
        icons: ["https://tataswap.onrender.com/favicon.ico"]
      }
    });
    await wcProvider.connect();
    await finalizeProviderConnect(wcProvider, "WalletConnect");
  } catch (err) {
    const panel = document.getElementById("wcExtraPanel");
    if (panel) panel.classList.remove("hidden");
    alert("WalletConnect note: " + (err?.message || "Could not open WalletConnect session. Choose any mobile wallet below."));
  }
}

async function saveAndLaunchWalletConnect() {
  const raw = (document.getElementById("wcProjectIdInput")?.value || "").trim();
  if (!raw || raw.length < 16) {
    return alert("Enter a valid Reown / WalletConnect Project ID (from cloud.reown.com).");
  }
  wcProjectIdCache = raw;
  localStorage.setItem("tataswap_wc_project_id", raw);
  await connectWalletConnect();
}

function disconnectWallet() {
  if (activeProvider?.disconnect) {
    try { activeProvider.disconnect(); } catch {}
  }
  activeProvider = null;
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

async function connectManualAddress() {
  const raw = (document.getElementById("manualWalletInput")?.value || "").trim();
  const isEvmAddr = /^0x[a-fA-F0-9]{40}$/.test(raw);
  if (!isEvmAddr) {
    return alert("Please enter a valid 0x EVM wallet address.");
  }
  walletAddress = raw;
  walletMode = "address";
  localStorage.setItem("tataswap_wallet", walletAddress);
  localStorage.setItem("tataswap_wallet_mode", walletMode);
  updateWalletHeaderUi();
  closeWalletModal();
  await refreshBalance();
}

async function connectOwnerWalletDirect() {
  walletAddress = OFFICIAL_OWNER_WALLET;
  walletMode = "address";
  localStorage.setItem("tataswap_wallet", walletAddress);
  localStorage.setItem("tataswap_wallet_mode", walletMode);
  updateWalletHeaderUi();
  closeWalletModal();
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
}

function flipSwap() {
  const el1 = document.getElementById("swapInToken");
  const el2 = document.getElementById("swapOutToken");
  const t = el1.value;
  el1.value = el2.value;
  el2.value = t;
  recalcSwap();
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
  if (rEl) rEl.value = cfg.router;
  if (lEl) lEl.value = cfg.listing;
}

function saveContractConfig() {
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

window.addEventListener("load", () => {
  const hash = window.location.hash.replace("#", "");
  if (hash && document.getElementById("page-" + hash)) openPage(hash);
  updateWalletHeaderUi();
  renderLivePriceCards();
  renderCreatePoolUi();
  renderOnChainPools();
  refreshBlock();
  recalcSwap();
  renderTokensTable();
  renderPortfolioHoldings();
  renderLocalHistory();
  refreshLivePrices();
  syncFromBackend();
  if (walletAddress) refreshBalance();
  setInterval(refreshLivePrices, 25000);
});
