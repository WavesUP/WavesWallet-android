/*
 * Created by Eduard Zaydel on 1/4/2019
 * Copyright © 2019 Waves Platform. All rights reserved.
 */

package com.wavesplatform.wallet.v2.ui.welcome

import moxy.InjectViewState
import com.wavesplatform.wallet.v2.ui.base.presenter.BasePresenter
import javax.inject.Inject

@InjectViewState
class WelcomePresenter @Inject constructor() : BasePresenter<WelcomeView>() {

    fun saveLanguage(lang: String) {
        preferenceHelper.setLanguage(lang)
    }
}
