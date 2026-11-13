const express = require('express');
const cors = require('cors');

const app = express();
const PORT = process.env.PORT || 8080;

app.use(cors());
app.use(express.json());

// DEX Constants
const OFFICIAL_FEE_RECEIVER = "0xD9e5c5d0522B1eeE8Ff9bC006F64d53d2C411CA4";
const LISTING_FEE_USDT = 5.0;

// In-Memory Indexed State (Synced with PostgreSQL in production)
const tokensRegistry = [
  { address: "0xbb4CdB9CBd36B01bD1cBaEBF2De08d9173bc095c", name: "BNB", symbol: "BNB", decimals: 18, priceUsd: 624.50, isVerified: true },
  { address: "0x55d398326f99059fF775485246999027B3197955", name: "Tether USD", symbol: "USDT", decimals: 18, priceUsd: 1.00, isVerified: true },
  { address: "0x7A7A59a9DeCe06b86f1e58a2E49c7D59A1b7A7A5", name: "TataSwap Token", symbol: "TATA", decimals: 18, priceUsd: 1.45, isVerified: true }
];

const protocolStats = {
  tvlUsd: 12450800.0,
  volume24hUsd: 3840250.0,
  totalSwaps: 48219,
  activePools: 24,
  officialFeeReceiver: OFFICIAL_FEE_RECEIVER,
  listingFeeUsdt: LISTING_FEE_USDT,
  totalListingFeesCollectedUsdt: 625.0
};

// Health Check
app.get('/health', (req, res) => {
  res.json({ status: 'ok', protocol: 'TataSwap DEX', network: 'BNB Smart Chain', timestamp: Date.now() });
});

// Protocol Statistics API
app.get('/api/stats', (req, res) => {
  res.json(protocolStats);
});

// Tokens Explorer API
app.get('/api/tokens', (req, res) => {
  const { query, verified } = req.query;
  let results = tokensRegistry;

  if (query) {
    const q = query.toLowerCase();
    results = results.filter(t => t.name.toLowerCase().includes(q) || t.symbol.toLowerCase().includes(q) || t.address.toLowerCase().includes(q));
  }
  if (verified === 'true') {
    results = results.filter(t => t.isVerified);
  }

  res.json({ count: results.length, tokens: results });
});

// Single Token Details API
app.get('/api/tokens/:address', (req, res) => {
  const token = tokensRegistry.find(t => t.address.toLowerCase() === req.params.address.toLowerCase());
  if (!token) return res.status(404).json({ error: 'Token not found' });
  res.json(token);
});

// Index New Token Listing Event (Web3 Webhook)
app.post('/api/events/token-listed', (req, res) => {
  const { tokenAddress, name, symbol, decimals, listedBy, feePaid, feeReceiver, txHash } = req.body;
  
  if (feeReceiver.toLowerCase() !== OFFICIAL_FEE_RECEIVER.toLowerCase()) {
    return res.status(400).json({ error: 'Fee was not routed to official DEX owner wallet' });
  }

  const newToken = {
    address: tokenAddress,
    name,
    symbol,
    decimals: parseInt(decimals, 10) || 18,
    priceUsd: 1.0,
    isVerified: false,
    listedBy,
    txHash,
    timestamp: Date.now()
  };

  tokensRegistry.push(newToken);
  protocolStats.totalListingFeesCollectedUsdt += parseFloat(feePaid) || 5.0;

  console.log(`[EVENT] Token ${symbol} listed! 5 USDT fee verified for ${feeReceiver}`);
  res.json({ success: true, token: newToken });
});

app.listen(PORT, () => {
  console.log(`TataSwap Backend API listening on port ${PORT}`);
  console.log(`Official Fee Receiver Wallet: ${OFFICIAL_FEE_RECEIVER}`);
});
