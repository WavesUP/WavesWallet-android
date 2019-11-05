/*
 * Created by Eduard Zaydel on 1/4/2019
 * Copyright © 2019 Waves Platform. All rights reserved.
 */

package com.wavesplatform.wallet.v2.ui.home.wallet.assets.token_burn.confirmation

import android.content.Intent
import android.os.Bundle
import android.view.animation.AnimationUtils
import moxy.presenter.InjectPresenter
import moxy.presenter.ProvidePresenter
import com.wavesplatform.sdk.utils.WavesConstants
import com.wavesplatform.sdk.model.response.node.transaction.BurnTransactionResponse
import com.wavesplatform.sdk.utils.getScaledAmount
import com.wavesplatform.wallet.R
import com.wavesplatform.wallet.v2.data.Constants
import com.wavesplatform.wallet.v2.data.analytics.AnalyticEvents
import com.wavesplatform.wallet.v2.data.analytics.analytics
import com.wavesplatform.wallet.v2.ui.base.view.BaseActivity
import com.wavesplatform.wallet.v2.ui.home.wallet.assets.token_burn.TokenBurnActivity.Companion.KEY_INTENT_AMOUNT
import com.wavesplatform.wallet.v2.ui.home.wallet.assets.token_burn.TokenBurnActivity.Companion.KEY_INTENT_ASSET_BALANCE
import com.wavesplatform.wallet.v2.ui.home.wallet.assets.token_burn.TokenBurnActivity.Companion.KEY_INTENT_BLOCKCHAIN_FEE
import com.wavesplatform.wallet.v2.util.makeTextHalfBold
import com.wavesplatform.wallet.v2.util.showError
import kotlinx.android.synthetic.main.activity_token_burn_confirmation.*
import pers.victor.ext.*
import javax.inject.Inject

class TokenBurnConfirmationActivity : BaseActivity(), TokenBurnConfirmationView {

    @Inject
    @InjectPresenter
    lateinit var presenter: TokenBurnConfirmationPresenter

    @ProvidePresenter
    fun providePresenter(): TokenBurnConfirmationPresenter = presenter

    override fun configLayoutRes() = R.layout.activity_token_burn_confirmation

    override fun onCreate(savedInstanceState: Bundle?) {
        translucentStatusBar = true
        overridePendingTransition(R.anim.slide_in_right, R.anim.null_animation)
        super.onCreate(savedInstanceState)
    }

    override fun onViewReady(savedInstanceState: Bundle?) {
        setupToolbar(toolbar_view, true, getString(R.string.token_burn_confirmation_toolbar_title), R.drawable.ic_toolbar_back_white)

        presenter.assetBalance = intent.getParcelableExtra(KEY_INTENT_ASSET_BALANCE)
        presenter.fee = intent.getLongExtra(KEY_INTENT_BLOCKCHAIN_FEE, 0L)
        val stringAmount = intent.getStringExtra(KEY_INTENT_AMOUNT)
        presenter.amount = stringAmount.toDouble()

        text_id_value.text = presenter.assetBalance!!.assetId
        text_sum.text = "-$stringAmount ${presenter.assetBalance!!.getName()}"
        text_sum.makeTextHalfBold()
        text_type_value.text = if (presenter.assetBalance!!.reissuable == true) {
            getString(R.string.token_burn_confirmationt_reissuable)
        } else {
            getString(R.string.token_burn_confirmationt_not_reissuable)
        }

        image_line_5.visiableIf { presenter.assetBalance?.getDescription().isNullOrEmpty() }
        text_description.visiableIf { presenter.assetBalance?.getDescription().isNullOrEmpty().not() }
        text_description.text = presenter.assetBalance?.getDescription()

        text_fee_value.text = "${getScaledAmount(presenter.fee, 8)} " +
                "${WavesConstants.CUSTOM_FEE_ASSET_NAME}"

        button_confirm.click {
            analytics.trackEvent(AnalyticEvents.BurnTokenConfirmTapEvent)
            presenter.burn()
            toolbar_view.invisiable()
            card_content.gone()
            card_progress.visiable()
            val rotation = AnimationUtils.loadAnimation(this@TokenBurnConfirmationActivity, R.anim.rotate)
            rotation.fillAfter = true
            image_loader.startAnimation(rotation)
        }

        text_leasing_result_value.text = getString(
                R.string.token_burn_confirmation_you_have_burned,
                stringAmount, presenter.assetBalance!!.getName()
        )
    }

    override fun onShowBurnSuccess(tx: BurnTransactionResponse?, totalBurn: Boolean) {
        completeBurnProcessing()
        relative_success.visiable()

        button_okay.click {
            onBackPressed()
        }
    }

    override fun onBackPressed() {
        if (presenter.success) {
            val intent = Intent().apply {
                putExtra(BUNDLE_TOTAL_BURN, presenter.totalBurn)
            }
            setResult(Constants.RESULT_OK, intent)
            exitFromActivity()
        } else {
            exitFromActivity()
        }
    }

    private fun exitFromActivity() {
        finish()
        overridePendingTransition(R.anim.null_animation, R.anim.slide_out_right)
    }

    override fun onShowError(errorMessageRes: String) {
        completeBurnProcessing()
        toolbar_view.visiable()
        card_content.visiable()
        showError(errorMessageRes, R.id.root)
    }

    override fun failedTokenBurnCauseSmart() {
        setResult(Constants.RESULT_SMART_ERROR)
        onBackPressed()
    }

    private fun completeBurnProcessing() {
        image_loader.clearAnimation()
        card_progress.gone()
    }

    override fun needToShowNetworkMessage() = true

    override fun onNetworkConnectionChanged(networkConnected: Boolean) {
        super.onNetworkConnectionChanged(networkConnected)
        button_confirm.isEnabled = networkConnected
    }

    companion object {
        const val BUNDLE_TOTAL_BURN = "total_burn"
    }
}
