# TataSwap – Full Decentralized Cryptocurrency Exchange (DEX)

TataSwap is a production-oriented, permissionless decentralized cryptocurrency exchange inspired by Uniswap V3 and PancakeSwap, natively built for **BNB Smart Chain** (with multi-chain architecture for Ethereum, Polygon, Arbitrum, and Base).

---

## 🔑 DEX Core Configurations

- **Project Name:** TataSwap
- **Default Network:** BNB Smart Chain (BSC Testnet & Mainnet)
- **Official DEX Owner & Fee Receiver Wallet:**  
  `0xD9e5c5d0522B1eeE8Ff9bC006F64d53d2C411CA4`
- **Official Token Listing Fee:** **5.00 USDT**

---

## 🌟 Key Features & Architecture

### 1. Permissionless Token Listing with Automated Fee Collection
- **100% Permissionless:** Any wallet user can list their own BEP-20 or ERC-20 token without admin approval.
- **On-Chain Automated Fee:** When a user registers a listing, the `TataSwapListingFee.sol` contract automatically transfers 5 USDT directly to the DEX Owner's official wallet (`0xD9e5c5d0522B1eeE8Ff9bC006F64d53d2C411CA4`).
- **RPC Contract Verification:** Inspects token bytecode, name, symbol, decimals, and total supply via live JSON-RPC calls.
- **Safety Labels:** Clearly tags verified tokens vs community unverified tokens to protect traders from malicious contracts.

### 2. AMM Swap Engine
- Constant Product Formula ($x \cdot y = k$) with concentrated liquidity fee tiers (0.05%, 0.3%, 1.0%).
- Live quote calculation, slippage protection (0.1%, 0.5%, 1.0%, custom), price impact analysis, minimum received guarantees, and deadline checks.
- Transparent trading fees (0.25% standard: 0.20% to Liquidity Providers, 0.05% to Protocol Fee Treasury).

### 3. Liquidity Pools
- Create trading pairs, supply dual-sided liquidity, mint LP tokens, and earn swap fees.
- Real-time pool reserve tracking, pool share percentage calculation, and dynamic APR metrics.

### 4. 12 Dedicated DEX Screens (Android Jetpack Compose & Web)
1. **Home Dashboard:** Protocol TVL, 24h volume, quick swap card, trending tokens, recent transactions.
2. **Swap:** Token selector, live rate quotes, slippage tolerance, gas estimation, on-chain execution.
3. **Explore Tokens:** Searchable token registry with filters (Verified, Community, Top Gainers).
4. **Token Details:** Price chart canvas, contract address copy, BscScan explorer link, liquidity metrics.
5. **Create Token Listing:** Permissionless listing wizard with automated 5 USDT fee to `0xD9e5c5d0522B1eeE8Ff9bC006F64d53d2C411CA4`.
6. **Create Liquidity Pool:** Custom token pair creator with fee tier selection.
7. **Add Liquidity:** Auto-balanced deposit calculation and LP minting.
8. **Remove Liquidity:** Percentage slider (25% to 100%) and token withdrawal preview.
9. **Portfolio:** Wallet net worth, token balances, active LP positions, accrued fee earnings.
10. **Transaction History:** Live list of swaps, additions, and listings with verified tx hashes.
11. **Analytics:** TVL and volume chart, fee collection analytics, top pools leaderboard.
12. **Admin Dashboard:** Owner controls for fee modification, receiver wallet update, emergency pause toggle, and verification badges.
13. **Web DEX View:** Interactive in-app Web3 DEX responsive interface.

---

## 📜 Smart Contracts (`/contracts`)

- `TataSwapListingFee.sol`: Collects 5 USDT listing fee and routes it to `0xD9e5c5d0522B1eeE8Ff9bC006F64d53d2C411CA4`. Includes reentrancy protection, emergency pause, and event logging (`TokenListed`).
- `TataSwapFactory.sol`: Deploys and indexes all liquidity pool pairs.
- `TataSwapRouter.sol`: Multi-hop swap execution, deadline enforcement, slippage validation, liquidity addition.
- `TataSwapPool.sol`: Core AMM pool with constant product math, `mint`, `burn`, and `swap`.
- `TataSwapFeeManager.sol`: Configures fee distribution between LPs and protocol treasury.

---

## 🚀 Deployment Instructions

### Smart Contracts (Hardhat)
```bash
# Install dependencies
npm install --save-dev hardhat @nomicfoundation/hardhat-toolbox

# Run unit tests
npx hardhat test

# Deploy to BNB Smart Chain Testnet
npx hardhat run scripts/deploy.js --network bscTestnet

# Deploy to BNB Smart Chain Mainnet
npx hardhat run scripts/deploy.js --network bscMainnet
```

### Backend Indexer (Node.js & Google Cloud Run)
```bash
cd backend
npm install
npm start
```

### Database (Google Cloud SQL)
Import `backend/schema.sql` into your Cloud SQL PostgreSQL instance to index token metadata, pool reserves, and listing events.

---

## 🔒 Security Checklist Prior to Mainnet

1. Conduct a full independent third-party audit of all Solidity smart contracts.
2. Keep listing fee receiving keys secure with multi-signature hardware custody (e.g. Gnosis Safe).
3. Verify contract source code on BscScan using Hardhat Etherscan plugin.
4. Ensure emergency pause multi-sig authorization rules are active.
