// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

/**
 * @dev Interface of the ERC20 standard as defined in the EIP.
 */
interface IERC20 {
    function totalSupply() external view returns (uint256);
    function balanceOf(address account) external view returns (uint256);
    function transfer(address to, uint256 value) external returns (bool);
    function allowance(address owner, address spender) external view returns (uint256);
    function approve(address spender, uint256 value) external returns (bool);
    function transferFrom(address from, address to, uint256 value) external returns (bool);
}

/**
 * @title TataSwapListingFee
 * @notice Permissionless On-Chain Token Listing Fee Collection System.
 * Every user listing a token must pay the configured listing fee (5 USDT).
 * The listing fee is automatically transferred directly to the DEX owner's configured wallet.
 */
contract TataSwapListingFee {
    address public owner;
    address public feeReceiver;
    address public paymentToken; // e.g. USDT on BSC
    uint256 public listingFeeAmount; // e.g. 5 * 10^18 (5 USDT)
    bool public isPaused;

    // Mapping to store registered tokens
    mapping(address => bool) public isTokenListed;
    mapping(address => TokenListingDetails) public tokenListings;
    mapping(address => bool) public feeExemptWallets;
    address[] public listedTokens;

    struct TokenListingDetails {
        address tokenAddress;
        string name;
        string symbol;
        uint8 decimals;
        address listedBy;
        uint256 feePaid;
        uint256 timestamp;
        bool isVerified;
    }

    event TokenListed(
        address indexed token,
        address indexed listedBy,
        string name,
        string symbol,
        uint256 feePaid,
        address feeReceiver,
        uint256 timestamp
    );
    event ListingFeeUpdated(uint256 oldFee, uint256 newFee);
    event FeeReceiverUpdated(address oldReceiver, address newReceiver);
    event PaymentTokenUpdated(address oldToken, address newToken);
    event EmergencyPauseToggled(bool isPaused);
    event TokenVerificationStatusUpdated(address indexed token, bool isVerified);
    event FeeExemptionUpdated(address indexed account, bool isExempt);

    modifier onlyOwner() {
        require(msg.sender == owner, "TataSwap: caller is not the owner");
        _;
    }

    modifier whenNotPaused() {
        require(!isPaused, "TataSwap: listing contract is paused");
        _;
    }

    modifier nonReentrant() {
        require(_locked == 1, "TataSwap: reentrancy guard triggered");
        _locked = 2;
        _;
        _locked = 1;
    }
    uint256 private _locked = 1;

    constructor(
        address _initialFeeReceiver,
        address _paymentToken,
        uint256 _initialFeeAmount
    ) {
        require(_initialFeeReceiver != address(0), "TataSwap: invalid fee receiver");
        owner = msg.sender;
        // Default official fee receiver: 0x3f48248FFE235a5205d1D781fB6c852c71fc1556
        feeReceiver = _initialFeeReceiver;
        paymentToken = _paymentToken;
        listingFeeAmount = _initialFeeAmount; // 5 USDT
        isPaused = false;
        feeExemptWallets[msg.sender] = true;
        feeExemptWallets[_initialFeeReceiver] = true;
    }

    /**
     * @notice Returns true when an account is exempt from paying token listing fees.
     * App owner and official feeReceiver are always exempt.
     */
    function isFeeExempt(address _account) public view returns (bool) {
        return _account == owner || _account == feeReceiver || feeExemptWallets[_account];
    }

    /**
     * @notice Permissionless token listing function.
     * Regular users pay the configured listing fee (5 USDT).
     * App owner / feeReceiver / exempt wallets pay ZERO (0) fee.
     */
    function listToken(
        address _tokenAddress,
        string calldata _name,
        string calldata _symbol,
        uint8 _decimals
    ) external nonReentrant whenNotPaused {
        require(_tokenAddress != address(0), "TataSwap: invalid token address");
        require(!isTokenListed[_tokenAddress], "TataSwap: token already listed");

        bool exempt = isFeeExempt(msg.sender);
        uint256 effectiveFee = exempt ? 0 : listingFeeAmount;

        // Collect listing fee only for non-exempt users (supports standard & non-bool USDT like Ethereum)
        if (effectiveFee > 0 && paymentToken != address(0)) {
            (bool ok, bytes memory ret) = paymentToken.call(
                abi.encodeWithSelector(
                    IERC20.transferFrom.selector,
                    msg.sender,
                    feeReceiver,
                    effectiveFee
                )
            );
            require(ok && (ret.length == 0 || abi.decode(ret, (bool))), "TataSwap: listing fee transfer failed");
        }

        // Register token
        isTokenListed[_tokenAddress] = true;
        listedTokens.push(_tokenAddress);

        tokenListings[_tokenAddress] = TokenListingDetails({
            tokenAddress: _tokenAddress,
            name: _name,
            symbol: _symbol,
            decimals: _decimals,
            listedBy: msg.sender,
            feePaid: effectiveFee,
            timestamp: block.timestamp,
            isVerified: exempt // owner-listed tokens start verified
        });

        emit TokenListed(
            _tokenAddress,
            msg.sender,
            _name,
            _symbol,
            effectiveFee,
            feeReceiver,
            block.timestamp
        );
    }

    /**
     * @notice Owner function to update fee exemption for a wallet.
     */
    function setFeeExempt(address _account, bool _exempt) external onlyOwner {
        require(_account != address(0), "TataSwap: invalid account");
        feeExemptWallets[_account] = _exempt;
        emit FeeExemptionUpdated(_account, _exempt);
    }

    /**
     * @notice Owner function to update the listing fee amount.
     */
    function setListingFee(uint256 _newFee) external onlyOwner {
        emit ListingFeeUpdated(listingFeeAmount, _newFee);
        listingFeeAmount = _newFee;
    }

    /**
     * @notice Owner function to update the fee receiving wallet address.
     */
    function setFeeReceiver(address _newReceiver) external onlyOwner {
        require(_newReceiver != address(0), "TataSwap: invalid receiver");
        emit FeeReceiverUpdated(feeReceiver, _newReceiver);
        feeReceiver = _newReceiver;
        feeExemptWallets[_newReceiver] = true;
    }

    /**
     * @notice Owner function to update the payment token (e.g. USDT).
     */
    function setPaymentToken(address _newToken) external onlyOwner {
        require(_newToken != address(0), "TataSwap: invalid payment token");
        emit PaymentTokenUpdated(paymentToken, _newToken);
        paymentToken = _newToken;
    }

    /**
     * @notice Emergency pause switch.
     */
    function setEmergencyPause(bool _paused) external onlyOwner {
        isPaused = _paused;
        emit EmergencyPauseToggled(_paused);
    }

    /**
     * @notice Manage token verification label.
     */
    function setTokenVerification(address _tokenAddress, bool _isVerified) external onlyOwner {
        require(isTokenListed[_tokenAddress], "TataSwap: token not listed");
        tokenListings[_tokenAddress].isVerified = _isVerified;
        emit TokenVerificationStatusUpdated(_tokenAddress, _isVerified);
    }

    function getListedTokensCount() external view returns (uint256) {
        return listedTokens.length;
    }
}
