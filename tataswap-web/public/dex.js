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
  },
  solana_mainnet: {
    name: "Solana Mainnet",
    chainId: 101,
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
  { symbol: "TATA", name: "TataSwap Token", address: "0x7A7A59a9DeCe06b86f1e58a2E49c7D59A1b7A7A5", decimals: 18, price: 1.45, change24h: 4.2, balance: 0, verified: true },
  { symbol: "ETH", name: "Ethereum", address: "0x2170Ed0880ac9A755fd29B2688956BD959F933F8", decimals: 18, price: 3210.0, change24h: 0.0, balance: 0, verified: true },
  { symbol: "BNB", name: "BNB", address: "0xbb4CdB9CBd36B01bD1cBaEBF2De08d9173bc095c", decimals: 18, price: 624.5, change24h: 0.0, balance: 0, verified: true },
  { symbol: "POL", name: "Polygon", address: "0x0000000000000000000000000000000000001010", decimals: 18, price: 0.55, change24h: 0.0, balance: 0, verified: true },
  { symbol: "CORE", name: "Core DAO", address: "0x40375C92d9FAf44d2f9db9Bd9ba41a3317a2404f", decimals: 18, price: 1.25, change24h: 0.0, balance: 0, verified: true },
  { symbol: "SOL", name: "Solana", address: "So11111111111111111111111111111111111111112", decimals: 9, price: 185.0, change24h: 0.0, balance: 0, verified: true },
  { symbol: "USDT", name: "Tether USD", address: "0x55d398326f99059fF775485246999027B3197955", decimals: 18, price: 1.0, change24h: 0.0, balance: 0, verified: true }
];

let activeNetKey = "tataservice";
let walletAddress = localStorage.getItem("tataswap_wallet") || null;
let walletMode = localStorage.getItem("tataswap_wallet_mode") || "none";
let nativeBalanceVal = 0;
let discoveredProviders = [];

window.addEventListener("eip6963:announceProvider", (event) => {
  if (event?.detail?.provider) {
    discoveredProviders.push(event.detail);
  }
});
window.dispatchEvent(new Event("eip6963:requestProvider"));

