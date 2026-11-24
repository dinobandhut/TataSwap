const NETWORKS = {
  tataservice: {
    name: "TataService Blockchain",
    chainIdHex: "0xc5cb1",
    symbol: "TATA",
    rpc: "https://rpctata.duckdns.org/rpc",
    explorer: "https://exploretata.duckdns.org",
    isEvm: true
  },
  eth_mainnet: {
    name: "Ethereum Mainnet",
    chainIdHex: "0x1",
    symbol: "ETH",
    rpc: "https://public.1rpc.io/eth",
    explorer: "https://etherscan.io",
    isEvm: true
  },
  bsc_mainnet: {
    name: "BNB Smart Chain",
    chainIdHex: "0x38",
    symbol: "BNB",
    rpc: "https://public.1rpc.io/bnb",
    explorer: "https://bscscan.com",
    isEvm: true
  },
  polygon_mainnet: {
    name: "Polygon Mainnet",
    chainIdHex: "0x89",
    symbol: "POL",
    rpc: "https://public.1rpc.io/matic",
    explorer: "https://polygonscan.com",
    isEvm: true
  },
  core_mainnet: {
    name: "Core Blockchain",
    chainIdHex: "0x45c",
    symbol: "CORE",
    rpc: "https://rpc.coredao.org",
    explorer: "https://scan.coredao.org",
    isEvm: true
  },
  solana_mainnet: {
    name: "Solana Mainnet",
    chainIdHex: null,
    symbol: "SOL",
    rpc: "https://api.mainnet.solana.com",
    explorer: "https://solscan.io",
    isEvm: false
  }
};

const OFFICIAL_FEE_RECEIVER = "0xD9e5c5d0522B1eeE8Ff9bC006F64d53d2C411CA4";
const OFFICIAL_OWNER_WALLET = "0xD9e5c5d0522B1eeE8Ff9bC006F64d53d2C411CA4";
const PRICES = { TATA: 1.45, ETH: 3210.0, BNB: 624.5, POL: 0.55, CORE: 1.25, SOL: 185.0, USDT: 1.0 };
const CHANGES_24H = { TATA: 4.2, ETH: 0.0, BNB: 0.0, POL: 0.0, CORE: 0.0, SOL: 0.0, USDT: 0.0 };

let TOKENS = [
  { symbol: "TATA", name: "TataSwap Token", address: "0x7A7A59a9DeCe06b86f1e58a2E49c7D59A1b7A7A5", price: 1.45, change24h: 4.2, verified: true },
  { symbol: "ETH", name: "Ethereum", address: "0x2170Ed0880ac9A755fd29B2688956BD959F933F8", price: 3210.0, change24h: 0.0, verified: true },
  { symbol: "BNB", name: "BNB", address: "0xbb4CdB9CBd36B01bD1cBaEBF2De08d9173bc095c", price: 624.5, change24h: 0.0, verified: true },
  { symbol: "POL", name: "Polygon", address: "0x0000000000000000000000000000000000001010", price: 0.55, change24h: 0.0, verified: true },
  { symbol: "CORE", name: "Core DAO", address: "0x40375C92d9FAf44d2f9db9Bd9ba41a3317a2404f", price: 1.25, change24h: 0.0, verified: true },
  { symbol: "SOL", name: "Solana", address: "So11111111111111111111111111111111111111112", price: 185.0, change24h: 0.0, verified: true },
  { symbol: "USDT", name: "Tether USD", address: "0x55d398326f99059fF775485246999027B3197955", price: 1.0, change24h: 0.0, verified: true }
];

let activeNetKey = "tataservice";
let walletAddress = null;

