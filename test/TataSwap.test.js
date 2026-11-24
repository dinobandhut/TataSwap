const { expect } = require("chai");
const { ethers } = require("hardhat");

describe("TataSwap Smart Contracts Suite", function () {
  let owner, user, feeReceiver, mockUsdt, listingFeeContract, factory;
  const OFFICIAL_RECEIVER = "0xD9e5c5d0522B1eeE8Ff9bC006F64d53d2C411CA4";
  const LISTING_FEE = ethers.parseUnits("5.0", 18); // 5 USDT

  beforeEach(async function () {
    [owner, user, feeReceiver] = await ethers.getSigners();

    // 1. Deploy Mock USDT
    const MockToken = await ethers.getContractFactory("MockERC20");
    // MockToken helper or inline standard ERC20 for tests
  });

  it("Should configure the official DEX fee receiver to 0xD9e5c5d0522B1eeE8Ff9bC006F64d53d2C411CA4", async function () {
    const ListingFee = await ethers.getContractFactory("TataSwapListingFee");
    const listingFee = await ListingFee.deploy(
      OFFICIAL_RECEIVER,
      ethers.ZeroAddress,
      LISTING_FEE
    );
    await listingFee.waitForDeployment();

    expect(await listingFee.feeReceiver()).to.equal(OFFICIAL_RECEIVER);
    expect(await listingFee.listingFeeAmount()).to.equal(LISTING_FEE);
  });

  it("Should allow only owner to modify listing fee and fee receiver", async function () {
    const ListingFee = await ethers.getContractFactory("TataSwapListingFee");
    const listingFee = await ListingFee.deploy(
      OFFICIAL_RECEIVER,
      ethers.ZeroAddress,
      LISTING_FEE
    );
    await listingFee.waitForDeployment();

    // User attempts to change fee
    await expect(
      listingFee.connect(user).setListingFee(ethers.parseUnits("10.0", 18))
    ).to.be.revertedWith("TataSwap: caller is not the owner");

    // Owner changes fee
    await listingFee.connect(owner).setListingFee(ethers.parseUnits("10.0", 18));
    expect(await listingFee.listingFeeAmount()).to.equal(ethers.parseUnits("10.0", 18));
  });

  it("Should respect emergency pause switch", async function () {
    const ListingFee = await ethers.getContractFactory("TataSwapListingFee");
    const listingFee = await ListingFee.deploy(
      OFFICIAL_RECEIVER,
      ethers.ZeroAddress,
      LISTING_FEE
    );
    await listingFee.waitForDeployment();

    await listingFee.connect(owner).setEmergencyPause(true);
    expect(await listingFee.isPaused()).to.equal(true);

    await expect(
      listingFee.connect(user).listToken(user.address, "Test", "TST", 18)
    ).to.be.revertedWith("TataSwap: listing contract is paused");
  });
});
