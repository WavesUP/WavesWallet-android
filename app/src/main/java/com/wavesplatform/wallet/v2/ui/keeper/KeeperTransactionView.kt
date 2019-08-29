package com.wavesplatform.wallet.v2.ui.keeper

import com.wavesplatform.sdk.model.request.node.BaseTransaction
import com.wavesplatform.sdk.model.response.node.AssetsDetailsResponse
import com.wavesplatform.sdk.model.response.node.transaction.BaseTransactionResponse
import com.wavesplatform.wallet.v2.ui.base.view.BaseMvpView

interface KeeperTransactionView : BaseMvpView {
    fun onSuccessSend(transaction: BaseTransactionResponse)
    fun onError(error: Throwable)
    fun onSuccessSign(transaction: BaseTransaction)
    fun onReceivedAsset(asset: AssetsDetailsResponse)
}