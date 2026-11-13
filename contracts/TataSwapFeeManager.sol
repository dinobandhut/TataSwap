// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

/**
 * @title TataSwapFeeManager
 * @notice Protocol fee configuration and treasury distribution.
 * Transparent swap fee rules: 0.25% total fee (0.20% to Liquidity Providers, 0.05% to Protocol Treasury).
 */
contract TataSwapFeeManager {
    address public owner;
    address public protocolFeeTreasury;
    uint24 public defaultSwapFee; // in hundredths of a pip (e.g. 2500 = 0.25%)
    uint24 public protocolFeeShare; // in basis points (e.g. 2000 = 20% of the 0.25% fee = 0.05% of trade)

    event ProtocolTreasuryUpdated(address oldTreasury, address newTreasury);
    event SwapFeeUpdated(uint24 oldFee, uint24 newFee);
    event ProtocolFeeCollected(address indexed token, address indexed recipient, uint256 amount);

    modifier onlyOwner() {
        require(msg.sender == owner, "TataSwap: caller is not owner");
        _;
    }

    constructor(address _initialTreasury) {
        require(_initialTreasury != address(0), "TataSwap: invalid treasury");
        owner = msg.sender;
        // Default official treasury / fee receiver: 0xD9e5c5d0522B1eeE8Ff9bC006F64d53d2C411CA4
        protocolFeeTreasury = _initialTreasury;
        defaultSwapFee = 2500; // 0.25%
        protocolFeeShare = 2000; // 20% of fee to treasury (0.05% net)
    }

    function setProtocolTreasury(address _newTreasury) external onlyOwner {
        require(_newTreasury != address(0), "TataSwap: invalid address");
        emit ProtocolTreasuryUpdated(protocolFeeTreasury, _newTreasury);
        protocolFeeTreasury = _newTreasury;
    }

    function setSwapFee(uint24 _newFee) external onlyOwner {
        require(_newFee <= 10000, "TataSwap: fee exceeds 1%");
        emit SwapFeeUpdated(defaultSwapFee, _newFee);
        defaultSwapFee = _newFee;
    }
}
