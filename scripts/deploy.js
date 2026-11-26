const hre = require("hardhat");

async function main() {
  console.log("=== TataSwap Protocol Deployment ===");

  // Official DEX Fee Receiver Wallet as configured:
  const OFFICIAL_FEE_RECEIVER = "0x3f48248FFE235a5205d1D781fB6c852c71fc1556";
  console.log(`DEX Owner & Fee Receiver: ${OFFICIAL_FEE_RECEIVER}`);

  // 1. Deploy TataSwapFactory
  const Factory = await hre.ethers.getContractFactory("TataSwapFactory");
  const factory = await Factory.deploy(OFFICIAL_FEE_RECEIVER);
  await factory.waitForDeployment();
  const factoryAddress = await factory.getAddress();
  console.log(`✓ TataSwapFactory deployed to: ${factoryAddress}`);

  // 2. Deploy TataSwapRouter
  const Router = await hre.ethers.getContractFactory("TataSwapRouter");
  const router = await Router.deploy(factoryAddress);
  await router.waitForDeployment();
  const routerAddress = await router.getAddress();
  console.log(`✓ TataSwapRouter deployed to: ${routerAddress}`);

  // 3. Deploy TataSwapFeeManager
  const FeeManager = await hre.ethers.getContractFactory("TataSwapFeeManager");
  const feeManager = await FeeManager.deploy(OFFICIAL_FEE_RECEIVER);
  await feeManager.waitForDeployment();
  const feeManagerAddress = await feeManager.getAddress();
  console.log(`✓ TataSwapFeeManager deployed to: ${feeManagerAddress}`);

  // 4. Deploy TataSwapListingFee (Configured with 5 USDT fee to official receiver)
  // Mainnet USDT addresses by chainId (override with process.env.USDT_ADDRESS for TataService / Core)
  const network = await hre.ethers.provider.getNetwork();
  const mainnetUsdtByChain = {
    1n: "0xdAC17F958D2ee523a2206206994597C13D831ec7",     // Ethereum Mainnet USDT
    56n: "0x55d398326f99059fF775485246999027B3197955",    // BNB Smart Chain Mainnet USDT
    137n: "0xc2132D05D31c914a87C6611C10748AEb04B58e8F",   // Polygon Mainnet USDT
    1116n: "0x900101d06A7426441Ae63e9AB3B9b0F63Be145F1",  // Core Mainnet USDT
  };
  const usdtAddress =
    process.env.USDT_ADDRESS ||
    mainnetUsdtByChain[network.chainId] ||
    "0x55d398326f99059fF775485246999027B3197955";

  const LISTING_FEE_AMOUNT = hre.ethers.parseUnits("5.0", 18); // 5 USDT

  const ListingFee = await hre.ethers.getContractFactory("TataSwapListingFee");
  const listingFee = await ListingFee.deploy(
    OFFICIAL_FEE_RECEIVER,
    usdtAddress,
    LISTING_FEE_AMOUNT
  );
  await listingFee.waitForDeployment();
  const listingFeeAddress = await listingFee.getAddress();
  console.log(`✓ TataSwapListingFee deployed to: ${listingFeeAddress}`);
  console.log(`  - Configured Fee: 5 USDT`);
  console.log(`  - Target Receiver Wallet: ${OFFICIAL_FEE_RECEIVER}`);

  console.log("\n=== Deployment Summary ===");
  console.log({
    Factory: factoryAddress,
    Router: routerAddress,
    FeeManager: feeManagerAddress,
    ListingFee: listingFeeAddress,
    FeeReceiver: OFFICIAL_FEE_RECEIVER,
    Network: network.name,
    ChainId: Number(network.chainId),
  });
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