function getEvmProvider() {
  if (window.ethereum) return window.ethereum;
  if (discoveredProviders.length > 0) return discoveredProviders[0].provider;
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
  const apiUrl = localStorage.getItem("tataswap_api_url") || (window.location.protocol.startsWith("http") ? "" : null);
  if (apiUrl === null) return;
  try {
    const [tokensRes, listingsRes] = await Promise.all([
      fetch(apiUrl + "/api/tokens"),
      fetch(apiUrl + "/api/listings")
    ]);
    if (tokensRes.ok) {
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
    if (listingsRes.ok) {
      const lData = await listingsRes.json();
      if (Array.isArray(lData.listings) && lData.listings.length > 0) {
        renderLocalHistory(lData.listings);
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
        walletMode = "solana";
        localStorage.setItem("tataswap_wallet", walletAddress);
        localStorage.setItem("tataswap_wallet_mode", walletMode);
        updateWalletHeaderUi();
        await refreshBalance();
      } catch (err) {
        console.warn("Solana wallet note:", err.message);
      }
    }
    return;
  }
  const provider = getEvmProvider();
  if (provider) {
    try {
      await provider.request({
        method: "wallet_switchEthereumChain",
        params: [{ chainId: net.chainIdHex }]
      });
    } catch (e) {
      if (e.code === 4902) {
        try {
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
        } catch (addErr) {
          console.warn("Add network failed:", addErr.message);
        }
      }
    }
  }
}

function updateDeepLinks() {
  const currentUrl = window.location.href;
  const hostPath = window.location.host + window.location.pathname;
  const mm = document.getElementById("deepLinkMetaMask");
  const tr = document.getElementById("deepLinkTrust");
  const ph = document.getElementById("deepLinkPhantom");
  if (mm) mm.href = `https://link.metamask.io/dapp/${hostPath}`;
  if (tr) tr.href = `https://link.trustwallet.com/open_url?coin_id=20000714&url=${encodeURIComponent(currentUrl)}`;
  if (ph) ph.href = `https://phantom.app/ul/browse/${encodeURIComponent(currentUrl)}`;
}

function connectWeb3Wallet() {
  updateDeepLinks();
  const modal = document.getElementById("walletModal");
  if (modal) {
    modal.classList.remove("hidden");
    modal.classList.add("flex");
  }
}

function closeWalletModal() {
  const modal = document.getElementById("walletModal");
  if (modal) {
    modal.classList.add("hidden");
    modal.classList.remove("flex");
  }
}

async function connectInjectedWallet() {
  const net = NETWORKS[activeNetKey];
  if (!net.isEvm) {
    if (!window.solana?.isPhantom) {
      window.open(`https://phantom.app/ul/browse/${encodeURIComponent(window.location.href)}`, "_blank");
      return;
    }
    try {
      const resp = await window.solana.connect();
      walletAddress = resp.publicKey.toString();
      walletMode = "solana";
      localStorage.setItem("tataswap_wallet", walletAddress);
      localStorage.setItem("tataswap_wallet_mode", walletMode);
      updateWalletHeaderUi();
      closeWalletModal();
      await refreshBalance();
    } catch (err) {
      alert("Solana Wallet error: " + err.message);
    }
    return;
  }

  const provider = getEvmProvider();
  if (!provider) {
    const mmUrl = `https://link.metamask.io/dapp/${window.location.host}${window.location.pathname}`;
    window.open(mmUrl, "_blank");
    return;
  }

  try {
    const accounts = await provider.request({ method: "eth_requestAccounts" });
    walletAddress = accounts[0];
    walletMode = "injected";
    localStorage.setItem("tataswap_wallet", walletAddress);
    localStorage.setItem("tataswap_wallet_mode", walletMode);
    updateWalletHeaderUi();
    closeWalletModal();
    await refreshBalance();
  } catch (err) {
    alert("Wallet connection error: " + err.message);
  }
}

async function connectManualAddress() {
  const raw = (document.getElementById("manualWalletInput")?.value || "").trim();
  const isEvmAddr = /^0x[a-fA-F0-9]{40}$/.test(raw);
  const isSolAddr = raw.length >= 32 && raw.length <= 44 && !raw.startsWith("0x");
  if (!isEvmAddr && !isSolAddr) {
    return alert("Please enter a valid 0x EVM wallet address or Solana address.");
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
    if (!net.isEvm) {
      if (walletAddress.startsWith("0x")) {
        document.getElementById("swapBalIn").innerText = "Balance: Switch to EVM for 0x wallet";
        document.getElementById("portBal").innerText = "Switch to EVM Chain";
        renderPortfolioHoldings();
        return;
      }
      const res = await rpcRequest("getBalance", [walletAddress]);
      nativeBalanceVal = (res?.value || 0) / 1e9;
      document.getElementById("swapBalIn").innerText = `Balance: ${nativeBalanceVal.toFixed(4)} SOL`;
      document.getElementById("portBal").innerText = `${nativeBalanceVal.toFixed(4)} SOL`;
      renderPortfolioHoldings();
      return;
    }

    if (!walletAddress.startsWith("0x")) return;
    const hex = await rpcRequest("eth_getBalance", [walletAddress, "latest"]);
    nativeBalanceVal = Number(BigInt(hex)) / 1e18;
    const sym = net.symbol;
    document.getElementById("swapBalIn").innerText = `Balance: ${nativeBalanceVal.toFixed(4)} ${sym}`;
    document.getElementById("portBal").innerText = `${nativeBalanceVal.toFixed(4)} ${sym}`;

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
  if (!net.isEvm) {
    window.open(`https://phantom.app/ul/browse/${encodeURIComponent(window.location.href)}`, "_blank");
    return;
  }
  if (!/^0x[a-fA-F0-9]{40}$/.test(toAddr)) return alert("Enter a valid 0x recipient address.");

  const provider = getEvmProvider();
  const amountWei = toWeiBigInt(amountStr, 18);

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
  if (!net.isEvm) return connectInjectedWallet();

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
  const symA = document.getElementById("poolTokenA")?.value || "TATA";
  const symB = document.getElementById("poolTokenB")?.value || "USDT";
  const tokA = TOKENS.find((t) => t.symbol === symA);
  const tokB = TOKENS.find((t) => t.symbol === symB);
  if (!tokA || !tokB || !/^0x[a-fA-F0-9]{40}$/.test(tokA.address) || !/^0x[a-fA-F0-9]{40}$/.test(tokB.address)) {
    return alert("Select two valid EVM tokens to create a pool.");
  }
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
    saveLocalTx({ title: `Create Pool ${symA}/${symB}`, subtitle: `${net.name} Mainnet`, txHash, explorerUrl: expUrl });
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
  const net = NETWORKS[activeNetKey];
  if (!net.isEvm) {
    if (addr.length < 32 || addr.length > 44) return alert("Enter a valid Solana SPL Mint address.");
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
  const apiBase = savedApi || (window.location.protocol.startsWith("http") ? "" : null);

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

  if (!updated) {
    try {
      const binRes = await fetch(
        "https://data-api.binance.vision/api/v3/ticker/24hr?symbols=" +
          encodeURIComponent(JSON.stringify(["ETHUSDT", "BNBUSDT", "SOLUSDT", "POLUSDT"]))
      );
      if (binRes.ok) {
        const rows = await binRes.json();
        const mapSym = { ETHUSDT: "ETH", BNBUSDT: "BNB", SOLUSDT: "SOL", POLUSDT: "POL" };
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

  if (!updated || !CHANGES_24H.CORE) {
    try {
      const cgRes = await fetch(
        "https://api.coingecko.com/api/v3/simple/price?ids=ethereum,binancecoin,polygon-ecosystem-token,matic-network,coredaoorg,solana,tether&vs_currencies=usd&include_24hr_change=true"
      );
      if (cgRes.ok) {
        const cg = await cgRes.json();
        if (!updated && cg.ethereum?.usd) { PRICES.ETH = cg.ethereum.usd; CHANGES_24H.ETH = cg.ethereum.usd_24h_change || 0; updated = true; }
        if (!updated && cg.binancecoin?.usd) { PRICES.BNB = cg.binancecoin.usd; CHANGES_24H.BNB = cg.binancecoin.usd_24h_change || 0; updated = true; }
        const pol = cg["polygon-ecosystem-token"] || cg["matic-network"];
        if (!updated && pol?.usd) { PRICES.POL = pol.usd; CHANGES_24H.POL = pol.usd_24h_change || 0; updated = true; }
        if (cg.coredaoorg?.usd) { PRICES.CORE = cg.coredaoorg.usd; CHANGES_24H.CORE = cg.coredaoorg.usd_24h_change || 0; updated = true; }
        if (!updated && cg.solana?.usd) { PRICES.SOL = cg.solana.usd; CHANGES_24H.SOL = cg.solana.usd_24h_change || 0; updated = true; }
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
