package com.wavesplatform.wallet.v2.ui.receive.invoice

import com.arellomobile.mvp.InjectViewState
import com.wavesplatform.wallet.v2.data.model.remote.response.AssetBalance
import com.wavesplatform.wallet.v2.ui.base.presenter.BasePresenter
import javax.inject.Inject

@InjectViewState
class InvoicePresenter @Inject constructor() :BasePresenter<InvoiceView>(){
    var assetBalance: AssetBalance? = null
}
