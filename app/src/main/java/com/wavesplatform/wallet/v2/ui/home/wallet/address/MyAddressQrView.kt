/*
 * Created by Eduard Zaydel on 1/4/2019
 * Copyright © 2019 Waves Platform. All rights reserved.
 */

package com.wavesplatform.wallet.v2.ui.home.wallet.address

import android.graphics.Bitmap
import androidx.appcompat.widget.AppCompatImageView
import com.wavesplatform.sdk.model.response.node.transaction.AliasTransactionResponse
import com.wavesplatform.wallet.v2.ui.base.view.BaseMvpView

interface MyAddressQrView : BaseMvpView {
    fun showQRCode(qrCode: Bitmap?)
    fun afterSuccessGenerateAvatar(bitmap: Bitmap, imageView: AppCompatImageView)
    fun afterSuccessLoadAliases(ownAliases: MutableList<AliasTransactionResponse>)
}
