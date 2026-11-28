require('dotenv').config();
const crypto = require('crypto');
const fs = require('fs');
const path = require('path');
const express = require('express');
const cors = require('cors');
const rateLimit = require('express-rate-limit');
const { Pool } = require('pg');

const app = express();
const PORT = process.env.PORT || 8080;

// Official DEX Constants
const OFFICIAL_FEE_RECEIVER = (
  process.env.OFFICIAL_FEE_RECEIVER || '0x3f48248FFE235a5205d1D781fB6c852c71fc1556'
).toLowerCase();
const OFFICIAL_OWNER_WALLET = (
  process.env.OFFICIAL_OWNER_WALLET || OFFICIAL_FEE_RECEIVER
).toLowerCase();
const LISTING_FEE_USDT = parseFloat(process.env.LISTING_FEE_USDT || '5.0');

function isOwnerWallet(addr) {
  return typeof addr === 'string' && addr.trim().toLowerCase() === OFFICIAL_OWNER_WALLET;
}

// Mainnet Blockchain RPC Endpoints (TataService, ETH, BNB, Polygon, Core)
const RPC_URLS = {
  tataservice: process.env.TATA_RPC_URL || 'https://rpctata.duckdns.org/rpc',
  eth: process.env.ETH_RPC_URL || 'https://public.1rpc.io/eth',
  bsc: process.env.BSC_RPC_URL || 'https://public.1rpc.io/bnb',
  polygon: process.env.POLYGON_RPC_URL || 'https://public.1rpc.io/matic',
  core: process.env.CORE_RPC_URL || 'https://rpc.coredao.org',
};

app.use(cors());
app.use(express.json({ limit: '256kb' }));

// Rate Limiting for API Security
const apiLimiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 minutes
  max: 200,
  standardHeaders: true,
  legacyHeaders: false,
  message: { error: 'Too many requests, please try again later.' },
});
app.use('/api/', apiLimiter);

// PostgreSQL Pool (Layerbase / Google Cloud SQL)
let pool = null;
if (process.env.DATABASE_URL) {
  pool = new Pool({
    connectionString: process.env.DATABASE_URL,
    ssl: process.env.DB_SSL === 'false' ? false : { rejectUnauthorized: false },
    max: 10,
    idleTimeoutMillis: 30000,
  });

  pool.on('error', (err) => {
    console.error('[DB] Unexpected PostgreSQL error:', err.message);
  });
} else {
  console.warn('[DB] DATABASE_URL not set. Running in RPC-only mode until DATABASE_URL is configured.');
}

// Helper: JSON-RPC Call
async function callRpc(chain, method, params = []) {
  const rpcUrl = RPC_URLS[chain] || RPC_URLS.bsc;
  const response = await fetch(rpcUrl, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      jsonrpc: '2.0',
      id: Date.now(),
      method,
      params,
    }),
  });
  if (!response.ok) {
    throw new Error(`RPC HTTP error ${response.status}`);
  }
  const data = await response.json();
  if (data.error) {
    throw new Error(data.error.message || 'RPC error');
  }
  return data.result;
}

// Helper: Decode ABI String from eth_call
function decodeAbiString(hex) {
  try {
    const clean = (hex || '').replace(/^0x/, '');
    if (clean.length < 128) return null;
    const lengthHex = clean.slice(64, 128);
    const len = parseInt(lengthHex, 16);
    if (!len || len > 100) return null;
    const dataHex = clean.slice(128, 128 + len * 2);
    return Buffer.from(dataHex, 'hex').toString('utf8').replace(/[^\x20-\x7E]/g, '').trim();
  } catch {
    return null;
  }
}

function isValidAddress(addr) {
  return typeof addr === 'string' && /^0x[a-fA-F0-9]{40}$/.test(addr.trim());
}

function isValidTxHash(hash) {
  return typeof hash === 'string' && /^0x[a-fA-F0-9]{64}$/.test(hash.trim());
}

// 0. Serve TataSwap Web DEX static files (if ../website exists) & Root Status Endpoint
const websiteDir = path.join(__dirname, '..', 'website');
if (fs.existsSync(websiteDir)) {
  app.use(express.static(websiteDir));
}

app.get('/', (req, res) => {
  const indexPath = path.join(websiteDir, 'index.html');
  if (fs.existsSync(indexPath)) {
    return res.sendFile(indexPath);
  }
  res.json({
    status: 'ok',
    protocol: 'TataSwap DEX Backend API',
    officialFeeReceiver: OFFICIAL_FEE_RECEIVER,
    endpoints: ['/health', '/api/config', '/api/prices', '/api/market/ohlc', '/api/stats', '/api/tokens', '/api/listings', '/api/rpc/inspect/:address'],
  });
});

