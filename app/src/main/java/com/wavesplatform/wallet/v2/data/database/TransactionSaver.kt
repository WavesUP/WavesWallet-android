/*
 * Created by Eduard Zaydel on 1/4/2019
 * Copyright © 2019 Waves Platform. All rights reserved.
 */

package com.wavesplatform.wallet.v2.data.database

import com.vicpin.krealmextensions.*
import com.wavesplatform.sdk.model.response.data.AssetInfoResponse
import com.wavesplatform.sdk.model.response.node.HistoryTransactionResponse
import com.wavesplatform.wallet.v2.data.model.local.TransactionType
import com.wavesplatform.sdk.utils.*
import com.wavesplatform.wallet.v2.data.Events
import com.wavesplatform.wallet.v2.data.manager.DataServiceManager
import com.wavesplatform.wallet.v2.data.manager.NodeServiceManager
import com.wavesplatform.wallet.v2.data.model.db.SpamAssetDb
import com.wavesplatform.wallet.v2.data.model.db.TransactionDb
import com.wavesplatform.wallet.v2.data.model.db.TransferDb
import com.wavesplatform.wallet.v2.data.model.local.LeasingStatus
import com.wavesplatform.wallet.v2.util.RxEventBus
import com.wavesplatform.wallet.App
import com.wavesplatform.wallet.v2.util.WavesWallet
import com.wavesplatform.wallet.v2.util.getTransactionType
import com.wavesplatform.wallet.v2.util.transactionType
import io.reactivex.disposables.CompositeDisposable
import pyxis.uzuki.live.richutilskt.utils.runAsync
import pyxis.uzuki.live.richutilskt.utils.runOnUiThread
import javax.inject.Inject

@Deprecated("Temp class for saving transactions, should refactor")
class TransactionSaver @Inject constructor() {

    @Inject
    lateinit var rxEventBus: RxEventBus
    @Inject
    lateinit var nodeServiceManager: NodeServiceManager
    @Inject
    lateinit var dataServiceManager: DataServiceManager
    private var allAssets = arrayListOf<AssetInfoResponse>()
    private var subscriptions: CompositeDisposable = CompositeDisposable()
    private var currentLimit = DEFAULT_LIMIT
    private var prevLimit = DEFAULT_LIMIT
    private var needCheckToUpdateBalance = false

    fun saveTransactions(
            sortedList: List<HistoryTransactionResponse>,
            limit: Int = DEFAULT_LIMIT,
            changeListener: OnTransactionLimitChangeListener? = null
    ) {
        currentLimit = limit
        if (!WavesWallet.isAuthenticated()
                || sortedList.isEmpty()
                || limit < 1) {
            rxEventBus.post(Events.NeedUpdateHistoryScreen())
            return
        }

        runAsync {
            // check if exist last transaction
            queryAsync<TransactionDb>({ equalTo("id", sortedList[sortedList.size - 1].id) },
                    {
                        if (it.isEmpty()) {
                            // all list is new, need load more

                            if (currentLimit >= MAX_LIMIT) currentLimit = 50

                            if (prevLimit == DEFAULT_LIMIT) {
                                saveToDb(sortedList)
                            } else {
                                try {
                                    saveToDb(sortedList.subList(prevLimit - 1, sortedList.size - 1))
                                } catch (e: Exception) {
                                    currentLimit = 50
                                    runOnUiThread {
                                        rxEventBus.post(Events.StopUpdateHistoryScreen())
                                    }
                                }
                            }

                            // save previous count of loaded transactions for future cut list
                            prevLimit = currentLimit

                            // multiply current limit
                            currentLimit *= 2

                            changeListener.notNull { listener ->
                                listener.onChange(currentLimit)
                            }
                        } else {
                            // check if exist first transaction
                            queryAsync<TransactionDb>({ equalTo("id", sortedList[0].id) },
                                    {
                                        if (it.isEmpty()) {
                                            // only few new transaction
                                            needCheckToUpdateBalance = true
                                            saveToDb(sortedList)
                                        } else {
                                            runOnUiThread {
                                                rxEventBus.post(Events.StopUpdateHistoryScreen())
                                            }
                                        }
                                    })
                        }
                    })
        }
    }

