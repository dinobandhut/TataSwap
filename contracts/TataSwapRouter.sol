// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "./TataSwapFactory.sol";
import "./TataSwapPool.sol";

contract TataSwapRouter {
    address public immutable factory;

    modifier checkDeadline(uint256 deadline) {
        require(block.timestamp <= deadline, "TataSwap: EXPIRED_DEADLINE");
        _;
    }

    constructor(address _factory) {
        factory = _factory;
    }

    function addLiquidity(
        address tokenA,
        address tokenB,
        uint24 feeTier,
        uint256 amountADesired,
        uint256 amountBDesired,
        uint256 amountAMin,
        uint256 amountBMin,
        address to,
        uint256 deadline
    ) external checkDeadline(deadline) returns (uint256 amountA, uint256 amountB, uint256 liquidity) {
        address pool = TataSwapFactory(factory).getPool(tokenA, tokenB, feeTier);
        if (pool == address(0)) {
            pool = TataSwapFactory(factory).createPool(tokenA, tokenB, feeTier);
        }

        // Transfer tokens to pool
        IERC20Minimal(tokenA).transferFrom(msg.sender, pool, amountADesired);
        IERC20Minimal(tokenB).transferFrom(msg.sender, pool, amountBDesired);

        liquidity = TataSwapPool(pool).mint(to);
        amountA = amountADesired;
        amountB = amountBDesired;
        require(amountA >= amountAMin, "TataSwap: INSUFFICIENT_A_AMOUNT");
        require(amountB >= amountBMin, "TataSwap: INSUFFICIENT_B_AMOUNT");
    }

    function swapExactTokensForTokens(
        uint256 amountIn,
        uint256 amountOutMin,
        address[] calldata path,
        uint24[] calldata feeTiers,
        address to,
        uint256 deadline
    ) external checkDeadline(deadline) returns (uint256[] memory amounts) {
        require(path.length >= 2, "TataSwap: INVALID_PATH");
        amounts = new uint256[](path.length);
        amounts[0] = amountIn;

        for (uint256 i = 0; i < path.length - 1; i++) {
            address pool = TataSwapFactory(factory).getPool(path[i], path[i + 1], feeTiers[i]);
            require(pool != address(0), "TataSwap: NO_POOL");

            // transfer token to pool
            IERC20Minimal(path[i]).transferFrom(i == 0 ? msg.sender : address(this), pool, amounts[i]);

            // query pool output
            (address token0,) = path[i] < path[i + 1] ? (path[i], path[i + 1]) : (path[i + 1], path[i]);
            (uint112 reserve0, uint112 reserve1,) = TataSwapPool(pool).getReserves();
            (uint256 reserveIn, uint256 reserveOut) = path[i] == token0 ? (reserve0, reserve1) : (reserve1, reserve0);

            uint256 amountInWithFee = amounts[i] * 997;
            uint256 numerator = amountInWithFee * reserveOut;
            uint256 denominator = (reserveIn * 1000) + amountInWithFee;
            amounts[i + 1] = numerator / denominator;

            address recipient = i < path.length - 2 ? address(this) : to;
            (uint256 amount0Out, uint256 amount1Out) = path[i] == token0 ? (uint256(0), amounts[i + 1]) : (amounts[i + 1], uint256(0));
            TataSwapPool(pool).swap(amount0Out, amount1Out, recipient);
        }

        require(amounts[amounts.length - 1] >= amountOutMin, "TataSwap: INSUFFICIENT_OUTPUT_AMOUNT");
    }
}