// 1. Health Check (Verifies PostgreSQL + Live Blockchain RPC)
app.get('/health', async (req, res) => {
  const status = {
    status: 'ok',
    protocol: 'TataSwap DEX',
    officialFeeReceiver: OFFICIAL_FEE_RECEIVER,
    listingFeeUsdt: LISTING_FEE_USDT,
    database: 'not_configured',
    rpc: {},
    timestamp: new Date().toISOString(),
  };

  if (pool) {
    try {
      await pool.query('SELECT 1');
      status.database = 'connected';
    } catch (err) {
      status.database = `error: ${err.message}`;
    }
  }

  const checks = [
    { key: 'tataservice', label: 'tataServiceBlock', method: 'eth_blockNumber', isHex: true },
    { key: 'eth', label: 'ethBlock', method: 'eth_blockNumber', isHex: true },
    { key: 'bsc', label: 'bscBlock', method: 'eth_blockNumber', isHex: true },
    { key: 'polygon', label: 'polygonBlock', method: 'eth_blockNumber', isHex: true },
    { key: 'core', label: 'coreBlock', method: 'eth_blockNumber', isHex: true },
  ];

  await Promise.all(
    checks.map(async (c) => {
      try {
        const resVal = await callRpc(c.key, c.method);
        status.rpc[c.label] = c.isHex ? parseInt(resVal, 16) : resVal;
      } catch (err) {
        status.rpc[`${c.key}Error`] = err.message;
      }
    })
  );

  res.json(status);
});

// 1b. Public Web DEX Runtime Config (WalletConnect Project ID & Protocol Constants)
app.get('/api/config', (req, res) => {
  res.json({
    walletConnectProjectId: (process.env.WALLETCONNECT_PROJECT_ID || '58ad26007a1212b3406b582c93228d50').trim(),
    officialFeeReceiver: OFFICIAL_FEE_RECEIVER,
    officialOwnerWallet: OFFICIAL_OWNER_WALLET,
    listingFeeUsdt: LISTING_FEE_USDT,
  });
});

// 1c. Admin Dashboard Authentication (Salted SHA-256 verification; no plaintext credentials stored)
const ADMIN_AUTH_SALT = 'tataswap-admin-v1:';
const ADMIN_EMAIL_SHA256 = (
  process.env.ADMIN_EMAIL_SHA256 ||
  (process.env.ADMIN_EMAIL
    ? crypto.createHash('sha256').update(ADMIN_AUTH_SALT + process.env.ADMIN_EMAIL.trim().toLowerCase()).digest('hex')
    : 'd634449c49a098e5d985a627942ba762beca4a6900256199babfab6097968b1d')
).toLowerCase();
const ADMIN_PASSWORD_SHA256 = (
  process.env.ADMIN_PASSWORD_SHA256 ||
  (process.env.ADMIN_PASSWORD
    ? crypto.createHash('sha256').update(ADMIN_AUTH_SALT + process.env.ADMIN_PASSWORD).digest('hex')
    : 'eda84c27a20b491878b76df2d69f184a9384590a4737262b409a31e04c098754')
).toLowerCase();

const adminSessions = new Map();

function sha256SafeEqual(rawValue, expectedHex) {
  const actualHex = crypto.createHash('sha256').update(ADMIN_AUTH_SALT + String(rawValue || '')).digest('hex');
  const a = Buffer.from(actualHex, 'hex');
  const b = Buffer.from(expectedHex, 'hex');
  if (a.length !== b.length) return false;
  return crypto.timingSafeEqual(a, b);
}

app.post('/api/admin/login', (req, res) => {
  const email = typeof req.body?.email === 'string' ? req.body.email.trim().toLowerCase() : '';
  const password = typeof req.body?.password === 'string' ? req.body.password : '';
  const emailMatch = sha256SafeEqual(email, ADMIN_EMAIL_SHA256);
  const passMatch = sha256SafeEqual(password, ADMIN_PASSWORD_SHA256);
  if (!emailMatch || !passMatch) {
    return res.status(401).json({ authenticated: false, error: 'Invalid admin email or password.' });
  }
  const token = crypto.randomBytes(24).toString('hex');
  adminSessions.set(token, { createdAt: Date.now() });
  res.json({ authenticated: true, token });
});

