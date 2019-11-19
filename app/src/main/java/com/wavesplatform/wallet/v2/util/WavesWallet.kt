/*
 * Created by Eduard Zaydel on 1/4/2019
 * Copyright © 2019 Waves Platform. All rights reserved.
 */

package com.wavesplatform.wallet.v2.util

import com.wavesplatform.sdk.crypto.PrivateKeyAccount
import com.wavesplatform.sdk.crypto.WavesCrypto
import timber.log.Timber
import java.util.*

class WavesWallet(val seed: ByteArray) {

    private var guid: String = UUID.randomUUID().toString()
    private val account: PrivateKeyAccount = PrivateKeyAccount(seed)
    val address: String

    val publicKeyStr: String
        get() = account.publicKeyStr

    val privateKey: ByteArray
        get() = account.privateKey

    val privateKeyStr: String
        get() = account.privateKeyStr

    val seedStr: String
        get() = String(seed, Charsets.UTF_8)

    val aauid: String
        get() = WavesCrypto.base64encode(WavesCrypto.blake2b(instance?.address?.toByteArray()
                ?: byteArrayOf())).trim()

    init {
        address = WavesCrypto.addressFromPublicKey(account.publicKey, EnvironmentManager.netCode)
    }

    @Throws(Exception::class)
    constructor(walletData: String, password: String?) : this(WavesCrypto.base58decode(
            WavesCrypto.aesDecrypt(walletData, password)))

    @Throws(Exception::class)
    fun getEncryptedData(password: String?): String {
        return WavesCrypto.aesEncrypt(WavesCrypto.base58encode(seed), password)
    }

    companion object {

        private var instance: WavesWallet? = null

        /**
         * Create wallet from secret seed-phrase.
         * @param seed secret seed-phrase
         * @param guid
         */
        @JvmStatic
        fun createWallet(seed: String, guid: String = UUID.randomUUID().toString()): String {
            return try {
                instance = WavesWallet(seed.toByteArray(Charsets.UTF_8))
                instance!!.guid = guid
                guid
            } catch (e: Exception) {
                Timber.e(e, "WavesWallet: Error create WavesSdk wallet from seed")
                e.printStackTrace()
                ""
            }
        }

        /**
         * Create wallet.
         * @param encrypted Encrypted seed by password
         * @param password Password of seed
         * @param guid Optional. Set your specific id for the wallet
         * @return New generated guid or return from {@code guid} parameter
         */
        @JvmStatic
        fun createWallet(encrypted: String,
                         password: String,
                         guid: String = UUID.randomUUID().toString()): String {
            return try {
                instance = WavesWallet(encrypted, password)
                instance!!.guid = guid
                guid
            } catch (e: Exception) {
                Timber.e(e, "WavesWallet: Error create WavesSdk wallet from encrypted data")
                e.printStackTrace()
                ""
            }
        }

        @JvmStatic
        fun getWallet(): WavesWallet? {
            return instance
        }

        /**
         * Resets current wallet and loosing any data of it
         */
        @JvmStatic
        fun resetWallet() {
            instance = null
        }

        /**
         * @return Checks is platform set any active wallet
         */
        @JvmStatic
        fun isAuthenticated(): Boolean {
            return instance != null
        }

        /**
         * @return Address of current wallet
         */
        fun getAddress(): String {
            return instance?.address ?: ""
        }

        /**
         * @return Public key of current wallet
         */
        fun getPublicKeyStr(): String {
            return instance?.publicKeyStr ?: ""
        }

        fun getPrivateKey(): ByteArray {
            return instance?.privateKey ?: byteArrayOf()
        }

        /**
         * @return AUUID - Analytic universally unique identifier
         */
        fun getAuuid(): String {
            return instance?.aauid.orEmpty()
        }
    }
}