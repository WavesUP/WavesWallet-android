/*
 * Created by Eduard Zaydel on 1/4/2019
 * Copyright © 2019 Waves Platform. All rights reserved.
 */

package com.wavesplatform.wallet.v2.ui.auth.fingerprint

import android.os.Bundle
import moxy.presenter.InjectPresenter
import moxy.presenter.ProvidePresenter
import com.wavesplatform.sdk.WavesSdk
import com.wavesplatform.wallet.App
import com.wavesplatform.wallet.R
import com.wavesplatform.wallet.v2.ui.auth.passcode.create.CreatePassCodeActivity
import com.wavesplatform.wallet.v2.ui.base.view.BaseActivity
import com.wavesplatform.wallet.v2.ui.home.MainActivity
import com.wavesplatform.wallet.v2.ui.keeper.KeeperTransactionActivity
import com.wavesplatform.wallet.v2.util.launchActivity
import com.wavesplatform.wallet.v2.util.showError
import kotlinx.android.synthetic.main.activity_use_fingerprint.*
import pers.victor.ext.click
import javax.inject.Inject

class UseFingerprintActivity : BaseActivity(), UseFingerprintView {

    @Inject
    @InjectPresenter
    lateinit var presenter: UseFingerprintPresenter
    private var guid: String = ""

    @ProvidePresenter
    fun providePresenter(): UseFingerprintPresenter = presenter

    override fun configLayoutRes() = R.layout.activity_use_fingerprint

    override fun onCreate(savedInstanceState: Bundle?) {
        overridePendingTransition(R.anim.slide_in_right, R.anim.null_animation)
        super.onCreate(savedInstanceState)
    }

    override fun onViewReady(savedInstanceState: Bundle?) {
        button_use_fingerprint.click {
            val passCode = intent.extras.getString(CreatePassCodeActivity.KEY_INTENT_PASS_CODE)
            guid = intent.extras.getString(CreatePassCodeActivity.KEY_INTENT_GUID)

            val fingerprintDialog = FingerprintAuthDialogFragment.newInstance(guid, passCode)
            fingerprintDialog.isCancelable = false
            fingerprintDialog.show(supportFragmentManager, "fingerprintDialog")
            fingerprintDialog.setFingerPrintDialogListener(
                    object : FingerprintAuthDialogFragment.FingerPrintDialogListener {
                        override fun onSuccessRecognizedFingerprint() {
                            App.getAccessManager().setUseFingerPrint(guid, true)
                            launchActivity<MainActivity>(clear = true)
                        }

                        override fun onFingerprintLocked(message: String) {
                            onBackPressed()
                        }

                        override fun onShowErrorMessage(message: String) {
                            showError(message, R.id.content)
                            fingerprintDialog.dismiss()
                        }
                    })
        }

        button_do_it_later.click {
            onBackPressed()
        }
    }

    override fun onBackPressed() {
        App.getAccessManager().setUseFingerPrint(guid, false)
        if (WavesSdk.keeper().isKeeperIntent(intent)) {
            launchActivity<KeeperTransactionActivity>()
        } else {
            launchActivity<MainActivity>(clear = true)
        }
    }
}