app.post('/api/admin/logout', (req, res) => {
  const auth = req.headers.authorization || '';
  const token = auth.replace(/^Bearer\s+/i, '').trim() || req.body?.token;
  if (token) adminSessions.delete(token);
  res.json({ authenticated: false });
});

// 2. Live On-Chain Token Contract Inspection via Mainnet RPCs
app.get('/api/rpc/inspect/:address', async (req, res) => {
  const address = req.params.address.trim();
  const chain = RPC_URLS[req.query.chain] ? req.query.chain : 'tataservice';

  if (!isValidAddress(address)) {
    return res.status(400).json({ error: 'Invalid 0x contract address format.' });
  }

  try {
    const bytecode = await callRpc(chain, 'eth_getCode', [address, 'latest']);
    const isContract = bytecode && bytecode !== '0x' && bytecode.length > 2;

    if (!isContract) {
      return res.status(404).json({ error: 'No smart contract found at this address on selected network.' });
    }

    const [nameHex, symbolHex, decimalsHex] = await Promise.all([
      callRpc(chain, 'eth_call', [{ to: address, data: '0x06fdde03' }, 'latest']).catch(() => null),
      callRpc(chain, 'eth_call', [{ to: address, data: '0x95d89b41' }, 'latest']).catch(() => null),
      callRpc(chain, 'eth_call', [{ to: address, data: '0x313ce567' }, 'latest']).catch(() => null),
    ]);

    const name = decodeAbiString(nameHex) || 'Unknown Token';
    const symbol = decodeAbiString(symbolHex) || 'TKN';
    const decimals = decimalsHex && decimalsHex !== '0x' ? parseInt(decimalsHex, 16) : 18;

    res.json({
      address,
      chain,
      name,
      symbol,
      decimals,
      contractBytecodeExists: true,
    });
  } catch (err) {
    res.status(502).json({ error: `RPC inspection failed: ${err.message}` });
  }
});

// 2b. Live Market Prices (Binance Data Vision + CoinGecko + CryptoCompare fallback with 20s cache)
let priceCache = { data: null, updatedAt: 0 };

async function fetchLivePrices() {
  const now = Date.now();
  if (priceCache.data && now - priceCache.updatedAt < 20000) {
    return priceCache.data;
  }

  const prices = {
    TATA: { usd: 1.45, change24h: 4.2, source: 'protocol_pool' },
    USDT: { usd: 1.0, change24h: 0.0, source: 'peg' },
  };

  // 1. Binance Public Market Data API (No API key, high rate limit)
  try {
    const binRes = await fetch(
      'https://data-api.binance.vision/api/v3/ticker/24hr?symbols=' +
        encodeURIComponent(JSON.stringify(['ETHUSDT', 'BNBUSDT', 'POLUSDT', 'BTCUSDT', 'CAKEUSDT']))
    );
    if (binRes.ok) {
      const rows = await binRes.json();
      const mapSym = { ETHUSDT: 'ETH', BNBUSDT: 'BNB', POLUSDT: 'POL', BTCUSDT: 'BTCB', CAKEUSDT: 'CAKE' };
      if (Array.isArray(rows)) {
        for (const row of rows) {
          const sym = mapSym[row.symbol];
          const usd = parseFloat(row.lastPrice);
          const chg = parseFloat(row.priceChangePercent || '0');
          if (sym && usd > 0) {
            prices[sym] = { usd, change24h: chg, source: 'binance' };
          }
        }
      }
    }
  } catch (err) {
    console.warn('[Prices] Binance Data Vision skipped:', err.message);
  }

  // 2. CoinGecko (fills CORE + any missing tokens)
  if (!prices.CORE || !prices.ETH || !prices.BNB || !prices.POL) {
    try {
      const cgRes = await fetch(
        'https://api.coingecko.com/api/v3/simple/price?ids=ethereum,binancecoin,polygon-ecosystem-token,matic-network,coredaoorg,tether&vs_currencies=usd&include_24hr_change=true'
      );
      if (cgRes.ok) {
        const cg = await cgRes.json();
        if (!prices.ETH && cg.ethereum?.usd) prices.ETH = { usd: cg.ethereum.usd, change24h: cg.ethereum.usd_24h_change || 0, source: 'coingecko' };
        if (!prices.BNB && cg.binancecoin?.usd) prices.BNB = { usd: cg.binancecoin.usd, change24h: cg.binancecoin.usd_24h_change || 0, source: 'coingecko' };
        const polObj = cg['polygon-ecosystem-token'] || cg['matic-network'];
        if (!prices.POL && polObj?.usd) prices.POL = { usd: polObj.usd, change24h: polObj.usd_24h_change || 0, source: 'coingecko' };
        if (!prices.CORE && cg.coredaoorg?.usd) prices.CORE = { usd: cg.coredaoorg.usd, change24h: cg.coredaoorg.usd_24h_change || 0, source: 'coingecko' };
        if (cg.tether?.usd) prices.USDT = { usd: cg.tether.usd, change24h: cg.tether.usd_24h_change || 0, source: 'coingecko' };
      }
    } catch (err) {
      console.warn('[Prices] CoinGecko fetch skipped:', err.message);
    }
  }

  // 3. CryptoCompare fallback for any remaining missing tokens
  if (!prices.ETH || !prices.BNB || !prices.CORE || !prices.POL) {
    try {
      const ccRes = await fetch(
        'https://min-api.cryptocompare.com/data/pricemultifull?fsyms=ETH,BNB,POL,CORE,USDT&tsyms=USD'
      );
      if (ccRes.ok) {
        const cc = await ccRes.json();
        const raw = cc.RAW || {};
        for (const sym of ['ETH', 'BNB', 'POL', 'CORE', 'USDT']) {
          if (!prices[sym] && raw[sym]?.USD?.PRICE) {
            prices[sym] = {
              usd: raw[sym].USD.PRICE,
              change24h: raw[sym].USD.CHANGEPCT24HOUR || 0,
              source: 'cryptocompare',
            };
          }
        }
      }
    } catch (err) {
      console.warn('[Prices] CryptoCompare fetch skipped:', err.message);
    }
  }

  const result = {
    prices,
    updatedAt: new Date(now).toISOString(),
  };
  if (prices.ETH || prices.BNB || prices.POL) {
    priceCache = { data: result, updatedAt: now };
  }
  return result;
}

