/*
 * Created by Eduard Zaydel on 1/4/2019
 * Copyright © 2019 Waves Platform. All rights reserved.
 */

package com.wavesplatform.wallet.v2.ui.home.wallet.leasing

import moxy.InjectViewState
import com.vicpin.krealmextensions.queryAsSingle
import com.wavesplatform.sdk.model.response.node.AssetBalanceResponse
import com.wavesplatform.sdk.utils.RxUtil
import com.wavesplatform.wallet.v2.data.Constants
import com.wavesplatform.wallet.v2.data.model.db.TransactionDb
import com.wavesplatform.wallet.v2.data.model.local.LeasingStatus
import com.wavesplatform.wallet.v2.ui.base.presenter.BasePresenter
import com.wavesplatform.wallet.v2.util.WavesWallet
import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import pyxis.uzuki.live.richutilskt.utils.runAsync
import javax.inject.Inject

@InjectViewState
class LeasingPresenter @Inject constructor() : BasePresenter<LeasingView>() {

    var enableElevation: Boolean = false

    fun getActiveLeasing() {
        if (WavesWallet.isAuthenticated()) {
            runAsync {
                addSubscription(Observable.zip(nodeServiceManager.loadWavesBalance(),
                        queryAsSingle<TransactionDb> {
                            equalTo("status", LeasingStatus.ACTIVE.status)
                                    .and()
                                    .equalTo("transactionTypeId", Constants.ID_STARTED_LEASING_TYPE)
                        }.map {
                            return@map ArrayList(it.sortedByDescending { it.timestamp })
                        }.toObservable(),
                        BiFunction { t1: AssetBalanceResponse, t2: List<TransactionDb> ->
                            return@BiFunction Pair(t1, t2)
                        })
                        .compose(RxUtil.applySchedulersToObservable())
                        .subscribe({
                            viewState.showBalances(it.first)
                            viewState.showActiveLeasingTransaction(TransactionDb.convertFromDb(it.second))
                        }, {
                            it.printStackTrace()
                            viewState.afterFailedLoadLeasing()
                        }))
            }
        }
    }
}
