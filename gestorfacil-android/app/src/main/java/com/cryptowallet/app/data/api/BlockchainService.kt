package com.cryptowallet.app.data.api

import com.cryptowallet.app.data.crypto.Abi
import com.cryptowallet.app.data.crypto.EthereumTransaction
import com.cryptowallet.app.data.model.Chain
import com.cryptowallet.app.data.model.GasFees
import com.cryptowallet.app.data.model.TokenInfo
import java.math.BigInteger

/**
 * Orquesta operaciones contra la blockchain usando JSON-RPC.
 */
class BlockchainService(private val rpc: RpcClient = RpcClient()) {

    suspend fun getNativeBalance(chain: Chain, address: String): BigInteger {
        return rpc.getBalance(chain, address)
    }

    suspend fun getTokenBalance(chain: Chain, tokenAddress: String, address: String): BigInteger {
        val data = Abi.toHex(Abi.encodeBalanceOf(address))
        val result = rpc.callContract(chain, tokenAddress, data)
        return Abi.decodeUintFromHex(result)
    }

    data class TokenMetadata(val symbol: String, val name: String, val decimals: Int)

    suspend fun fetchTokenMetadata(chain: Chain, tokenAddress: String): TokenMetadata {
        val symbol = try {
            Abi.decodeStringFromHex(rpc.callContract(chain, tokenAddress, Abi.toHex(Abi.encodeNoArgs("symbol()"))))
        } catch (e: Exception) {
            ""
        }
        val name = try {
            Abi.decodeStringFromHex(rpc.callContract(chain, tokenAddress, Abi.toHex(Abi.encodeNoArgs("name()"))))
        } catch (e: Exception) {
            ""
        }
        val decimals = try {
            Abi.decodeUintFromHex(rpc.callContract(chain, tokenAddress, Abi.toHex(Abi.encodeNoArgs("decimals()")))).toInt()
        } catch (e: Exception) {
            18
        }
        return TokenMetadata(
            symbol.ifBlank { "TOKEN" },
            name.ifBlank { tokenAddress.take(8) },
            if (decimals in 0..36) decimals else 18
        )
    }

    suspend fun getFees(chain: Chain): GasFees {
        val supports1559 = chain.supportsEip1559
        val priority = if (supports1559) rpc.maxPriorityFeePerGas(chain) else null
        val gasPrice = rpc.gasPrice(chain)
        if (priority != null) {
            val baseFee = try {
                val block = rpc.latestBlock(chain)
                val base = block["baseFeePerGas"]?.toString()?.removeSurrounding("\"")
                if (base != null && base != "0x") BigInteger(base.removePrefix("0x"), 16) else null
            } catch (e: Exception) {
                null
            }
            val maxFee = if (baseFee != null) {
                // 20% de margen sobre la base fee + prioridad (más realista que duplicar)
                baseFee.multiply(BigInteger.valueOf(120)).divide(BigInteger.valueOf(100)).add(priority)
            } else {
                gasPrice
            }
            return GasFees(true, maxFee, priority, gasPrice)
        }
        return GasFees(false, null, null, gasPrice)
    }

    suspend fun estimateTransfer(
        chain: Chain,
        from: String,
        to: String,
        tokenAddress: String?,
        valueWei: BigInteger
    ): Long {
        return if (tokenAddress == null) {
            rpc.estimateGas(chain, from, to, "0x" + valueWei.toString(16))
        } else {
            val data = Abi.toHex(Abi.encodeTransfer(to, valueWei))
            rpc.estimateGas(chain, from, tokenAddress, "0x0", data)
        }
    }

    suspend fun sendSignedWithGasLimit(
        chain: Chain,
        privateKey: BigInteger,
        fromAddress: String,
        to: String,
        tokenAddress: String?,
        amountWei: BigInteger,
        gasLimit: Long,
        fees: GasFees
    ): String {
        val nonce = rpc.getTransactionCount(chain, fromAddress)
        val data = if (tokenAddress == null) ByteArray(0) else Abi.encodeTransfer(to, amountWei)
        val tx = buildParams(
            chain, privateKey, nonce,
            if (tokenAddress == null) to else tokenAddress,
            if (tokenAddress == null) amountWei else BigInteger.ZERO,
            data, gasLimit, fees
        )
        return rpc.sendRawTransaction(chain, tx)
    }

    suspend fun getTxStatus(chain: Chain, hash: String): String? {
        val receipt = rpc.getTransactionReceipt(chain, hash) ?: return null
        val status = receipt["status"]?.toString()?.removeSurrounding("\"")
        return if (status == "0x1") "success" else "failed"
    }

    private suspend fun buildParams(
        chain: Chain,
        privateKey: BigInteger,
        nonce: Long,
        to: String,
        value: BigInteger,
        data: ByteArray,
        gasLimit: Long,
        fees: GasFees
    ): String {
        return EthereumTransaction.signSignedTx(
            privateKey,
            EthereumTransaction.TxParameters(
                chainId = chain.id,
                nonce = nonce,
                to = to,
                value = value,
                data = data,
                gasLimit = gasLimit,
                gasPrice = if (fees.supportsEip1559) null else fees.gasPrice,
                maxFeePerGas = if (fees.supportsEip1559) fees.maxFeePerGas else null,
                maxPriorityFeePerGas = if (fees.supportsEip1559) fees.maxPriorityFeePerGas else null
            )
        )
    }
}
