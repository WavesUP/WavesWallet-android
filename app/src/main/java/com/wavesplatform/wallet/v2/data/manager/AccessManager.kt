/*
 * Created by Eduard Zaydel on 1/4/2019
 * Copyright © 2019 Waves Platform. All rights reserved.
 */

package com.wavesplatform.wallet.v2.data.manager

import android.content.Intent
import android.text.TextUtils
import com.vicpin.krealmextensions.RealmConfigStore
import com.wavesplatform.sdk.crypto.WavesCrypto
import com.wavesplatform.sdk.crypto.WavesCrypto.Companion.addressFromPublicKey
import com.wavesplatform.wallet.v2.util.WavesWallet
import com.wavesplatform.sdk.utils.*
import com.wavesplatform.wallet.v2.util.EnvironmentManager
import com.wavesplatform.wallet.App
import com.wavesplatform.wallet.v2.util.PrefsUtil
import com.wavesplatform.wallet.v2.data.database.DBHelper
import com.wavesplatform.wallet.v2.data.helpers.AuthHelper
import com.wavesplatform.wallet.v2.data.model.db.userdb.AddressBookUserDb
import com.wavesplatform.wallet.v2.data.service.UpdateApiDataService
import com.wavesplatform.wallet.v2.ui.auth.passcode.enter.EnterPassCodeActivity
import com.wavesplatform.wallet.v2.util.MigrationUtil
import com.wavesplatform.wallet.v2.util.deleteRecursive
import de.adorsys.android.securestoragelibrary.SecurePreferences
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.exceptions.Exceptions
import org.apache.commons.io.Charsets
import pers.victor.ext.app
import timber.log.Timber
import java.io.File
import java.util.*

class AccessManager(private var prefs: PrefsUtil, private var authHelper: AuthHelper) {

    private val pinStore = PinStoreService()
    private var loggedInGuid: String = ""

    fun validatePassCodeObservable(guid: String, passCode: String): Observable<String> {
        return readPassCodeObservable(
                guid, passCode, App.accessManager.getPassCodeInputFails(guid))
                .flatMap { password ->
                    writePassCodeObservable(guid, password, passCode)
                            .andThen(Observable.just<String>(password))
                }
                .compose(RxUtil.applySchedulersToObservable<String>())
    }

    private fun readPassCodeObservable(guid: String, passedPin: String, tryCount: Int): Observable<String> {
        return pinStore.readPassword(guid, passedPin, tryCount)
                .map { seed ->
                    try {
                        val encryptedPassword = prefs.getValue(
                                guid, PrefsUtil.KEY_ENCRYPTED_PASSWORD, "")
                        WavesCrypto.aesDecrypt(encryptedPassword, seed)
                    } catch (e: Exception) {
                        throw Exceptions.propagate(Throwable("Decrypt wallet failed"))
                    }
                }
    }

    fun writePassCodeObservable(guid: String, password: String?, passCode: String): Completable {
        return Completable.create { subscriber ->
            if (passCode.length != 4) {
                subscriber.onError(RuntimeException("Prohibited pin"))
            }

            try {
                val keyPassword = randomString()
                pinStore.writePassword(guid, passCode, keyPassword)
                        .subscribe({
                            val encryptedPassword = WavesCrypto.aesEncrypt(password, keyPassword)
                            prefs.setValue(guid, PrefsUtil.KEY_ENCRYPTED_PASSWORD, encryptedPassword)
                            if (!subscriber.isDisposed) {
                                subscriber.onComplete()
                            }
                        }, { err ->
                            if (!subscriber.isDisposed) {
                                subscriber.onError(err)
                            }
                        })
            } catch (e: Exception) {
                Timber.e(e,"AccessManager: writePassCodeObservable")
                if (!subscriber.isDisposed) {
                    subscriber.onError(RuntimeException("Failed to encrypt password"))
                }
            }
        }.compose(RxUtil.applySchedulersToCompletable())
    }

