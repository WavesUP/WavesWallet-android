/*
 * Created by Eduard Zaydel on 1/4/2019
 * Copyright © 2019 Waves Platform. All rights reserved.
 */

package com.wavesplatform.wallet.v2.ui.home.wallet

import moxy.InjectViewState
import com.wavesplatform.wallet.BuildConfig
import com.wavesplatform.wallet.v2.ui.base.presenter.BasePresenter
import com.wavesplatform.wallet.v2.util.Version
import com.wavesplatform.wallet.v2.util.PrefsUtil
import javax.inject.Inject

@InjectViewState
class WalletPresenter @Inject constructor() : BasePresenter<WalletView>() {
    var shadowEnable = false

    fun showTopBannerIfNeed() {
        if (!prefsUtil.getValue(PrefsUtil.KEY_IS_CLEARED_ALERT_ALREADY_SHOWN, false) &&
                prefsUtil.getValue(PrefsUtil.KEY_IS_NEED_TO_SHOW_CLEARED_ALERT, false)) {
            viewState.afterCheckClearedWallet()
        } else {
            checkNewAppUpdates()
        }
    }

    private fun checkNewAppUpdates() {
        val needUpdate = Version.needAppUpdate(BuildConfig.VERSION_NAME, preferenceHelper.lastAppVersion)
        viewState.afterCheckNewAppUpdates(needUpdate)
    }
}