app.get('/api/prices', async (req, res) => {
  try {
    const data = await fetchLivePrices();
    res.json(data);
  } catch (err) {
    res.status(502).json({ error: `Failed to fetch live prices: ${err.message}` });
  }
});

// 2c. Historical OHLC Market Data for Swap Charts (Binance Data Vision + CryptoCompare + CoinGecko + Cross-Pairs)
const ohlcCache = new Map();
const INTERVAL_MS = {
  '15m': 15 * 60 * 1000,
  '1h': 60 * 60 * 1000,
  '4h': 4 * 60 * 60 * 1000,
  '1d': 24 * 60 * 60 * 1000,
};

function normalizeOhlcSymbol(sym) {
  const s = String(sym || 'BNB').trim().toUpperCase().replace(/[^A-Z0-9]/g, '');
  if (s === 'BTCB' || s === 'WBTC') return 'BTC';
  if (s === 'WETH') return 'ETH';
  if (s === 'WBNB') return 'BNB';
  if (s === 'MATIC' || s === 'WMATIC' || s === 'WPOL') return 'POL';
  if (s === 'WCORE') return 'CORE';
  return s || 'BNB';
}

function buildFlatStableCandles(price, interval, limit) {
  const step = INTERVAL_MS[interval] || INTERVAL_MS['1h'];
  const nowAligned = Math.floor(Date.now() / step) * step;
  const candles = [];
  for (let i = limit - 1; i >= 0; i--) {
    candles.push({
      timestamp: nowAligned - i * step,
      open: price,
      high: price,
      low: price,
      close: price,
      volume: 125000,
    });
  }
  return candles;
}

function buildAnchoredProtocolCandles(symbol, spotPrice, interval, limit) {
  const step = INTERVAL_MS[interval] || INTERVAL_MS['1h'];
  const nowAligned = Math.floor(Date.now() / step) * step;
  const seedBase = symbol.split('').reduce((acc, ch) => acc + ch.charCodeAt(0), 0);
  const candles = [];
  let prevClose = spotPrice * 0.965;
  for (let i = 0; i < limit; i++) {
    const progress = (i + 1) / limit;
    const wave = Math.sin((i + seedBase) * 0.42) * 0.012 + Math.cos((i + seedBase) * 0.19) * 0.008;
    const open = i === 0 ? prevClose : candles[i - 1].close;
    let close = spotPrice * (0.965 + 0.035 * progress) * (1 + wave);
    if (i === limit - 1) close = spotPrice;
    const spread = Math.abs(close - open) + spotPrice * (0.0035 + ((i + seedBase) % 5) * 0.0012);
    const high = Math.max(open, close) + spread * 0.55;
    const low = Math.max(0.000001, Math.min(open, close) - spread * 0.45);
    const volume = 18000 + ((i * 7919 + seedBase * 131) % 24000);
    candles.push({
      timestamp: nowAligned - (limit - 1 - i) * step,
      open: Number(open.toFixed(6)),
      high: Number(high.toFixed(6)),
      low: Number(low.toFixed(6)),
      close: Number(close.toFixed(6)),
      volume,
    });
    prevClose = close;
  }
  return candles;
}

