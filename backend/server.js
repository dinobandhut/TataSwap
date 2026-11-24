require('dotenv').config();
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
  process.env.OFFICIAL_FEE_RECEIVER || '0xD9e5c5d0522B1eeE8Ff9bC006F64d53d2C411CA4'
).toLowerCase();
const LISTING_FEE_USDT = parseFloat(process.env.LISTING_FEE_USDT || '5.0');

// Mainnet Blockchain RPC Endpoints (TataService, ETH, BNB, Polygon, Core, Solana)
const RPC_URLS = {
  tataservice: process.env.TATA_RPC_URL || 'https://rpctata.duckdns.org/rpc',
  eth: process.env.ETH_RPC_URL || 'https://public.1rpc.io/eth',
  bsc: process.env.BSC_RPC_URL || 'https://public.1rpc.io/bnb',
  polygon: process.env.POLYGON_RPC_URL || 'https://public.1rpc.io/matic',
  core: process.env.CORE_RPC_URL || 'https://rpc.coredao.org',
  solana: process.env.SOLANA_RPC_URL || 'https://api.mainnet.solana.com',
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
    { key: 'solana', label: 'solanaSlot', method: 'getSlot', isHex: false },
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

// 2. Live On-Chain Token Contract Inspection via Mainnet RPCs
app.get('/api/rpc/inspect/:address', async (req, res) => {
  const address = req.params.address.trim();
  const chain = RPC_URLS[req.query.chain] ? req.query.chain : 'tataservice';

  if (chain === 'solana') {
    if (address.length < 32 || address.length > 44) {
      return res.status(400).json({ error: 'Invalid Solana SPL Mint address format.' });
    }
    try {
      const acc = await callRpc('solana', 'getAccountInfo', [address, { encoding: 'jsonParsed' }]);
      const info = acc?.value?.data?.parsed?.info;
      return res.json({
        address,
        chain: 'solana',
        name: 'Solana SPL Token',
        symbol: 'SPL',
        decimals: info?.decimals ?? 9,
        contractBytecodeExists: Boolean(acc?.value),
      });
    } catch (err) {
      return res.status(502).json({ error: `Solana RPC inspection failed: ${err.message}` });
    }
  }

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
      listingFeeUsdt: LISTING_FEE_USDT,
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

    const result = await pool.query(sql, params);
    res.json({ count: result.rows.length, tokens: result.rows });
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

// 7. Index Verified On-Chain Token Listing Event (Requires valid TxHash verified via RPC)
app.post('/api/events/token-listed', async (req, res) => {
  if (!pool) {
    return res.status(503).json({ error: 'Database not configured. Set DATABASE_URL in environment.' });
  }

  const { tokenAddress, name, symbol, decimals, listedBy, feePaid, feeReceiver, txHash, chain } = req.body;

  if (!isValidAddress(tokenAddress) || !isValidAddress(listedBy) || !isValidTxHash(txHash)) {
    return res.status(400).json({ error: 'Invalid tokenAddress, listedBy, or txHash format.' });
  }

  if (!feeReceiver || feeReceiver.toLowerCase() !== OFFICIAL_FEE_RECEIVER) {
    return res.status(400).json({
      error: `Fee receiver must match official DEX wallet: ${OFFICIAL_FEE_RECEIVER}`,
    });
  }

  const client = await pool.connect();
  try {
    // Verify transaction actually exists and succeeded on-chain via Mainnet RPC
    const targetChain = RPC_URLS[chain] && chain !== 'solana' ? chain : 'tataservice';
    const receipt = await callRpc(targetChain, 'eth_getTransactionReceipt', [txHash]);
    if (!receipt || receipt.status !== '0x1') {
      return res.status(400).json({ error: 'Transaction not found or reverted on blockchain.' });
    }
    const blockNumber = parseInt(receipt.blockNumber, 16);

    await client.query('BEGIN');

    await client.query(
      `INSERT INTO tokens (address, name, symbol, decimals, is_verified, listed_by, listing_tx_hash)
       VALUES ($1, $2, $3, $4, FALSE, $5, $6)
       ON CONFLICT (address) DO NOTHING`,
      [tokenAddress, name, symbol, parseInt(decimals, 10) || 18, listedBy, txHash]
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
        parseFloat(feePaid) || LISTING_FEE_USDT,
        OFFICIAL_FEE_RECEIVER,
        txHash,
        blockNumber,
      ]
    );

    await client.query('COMMIT');
    res.status(201).json({ success: true, listing: listingResult.rows[0] });
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