function isOwnerWalletConnected() {
  return Boolean(walletAddress && walletAddress.toLowerCase() === OFFICIAL_OWNER_WALLET.toLowerCase());
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
  if (ownerBadge) {
    ownerBadge.classList.toggle("hidden", !isOwner);
  }
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
    const net = NETWORKS[activeNetKey];
    if (!net.isEvm) {
      const slot = await rpcRequest("getSlot");
      document.getElementById("dashBlock").innerText = "Slot #" + Number(slot).toLocaleString();
      return;
    }
    const hex = await rpcRequest("eth_blockNumber");
    document.getElementById("dashBlock").innerText = "#" + parseInt(hex, 16).toLocaleString();
  } catch {
    document.getElementById("dashBlock").innerText = "Offline";
  }
}

async function syncFromBackend() {
  const apiUrl = localStorage.getItem("tataswap_api_url");
  if (!apiUrl) return;
  try {
    const [tokensRes, listingsRes] = await Promise.all([
      fetch(apiUrl + "/api/tokens"),
      fetch(apiUrl + "/api/listings")
    ]);
    if (tokensRes.ok) {
      const data = await tokensRes.json();
      if (Array.isArray(data.tokens) && data.tokens.length > 0) {
        TOKENS = data.tokens.map(t => ({
          symbol: t.symbol,
          name: t.name,
          address: t.address,
          price: parseFloat(t.price_usd) || 1.0,
          verified: Boolean(t.is_verified)
        }));
        document.getElementById("dashTokensCount").innerText = TOKENS.length;
        document.getElementById("explorerSourceLabel").innerText = "Synced live from Layerbase PostgreSQL via " + apiUrl;
        renderTokensTable();
      }
    }
    if (listingsRes.ok) {
      const lData = await listingsRes.json();
      if (Array.isArray(lData.listings) && lData.listings.length > 0) {
        document.getElementById("historyList").innerHTML = lData.listings.map(l => `
          <div class="p-3 rounded-xl bg-[#0d111a] border border-gray-800 mb-2 flex justify-between items-center">
            <div>
              <div class="font-bold text-white">Listed ${l.symbol} (${l.name})</div>
              <div class="text-[11px] text-gray-400 font-mono">Fee: ${l.fee_paid} USDT → ${l.fee_receiver}</div>
            </div>
            <div class="text-xs font-mono text-yellow-400">${l.tx_hash.slice(0, 10)}...</div>
          </div>
        `).join("");
      }
    }
  } catch (e) {
    console.warn("Backend sync skipped:", e.message);
  }
}

async function changeNetwork() {
  activeNetKey = document.getElementById("networkSelect").value;
  refreshBlock();
  if (walletAddress) refreshBalance();
  const net = NETWORKS[activeNetKey];
  if (!net.isEvm) {
    if (window.solana?.isPhantom) {
      try {
        const resp = await window.solana.connect();
        walletAddress = resp.publicKey.toString();
        document.getElementById("walletText").innerText = walletAddress.slice(0, 4) + "..." + walletAddress.slice(-4);
        document.getElementById("portAddr").innerText = walletAddress;
        await refreshBalance();
      } catch (err) {
        console.warn("Solana wallet note:", err.message);
      }
    }
    return;
  }
  if (window.ethereum) {
    try {
      await window.ethereum.request({
        method: "wallet_switchEthereumChain",
        params: [{ chainId: net.chainIdHex }]
      });
    } catch (e) {
      if (e.code === 4902) {
        try {
          await window.ethereum.request({
            method: "wallet_addEthereumChain",
            params: [{
              chainId: net.chainIdHex,
              chainName: net.name,
              nativeCurrency: { name: net.symbol, symbol: net.symbol, decimals: 18 },
              rpcUrls: [net.rpc],
              blockExplorerUrls: [net.explorer]
            }]
          });
        } catch (addErr) {
          console.warn("Add network failed:", addErr.message);
        }
      } else {
        console.warn("Network switch note:", e.message);
      }
    }
  }
}

