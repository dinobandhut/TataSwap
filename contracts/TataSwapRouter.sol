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
        require(_factory != address(0), "TataSwap: ZERO_FACTORY");
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
        require(amountADesired >= amountAMin, "TataSwap: INSUFFICIENT_A_AMOUNT");
        require(amountBDesired >= amountBMin, "TataSwap: INSUFFICIENT_B_AMOUNT");

        address pool = TataSwapFactory(factory).getPool(tokenA, tokenB, feeTier);
        if (pool == address(0)) {
            pool = TataSwapFactory(factory).createPool(tokenA, tokenB, feeTier);
        }

        require(IERC20Minimal(tokenA).transferFrom(msg.sender, pool, amountADesired), "TataSwap: TRANSFER_A_FAILED");
        require(IERC20Minimal(tokenB).transferFrom(msg.sender, pool, amountBDesired), "TataSwap: TRANSFER_B_FAILED");

        liquidity = TataSwapPool(pool).mint(to);
        amountA = amountADesired;
        amountB = amountBDesired;
    }

    function swapExactTokensForTokens(
        uint256 amountIn,
        uint256 amountOutMin,
        address[] calldata path,
        uint24[] calldata feeTiers,
        address to,
        uint256 deadline
    ) external checkDeadline(deadline) returns (uint256[] memory amounts) {
        require(path.length >= 2 && feeTiers.length == path.length - 1, "TataSwap: INVALID_PATH");
        amounts = new uint256[](path.length);
        amounts[0] = amountIn;

        for (uint256 i = 0; i < path.length - 1; i++) {
            address pool = TataSwapFactory(factory).getPool(path[i], path[i + 1], feeTiers[i]);
            require(pool != address(0), "TataSwap: NO_POOL");

            if (i == 0) {
                require(IERC20Minimal(path[i]).transferFrom(msg.sender, pool, amounts[i]), "TataSwap: TRANSFER_IN_FAILED");
            } else {
                require(IERC20Minimal(path[i]).transfer(pool, amounts[i]), "TataSwap: TRANSFER_HOP_FAILED");
            }

            (address token0,) = path[i] < path[i + 1] ? (path[i], path[i + 1]) : (path[i + 1], path[i]);
            (uint112 reserve0, uint112 reserve1,) = TataSwapPool(pool).getReserves();
            (uint256 reserveIn, uint256 reserveOut) = path[i] == token0 ? (reserve0, reserve1) : (reserve1, reserve0);

            uint256 amountInWithFee = amounts[i] * (1000000 - uint256(feeTiers[i]));
            amounts[i + 1] = (amountInWithFee * reserveOut) / ((reserveIn * 1000000) + amountInWithFee);

            address recipient = i < path.length - 2 ? address(this) : to;
            (uint256 amount0Out, uint256 amount1Out) = path[i] == token0
                ? (uint256(0), amounts[i + 1])
                : (amounts[i + 1], uint256(0));
            TataSwapPool(pool).swap(amount0Out, amount1Out, recipient);
        }

        require(amounts[amounts.length - 1] >= amountOutMin, "TataSwap: INSUFFICIENT_OUTPUT_AMOUNT");
    }
}
