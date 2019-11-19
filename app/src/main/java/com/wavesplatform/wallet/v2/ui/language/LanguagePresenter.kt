/*
 * Created by Eduard Zaydel on 1/4/2019
 * Copyright © 2019 Waves Platform. All rights reserved.
 */

package com.wavesplatform.wallet.v2.ui.language

import moxy.InjectViewState
import com.wavesplatform.wallet.v2.data.model.local.Language
import com.wavesplatform.wallet.v2.data.model.local.LanguageItem
import com.wavesplatform.wallet.v2.ui.base.presenter.BasePresenter
import javax.inject.Inject

@InjectViewState
class LanguagePresenter @Inject constructor() : BasePresenter<LanguageView>() {
    var currentLanguagePosition: Int = -1

    fun getLanguages(): MutableList<LanguageItem>? {
        return Language.getLanguagesItems()
    }

    fun saveLanguage(lang: String) {
        preferenceHelper.setLanguage(lang)
    }
}
