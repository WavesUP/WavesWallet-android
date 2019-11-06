/*
 * Created by Eduard Zaydel on 1/4/2019
 * Copyright © 2019 Waves Platform. All rights reserved.
 */

package com.wavesplatform.wallet.v2.ui.home.quick_action.receive.address_view

import android.graphics.Bitmap
import moxy.InjectViewState
import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import com.wavesplatform.wallet.v2.util.zxing.Contents
import com.wavesplatform.wallet.v2.util.zxing.encode.QRCodeEncoder
import com.wavesplatform.wallet.v2.ui.base.presenter.BasePresenter
import com.wavesplatform.sdk.utils.RxUtil
import io.reactivex.Observable
import javax.inject.Inject

@InjectViewState
class ReceiveAddressViewPresenter @Inject constructor() : BasePresenter<ReceiveAddressView>() {
    fun generateQRCode(text: String, size: Int) {
        addSubscription(generateQrCodeObservable(text, size)
                .compose(RxUtil.applyObservableDefaultSchedulers())
                .subscribe {
                    viewState.showQRCode(it)
                })
    }

    private fun generateQrCodeObservable(uri: String, dimensions: Int): Observable<Bitmap> {
        return Observable.defer {
            var bitmap: Bitmap? = null
            val qrCodeEncoder = QRCodeEncoder(uri, null, Contents.Type.TEXT, BarcodeFormat.QR_CODE.toString(), dimensions)
            try {
                bitmap = qrCodeEncoder.encodeAsBitmap()
            } catch (e: WriterException) {
                return@defer Observable.error<Bitmap>(e)
            }

            if (bitmap == null) {
                return@defer Observable.error<Bitmap>(Throwable("Bitmap was null"))
            } else {
                return@defer Observable.just<Bitmap>(bitmap)
            }
        }
    }
}