async function connectWeb3Wallet() {
  const net = NETWORKS[activeNetKey];
  if (!net.isEvm) {
    if (!window.solana?.isPhantom) {
      alert("Phantom Wallet not detected for Solana Mainnet. Please install Phantom or switch to an EVM network.");
      return;
    }
    try {
      const resp = await window.solana.connect();
      walletAddress = resp.publicKey.toString();
      document.getElementById("walletText").innerText = walletAddress.slice(0, 4) + "..." + walletAddress.slice(-4);
      document.getElementById("portAddr").innerText = walletAddress;
      await refreshBalance();
    } catch (err) {
      alert("Solana Wallet error: " + err.message);
    }
    return;
  }
  if (!window.ethereum) {
    alert("MetaMask or Trust Wallet not detected. Please open inside a Web3 wallet browser or install MetaMask.");
    return;
  }
  try {
    const accounts = await window.ethereum.request({ method: "eth_requestAccounts" });
    walletAddress = accounts[0];
    document.getElementById("walletText").innerText = walletAddress.slice(0, 6) + "..." + walletAddress.slice(-4);
    document.getElementById("portAddr").innerText = walletAddress;
    updateListingFeeUi();
    await refreshBalance();
  } catch (err) {
    alert("Wallet error: " + err.message);
  }
}

async function refreshBalance() {
  if (!walletAddress) return;
  const net = NETWORKS[activeNetKey];
  try {
    if (!net.isEvm) {
      if (walletAddress.startsWith("0x")) {
        document.getElementById("swapBalIn").innerText = "Balance: Connect Phantom for SOL";
        document.getElementById("portBal").innerText = "Connect Phantom (SOL)";
        return;
      }
      const res = await rpcRequest("getBalance", [walletAddress]);
      const sol = ((res?.value || 0) / 1e9).toFixed(4);
      document.getElementById("swapBalIn").innerText = `Balance: ${sol} SOL`;
      document.getElementById("portBal").innerText = `${sol} SOL`;
      return;
    }
    const hex = await rpcRequest("eth_getBalance", [walletAddress, "latest"]);
    const bal = (Number(BigInt(hex)) / 1e18).toFixed(4);
    const sym = net.symbol;
    document.getElementById("swapBalIn").innerText = `Balance: ${bal} ${sym}`;
    document.getElementById("portBal").innerText = `${bal} ${sym}`;
  } catch (e) {
    console.error(e);
  }
}

function recalcSwap() {
  const sIn = document.getElementById("swapInToken").value;
  const sOut = document.getElementById("swapOutToken").value;
  const amtIn = parseFloat(document.getElementById("swapAmountIn").value) || 0;
  const slip = parseFloat(document.getElementById("slippageVal").value) || 0.5;
  const rate = PRICES[sIn] / PRICES[sOut];
  const out = amtIn * rate * 0.9975;
  const minRec = out * (1 - slip / 100);

  document.getElementById("swapAmountOut").innerText = out.toFixed(4);
  document.getElementById("swapRate").innerText = `1 ${sIn} = ${rate.toFixed(4)} ${sOut}`;
  document.getElementById("swapMinRec").innerText = `${minRec.toFixed(4)} ${sOut}`;
}

function flipSwap() {
  const el1 = document.getElementById("swapInToken");
  const el2 = document.getElementById("swapOutToken");
  const t = el1.value;
  el1.value = el2.value;
  el2.value = t;
  recalcSwap();
}

