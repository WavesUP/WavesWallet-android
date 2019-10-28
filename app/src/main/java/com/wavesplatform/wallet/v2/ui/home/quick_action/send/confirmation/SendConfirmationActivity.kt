/*
 * Created by Eduard Zaydel on 1/4/2019
 * Copyright © 2019 Waves Platform. All rights reserved.
 */

package com.wavesplatform.wallet.v2.ui.home.quick_action.send.confirmation

import android.app.Activity
import android.os.Bundle
import android.text.TextUtils
import android.view.animation.AnimationUtils
import android.view.inputmethod.EditorInfo
import com.arellomobile.mvp.presenter.InjectPresenter
import com.arellomobile.mvp.presenter.ProvidePresenter
import com.jakewharton.rxbinding2.widget.RxTextView
import com.vicpin.krealmextensions.queryFirst
import com.wavesplatform.sdk.utils.WavesConstants
import com.wavesplatform.sdk.model.response.node.transaction.TransferTransactionResponse
import com.wavesplatform.sdk.utils.MoneyUtil
import com.wavesplatform.sdk.utils.getScaledAmount
import com.wavesplatform.sdk.utils.stripZeros
import com.wavesplatform.wallet.R
import com.wavesplatform.wallet.v2.data.model.db.AssetInfoDb
import com.wavesplatform.wallet.v2.data.model.db.userdb.AddressBookUserDb
import com.wavesplatform.wallet.v2.data.analytics.AnalyticEvents
import com.wavesplatform.wallet.v2.data.analytics.analytics
import com.wavesplatform.wallet.v2.ui.base.view.BaseActivity
import com.wavesplatform.wallet.v2.ui.home.profile.address_book.AddressBookActivity
import com.wavesplatform.wallet.v2.ui.home.profile.address_book.add.AddAddressActivity
import com.wavesplatform.wallet.v2.ui.home.quick_action.send.SendPresenter
import com.wavesplatform.wallet.v2.util.*
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.activity_send_confirmation.*
import pers.victor.ext.*
import pyxis.uzuki.live.richutilskt.utils.hideKeyboard
import java.math.BigDecimal
import javax.inject.Inject

class SendConfirmationActivity : BaseActivity(), SendConfirmationView {

    @Inject
    @InjectPresenter
    lateinit var presenter: SendConfirmationPresenter

    @ProvidePresenter
    fun providePresenter(): SendConfirmationPresenter = presenter

    override fun configLayoutRes() = R.layout.activity_send_confirmation

    override fun onCreate(savedInstanceState: Bundle?) {
        translucentStatusBar = true
        overridePendingTransition(R.anim.slide_in_right, R.anim.null_animation)
        super.onCreate(savedInstanceState)
    }

