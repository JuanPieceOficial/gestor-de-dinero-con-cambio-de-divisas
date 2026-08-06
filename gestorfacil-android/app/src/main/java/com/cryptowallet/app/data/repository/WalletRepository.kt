package com.cryptowallet.app.data.repository

import android.content.Context
import com.cryptowallet.app.data.api.BlockchainService
import com.cryptowallet.app.data.api.Chains
import com.cryptowallet.app.data.api.PriceClient
import com.cryptowallet.app.data.crypto.AesGcm
import com.cryptowallet.app.data.crypto.Bip39
import com.cryptowallet.app.data.crypto.EthCrypto
import com.cryptowallet.app.data.crypto.HdKeys
import com.cryptowallet.app.data.crypto.PinHasher
import com.cryptowallet.app.data.db.SettingEntity
import com.cryptowallet.app.data.db.TokenEntity
import com.cryptowallet.app.data.db.TxRecordEntity
import com.cryptowallet.app.data.db.WalletDatabase
import com.cryptowallet.app.data.keystore.KeyStoreManager
import com.cryptowallet.app.data.model.AccountInfo
import com.cryptowallet.app.data.model.Chain
import com.cryptowallet.app.data.model.GasFees
import com.cryptowallet.app.data.model.PendingTx
import com.cryptowallet.app.data.model.TokenBalance
import com.cryptowallet.app.data.model.TokenInfo
import java.math.BigDecimal
import java.math.BigInteger
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Repositorio central: gestiona el seed cifrado (clave derivada del PIN),
 * la clave en memoria, los tokens, los saldos y los envíos.
 */
