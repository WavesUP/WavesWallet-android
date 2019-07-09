/*
 * Created by Eduard Zaydel on 1/4/2019
 * Copyright © 2019 Waves Platform. All rights reserved.
 */

package com.wavesplatform.wallet.v2.ui.home.quick_action.receive.cryptocurrency

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v4.view.ViewCompat
import android.view.View
import com.arellomobile.mvp.presenter.InjectPresenter
import com.arellomobile.mvp.presenter.ProvidePresenter
import com.ethanhua.skeleton.Skeleton
import com.ethanhua.skeleton.SkeletonScreen
import com.wavesplatform.sdk.model.response.node.AssetBalanceResponse
import com.wavesplatform.sdk.utils.notNull
import com.wavesplatform.wallet.R
import com.wavesplatform.wallet.v2.data.analytics.AnalyticEvents
import com.wavesplatform.wallet.v2.data.analytics.analytics
import com.wavesplatform.wallet.v2.data.model.remote.response.gateway.GatewayDeposit
import com.wavesplatform.wallet.v2.ui.base.view.BaseFragment
import com.wavesplatform.wallet.v2.ui.home.quick_action.receive.address_view.ReceiveAddressViewActivity
import com.wavesplatform.wallet.v2.ui.home.quick_action.receive.invoice.InvoiceFragment
import com.wavesplatform.wallet.v2.ui.home.wallet.your_assets.YourAssetsActivity
import com.wavesplatform.wallet.v2.util.findByGatewayId
import com.wavesplatform.wallet.v2.util.launchActivity
import com.wavesplatform.wallet.v2.util.showError
import kotlinx.android.synthetic.main.fragment_cryptocurrency.*
import kotlinx.android.synthetic.main.content_asset_card.*
import pers.victor.ext.*
import java.math.BigDecimal
import javax.inject.Inject

class CryptoCurrencyFragment : BaseFragment(), CryptoCurrencyView {

    @Inject
    @InjectPresenter
    lateinit var presenter: CryptoCurrencyPresenter
    private var skeletonView: SkeletonScreen? = null

    @ProvidePresenter
    fun providePresenter(): CryptoCurrencyPresenter = presenter

    override fun configLayoutRes(): Int = R.layout.fragment_cryptocurrency

    override fun onViewReady(savedInstanceState: Bundle?) {
        if (arguments == null) {
            assetChangeEnable(true)
        } else {
            val assetBalance = arguments!!.getParcelable<AssetBalanceResponse>(
                    YourAssetsActivity.BUNDLE_ASSET_ITEM)
            setAssetBalance(assetBalance)
            assetChangeEnable(false)
        }

        button_continue.click {
            if (presenter.depositAddress != null) {
                presenter.assetBalance?.getName()?.let { name ->
                    analytics.trackEvent(AnalyticEvents.WalletAssetsReceiveTapEvent(name))
                }
                launchActivity<ReceiveAddressViewActivity>(REQUEST_CODE_ADDRESS_SCREEN) {
                    putExtra(YourAssetsActivity.BUNDLE_ASSET_ITEM, presenter.assetBalance)
                    putExtra(YourAssetsActivity.BUNDLE_ADDRESS, presenter.depositAddress ?: "")
                }
            }
        }
        presenter.nextStepValidation = false
        needMakeButtonEnable()
    }