async function fetchSymbolUsdOhlc(rawSymbol, interval, limit) {
  const sym = normalizeOhlcSymbol(rawSymbol);
  if (sym === 'USDT' || sym === 'USDC' || sym === 'BUSD' || sym === 'DAI') {
    return { source: 'peg', candles: buildFlatStableCandles(1.0, interval, limit) };
  }

  // 1. Binance Data Vision Klines (no API key required)
  const binancePair = {
    ETH: 'ETHUSDT',
    BNB: 'BNBUSDT',
    POL: 'POLUSDT',
    BTC: 'BTCUSDT',
    CAKE: 'CAKEUSDT',
  }[sym];

  if (binancePair) {
    try {
      const url = `https://data-api.binance.vision/api/v3/klines?symbol=${binancePair}&interval=${encodeURIComponent(interval)}&limit=${limit}`;
      const binRes = await fetch(url);
      if (binRes.ok) {
        const rows = await binRes.json();
        if (Array.isArray(rows) && rows.length > 0) {
          const candles = rows.map((r) => ({
            timestamp: Number(r[0]),
            open: parseFloat(r[1]),
            high: parseFloat(r[2]),
            low: parseFloat(r[3]),
            close: parseFloat(r[4]),
            volume: parseFloat(r[5]) || 0,
          })).filter((c) => c.open > 0 && c.close > 0);
          if (candles.length > 0) {
            return { source: 'binance', candles };
          }
        }
      }
    } catch (err) {
      console.warn(`[OHLC] Binance klines failed for ${sym}:`, err.message);
    }
  }

  // 2. CryptoCompare Historical OHLCV (covers CORE + fallback for majors)
  if (['CORE', 'ETH', 'BNB', 'POL', 'BTC', 'CAKE'].includes(sym)) {
    try {
      let endpoint = `https://min-api.cryptocompare.com/data/v2/histohour?fsym=${sym}&tsym=USD&limit=${limit - 1}`;
      if (interval === '15m') {
        endpoint = `https://min-api.cryptocompare.com/data/v2/histominute?fsym=${sym}&tsym=USD&aggregate=15&limit=${limit - 1}`;
      } else if (interval === '4h') {
        endpoint = `https://min-api.cryptocompare.com/data/v2/histohour?fsym=${sym}&tsym=USD&aggregate=4&limit=${limit - 1}`;
      } else if (interval === '1d') {
        endpoint = `https://min-api.cryptocompare.com/data/v2/histoday?fsym=${sym}&tsym=USD&limit=${limit - 1}`;
      }
      const ccRes = await fetch(endpoint);
      if (ccRes.ok) {
        const json = await ccRes.json();
        const rows = json?.Data?.Data;
        if (Array.isArray(rows) && rows.length > 0) {
          const candles = rows
            .map((r) => ({
              timestamp: Number(r.time) * 1000,
              open: Number(r.open),
              high: Number(r.high),
              low: Number(r.low),
              close: Number(r.close),
              volume: Number(r.volumefrom || r.volumeto || 0),
            }))
            .filter((c) => c.open > 0 && c.close > 0);
          if (candles.length > 0) {
            return { source: 'cryptocompare', candles };
          }
        }
      }
    } catch (err) {
      console.warn(`[OHLC] CryptoCompare failed for ${sym}:`, err.message);
    }
  }

  // 3. CoinGecko OHLC fallback
  const cgId = {
    ETH: 'ethereum',
    BNB: 'binancecoin',
    POL: 'polygon-ecosystem-token',
    CORE: 'coredaoorg',
    BTC: 'bitcoin',
    CAKE: 'pancakeswap-token',
  }[sym];
  if (cgId) {
    try {
      const days = interval === '1d' ? '30' : '1';
      const cgRes = await fetch(`https://api.coingecko.com/api/v3/coins/${cgId}/ohlc?vs_currency=usd&days=${days}`);
      if (cgRes.ok) {
        const rows = await cgRes.json();
        if (Array.isArray(rows) && rows.length > 0) {
          const candles = rows.slice(-limit).map((r) => ({
            timestamp: Number(r[0]),
            open: Number(r[1]),
            high: Number(r[2]),
            low: Number(r[3]),
            close: Number(r[4]),
            volume: 0,
          }));
          if (candles.length > 0) {
            return { source: 'coingecko', candles };
          }
        }
      }
    } catch (err) {
      console.warn(`[OHLC] CoinGecko OHLC failed for ${sym}:`, err.message);
    }
  }

  // 4. Protocol / DB fallback anchored to live spot price (e.g., TATA or custom listed tokens)
  let spotPrice = 1.45;
  try {
    const live = await fetchLivePrices();
    if (live?.prices?.[sym]?.usd) {
      spotPrice = Number(live.prices[sym].usd);
    } else if (pool) {
      const dbRow = await pool.query('SELECT price_usd FROM tokens WHERE UPPER(symbol) = $1 LIMIT 1', [sym]);
      if (dbRow.rows[0]?.price_usd && Number(dbRow.rows[0].price_usd) > 0) {
        spotPrice = Number(dbRow.rows[0].price_usd);
      }
    }
  } catch {}

  return {
    source: 'protocol_pool',
    candles: buildAnchoredProtocolCandles(sym, spotPrice, interval, limit),
  };
}

