package com.cryptowallet.app.data.crypto

import java.math.BigInteger

/**
 * Construcción y firma de transacciones Ethereum (EIP-155 legacy y EIP-1559).
 */
object EthereumTransaction {

    data class TxParameters(
        val chainId: Long,
        val nonce: Long,
        val to: String,
        val value: BigInteger,
        val data: ByteArray,
        val gasLimit: Long,
        val gasPrice: BigInteger? = null,
        val maxFeePerGas: BigInteger? = null,
        val maxPriorityFeePerGas: BigInteger? = null,
    )

    fun encodeNumber(value: Long): Rlp.Item.Bytes =
        Rlp.Item.Bytes(Rlp.encodeBigInteger(BigInteger.valueOf(value)))

    fun encodeBig(value: BigInteger): Rlp.Item.Bytes =
        Rlp.Item.Bytes(Rlp.encodeBigInteger(value))

    fun encodeAddress(address: String): Rlp.Item.Bytes {
        val clean = address.removePrefix("0x")
        if (clean.isEmpty()) return Rlp.Item.Bytes(ByteArray(0))
        return Rlp.Item.Bytes(clean.hexToBytes())
    }

    fun encodeBytes(hex: String): Rlp.Item.Bytes {
        val clean = hex.removePrefix("0x")
        if (clean.isEmpty()) return Rlp.Item.Bytes(ByteArray(0))
        return Rlp.Item.Bytes(clean.hexToBytes())
    }

    /**
     * Devuelve la transacción firmada lista para eth_sendRawTransaction (con prefijo 0x).
     */
    fun signSignedTx(privateKey: BigInteger, tx: TxParameters): String {
        val is1559 = tx.maxFeePerGas != null && tx.maxPriorityFeePerGas != null

        val raw: ByteArray
        if (is1559) {
            val unsigned: List<Rlp.Item> = listOf(
                encodeNumber(tx.chainId),
                encodeNumber(tx.nonce),
                encodeBig(tx.maxPriorityFeePerGas!!),
                encodeBig(tx.maxFeePerGas!!),
                encodeNumber(tx.gasLimit),
                encodeAddress(tx.to),
                encodeBig(tx.value),
                Rlp.Item.Bytes(tx.data),
                Rlp.Item.ItemList(emptyList())
            )
            val signingPayload = byteArrayOf(0x02.toByte()) + Rlp.encode(Rlp.Item.ItemList(unsigned))
            val hash = Keccak.digest256(signingPayload)
            val sig = EthCrypto.signDigest(privateKey, hash)
            val signed: List<Rlp.Item> = listOf(
                encodeNumber(tx.chainId),
                encodeNumber(tx.nonce),
                encodeBig(tx.maxPriorityFeePerGas!!),
                encodeBig(tx.maxFeePerGas!!),
                encodeNumber(tx.gasLimit),
                encodeAddress(tx.to),
                encodeBig(tx.value),
                Rlp.Item.Bytes(tx.data),
                Rlp.Item.ItemList(emptyList()),
                encodeBig(BigInteger.valueOf(sig.yParity.toLong())),
                encodeBig(sig.r),
                encodeBig(sig.s)
            )
            raw = byteArrayOf(0x02.toByte()) + Rlp.encode(Rlp.Item.ItemList(signed))
        } else {
            val gasPrice = tx.gasPrice ?: error("Se requiere gasPrice para transacciones legacy")
            val unsigned: List<Rlp.Item> = listOf(
                encodeNumber(tx.nonce),
                encodeBig(gasPrice),
                encodeNumber(tx.gasLimit),
                encodeAddress(tx.to),
                encodeBig(tx.value),
                Rlp.Item.Bytes(tx.data),
                encodeNumber(tx.chainId),
                encodeNumber(0),
                encodeNumber(0)
            )
            val hash = Keccak.digest256(Rlp.encode(Rlp.Item.ItemList(unsigned)))
            val sig = EthCrypto.signDigest(privateKey, hash)
            val v = sig.v(tx.chainId)
            val signed: List<Rlp.Item> = listOf(
                encodeNumber(tx.nonce),
                encodeBig(gasPrice),
                encodeNumber(tx.gasLimit),
                encodeAddress(tx.to),
                encodeBig(tx.value),
                Rlp.Item.Bytes(tx.data),
                encodeNumber(v),
                encodeBig(sig.r),
                encodeBig(sig.s)
            )
            raw = Rlp.encode(Rlp.Item.ItemList(signed))
        }
        return "0x" + raw.toHex()
    }

    fun addressToBytes(address: String): ByteArray {
        return address.removePrefix("0x").hexToBytes()
    }

    private fun ByteArray.toHex(): String {
        val sb = StringBuilder(size * 2)
        for (b in this) sb.append("%02x".format(b))
        return sb.toString()
    }

    private fun String.hexToBytes(): ByteArray {
        val clean = if (startsWith("0x")) substring(2) else this
        if (clean.length % 2 != 0) throw IllegalArgumentException("Hex de longitud impar")
        val out = ByteArray(clean.length / 2)
        for (i in out.indices) {
            out[i] = clean.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
        return out
    }
}
