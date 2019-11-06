/*
 * Created by Eduard Zaydel on 1/4/2019
 * Copyright © 2019 Waves Platform. All rights reserved.
 */

package com.wavesplatform.wallet.v2.ui.custom

import android.content.Context
import androidx.appcompat.widget.AppCompatTextView
import android.util.AttributeSet
import com.wavesplatform.wallet.R
import pers.victor.ext.findColor
import pers.victor.ext.findDrawable

class SpamTag : AppCompatTextView {

    constructor(context: Context) : super(context) {
        provideTagStyle()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        provideTagStyle()
    }

    private fun provideTagStyle() {
        this.text = context.getString(R.string.wallet_assets_spam_tag)
        this.isAllCaps = true
        this.setTextColor(findColor(R.color.basic500))
        this.textSize = 11f
        this.background = findDrawable(R.drawable.bg_spam_tag)
    }
}