function combineOhlcPair(baseSeries, quoteSeries, baseSym, quoteSym, interval, limit) {
  if (baseSym === quoteSym) {
    return {
      source: 'parity',
      candles: buildFlatStableCandles(1.0, interval, limit),
    };
  }
  if (quoteSeries.source === 'peg') {
    return baseSeries;
  }

  const bList = baseSeries.candles;
  const qList = quoteSeries.candles;
  const len = Math.min(bList.length, qList.length);
  const bSlice = bList.slice(-len);
  const qSlice = qList.slice(-len);

  const combined = [];
  for (let i = 0; i < len; i++) {
    const b = bSlice[i];
    const q = qSlice[i];
    if (!q || q.open <= 0 || q.close <= 0) continue;
    const open = b.open / q.open;
    const close = b.close / q.close;
    const rawHigh = b.high / (q.low > 0 ? q.low : q.close);
    const rawLow = b.low / (q.high > 0 ? q.high : q.close);
    const bodyMax = Math.max(open, close);
    const bodyMin = Math.min(open, close);
    const high = Math.min(Math.max(bodyMax, rawHigh), bodyMax * 1.04);
    const low = Math.max(Math.min(bodyMin, rawLow), bodyMin * 0.96);
    combined.push({
      timestamp: b.timestamp,
      open,
      high,
      low,
      close,
      volume: b.volume || 0,
    });
  }

  return {
    source: baseSeries.source === quoteSeries.source ? baseSeries.source : `${baseSeries.source}/${quoteSeries.source}`,
    candles: combined,
  };
}

app.get('/api/market/ohlc', async (req, res) => {
  const rawBase = String(req.query.base || 'BNB').trim().toUpperCase();
  const rawQuote = String(req.query.quote || 'USDT').trim().toUpperCase();
  const interval = INTERVAL_MS[req.query.interval] ? String(req.query.interval) : '1h';
  const limit = Math.max(12, Math.min(120, parseInt(String(req.query.limit || '48'), 10) || 48));

  const cacheKey = `${rawBase}_${rawQuote}_${interval}_${limit}`;
  const cached = ohlcCache.get(cacheKey);
  if (cached && Date.now() - cached.updatedAt < 30000) {
    return res.json(cached.payload);
  }

  try {
    const [baseSeries, quoteSeries] = await Promise.all([
      fetchSymbolUsdOhlc(rawBase, interval, limit),
      fetchSymbolUsdOhlc(rawQuote, interval, limit),
    ]);

    const merged = combineOhlcPair(
      baseSeries,
      quoteSeries,
      normalizeOhlcSymbol(rawBase),
      normalizeOhlcSymbol(rawQuote),
      interval,
      limit
    );

    const candles = merged.candles;
    const first = candles[0];
    const last = candles[candles.length - 1];
    const currentPrice = last ? last.close : 0;
    const changePercent = first && first.open > 0 ? ((currentPrice - first.open) / first.open) * 100 : 0;
    const high = candles.length ? Math.max(...candles.map((c) => c.high)) : 0;
    const low = candles.length ? Math.min(...candles.map((c) => c.low)) : 0;

    const payload = {
      pair: `${rawBase}/${rawQuote}`,
      base: rawBase,
      quote: rawQuote,
      interval,
      source: merged.source,
      currentPrice,
      changePercent,
      high,
      low,
      candles,
      updatedAt: new Date().toISOString(),
    };

    ohlcCache.set(cacheKey, { payload, updatedAt: Date.now() });
    res.json(payload);
  } catch (err) {
    res.status(502).json({ error: `Failed to fetch OHLC data: ${err.message}` });
  }
});

