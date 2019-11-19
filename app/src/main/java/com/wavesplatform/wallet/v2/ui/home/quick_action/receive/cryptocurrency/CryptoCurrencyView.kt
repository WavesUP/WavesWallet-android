/*
 * Created by Eduard Zaydel on 1/4/2019
 * Copyright © 2019 Waves Platform. All rights reserved.
 */

package com.wavesplatform.wallet.v2.ui.home.quick_action.receive.cryptocurrency

import moxy.viewstate.strategy.SkipStrategy
import moxy.viewstate.strategy.StateStrategyType
import com.wavesplatform.wallet.v2.data.model.remote.response.gateway.GatewayDeposit
import com.wavesplatform.wallet.v2.ui.base.view.BaseMvpView
import java.math.BigDecimal

@StateStrategyType(SkipStrategy::class)
interface CryptoCurrencyView : BaseMvpView {
    fun onShowError(message: String)
    fun onGatewayError()
    fun onSuccessInitDeposit(response: GatewayDeposit)
}