    private fun saveToDb(transactions: List<HistoryTransactionResponse>) {

        // grab all assetsIds
        val tempGrabbedAssets = mutableListOf<String?>()
        transactions.forEach { transition ->
            transition.order1?.assetPair?.notNull { assetPair ->
                tempGrabbedAssets.add(assetPair.amountAsset)
                tempGrabbedAssets.add(assetPair.priceAsset)
            }
            if (!transition.payment.isNullOrEmpty()){
                transition.payment.first()?.notNull { payment ->
                    tempGrabbedAssets.add(payment.assetId)
                }
            }
            tempGrabbedAssets.add(transition.assetId)
            tempGrabbedAssets.add(transition.feeAssetId)
        }

        // filter all without duplicates
        val allTransactionsAssets = tempGrabbedAssets.asSequence()
                .filter { !it.isNullOrEmpty() }
                .distinct()
                .toMutableList()

        subscriptions.add(dataServiceManager.assets(ids = allTransactionsAssets)
                .compose(RxUtil.applyObservableDefaultSchedulers())
                .subscribe ({
                    mergeAndSaveAllAssets(ArrayList(it)) { assetsInfo ->
                        transactions.forEach { trans ->
                            if (trans.assetId.isNullOrEmpty()) {
                                trans.asset = WavesConstants.WAVES_ASSET_INFO
                            } else {
                                trans.asset = allAssets.firstOrNull { it.id == trans.assetId }
                            }

                            if (trans.feeAssetId.isNullOrEmpty()) {
                                trans.feeAssetObject = WavesConstants.WAVES_ASSET_INFO
                            } else {
                                trans.feeAssetObject = allAssets.firstOrNull { it.id == trans.feeAssetId }
                            }

                            if (!trans.payment.isNullOrEmpty()) {
                                trans.payment.first()?.let { payment ->
                                    if (payment.assetId.isNullOrEmpty()) {
                                        payment.asset = WavesConstants.WAVES_ASSET_INFO
                                    } else {
                                        payment.asset = allAssets.firstOrNull { it.id == payment.assetId }
                                    }
                                }
                            }

                            when {
                                trans.recipient.isAlias() -> {
                                    val aliasName = trans.recipient.parseAlias()
                                    loadAliasAddress(aliasName) { address ->
                                        trans.recipientAddress = address
                                        trans.transactionTypeId = getTransactionType(
                                                trans, WavesWallet.getAddress())
                                        TransactionDb(trans).save()
                                    }

                                }
                                trans.lease?.recipient?.isAlias() == true -> {
                                    val aliasName = trans.lease?.recipient?.parseAlias()
                                    loadAliasAddress(aliasName) { address ->
                                        trans.lease?.recipientAddress = address
                                        trans.transactionTypeId = getTransactionType(
                                                trans, WavesWallet.getAddress())
                                        TransactionDb(trans).save()
                                    }
                                }
                                else -> {
                                    trans.recipientAddress = trans.recipient
                                    trans.lease?.recipientAddress = trans.lease?.recipient
                                }
                            }

                            trans.transfers.forEach { transfer ->
                                when {
                                    transfer.recipient.isAlias() -> {
                                        val aliasName = transfer.recipient.parseAlias()
                                        loadAliasAddress(aliasName) { address ->
                                            transfer.recipientAddress = address
                                            TransferDb(transfer).save()
                                        }
                                    }
                                    else -> transfer.recipientAddress = transfer.recipient
                                }
                            }

                            if (trans.order1 != null) {
                                val amountAsset =
                                        if (trans.order1?.assetPair?.amountAsset.isNullOrEmpty()) {
                                            WavesConstants.WAVES_ASSET_INFO
                                        } else {
                                            allAssets.firstOrNull { it.id == trans.order1?.assetPair?.amountAsset }
                                        }
                                val priceAsset =
                                        if (trans.order1?.assetPair?.priceAsset.isNullOrEmpty()) {
                                            WavesConstants.WAVES_ASSET_INFO
                                        } else {
                                            allAssets.firstOrNull { it.id == trans.order1?.assetPair?.priceAsset }
                                        }

                                trans.order1?.assetPair?.amountAssetObject = amountAsset
                                trans.order1?.assetPair?.priceAssetObject = priceAsset
                                trans.order2.notNull {
                                    it.assetPair?.amountAssetObject = amountAsset
                                    it.assetPair?.priceAssetObject = priceAsset
                                }
                            }
                            trans.transactionTypeId = getTransactionType(
                                    trans, WavesWallet.getAddress())
                        }

                        if (needCheckToUpdateBalance) {
                            needCheckToUpdateBalance = false
                            val needUpdateBalance = transactions.any {
                                it.transactionType() == TransactionType.SPAM_RECEIVE_TYPE || it.transactionType() == TransactionType.RECEIVED_TYPE ||
                                        it.transactionType() == TransactionType.MASS_SPAM_RECEIVE_TYPE || it.transactionType() == TransactionType.MASS_RECEIVE_TYPE ||
                                        it.transactionType() == TransactionType.EXCHANGE_TYPE
                            }
                            if (needUpdateBalance) {
                                rxEventBus.post(Events.UpdateAssetsBalance())
                            }
                        }

                        // check old started leasing transaction correct status
                        val canceledLeasingTransactions = transactions
                                .filter { it.transactionType() == TransactionType.CANCELED_LEASING_TYPE }
                        if (canceledLeasingTransactions.isNotEmpty()) {
                            canceledLeasingTransactions.forEach {
                                val first = queryFirst<TransactionDb> { equalTo("id", it.leaseId) }
                                if (first?.status != LeasingStatus.CANCELED.status) {
                                    first?.status = LeasingStatus.CANCELED.status
                                    first?.save()
                                }
                            }
                        }

                        TransactionDb.convertToDb(transactions).saveAll()
                        runOnUiThread {
                            rxEventBus.post(Events.NeedUpdateHistoryScreen())
                        }
                    }
                }, {
                    it.printStackTrace()
                }))
    }

    private fun loadAliasAddress(alias: String?, listener: (String?) -> Unit) {
        if (App.accessManager.isAuthenticated()) {
            alias.notNull {
                subscriptions.add(dataServiceManager.loadAlias(it)
                        .compose(RxUtil.applyObservableDefaultSchedulers())
                        .subscribe {
                            listener.invoke(it.address)
                        })
            }
        }
    }

    private fun mergeAndSaveAllAssets(arrayList: ArrayList<AssetInfoResponse>, callback: (ArrayList<AssetInfoResponse>) -> Unit) {
        runAsync {
            queryAllAsync<SpamAssetDb> { spams ->
                val spamMap = spams.associateBy { it.assetId }
                val allAssetMap = allAssets.associateBy { it.id }
                arrayList.iterator().forEach { newAsset ->
                    if (allAssetMap[newAsset.id] == null) {
                        newAsset.isSpam = spamMap[newAsset.id] != null
                        allAssets.add(newAsset)
                    }
                }
                callback(allAssets)
            }
        }
    }

    companion object {
        const val MAX_LIMIT = 1000
        const val DEFAULT_LIMIT = 100
    }

    interface OnTransactionLimitChangeListener {
        fun onChange(limit: Int)
    }
}