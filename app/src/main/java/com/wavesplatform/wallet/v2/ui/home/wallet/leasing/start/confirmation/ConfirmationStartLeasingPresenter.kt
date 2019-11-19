/*
 * Created by Eduard Zaydel on 1/4/2019
 * Copyright © 2019 Waves Platform. All rights reserved.
 */

package com.wavesplatform.wallet.v2.ui.home.wallet.leasing.start.confirmation

import moxy.InjectViewState
import com.wavesplatform.sdk.utils.MoneyUtil
import com.wavesplatform.sdk.model.request.node.LeaseTransaction
import com.wavesplatform.sdk.utils.isSmartError
import com.wavesplatform.sdk.utils.makeAsAlias
import com.wavesplatform.wallet.v2.ui.base.presenter.BasePresenter
import com.wavesplatform.sdk.utils.RxUtil
import com.wavesplatform.wallet.v2.util.errorBody
import javax.inject.Inject

@InjectViewState
class ConfirmationStartLeasingPresenter @Inject constructor() : BasePresenter<ConfirmationStartLeasingView>() {

    private var createLeasingRequest: LeaseTransaction? = null
    var recipientIsAlias = false
    var address: String = ""
    var amount: String = ""
    var fee = 0L

    var success = false

    fun startLeasing() {
        createLeasingRequest = if (recipientIsAlias) {
            LeaseTransaction(
                    recipient = address.makeAsAlias(),
                    amount = MoneyUtil.getUnscaledValue(amount, 8))
        } else {
            LeaseTransaction(
                    recipient = address,
                    amount = MoneyUtil.getUnscaledValue(amount, 8))
        }
        addSubscription(nodeServiceManager.startLeasing(createLeasingRequest!!, fee)
                .compose(RxUtil.applyObservableDefaultSchedulers())
                .subscribe({
                    success = true
                    viewState.successStartLeasing()
                    viewState.showProgressBar(false)
                }, {
                    it.printStackTrace()
                    viewState.showProgressBar(false)

                    it.errorBody()?.let { error ->
                        if (error.isSmartError()) {
                            viewState.failedStartLeasingCauseSmart()
                        } else {
                            viewState.failedStartLeasing(error.message)
                        }
                    }
                }))
    }
}
