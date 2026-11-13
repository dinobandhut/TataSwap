-- TataSwap PostgreSQL Database Schema
-- Deployed on Google Cloud SQL (PostgreSQL 15+)

CREATE TABLE IF NOT EXISTS tokens (
    address VARCHAR(42) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    symbol VARCHAR(20) NOT NULL,
    decimals INT NOT NULL DEFAULT 18,
    total_supply VARCHAR(78),
    price_usd NUMERIC(18, 6) DEFAULT 0.0,
    volume_24h NUMERIC(24, 2) DEFAULT 0.0,
    liquidity_usd NUMERIC(24, 2) DEFAULT 0.0,
    is_verified BOOLEAN DEFAULT FALSE,
    listed_by VARCHAR(42),
    listing_tx_hash VARCHAR(66),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS liquidity_pools (
    id VARCHAR(100) PRIMARY KEY,
    token0_address VARCHAR(42) REFERENCES tokens(address),
    token1_address VARCHAR(42) REFERENCES tokens(address),
    fee_tier INT NOT NULL, -- 500, 3000, 10000
    pool_address VARCHAR(42) UNIQUE NOT NULL,
    reserve0 NUMERIC(36, 18) DEFAULT 0,
    reserve1 NUMERIC(36, 18) DEFAULT 0,
    tvl_usd NUMERIC(24, 2) DEFAULT 0.0,
    volume_24h NUMERIC(24, 2) DEFAULT 0.0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS token_listings (
    id SERIAL PRIMARY KEY,
    token_address VARCHAR(42) NOT NULL,
    name VARCHAR(100) NOT NULL,
    symbol VARCHAR(20) NOT NULL,
    decimals INT NOT NULL,
    listed_by VARCHAR(42) NOT NULL,
    fee_paid NUMERIC(18, 6) NOT NULL, -- 5.00 USDT
    fee_receiver VARCHAR(42) NOT NULL DEFAULT '0xD9e5c5d0522B1eeE8Ff9bC006F64d53d2C411CA4',
    tx_hash VARCHAR(66) UNIQUE NOT NULL,
    block_number BIGINT NOT NULL,
    timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS swaps (
    id SERIAL PRIMARY KEY,
    pool_id VARCHAR(100) REFERENCES liquidity_pools(id),
    sender VARCHAR(42) NOT NULL,
    recipient VARCHAR(42) NOT NULL,
    amount0_in NUMERIC(36, 18) DEFAULT 0,
    amount1_in NUMERIC(36, 18) DEFAULT 0,
    amount0_out NUMERIC(36, 18) DEFAULT 0,
    amount1_out NUMERIC(36, 18) DEFAULT 0,
    tx_hash VARCHAR(66) NOT NULL,
    timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for high throughput queries
CREATE INDEX IF NOT EXISTS idx_tokens_symbol ON tokens(symbol);
CREATE INDEX IF NOT EXISTS idx_token_listings_receiver ON token_listings(fee_receiver);
CREATE INDEX IF NOT EXISTS idx_swaps_timestamp ON swaps(timestamp DESC);