    private fun needMakeButtonEnable() {
        button_continue.isEnabled = presenter.nextStepValidation && isNetworkConnected()
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        if (savedInstanceState != null) {
            presenter.assetBalance.notNull {
                skeletonView?.show()
                setAssetBalance(it)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_SELECT_ASSET && resultCode == Activity.RESULT_OK) {
            val assetBalance = data?.getParcelableExtra<AssetBalanceResponse>(YourAssetsActivity.BUNDLE_ASSET_ITEM)
            setAssetBalance(assetBalance)
        } else if (requestCode == InvoiceFragment.REQUEST_CODE_ADDRESS_SCREEN && resultCode == Activity.RESULT_OK) {
            onBackPressed()
        }
    }

    override fun onSuccessInitDeposit(response: GatewayDeposit) {
        skeletonView?.hide()
        if (response.currencyFrom.isNullOrEmpty()) {
            presenter.nextStepValidation = false
            needMakeButtonEnable()
            onGatewayError()
            return
        }

        attention_title?.text = getString(R.string.receive_minimum_amount,
                response.minLimit, response.currencyFrom)
        attention_subtitle?.text = getString(R.string.receive_warning_will_send,
                response.minLimit,
                response.currencyFrom)
        if (findByGatewayId("ETH")!!.assetId == presenter.assetBalance!!.assetId) {
            warning_crypto_title?.text = getString(R.string.receive_gateway_info_gateway_warning_eth_title)
            warning_crypto_subtitle?.text = getString(R.string.receive_gateway_info_gateway_warning_eth_subtitle)
        } else {
            warning_crypto_title?.text = getString(R.string.receive_warning_crypto, response.currencyFrom)
            warning_crypto_subtitle?.text = getString(R.string.receive_will_send_other_currency)
        }

        presenter.nextStepValidation = true
        needMakeButtonEnable()

        warning_layout?.visiable()
        container_info?.visiable()
        button_continue.isEnabled = true
    }

    override fun onShowError(message: String) {
        skeletonView?.hide()
        container_info?.gone()
        showError(message, R.id.root)
    }

    override fun onGatewayError() {
        skeletonView!!.hide()
        attention_title?.text = getString(R.string.send_gateway_error_title)
        attention_subtitle?.text = getString(R.string.send_gateway_error_subtitle)
        warning_layout?.gone()
        button_continue?.isEnabled = false
    }

    private fun setAssetBalance(assetBalance: AssetBalanceResponse?) {
        presenter.assetBalance = assetBalance

        image_asset_icon.setAsset(assetBalance)
        text_asset_name.text = assetBalance?.getName()
        text_asset_value.text = assetBalance?.getDisplayAvailableBalance()

        if (assetBalance?.isFavorite == true){
            image_is_favorite.visiable()
        }else{
            image_is_favorite.gone()
        }

        text_asset.gone()
        container_asset.visiable()

        presenter.nextStepValidation = true
        needMakeButtonEnable()

        if (assetBalance != null) {
            presenter.initDeposit(assetBalance.assetId)
            skeletonView = Skeleton.bind(container_info)
                    .color(R.color.basic50)
                    .load(R.layout.item_skeleton_crypto_warning)
                    .show()
            container_info.visiable()
        }
    }

    private fun assetChangeEnable(enable: Boolean) {
        if (enable) {
            text_asset.click { launchAssets() }
            container_asset.click { launchAssets() }
            image_change.visibility = View.VISIBLE
            ViewCompat.setElevation(edit_asset_card, dp2px(2).toFloat())
            edit_asset_layout.background = null
            edit_asset_card.setCardBackgroundColor(ContextCompat.getColor(
                    activity!!, R.color.white))
        } else {
            text_asset.click {
                // disable clicks
            }
            container_asset.click {
                // disable clicks
            }
            image_change.visibility = View.GONE
            ViewCompat.setElevation(edit_asset_card, 0F)
            edit_asset_layout.background = ContextCompat.getDrawable(
                    activity!!, R.drawable.shape_rect_bordered_accent50)
            edit_asset_card.setCardBackgroundColor(ContextCompat.getColor(
                    activity!!, R.color.basic50))
        }
    }

    private fun launchAssets() {
        launchActivity<YourAssetsActivity>(requestCode = REQUEST_SELECT_ASSET) {
            putExtra(YourAssetsActivity.CRYPTO_CURRENCY, true)
            presenter.assetBalance.notNull {
                putExtra(YourAssetsActivity.BUNDLE_ASSET_ID, it.assetId)
            }
        }
    }

    override fun onNetworkConnectionChanged(networkConnected: Boolean) {
        super.onNetworkConnectionChanged(networkConnected)
        button_continue.isEnabled = presenter.nextStepValidation && networkConnected
    }

    override fun onDestroyView() {
        skeletonView?.hide()
        super.onDestroyView()
    }

    companion object {
        const val REQUEST_CODE_ADDRESS_SCREEN = 101
        const val REQUEST_SELECT_ASSET = 10001

        fun newInstance(assetBalance: AssetBalanceResponse?): CryptoCurrencyFragment {
            val fragment = CryptoCurrencyFragment()
            if (assetBalance == null) {
                return fragment
            }
            val args = Bundle()
            args.putParcelable(YourAssetsActivity.BUNDLE_ASSET_ITEM, assetBalance)
            fragment.arguments = args
            return fragment
        }
    }
}