    fun storeWalletData(seed: String, password: String, walletName: String, skipBackup: Boolean): String {
        try {
            loggedInGuid = WavesWallet.createWallet(seed)
            prefs.setGlobalValue(PrefsUtil.GLOBAL_LAST_LOGGED_IN_GUID, loggedInGuid)
            prefs.addGlobalListValue(EnvironmentManager.name +
                    PrefsUtil.LIST_WALLET_GUIDS, loggedInGuid)
            prefs.setValue(PrefsUtil.KEY_PUB_KEY, WavesWallet.getWallet()!!.publicKeyStr)
            prefs.setValue(PrefsUtil.KEY_WALLET_NAME, walletName)
            prefs.setValue(PrefsUtil.KEY_ENCRYPTED_WALLET, WavesWallet.getWallet()!!.getEncryptedData(password))
            authHelper.configureDB(WavesWallet.getWallet()!!.address, loggedInGuid)
            MigrationUtil.checkOldAddressBook(prefs, loggedInGuid)
            prefs.setValue(PrefsUtil.KEY_SKIP_BACKUP, skipBackup)
            return loggedInGuid
        } catch (e: Exception) {
            Timber.e(e,"AccessManager: storeWalletData")
            return ""
        }
    }

    fun storeWalletName(address: String, name: String) {
        if (!TextUtils.isEmpty(address) && !TextUtils.isEmpty(name)) {
            val searchWalletGuid = findGuidBy(address)
            prefs.setGlobalValue(searchWalletGuid + PrefsUtil.KEY_WALLET_NAME, name)
        }
    }

    fun findGuidBy(address: String): String {
        val guids = prefs.getGlobalValueList(
                EnvironmentManager.name + PrefsUtil.LIST_WALLET_GUIDS)
        var resultGuid = ""
        for (guid in guids) {
            val publicKey = prefs.getValue(guid, PrefsUtil.KEY_PUB_KEY, "")
            if (addressFromPublicKey(WavesCrypto.base58decode(publicKey), EnvironmentManager.netCode)
                    == address) {
                resultGuid = guid
            }
        }
        return resultGuid
    }

    fun isAccountNameExist(checkedName: String): Boolean {
        if (TextUtils.isEmpty(checkedName)) {
            return false
        }

        val guids = prefs.getGlobalValueList(
                EnvironmentManager.name + PrefsUtil.LIST_WALLET_GUIDS)
        for (guid in guids) {
            if (checkedName == prefs.getValue(guid, PrefsUtil.KEY_WALLET_NAME, "")) {
                return true
            }
        }
        return false
    }

    fun isAccountWithSeedExist(seed: String): Boolean {
        if (TextUtils.isEmpty(seed)) {
            return false
        }

        val tempWallet = WavesWallet(seed.toByteArray(Charsets.UTF_8))
        val guids = prefs.getGlobalValueList(
                EnvironmentManager.name + PrefsUtil.LIST_WALLET_GUIDS)
        for (guid in guids) {
            if (tempWallet.publicKeyStr == prefs.getValue(guid, PrefsUtil.KEY_PUB_KEY, "")) {
                return true
            }
        }
        return false
    }

    fun getCurrentWavesWalletEncryptedData(): String {
        return getWalletData(loggedInGuid)
    }

    fun getLoggedInGuid(): String {
        return loggedInGuid
    }

    fun getLastLoggedInGuid(): String {
        return prefs.guid
    }

    fun setLastLoggedInGuid(guid: String) {
        prefs.setGlobalValue(PrefsUtil.GLOBAL_LAST_LOGGED_IN_GUID, guid)
        loggedInGuid = guid
    }

    fun resetWallet() {
        addToRecentSavedWallets(loggedInGuid)
        clearRealmConfiguration()
        WavesWallet.resetWallet()
        loggedInGuid = ""
    }

    fun setWallet(guid: String, password: String) {
        WavesWallet.createWallet(getWalletData(guid), password, guid)
        setLastLoggedInGuid(guid)
        authHelper.configureDB(getWallet()?.address, guid)
        MigrationUtil.checkOldAddressBook(prefs, guid)
    }

    fun isAuthenticated(): Boolean {
        return WavesWallet.isAuthenticated()
    }

    fun getWallet(): WavesWallet? {
        return WavesWallet.getWallet()
    }

    private fun createAddressBookCurrentAccount(): AddressBookUserDb? {
        if (TextUtils.isEmpty(loggedInGuid)) {
            return null
        }

        val name = prefs.getGlobalValue(loggedInGuid + PrefsUtil.KEY_WALLET_NAME, "")
        val publicKey = prefs.getGlobalValue(loggedInGuid + PrefsUtil.KEY_PUB_KEY, "")

        return if (TextUtils.isEmpty(publicKey) || TextUtils.isEmpty(name)) {
            null
        } else AddressBookUserDb(addressFromPublicKey(
                WavesCrypto.base58decode(publicKey), EnvironmentManager.netCode), name)
    }