class WalletRepository(
    private val context: Context,
    private val database: WalletDatabase,
    private val keystore: KeyStoreManager = KeyStoreManager(),
    private val blockchain: BlockchainService = BlockchainService(),
    private val prices: PriceClient = PriceClient()
) {

    companion object {
        private const val KEY_HAS_WALLET = "has_wallet"
        private const val KEY_ENCRYPTED_MNEMONIC = "encrypted_mnemonic"
        private const val KEY_PIN_SALT = "pin_salt"
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_DATA_SALT = "data_salt"
        private const val KEY_ACTIVE_ACCOUNT = "active_account"
        private const val KEY_PIN_FAILURES = "pin_failures"
        private const val KEY_LOCKOUT_UNTIL = "lockout_until"
        private const val KEY_TESTNET = "testnet_enabled"
        private const val KEY_FIAT_CURRENCY = "fiat_currency"

        private const val MAX_PIN_FAILURES = 5
        private const val BASE_LOCKOUT_MS = 30_000L
        private const val MAX_LOCKOUT_MS = 300_000L
    }

    @Volatile
    private var unlockedMnemonic: String? = null

    @Volatile
    private var unlockedPrivateKey: BigInteger? = null

    private val seedingDone = AtomicBoolean(false)

    suspend fun hasWallet(): Boolean {
        return database.settingsDao().get(KEY_HAS_WALLET) == "1"
    }

    suspend fun isUnlocked(): Boolean = unlockedMnemonic != null

    suspend fun createWallet(mnemonic: String, pin: String): AccountInfo {
        persistWallet(mnemonic, pin)
        seedDefaultTokens()
        val account = deriveAccount(mnemonic, 0)
        database.accountDao().upsert(
            com.cryptowallet.app.data.db.AccountEntity(
                id = 0, index = 0, address = account.address, name = account.name
            )
        )
        unlockedMnemonic = mnemonic
        unlockedPrivateKey = privateKeyFromMnemonic(mnemonic, 0)
        return account
    }

    suspend fun restoreWallet(mnemonic: String, pin: String): AccountInfo {
        require(Bip39.validateMnemonic(context, mnemonic)) { "La frase de recuperación no es válida" }
        return createWallet(mnemonic.trim().lowercase().split(Regex("\\s+")).joinToString(" "), pin)
    }

    suspend fun unlock(pin: String): Boolean {
        if (lockoutRemainingMs() > 0) return false
        val saltEncoded = database.settingsDao().get(KEY_PIN_SALT) ?: return false
        val hashEncoded = database.settingsDao().get(KEY_PIN_HASH) ?: return false
        val expected = PinHasher.decode(hashEncoded)
        val actual = PinHasher.hash(pin, PinHasher.decodeSalt(saltEncoded))
        if (!expected.contentEquals(actual)) {
            registerFailedAttempt()
            return false
        }
        val mnemonic = decryptMnemonic(pin)
        clearFailedAttempts()
        unlockedMnemonic = mnemonic
        val account = getActiveAccount()
        unlockedPrivateKey = privateKeyFromMnemonic(mnemonic, account.index)
        return true
    }

    /**
     * Desencripta el seed. Formato nuevo: cifrado con clave derivada del PIN.
     * Formato antiguo (v1.1.0): cifrado con Android Keystore; se migra al nuevo.
     */
    private suspend fun decryptMnemonic(pin: String): String {
        val encrypted = database.settingsDao().get(KEY_ENCRYPTED_MNEMONIC)
            ?: error("Billetera no configurada")
        val dataSaltEncoded = database.settingsDao().get(KEY_DATA_SALT)
        return if (dataSaltEncoded != null) {
            val dataKey = PinHasher.deriveKey(pin, PinHasher.decodeSalt(dataSaltEncoded))
            AesGcm.decrypt(dataKey, encrypted)
        } else {
            val mnemonic = keystore.decrypt(encrypted)
            migrateEncryption(pin, mnemonic)
            mnemonic
        }
    }

    private suspend fun migrateEncryption(pin: String, mnemonic: String) {
        // No re-cifrar datos corruptos: si el seed descifrado no es una frase
        // BIP-39 válida, mejor fallar con un error claro.
        require(Bip39.validateMnemonic(context, mnemonic)) { "Seed almacenado inválido" }
        val dataSalt = PinHasher.newSalt()
        val dataKey = PinHasher.deriveKey(pin, dataSalt)
        database.settingsDao().put(SettingEntity(KEY_DATA_SALT, PinHasher.encodeSalt(dataSalt)))
        database.settingsDao().put(SettingEntity(KEY_ENCRYPTED_MNEMONIC, AesGcm.encrypt(dataKey, mnemonic)))
    }

    suspend fun changePin(oldPin: String, newPin: String): Boolean {
        if (!unlock(oldPin)) return false
        val mnemonic = unlockedMnemonic ?: return false
        persistWallet(mnemonic, newPin)
        return true
    }

    private suspend fun persistWallet(mnemonic: String, pin: String) {
        val salt = PinHasher.newSalt()
        val hash = PinHasher.hash(pin, salt)
        val dataSalt = PinHasher.newSalt()
        val dataKey = PinHasher.deriveKey(pin, dataSalt)
        database.settingsDao().put(SettingEntity(KEY_PIN_SALT, PinHasher.encodeSalt(salt)))
        database.settingsDao().put(SettingEntity(KEY_PIN_HASH, PinHasher.encode(hash)))
        database.settingsDao().put(SettingEntity(KEY_DATA_SALT, PinHasher.encodeSalt(dataSalt)))
        database.settingsDao().put(SettingEntity(KEY_ENCRYPTED_MNEMONIC, AesGcm.encrypt(dataKey, mnemonic)))
        database.settingsDao().put(SettingEntity(KEY_HAS_WALLET, "1"))
        database.settingsDao().put(SettingEntity(KEY_ACTIVE_ACCOUNT, "0"))
    }

    fun lock() {
        unlockedMnemonic = null
        unlockedPrivateKey = null
    }

    suspend fun getBackupMnemonic(): String? {
        return unlockedMnemonic
    }

    suspend fun deleteWallet() {
        lock()
        database.settingsDao().deleteAll()
        database.accountDao().deleteAll()
        database.tokenDao().deleteAll()
        database.txDao().deleteAll()
    }

    // ---------- Bloqueo por PIN ----------

    suspend fun lockoutRemainingMs(): Long {
        val until = database.settingsDao().get(KEY_LOCKOUT_UNTIL)?.toLongOrNull() ?: 0L
        return (until - System.currentTimeMillis()).coerceAtLeast(0L)
    }

    private suspend fun registerFailedAttempt() {
        val failures = (database.settingsDao().get(KEY_PIN_FAILURES) ?: "0").toInt() + 1
        database.settingsDao().put(SettingEntity(KEY_PIN_FAILURES, failures.toString()))
        // Solo se bloquea tras MAX_PIN_FAILURES intentos fallidos consecutivos
        if (failures >= MAX_PIN_FAILURES) {
            val extra = failures - MAX_PIN_FAILURES
            val lockoutMs = (BASE_LOCKOUT_MS * (extra + 1)).coerceAtMost(MAX_LOCKOUT_MS)
            database.settingsDao().put(SettingEntity(KEY_LOCKOUT_UNTIL, (System.currentTimeMillis() + lockoutMs).toString()))
        }
    }

    private suspend fun clearFailedAttempts() {
        database.settingsDao().remove(KEY_PIN_FAILURES)
        database.settingsDao().remove(KEY_LOCKOUT_UNTIL)
    }

    // ---------- Preferencias ----------

    suspend fun isTestnetEnabled(): Boolean = database.settingsDao().get(KEY_TESTNET) == "1"

    suspend fun setTestnetEnabled(enabled: Boolean) {
        database.settingsDao().put(SettingEntity(KEY_TESTNET, if (enabled) "1" else "0"))
        // No se borran tokens: el portfolio filtra por cadena activa y los
        // tokens custom se conservan al alternar.
        seedingDone.set(false)
        seedDefaultTokens()
    }

    suspend fun activeChains(): List<Chain> = if (isTestnetEnabled()) Chains.testnets else Chains.all

    suspend fun getFiatCurrency(): String = database.settingsDao().get(KEY_FIAT_CURRENCY) ?: "USD"

    suspend fun setFiatCurrency(code: String) {
        database.settingsDao().put(SettingEntity(KEY_FIAT_CURRENCY, code))
    }

    // ---------- Cuentas y tokens ----------

    suspend fun getActiveAccount(): AccountInfo {
        val index = (database.settingsDao().get(KEY_ACTIVE_ACCOUNT) ?: "0").toInt()
        val account = database.accountDao().getById(index)
        return if (account != null) {
            AccountInfo(account.index, account.address, account.name)
        } else {
            AccountInfo(0, "", "Cuenta 1")
        }
    }

    suspend fun getActiveAddress(): String {
        val account = getActiveAccount()
        if (account.address.isNotEmpty()) return account.address
        val mnemonic = unlockedMnemonic ?: error("Billetera bloqueada")
        val acc = deriveAccount(mnemonic, 0)
        return acc.address
    }

    suspend fun seedDefaultTokens() {
        if (!seedingDone.compareAndSet(false, true)) return
        val existingIds = database.tokenDao().getAllSync().map { it.id }.toSet()
        val chains = if (isTestnetEnabled()) Chains.testnets else Chains.all
        val tokens = mutableListOf<TokenEntity>()
        for (chain in chains) {
            if ("${chain.id}:" in existingIds) continue // cadena ya sembrada
            tokens.add(
                TokenEntity(
                    id = tokenId(chain.id, ""),
                    chainId = chain.id,
                    address = "",
                    symbol = chain.nativeSymbol,
                    name = chain.nativeName,
                    decimals = chain.nativeDecimals,
                    isNative = true,
                    enabled = true,
                    custom = false
                )
            )
            val addresses = Chains.tokenAddresses[chain.id] ?: emptyMap()
            for (meta in Chains.defaultTokenList[chain.id].orEmpty()) {
                val symbol = meta.first
                if (symbol == chain.nativeSymbol) continue
                val addr = addresses[symbol] ?: continue
                tokens.add(
                    TokenEntity(
                        id = tokenId(chain.id, addr.lowercase()),
                        chainId = chain.id,
                        address = addr,
                        symbol = symbol,
                        name = meta.second,
                        decimals = meta.third,
                        isNative = false,
                        enabled = true,
                        custom = false
                    )
                )
            }
        }
        database.tokenDao().upsertAll(tokens)
    }

    suspend fun addCustomToken(chain: Chain, contractAddress: String): TokenInfo {
        val clean = EthCrypto.toChecksumAddress(contractAddress)
        require(EthCrypto.isValidAddress(clean)) { "Dirección de contrato inválida" }
        val metadata = blockchain.fetchTokenMetadata(chain, clean)
        val entity = TokenEntity(
            id = tokenId(chain.id, clean.lowercase()),
            chainId = chain.id,
            address = clean,
            symbol = metadata.symbol,
            name = metadata.name,
            decimals = metadata.decimals,
            isNative = false,
            enabled = true,
            custom = true
        )
        database.tokenDao().upsert(entity)
        return TokenInfo(chain.id, clean, metadata.symbol, metadata.name, metadata.decimals)
    }

    suspend fun removeToken(token: TokenEntity) {
        database.tokenDao().delete(token)
    }

    suspend fun setTokenEnabled(tokenId: String, enabled: Boolean) {
        database.tokenDao().setEnabled(tokenId, enabled)
    }

    suspend fun getEnabledTokens(): List<TokenEntity> = database.tokenDao().getAllSync()

    suspend fun getTokensForChain(chainId: Long): List<TokenEntity> = database.tokenDao().getForChainSync(chainId)

    suspend fun getTokenBalanceWei(chain: Chain, token: TokenEntity, address: String): BigInteger {
        return if (token.isNative) {
            blockchain.getNativeBalance(chain, address)
        } else {
            blockchain.getTokenBalance(chain, token.address, address)
        }
    }

    suspend fun loadPortfolio(): PortfolioResult {
        val address = getActiveAddress()
        val tokens = getEnabledTokens()
        val chains = if (isTestnetEnabled()) Chains.testnets else Chains.all
        val fiatCode = getFiatCurrency().lowercase()
        val nativeIds = mutableMapOf<String, String>()
        for (chain in chains) {
            if (chain.coinGeckoId.isNotEmpty()) nativeIds[chain.id.toString()] = chain.coinGeckoId
        }
        val nativePrices = prices.nativePrices(nativeIds, fiatCode)

        val results = mutableListOf<TokenBalance>()
        var total = 0.0

        val activeChainIds = chains.map { it.id }.toSet()
        val byChain = tokens.groupBy { it.chainId }
        for ((chainId, chainTokens) in byChain) {
            if (chainId !in activeChainIds) continue // ocultar cadenas del otro modo
            val chain = Chains.byId(chainId) ?: continue
            val nativePrice = nativePrices[chain.coinGeckoId] ?: 0.0

            val tokenAddresses = chainTokens.filter { !it.isNative }.map { it.address.lowercase() }
            val tokenPrices = if (tokenAddresses.isNotEmpty() && chain.coinGeckoChain.isNotEmpty()) {
                prices.tokenPrices(chain.coinGeckoChain, tokenAddresses, fiatCode)
            } else {
                emptyMap()
            }

            for (token in chainTokens) {
                val raw = if (token.isNative) {
                    blockchain.getNativeBalance(chain, address)
                } else {
                    blockchain.getTokenBalance(chain, token.address, address)
                }
                val price = if (token.isNative) nativePrice else (tokenPrices[token.address.lowercase()] ?: 0.0)
                val balance = raw.movePointLeft(token.decimals)
                val fiat = balance.toDouble() * price
                total += fiat
                results.add(
                    TokenBalance(
                        token = TokenInfo(
                            chainId = chain.id,
                            address = token.address,
                            symbol = token.symbol,
                            name = token.name,
                            decimals = token.decimals,
                            isNative = token.isNative,
                            custom = token.custom
                        ),
                        rawBalance = raw,
                        usdPrice = price,
                        fiatValue = fiat
                    )
                )
            }
        }
        return PortfolioResult(results, total, nativePrices)
    }

    suspend fun estimateSend(
        chain: Chain,
        token: TokenEntity,
        to: String,
        amountWei: BigInteger
    ): SendEstimate {
        val address = getActiveAddress()
        val fees = blockchain.getFees(chain)
        val gasLimit = blockchain.estimateTransfer(chain, address, to, token.address.ifEmpty { null }, amountWei)
        return SendEstimate(fees, gasLimit)
    }

    suspend fun send(
        chain: Chain,
        token: TokenEntity,
        to: String,
        amountWei: BigInteger,
        gasLimit: Long,
        fees: GasFees
    ): PendingTx {
        val privateKey = unlockedPrivateKey ?: error("Billetera bloqueada")
        val from = getActiveAddress()
        val hash = blockchain.sendSignedWithGasLimit(
            chain, privateKey, from, to,
            token.address.ifEmpty { null }, amountWei, gasLimit, fees
        )
        val record = TxRecordEntity(
            id = "${chain.id}:${hash}",
            chainId = chain.id,
            hash = hash,
            from = from,
            to = to,
            tokenAddress = token.address.ifEmpty { null },
            tokenSymbol = token.symbol,
            amount = amountWei.movePointLeft(token.decimals).stripTrailingZeros().toPlainString(),
            amountRaw = amountWei.toString(),
            feeWei = if (fees.supportsEip1559) fees.maxFeePerGas?.multiply(BigInteger.valueOf(gasLimit))?.toString()
            ?: "" else fees.gasPrice?.multiply(BigInteger.valueOf(gasLimit))?.toString() ?: "",
            status = "pending",
            timestamp = System.currentTimeMillis(),
            type = "send"
        )
        database.txDao().upsert(record)
        return PendingTx(
            hash, chain.id, to, record.tokenAddress, token.symbol,
            record.amount, record.amountRaw, record.feeWei, record.timestamp
        )
    }

    suspend fun refreshPendingTx() {
        val pending = database.txDao().getAllSync().filter { it.status == "pending" }
        for (tx in pending) {
            val chain = Chains.byId(tx.chainId) ?: continue
            val status = blockchain.getTxStatus(chain, tx.hash)
            if (status != null) {
                database.txDao().updateStatus(tx.hash, tx.chainId, status)
            }
        }
    }

    suspend fun getTxHistory(): List<TxRecordEntity> = database.txDao().getAllSync()

    fun privateKeyForActiveAccountOrNull(): BigInteger? = unlockedPrivateKey

    private fun deriveAccount(mnemonic: String, index: Int): AccountInfo {
        val seed = Bip39.mnemonicToSeed(mnemonic)
        val extKey = HdKeys.evmAccount(seed, index)
        val pair = EthCrypto.keyPairFromPrivateBytes(extKey.privateKeyBytes)
        return AccountInfo(index, pair.checksumAddress, "Cuenta ${index + 1}")
    }

    private fun privateKeyFromMnemonic(mnemonic: String, index: Int): BigInteger {
        val seed = Bip39.mnemonicToSeed(mnemonic)
        val extKey = HdKeys.evmAccount(seed, index)
        return extKey.key
    }

    private fun tokenId(chainId: Long, address: String): String = "$chainId:${address.lowercase()}"

    data class PortfolioResult(
        val balances: List<TokenBalance>,
        val totalFiat: Double,
        val nativePrices: Map<String, Double>
    )

    data class SendEstimate(val fees: GasFees, val gasLimit: Long)

    private fun BigInteger.movePointLeft(decimals: Int): BigDecimal =
        BigDecimal(this).movePointLeft(decimals)
}