function submitSwap() {
  const notice = document.getElementById("swapNotice");
  if (!walletAddress) return connectWeb3Wallet();
  notice.innerText = `Connect deployed TataSwapRouter.sol contract address on ${NETWORKS[activeNetKey].name} to sign live on-chain swaps.`;
  notice.classList.remove("hidden");
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
  const net = NETWORKS[activeNetKey];
  if (!net.isEvm) {
    if (addr.length < 32 || addr.length > 44) {
      return alert("Enter a valid Solana SPL Mint address.");
    }
    try {
      const acc = await rpcRequest("getAccountInfo", [addr, { encoding: "jsonParsed" }]);
      if (!acc?.value) return alert("Solana SPL Mint account not found on Mainnet.");
      const decimals = acc?.value?.data?.parsed?.info?.decimals ?? 9;
      document.getElementById("lpName").innerText = "Solana SPL Token";
      document.getElementById("lpSym").innerText = "SPL";
      document.getElementById("lpDec").innerText = decimals;
      document.getElementById("listPreview").classList.remove("hidden");
    } catch (err) {
      alert("Solana RPC error: " + err.message);
    }
    return;
  }
  if (!/^0x[a-fA-F0-9]{40}$/.test(addr)) {
    return alert("Enter a valid 0x contract address.");
  }
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

function submitListToken() {
  const msg = document.getElementById("listResultMsg");
  if (!walletAddress) return connectWeb3Wallet();
  if (isOwnerWalletConnected()) {
    msg.innerText = `Owner Wallet Verified (${OFFICIAL_OWNER_WALLET}): 0.00 USDT listing fee! Calling TataSwapListingFee.listToken() with zero fee deduction.`;
    msg.className = "text-xs text-center text-emerald-400 font-bold";
  } else {
    msg.innerText = `Ready: Sign USDT approval & TataSwapListingFee.listToken() to pay 5 USDT to ${OFFICIAL_FEE_RECEIVER}.`;
    msg.className = "text-xs text-center text-amber-300";
  }
  msg.classList.remove("hidden");
}

function formatUsd(val) {
  const n = Number(val) || 0;
  return n < 1 ? n.toFixed(4) : n.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

function renderLivePriceCards() {
  const symbols = ["ETH", "BNB", "SOL", "POL", "CORE", "TATA"];
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

  if (savedApi) {
    try {
      const res = await fetch(`${savedApi}/api/prices`);
      if (res.ok) {
        const data = await res.json();
        if (data?.prices) {
          for (const [sym, info] of Object.entries(data.prices)) {
            if (info?.usd) {
              PRICES[sym] = Number(info.usd);
              CHANGES_24H[sym] = Number(info.change24h || 0);
              updated = true;
            }
          }
        }
      }
    } catch (e) {
      console.warn("Backend /api/prices fallback:", e.message);
    }
  }

  if (!updated) {
    try {
      const cgRes = await fetch(
        "https://api.coingecko.com/api/v3/simple/price?ids=ethereum,binancecoin,polygon-ecosystem-token,matic-network,coredaoorg,solana,tether&vs_currencies=usd&include_24hr_change=true"
      );
      if (cgRes.ok) {
        const cg = await cgRes.json();
        if (cg.ethereum?.usd) { PRICES.ETH = cg.ethereum.usd; CHANGES_24H.ETH = cg.ethereum.usd_24h_change || 0; updated = true; }
        if (cg.binancecoin?.usd) { PRICES.BNB = cg.binancecoin.usd; CHANGES_24H.BNB = cg.binancecoin.usd_24h_change || 0; updated = true; }
        const pol = cg["polygon-ecosystem-token"] || cg["matic-network"];
        if (pol?.usd) { PRICES.POL = pol.usd; CHANGES_24H.POL = pol.usd_24h_change || 0; updated = true; }
        if (cg.coredaoorg?.usd) { PRICES.CORE = cg.coredaoorg.usd; CHANGES_24H.CORE = cg.coredaoorg.usd_24h_change || 0; updated = true; }
        if (cg.solana?.usd) { PRICES.SOL = cg.solana.usd; CHANGES_24H.SOL = cg.solana.usd_24h_change || 0; updated = true; }
        if (cg.tether?.usd) { PRICES.USDT = cg.tether.usd; CHANGES_24H.USDT = cg.tether.usd_24h_change || 0; }
      }
    } catch (e) {
      console.warn("Direct CoinGecko fallback skipped:", e.message);
    }
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
  renderLivePriceCards();
  refreshBlock();
  recalcSwap();
  renderTokensTable();
  syncFromBackend();
  refreshLivePrices();
  setInterval(refreshLivePrices, 30000);
});