    fun deleteCurrentWavesWallet(): Boolean {
        val currentUser = createAddressBookCurrentAccount()
        return if (currentUser == null) {
            false
        } else {
            deleteWavesWallet(currentUser.address)
            true
        }
    }

    private fun clearRealmConfiguration() {
        app.stopService(Intent(app, UpdateApiDataService::class.java))
        val f = RealmConfigStore::class.java.getDeclaredField("configMap") // NoSuchFieldException
        f.isAccessible = true
        val configMap = f.get(RealmConfigStore::class.java) as MutableMap<*, *>
        configMap.clear()
    }

    private fun deleteRealmDBForAccount(guid: String, fullClear: Boolean = true) {
        fun deleteDBWith(dbName: String) {
            // force delete db
            try {
                val dbFile = File(DBHelper.getInstance().realmConfig.realmDirectory,
                        String.format("%s.realm", dbName))
                val dbLockFile = File(DBHelper.getInstance().realmConfig.realmDirectory,
                        String.format("%s.realm.lock", dbName))
                dbFile.delete()
                dbLockFile.delete()
                deleteRecursive(File(DBHelper.getInstance().realmConfig.realmDirectory,
                        String.format("%s.realm.management", dbName)))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        deleteDBWith(guid)
        if (fullClear) {
            deleteDBWith("${guid}_userdata")
        }
    }

    fun deleteWavesWallet(address: String) {
        val searchWalletGuid = App.accessManager.findGuidBy(address)

        prefs.removeValue(searchWalletGuid, PrefsUtil.KEY_PUB_KEY)
        prefs.removeValue(searchWalletGuid, PrefsUtil.KEY_WALLET_NAME)
        prefs.removeValue(searchWalletGuid, PrefsUtil.KEY_ENCRYPTED_WALLET)
        prefs.removeValue(searchWalletGuid, PrefsUtil.KEY_SKIP_BACKUP)
        prefs.removeValue(searchWalletGuid, PrefsUtil.KEY_LAST_UPDATE_DEX_INFO)
        prefs.removeValue(searchWalletGuid, PrefsUtil.KEY_ENCRYPTED_PASSWORD)
        prefs.removeValue(searchWalletGuid, PrefsUtil.KEY_ACCOUNT_FIRST_OPEN)
        prefs.removeValue(searchWalletGuid, PrefsUtil.KEY_DEFAULT_ASSETS)
        prefs.removeValue(searchWalletGuid, PrefsUtil.KEY_NEED_UPDATE_TRANSACTION_AFTER_CHANGE_SPAM_SETTINGS)

        prefs.setGlobalValue(EnvironmentManager.name +
                PrefsUtil.LIST_WALLET_GUIDS, createGuidsListWithout(searchWalletGuid))

        if (searchWalletGuid == getLoggedInGuid()) {
            loggedInGuid = ""
            prefs.removeGlobalValue(PrefsUtil.GLOBAL_LAST_LOGGED_IN_GUID)
        }

        removeFromRecentSavedWallets(searchWalletGuid)
        deleteRealmAndCleanConfigs(searchWalletGuid, true)
    }

    private fun deleteRealmAndCleanConfigs(guid: String, fullClear: Boolean = true) {
        deleteRealmDBForAccount(guid, fullClear)
        clearRealmConfiguration()
    }

    private fun createGuidsListWithout(guidToRemove: String): Array<String> {
        val guids = prefs.getGlobalValueList(
                EnvironmentManager.name + PrefsUtil.LIST_WALLET_GUIDS)
        val resultGuidsList = ArrayList<String>()
        for (guid in guids) {
            if (guid != guidToRemove) {
                resultGuidsList.add(guid)
            }
        }
        return resultGuidsList.toTypedArray()
    }

    fun getWalletData(guid: String): String {
        return if (TextUtils.isEmpty(guid)) {
            ""
        } else {
            prefs.getGlobalValue(guid + PrefsUtil.KEY_ENCRYPTED_WALLET, "")
        }
    }

    fun getWalletName(guid: String): String {
        return if (TextUtils.isEmpty(guid)) {
            ""
        } else {
            prefs.getGlobalValue(guid + PrefsUtil.KEY_WALLET_NAME, "")
        }
    }

    fun getWalletAddress(guid: String): String {
        if (TextUtils.isEmpty(guid)) {
            return ""
        }
        val publicKey = prefs.getValue(guid, PrefsUtil.KEY_PUB_KEY, "")
        return addressFromPublicKey(WavesCrypto.base58decode(publicKey), EnvironmentManager.netCode)
    }

    fun storePassword(guid: String, publicKeyStr: String, encryptedPassword: String) {
        prefs.setGlobalValue(PrefsUtil.GLOBAL_LAST_LOGGED_IN_GUID, guid)
        prefs.setValue(PrefsUtil.KEY_PUB_KEY, publicKeyStr)
        prefs.setValue(PrefsUtil.KEY_ENCRYPTED_WALLET, encryptedPassword)
    }

    fun incrementPassCodeInputFails(guid: String) {
        if (!TextUtils.isEmpty(guid)) {
            prefs.setValue(guid, PrefsUtil.KEY_PIN_FAILS, getPassCodeInputFails(guid) + 1)
        }
    }

    fun getPassCodeInputFails(guid: String): Int {
        if (!TextUtils.isEmpty(guid)) {
            return prefs.getValue(guid, PrefsUtil.KEY_PIN_FAILS, 0)
        }
        return 0
    }

    fun resetPassCodeInputFails(guid: String) {
        if (!TextUtils.isEmpty(guid)) {
            prefs.removeValue(guid, PrefsUtil.KEY_PIN_FAILS)
        }
    }

    fun setCurrentAccountBackupDone() {
        prefs.setValue(PrefsUtil.KEY_SKIP_BACKUP, false)
    }

    fun isCurrentAccountBackupSkipped(): Boolean {
        return prefs.getValue(PrefsUtil.KEY_SKIP_BACKUP, true)
    }

    fun setUseFingerPrint(guid: String, use: Boolean) {
        if (!use) {
            prefs.removeValue(guid, PrefsUtil.KEY_USE_FINGERPRINT)
            SecurePreferences.removeValue(guid + EnterPassCodeActivity.KEY_INTENT_PASS_CODE)
        }
        prefs.setValue(guid, PrefsUtil.KEY_USE_FINGERPRINT, use)
    }

    fun isGuidUseFingerPrint(guid: String): Boolean {
        return prefs.getGuidValue(guid, PrefsUtil.KEY_USE_FINGERPRINT, false)
    }

    fun isUseFingerPrint(guid: String): Boolean {
        return prefs.getGuidValue(guid, PrefsUtil.KEY_USE_FINGERPRINT, false)
    }

    fun addToRecentSavedWallets(guid: String) {
        val recents = prefs.getGlobalValueList(
                EnvironmentManager.name + PrefsUtil.LIST_RECENT_SAVED_WALLETS).toMutableList()

        fun swapAccountIfExistOr(guid: String, notExistFunc: () -> Unit) {
            if (recents.contains(guid)) {
                recents.removeAt(recents.indexOf(guid))
                recents.add(guid)
            } else {
                notExistFunc.invoke()
            }
        }

        if (recents.size >= MAX_RECENT_SAVED_ACCOUNTS) {
            swapAccountIfExistOr(guid) {
                // delete the oldest account DB and configs and add new account to list
                deleteRealmAndCleanConfigs(recents.first(), false)
                recents.removeAt(0)
                recents.add(guid)
            }
        } else {
            swapAccountIfExistOr(guid) {
                recents.add(guid)
            }
        }

        prefs.setGlobalValue(EnvironmentManager.name +
                PrefsUtil.LIST_RECENT_SAVED_WALLETS, recents.toTypedArray())
    }

    fun removeFromRecentSavedWallets(guid: String) {
        val recents = prefs.getGlobalValueList(
                EnvironmentManager.name + PrefsUtil.LIST_RECENT_SAVED_WALLETS).toMutableList()
        if (recents.contains(guid)) {
            recents.removeAt(recents.indexOf(guid))
        }

        prefs.setGlobalValue(EnvironmentManager.name +
                PrefsUtil.LIST_RECENT_SAVED_WALLETS, recents.toTypedArray())
    }

    companion object {
        const val MAX_RECENT_SAVED_ACCOUNTS = 3
    }
}