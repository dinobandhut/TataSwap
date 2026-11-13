const hre = require("hardhat");

async function main() {
  console.log("=== TataSwap Protocol Deployment ===");

  // Official DEX Fee Receiver Wallet as configured:
  const OFFICIAL_FEE_RECEIVER = "0xD9e5c5d0522B1eeE8Ff9bC006F64d53d2C411CA4";
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
  // BSC Testnet USDT: 0x337610d27c682E347C9cD60BE4b3b107C9d34dDd
  // BSC Mainnet USDT: 0x55d398326f99059fF775485246999027B3197955
  const network = await hre.ethers.provider.getNetwork();
  const isMainnet = network.chainId === 56n;
  const usdtAddress = isMainnet
    ? "0x55d398326f99059fF775485246999027B3197955"
    : "0x337610d27c682E347C9cD60BE4b3b107C9d34dDd";

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
