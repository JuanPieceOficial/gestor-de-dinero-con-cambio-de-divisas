package com.cryptowallet.app.data.model

import java.math.BigDecimal
import java.math.BigInteger

data class Chain(
    val id: Long,
    val name: String,
    val shortName: String,
    val nativeSymbol: String,
    val nativeName: String,
    val nativeDecimals: Int = 18,
    val rpcUrls: List<String>,
    val explorerUrl: String,
    val coinGeckoId: String,
    val coinGeckoChain: String,
    val supportsEip1559: Boolean = true
)

data class GasFees(
    val supportsEip1559: Boolean,
    val maxFeePerGas: BigInteger?,
    val maxPriorityFeePerGas: BigInteger?,
    val gasPrice: BigInteger?
)

data class TokenBalance(
    val token: TokenInfo,
    val rawBalance: BigInteger,
    val usdPrice: Double,
    val fiatValue: Double
) {
    val balance: BigDecimal
        get() = BigDecimal(rawBalance).movePointLeft(token.decimals)
}

data class TokenInfo(
    val chainId: Long,
    val address: String,
    val symbol: String,
    val name: String,
    val decimals: Int,
    val isNative: Boolean = false,
    val custom: Boolean = false
)

data class AccountInfo(
    val index: Int,
    val address: String,
    val name: String
)

data class PendingTx(
    val hash: String,
    val chainId: Long,
    val to: String,
    val tokenAddress: String?,
    val tokenSymbol: String,
    val amount: String,
    val amountRaw: String,
    val feeWei: String,
    val timestamp: Long
)
