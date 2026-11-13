// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

interface IERC20Minimal {
    function transfer(address to, uint256 value) external returns (bool);
    function transferFrom(address from, address to, uint256 value) external returns (bool);
    function balanceOf(address account) external view returns (uint256);
}

contract TataSwapPool {
    address public immutable factory;
    address public immutable token0;
    address public immutable token1;
    uint24 public immutable feeTier; // 500 = 0.05%, 3000 = 0.3%, 10000 = 1.0%

    uint112 private reserve0;
    uint112 private reserve1;
    uint32 private blockTimestampLast;

    uint256 public totalSupply;
    mapping(address => uint256) public balanceOf;

    uint256 private _locked = 1;
    modifier nonReentrant() {
        require(_locked == 1, "TataSwap: reentrant call");
        _locked = 2;
        _;
        _locked = 1;
    }

    event Mint(address indexed sender, uint256 amount0, uint256 amount1);
    event Burn(address indexed sender, uint256 amount0, uint256 amount1, address indexed to);
    event Swap(
        address indexed sender,
        uint256 amount0In,
        uint256 amount1In,
        uint256 amount0Out,
        uint256 amount1Out,
        address indexed to
    );
    event Sync(uint112 reserve0, uint112 reserve1);

    constructor(address _token0, address _token1, uint24 _feeTier) {
        factory = msg.sender;
        token0 = _token0;
        token1 = _token1;
        feeTier = _feeTier;
    }

    function getReserves() public view returns (uint112 _reserve0, uint112 _reserve1, uint32 _blockTimestampLast) {
        _reserve0 = reserve0;
        _reserve1 = reserve1;
        _blockTimestampLast = blockTimestampLast;
    }

    function mint(address to) external nonReentrant returns (uint256 liquidity) {
        (uint112 _reserve0, uint112 _reserve1,) = getReserves();
        uint256 balance0 = IERC20Minimal(token0).balanceOf(address(this));
        uint256 balance1 = IERC20Minimal(token1).balanceOf(address(this));
        uint256 amount0 = balance0 - _reserve0;
        uint256 amount1 = balance1 - _reserve1;

        if (totalSupply == 0) {
            liquidity = sqrt(amount0 * amount1);
            require(liquidity > 1000, "TataSwap: minimum liquidity not met");
        } else {
            uint256 l0 = (amount0 * totalSupply) / _reserve0;
            uint256 l1 = (amount1 * totalSupply) / _reserve1;
            liquidity = l0 < l1 ? l0 : l1;
        }

        require(liquidity > 0, "TataSwap: insufficient liquidity minted");
        balanceOf[to] += liquidity;
        totalSupply += liquidity;

        _update(balance0, balance1);
        emit Mint(msg.sender, amount0, amount1);
    }

    function burn(address to) external nonReentrant returns (uint256 amount0, uint256 amount1) {
        uint256 liquidity = balanceOf[address(this)];
        uint256 balance0 = IERC20Minimal(token0).balanceOf(address(this));
        uint256 balance1 = IERC20Minimal(token1).balanceOf(address(this));

        amount0 = (liquidity * balance0) / totalSupply;
        amount1 = (liquidity * balance1) / totalSupply;
        require(amount0 > 0 && amount1 > 0, "TataSwap: insufficient liquidity burned");

        balanceOf[address(this)] -= liquidity;
        totalSupply -= liquidity;

        require(IERC20Minimal(token0).transfer(to, amount0), "TataSwap: transfer 0 failed");
        require(IERC20Minimal(token1).transfer(to, amount1), "TataSwap: transfer 1 failed");

        _update(IERC20Minimal(token0).balanceOf(address(this)), IERC20Minimal(token1).balanceOf(address(this)));
        emit Burn(msg.sender, amount0, amount1, to);
    }

    function swap(uint256 amount0Out, uint256 amount1Out, address to) external nonReentrant {
        require(amount0Out > 0 || amount1Out > 0, "TataSwap: insufficient output");
        (uint112 _reserve0, uint112 _reserve1,) = getReserves();
        require(amount0Out < _reserve0 && amount1Out < _reserve1, "TataSwap: insufficient reserve");

        if (amount0Out > 0) require(IERC20Minimal(token0).transfer(to, amount0Out), "TataSwap: transfer 0 failed");
        if (amount1Out > 0) require(IERC20Minimal(token1).transfer(to, amount1Out), "TataSwap: transfer 1 failed");

        uint256 balance0 = IERC20Minimal(token0).balanceOf(address(this));
        uint256 balance1 = IERC20Minimal(token1).balanceOf(address(this));

        uint256 amount0In = balance0 > _reserve0 - amount0Out ? balance0 - (_reserve0 - amount0Out) : 0;
        uint256 amount1In = balance1 > _reserve1 - amount1Out ? balance1 - (_reserve1 - amount1Out) : 0;
        require(amount0In > 0 || amount1In > 0, "TataSwap: insufficient input");

        // Verify constant product formula with fee tier deduction
        uint256 feeMultiplier = 1000000 - feeTier;
        uint256 balance0Adjusted = (balance0 * 1000000) - (amount0In * feeTier);
        uint256 balance1Adjusted = (balance1 * 1000000) - (amount1In * feeTier);
        require(
            balance0Adjusted * balance1Adjusted >= uint256(_reserve0) * uint256(_reserve1) * (1000000**2),
            "TataSwap: K value invariant violated"
        );

        _update(balance0, balance1);
        emit Swap(msg.sender, amount0In, amount1In, amount0Out, amount1Out, to);
    }

    function _update(uint256 balance0, uint256 balance1) private {
        require(balance0 <= type(uint112).max && balance1 <= type(uint112).max, "TataSwap: OVERFLOW");
        reserve0 = uint112(balance0);
        reserve1 = uint112(balance1);
        blockTimestampLast = uint32(block.timestamp % 2**32);
        emit Sync(reserve0, reserve1);
    }

    function sqrt(uint256 y) internal pure returns (uint256 z) {
        if (y > 3) {
            z = y;
            uint256 x = y / 2 + 1;
            while (x < z) {
                z = x;
                x = (y / x + x) / 2;
            }
        } else if (y != 0) {
            z = 1;
        }
    }
}