    override fun onViewReady(savedInstanceState: Bundle?) {
        setupToolbar(toolbar_view, true,
                getString(R.string.send_confirmation_toolbar_title),
                R.drawable.ic_toolbar_back_white)

        if (intent == null || intent.extras == null) {
            onBackPressed()
        }

        presenter.selectedAsset = intent!!.extras!!.getParcelable(KEY_INTENT_SELECTED_ASSET)
        presenter.recipient = intent!!.extras!!.getString(KEY_INTENT_SELECTED_RECIPIENT)
        presenter.amount = BigDecimal(intent!!.extras!!.getString(KEY_INTENT_SELECTED_AMOUNT))
        presenter.moneroPaymentId = intent!!.extras!!.getString(KEY_INTENT_MONERO_PAYMENT_ID)
        presenter.assetInfo = queryFirst<AssetInfoDb> {
            equalTo("id", presenter.selectedAsset!!.assetId)
        }?.convertFromDb()
        presenter.type = intent!!.extras!!.getSerializable(KEY_INTENT_TYPE) as SendPresenter.Type
        presenter.blockchainCommission = intent!!.extras!!.getLong(KEY_INTENT_BLOCKCHAIN_COMMISSION)
        presenter.feeAsset = intent?.extras?.getParcelable(KEY_INTENT_FEE_ASSET)
                ?: find(WavesConstants.WAVES_ASSET_ID_EMPTY)

        if (presenter.type == SendPresenter.Type.GATEWAY
                || presenter.type == SendPresenter.Type.WAVES_ENTERPRISE
                || presenter.type == SendPresenter.Type.ERGO) {
            presenter.gatewayCommission = BigDecimal(
                    intent!!.extras!!.getString(KEY_INTENT_GATEWAY_COMMISSION))
            text_sum.text = "-${(presenter.amount + presenter.gatewayCommission)
                    .toPlainString()
                    .stripZeros()}"
            text_gateway_fee_value.text = "${presenter.gatewayCommission.toPlainString().stripZeros()}" +
                    " ${presenter.selectedAsset!!.getName()}"
            gateway_commission_layout.visiable()
        } else {
            text_sum.text = "-${(presenter.amount)
                    .toPlainString()
                    .stripZeros()}"
        }

        if (isShowTicker(presenter.selectedAsset?.assetId)) {
            val ticker = presenter.assetInfo?.getTokenTicker()
            if (!ticker.isNullOrBlank()) {
                text_tag.text = ticker
                text_tag.visiable()
            }
        } else {
            text_sum.text = "${text_sum.text} ${presenter.selectedAsset?.getName()}"
        }

        text_sum.makeTextHalfBold()

        text_sent_to_address.text = presenter.recipient
        presenter.getAddressName(presenter.recipient!!)
        text_fee_value.text = "${getScaledAmount(presenter.blockchainCommission, presenter.feeAsset?.getDecimals()
                ?: 8)} " +
                presenter.feeAsset?.getName()

        if (presenter.type == SendPresenter.Type.GATEWAY
                || presenter.type == SendPresenter.Type.WAVES_ENTERPRISE
                || presenter.type == SendPresenter.Type.ERGO) {
            attachment_layout.gone()
        } else {
            attachment_layout.visiable()
            eventSubscriptions.add(RxTextView.textChanges(edit_optional_message)
                    .map { text -> text.toString() }
                    .map { text ->
                        val isValidDescription = validateDescription(text)

                        return@map isValidDescription to text
                    }
                    .filter { (isValid, _) -> isValid }
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { (_, description) ->
                        presenter.attachment = description
                    })
            if (intent.hasExtra(KEY_INTENT_ATTACHMENT)) {
                edit_optional_message.setText(intent!!.extras!!.getString(KEY_INTENT_ATTACHMENT))
            }
        }

        edit_optional_message.onAction(EditorInfo.IME_ACTION_DONE) {
            hideKeyboard()
            goNext()
        }

        button_confirm.click { goNext() }
    }

    private fun validateDescription(text: String): Boolean {
        val isValid = text.length <= 140
        button_confirm.isEnabled = isValid

        if (isValid) text_description_error.gone()
        else text_description_error.visiable()

        return isValid
    }

    override fun failedSendCauseSmart() {
        setResult(com.wavesplatform.wallet.v2.data.Constants.RESULT_SMART_ERROR)
        onBackPressed()
    }

    private fun goNext() {
        presenter.selectedAsset?.getName()?.let { name ->
            analytics.trackEvent(AnalyticEvents.WalletAssetsSendConfirmEvent(name))
        }
        showTransactionProcessing()
        presenter.confirmWithdrawTransaction()
    }

    override fun onShowTransactionSuccess(signed: TransferTransactionResponse) {
        completeTransactionProcessing()
        text_transaction_result.text = getString(
                R.string.send_success_you_have_sent_sum,
                MoneyUtil.getScaledText(signed.amount, presenter.selectedAsset),
                presenter.assetInfo?.name)
        button_okay.click {
            onBackPressed()
        }
        setSaveAddress(signed)
    }

    private fun completeTransactionProcessing() {
        image_loader.clearAnimation()
        card_progress.gone()
        relative_success.visiable()
    }

    private fun showTransactionProcessing() {
        toolbar_view.invisiable()
        card_content.gone()
        card_progress.visiable()
        val rotation = AnimationUtils
                .loadAnimation(this@SendConfirmationActivity, R.anim.rotate)
        rotation.fillAfter = true
        image_loader.startAnimation(rotation)
    }

    private fun cancelTransactionProcessing() {
        image_loader.clearAnimation()
        toolbar_view.visiable()
        card_content.visiable()
        card_progress.gone()
        relative_success.gone()
    }

    private fun setSaveAddress(signed: TransferTransactionResponse) {
        val addressBookUser = queryFirst<AddressBookUserDb> { equalTo("address", signed.recipient) }
        if (addressBookUser == null) {
            sent_to_address.text = signed.recipient
            add_address.visiable()
            add_address.click {
                launchActivity<AddAddressActivity>(AddressBookActivity.REQUEST_ADD_ADDRESS) {
                    putExtra(AddressBookActivity.BUNDLE_TYPE, AddressBookActivity.SCREEN_TYPE_NOT_EDITABLE)
                    putExtra(AddressBookActivity.BUNDLE_ADDRESS_ITEM, AddressBookUserDb(signed.recipient, ""))
                }
            }
        } else {
            add_address.gone()
        }
    }

    override fun showAddressBookUser(name: String) {
        if (!TextUtils.isEmpty(name)) {
            text_sent_to_name.text = name
            text_sent_to_name.visiable()
        } else {
            text_sent_to_address.textSize = 14f
            text_sent_to_address.setTextColor(findColor(R.color.disabled900))
            text_sent_to_name.gone()
        }
    }

    override fun hideAddressBookUser() {
        text_sent_to_address.textSize = 14f
        text_sent_to_address.setTextColor(findColor(R.color.disabled900))
        text_sent_to_name.gone()
    }

    override fun onShowError(res: Int) {
        cancelTransactionProcessing()
        showError(res, R.id.relative_root)
    }

    override fun onBackPressed() {
        if (presenter.success) {
            setResult(Activity.RESULT_OK)
            exitFromActivity()
        } else {
            exitFromActivity()
        }
    }

    private fun exitFromActivity() {
        finish()
        overridePendingTransition(R.anim.null_animation, R.anim.slide_out_right)
    }

    override fun needToShowNetworkMessage(): Boolean = true

    override fun onNetworkConnectionChanged(networkConnected: Boolean) {
        super.onNetworkConnectionChanged(networkConnected)
        button_confirm.isEnabled = networkConnected
    }

    companion object {
        const val KEY_INTENT_SELECTED_ASSET = "intent_selected_asset"
        const val KEY_INTENT_FEE_ASSET = "intent_fee_asset"
        const val KEY_INTENT_SELECTED_RECIPIENT = "intent_selected_recipient"
        const val KEY_INTENT_SELECTED_AMOUNT = "intent_selected_amount"
        const val KEY_INTENT_GATEWAY_COMMISSION = "intent_gateway_commission"
        const val KEY_INTENT_BLOCKCHAIN_COMMISSION = "intent_blockchain_commission"
        const val KEY_INTENT_ATTACHMENT = "intent_attachment"
        const val KEY_INTENT_MONERO_PAYMENT_ID = "intent_monero_payment_id"
        const val KEY_INTENT_TYPE = "intent_type"
    }
}
