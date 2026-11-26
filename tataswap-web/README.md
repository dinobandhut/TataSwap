# TataSwap Web (`dinobandhut/Tataswap-web`)

Official Web3 Frontend for **TataSwap DEX** — a permissionless AMM & token listing decentralized exchange on **BNB Smart Chain** (and EVM networks).

- **GitHub Repository:** `https://github.com/dinobandhut/Tataswap-web.git`
- **Official DEX Fee Receiver Wallet:** `0x3f48248FFE235a5205d1D781fB6c852c71fc1556`
- **Permissionless Listing Fee:** `5.00 USDT`

---

## Push This Folder to `Tataswap-web.git`

```bash
cd tataswap-web
git init
git branch -M main
git remote add origin https://github.com/dinobandhut/Tataswap-web.git
git add .
git commit -m "Initial TataSwap Web DEX release"
git push -u origin main
```

---

## Deploy on Cloudflare Workers / Pages

In Cloudflare Build Settings for `dinobandhut/Tataswap-web`:
- **Root directory:** `/`
- **Build command:** `npm run build` *(or leave empty)*
- **Deploy command:** `npx wrangler deploy`
