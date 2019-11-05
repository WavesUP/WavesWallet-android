/*
 * Created by Eduard Zaydel on 1/4/2019
 * Copyright © 2019 Waves Platform. All rights reserved.
 */

package com.wavesplatform.wallet.v2.ui.home.quick_action.receive.card

import android.text.TextUtils
import moxy.InjectViewState
import com.vicpin.krealmextensions.queryAsSingle
import com.wavesplatform.sdk.utils.WavesConstants
import com.wavesplatform.sdk.model.response.node.AssetBalanceResponse
import com.wavesplatform.wallet.App
import com.wavesplatform.wallet.R
import com.wavesplatform.wallet.v2.data.model.db.AssetBalanceDb
import com.wavesplatform.wallet.v2.data.manager.gateway.manager.CoinomatDataManager
import com.wavesplatform.wallet.v2.ui.base.presenter.BasePresenter
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import pyxis.uzuki.live.richutilskt.utils.runAsync
import pyxis.uzuki.live.richutilskt.utils.runOnUiThread
import javax.inject.Inject

@InjectViewState
class CardPresenter @Inject constructor() : BasePresenter<CardView>() {

    @Inject
    lateinit var coinomatServiceManager: CoinomatDataManager

    var crypto: String = WavesConstants.WAVES_ASSET_ID_FILLED
    private var amount: String = "0"
    var fiat: String = "USD"
    private var min: Float = 0F
    private var max: Float = 0F
    var asset: AssetBalanceResponse? = null
    private var rate = ""

    fun invalidate() {
        viewState.showWaves(asset)
        viewState.showRate(rate)
        viewState.showLimits(min.toString(), max.toString(), fiat)
    }

    fun isValid(): Boolean {
        return !TextUtils.isEmpty(amount) && amount.toFloat() >= min && amount.toFloat() <= max
    }

    fun loadWaves() {
        runAsync {
            val singleData: Single<List<AssetBalanceDb>> = queryAsSingle { equalTo("assetId", "") }
            addSubscription(singleData
                    .subscribeOn(Schedulers.computation())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        if (it.size == 1) {
                            runOnUiThread {
                                asset = it[0].convertFromDb()
                                viewState.showWaves(asset)
                            }
                        }
                    }, {
                        viewState.showError(App.getAppContext()
                                .getString(R.string.receive_error_network))
                    }))
        }
    }

    fun fiatChanged(fiat: String) {
        this.fiat = fiat
        loadLimits()
        loadRate()
    }

    fun amountChanged(amount: String) {
        if (!TextUtils.isEmpty(amount)) {
            this.amount = amount
        }
        loadRate()
    }

    private fun loadRate() {
        if (TextUtils.isEmpty(amount) || amount == "0") {
            runOnUiThread {
                viewState.showRate("0")
            }
            return
        }

        runAsync {
            addSubscription(coinomatServiceManager.loadRate(crypto, getWavesAddress(), fiat, amount).subscribe({ rate ->
                this.rate = rate
                runOnUiThread {
                    viewState.showRate(rate)
                }
            }, {
                runOnUiThread {
                    viewState.onGatewayError()
                }
            }))
        }
    }

    private fun loadLimits() {
        runAsync {
            addSubscription(coinomatServiceManager.loadLimits(crypto, getWavesAddress(), fiat).subscribe({ limits ->
                min = if (limits?.min == null) {
                    0F
                } else {
                    limits.min!!.toFloat()
                }
                max = if (limits?.max == null) {
                    0F
                } else {
                    limits.max!!.toFloat()
                }
                runOnUiThread {
                    viewState.showLimits(limits.min, limits.max, fiat)
                }
            }, {
                runOnUiThread {
                    viewState.showError(App.getAppContext()
                            .getString(R.string.receive_error_network))
                }
            }))
        }
    }

    fun createLink(): String {
        return "https://coinomat.com/api/v2/indacoin/buy.php?" +
                "crypto=$crypto" +
                "&fiat=$fiat" +
                "&address=${getWavesAddress()}" +
                "&amount=$amount"
    }
}
