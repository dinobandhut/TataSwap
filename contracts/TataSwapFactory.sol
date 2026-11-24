// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "./TataSwapPool.sol";

contract TataSwapFactory {
    address public owner;
    address public feeReceiver;
    
    mapping(address => mapping(address => mapping(uint24 => address))) public getPool;
    address[] public allPools;

    event PoolCreated(
        address indexed token0,
        address indexed token1,
        uint24 indexed feeTier,
        address pool,
        uint256 allPoolsLength
    );

    constructor(address _feeReceiver) {
        owner = msg.sender;
        feeReceiver = _feeReceiver;
    }

    function createPool(
        address tokenA,
        address tokenB,
        uint24 feeTier
    ) external returns (address pool) {
        require(tokenA != tokenB, "TataSwap: IDENTICAL_ADDRESSES");
        (address token0, address token1) = tokenA < tokenB ? (tokenA, tokenB) : (tokenB, tokenA);
        require(token0 != address(0), "TataSwap: ZERO_ADDRESS");
        require(getPool[token0][token1][feeTier] == address(0), "TataSwap: POOL_EXISTS");

        pool = address(new TataSwapPool(token0, token1, feeTier));
        getPool[token0][token1][feeTier] = pool;
        getPool[token1][token0][feeTier] = pool; // populate mapping in the reverse direction
        allPools.push(pool);

        emit PoolCreated(token0, token1, feeTier, pool, allPools.length);
    }

    function allPoolsLength() external view returns (uint256) {
        return allPools.length;
    }
}