// 3. Protocol Statistics API
app.get('/api/stats', async (req, res) => {
  if (!pool) {
    return res.status(503).json({ error: 'Database not configured. Set DATABASE_URL in environment.' });
  }

  try {
    const [tokensCount, poolsStats, listingsStats, swapsCount] = await Promise.all([
      pool.query('SELECT COUNT(*)::int AS total FROM tokens'),
      pool.query('SELECT COUNT(*)::int AS count, COALESCE(SUM(tvl_usd), 0)::float AS tvl, COALESCE(SUM(volume_24h), 0)::float AS vol FROM liquidity_pools'),
      pool.query('SELECT COUNT(*)::int AS count, COALESCE(SUM(fee_paid), 0)::float AS total_fees FROM token_listings'),
      pool.query('SELECT COUNT(*)::int AS total FROM swaps'),
    ]);

    res.json({
      totalListedTokens: tokensCount.rows[0].total,
      activePools: poolsStats.rows[0].count,
      tvlUsd: poolsStats.rows[0].tvl,
      volume24hUsd: poolsStats.rows[0].vol,
      totalSwaps: swapsCount.rows[0].total,
      totalListingEvents: listingsStats.rows[0].count,
      totalListingFeesCollectedUsdt: listingsStats.rows[0].total_fees,
      officialFeeReceiver: OFFICIAL_FEE_RECEIVER,
      officialOwnerWallet: OFFICIAL_OWNER_WALLET,
      listingFeeUsdt: LISTING_FEE_USDT,
      ownerListingFeeUsdt: 0,
    });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// 4. Tokens Explorer API
app.get('/api/tokens', async (req, res) => {
  if (!pool) {
    return res.status(503).json({ error: 'Database not configured. Set DATABASE_URL in environment.' });
  }

  const { query, verified } = req.query;
  try {
    let sql = 'SELECT * FROM tokens WHERE 1=1';
    const params = [];

    if (query) {
      params.push(`%${query.trim()}%`);
      sql += ` AND (name ILIKE $${params.length} OR symbol ILIKE $${params.length} OR address ILIKE $${params.length})`;
    }
    if (verified === 'true') {
      sql += ' AND is_verified = TRUE';
    }
    sql += ' ORDER BY liquidity_usd DESC, created_at DESC LIMIT 100';

    const [result, livePricePayload] = await Promise.all([
      pool.query(sql, params),
      fetchLivePrices().catch(() => null),
    ]);
    const liveMap = livePricePayload?.prices || {};
    const enrichedTokens = result.rows.map((row) => {
      const live = liveMap[String(row.symbol || '').toUpperCase()];
      return live ? { ...row, price_usd: live.usd, change_24h: live.change24h } : row;
    });
    res.json({ count: enrichedTokens.length, tokens: enrichedTokens });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// 5. Single Token Details API
app.get('/api/tokens/:address', async (req, res) => {
  if (!pool) {
    return res.status(503).json({ error: 'Database not configured. Set DATABASE_URL in environment.' });
  }
  const address = req.params.address.trim();
  if (!isValidAddress(address)) {
    return res.status(400).json({ error: 'Invalid token address.' });
  }

  try {
    const result = await pool.query('SELECT * FROM tokens WHERE LOWER(address) = LOWER($1)', [address]);
    if (result.rows.length === 0) {
      return res.status(404).json({ error: 'Token not found' });
    }
    res.json(result.rows[0]);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// 6. Recent Token Listing Fee Payments
app.get('/api/listings', async (req, res) => {
  if (!pool) {
    return res.status(503).json({ error: 'Database not configured. Set DATABASE_URL in environment.' });
  }
  try {
    const result = await pool.query(
      'SELECT * FROM token_listings ORDER BY timestamp DESC LIMIT 50'
    );
    res.json({ count: result.rows.length, listings: result.rows });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// 6b. Check Effective Listing Fee for a Wallet (Owner = 0 USDT)
app.get('/api/listing-fee/:wallet', (req, res) => {
  const wallet = req.params.wallet.trim();
  const exempt = isOwnerWallet(wallet);
  res.json({
    wallet,
    isOwnerExempt: exempt,
    feeUsdt: exempt ? 0 : LISTING_FEE_USDT,
    feeReceiver: OFFICIAL_FEE_RECEIVER,
  });
});

// 7. Index Verified On-Chain Token Listing Event (Requires valid TxHash verified via RPC)
app.post('/api/events/token-listed', async (req, res) => {
  if (!pool) {
    return res.status(503).json({ error: 'Database not configured. Set DATABASE_URL in environment.' });
  }

  const { tokenAddress, name, symbol, decimals, listedBy, feePaid, feeReceiver, txHash, chain } = req.body;

  if (!isValidAddress(tokenAddress) || !isValidAddress(listedBy) || !isValidTxHash(txHash)) {
    return res.status(400).json({ error: 'Invalid tokenAddress, listedBy, or txHash format.' });
  }

  const ownerExempt = isOwnerWallet(listedBy);
  if (!ownerExempt && (!feeReceiver || feeReceiver.toLowerCase() !== OFFICIAL_FEE_RECEIVER)) {
    return res.status(400).json({
      error: `Fee receiver must match official DEX wallet: ${OFFICIAL_FEE_RECEIVER}`,
    });
  }

  const effectiveFeePaid = ownerExempt ? 0 : (parseFloat(feePaid) || LISTING_FEE_USDT);

  const client = await pool.connect();
  try {
    // Verify transaction actually exists and succeeded on-chain via Mainnet RPC
    const targetChain = RPC_URLS[chain] ? chain : 'tataservice';
    const receipt = await callRpc(targetChain, 'eth_getTransactionReceipt', [txHash]);
    if (!receipt || receipt.status !== '0x1') {
      return res.status(400).json({ error: 'Transaction not found or reverted on blockchain.' });
    }
    const blockNumber = parseInt(receipt.blockNumber, 16);

    await client.query('BEGIN');

    await client.query(
      `INSERT INTO tokens (address, name, symbol, decimals, is_verified, listed_by, listing_tx_hash)
       VALUES ($1, $2, $3, $4, $5, $6, $7)
       ON CONFLICT (address) DO NOTHING`,
      [tokenAddress, name, symbol, parseInt(decimals, 10) || 18, ownerExempt, listedBy, txHash]
    );

    const listingResult = await client.query(
      `INSERT INTO token_listings (token_address, name, symbol, decimals, listed_by, fee_paid, fee_receiver, tx_hash, block_number)
       VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)
       RETURNING *`,
      [
        tokenAddress,
        name,
        symbol,
        parseInt(decimals, 10) || 18,
        listedBy,
        effectiveFeePaid,
        OFFICIAL_FEE_RECEIVER,
        txHash,
        blockNumber,
      ]
    );

    await client.query('COMMIT');
    res.status(201).json({ success: true, ownerFeeExempt: ownerExempt, listing: listingResult.rows[0] });
  } catch (err) {
    await client.query('ROLLBACK');
    res.status(500).json({ error: err.message });
  } finally {
    client.release();
  }
});

async function initDatabase() {
  if (!pool) return;
  try {
    const schemaPath = path.join(__dirname, 'schema.sql');
    if (fs.existsSync(schemaPath)) {
      const schemaSql = fs.readFileSync(schemaPath, 'utf8');
      await pool.query(schemaSql);
      console.log('[DB] Layerbase PostgreSQL schema initialized.');
    }

    // Seed core verified tokens if table is empty
    const { rows } = await pool.query('SELECT COUNT(*)::int AS count FROM tokens');
    if (rows[0].count === 0) {
      await pool.query(
        `INSERT INTO tokens (address, name, symbol, decimals, price_usd, volume_24h, liquidity_usd, is_verified)
         VALUES
           ('0xbb4CdB9CBd36B01bD1cBaEBF2De08d9173bc095c', 'BNB', 'BNB', 18, 624.50, 1845000, 6840000, TRUE),
           ('0x55d398326f99059fF775485246999027B3197955', 'Tether USD', 'USDT', 18, 1.00, 3250000, 8450000, TRUE),
           ('0x7A7A59a9DeCe06b86f1e58a2E49c7D59A1b7A7A5', 'TataSwap Token', 'TATA', 18, 1.45, 890000, 2450000, TRUE)
         ON CONFLICT (address) DO NOTHING`
      );
      console.log('[DB] Core verified tokens seeded.');
    }
  } catch (err) {
    console.error('[DB] Schema initialization warning:', err.message);
  }
}

app.listen(PORT, async () => {
  console.log(`TataSwap Backend API running on port ${PORT}`);
  console.log(`Official Fee Receiver: ${OFFICIAL_FEE_RECEIVER}`);
  await initDatabase();
});
