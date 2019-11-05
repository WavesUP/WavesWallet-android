/*
 * Created by Eduard Zaydel on 1/4/2019
 * Copyright © 2019 Waves Platform. All rights reserved.
 */

package com.wavesplatform.wallet.v2.ui.home.dex.trade.my_orders.details

import android.view.View
import androidx.appcompat.widget.AppCompatTextView
import com.jakewharton.rxbinding3.view.clicks
import com.wavesplatform.sdk.model.response.data.AssetInfoResponse
import com.wavesplatform.sdk.model.response.matcher.AssetPairOrderResponse
import com.wavesplatform.sdk.utils.MoneyUtil
import com.wavesplatform.sdk.utils.RxUtil
import com.wavesplatform.sdk.utils.WavesConstants
import com.wavesplatform.sdk.utils.stripZeros
import com.wavesplatform.wallet.R
import com.wavesplatform.wallet.v2.data.Constants
import com.wavesplatform.wallet.v2.data.manager.MatcherServiceManager
import com.wavesplatform.wallet.v2.data.manager.NodeServiceManager
import com.wavesplatform.wallet.v2.data.model.local.MyOrderTransaction
import com.wavesplatform.wallet.v2.data.model.local.OrderStatus
import com.wavesplatform.wallet.v2.data.model.local.TransactionType
import com.wavesplatform.wallet.v2.ui.base.view.BaseTransactionBottomSheetFragment
import com.wavesplatform.wallet.v2.util.*
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.content_bottom_sheet_my_orders_body.view.*
import kotlinx.android.synthetic.main.content_history_details_layout.view.*
import kotlinx.android.synthetic.main.content_my_orders_bottom_sheet_bottom_btns.view.*
import kotlinx.android.synthetic.main.fragment_history_bottom_sheet_base_info_layout.view.*
import pers.victor.ext.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.math.roundToInt

class MyOrderDetailsBottomSheetFragment : BaseTransactionBottomSheetFragment<MyOrderTransaction>() {

    @Inject
    lateinit var matcherServiceManager: MatcherServiceManager
    @Inject
    lateinit var nodeServiceManager: NodeServiceManager

    var cancelOrderListener: CancelOrderListener? = null

    override fun configLayoutRes(): Int {
        return R.layout.bottom_sheet_dialog_history_details
    }

    override fun setupHeader(data: MyOrderTransaction): View? {
        val view = inflater?.inflate(R.layout.content_history_details_layout, null, false)

        view?.let {
            view.text_transaction_name?.text = getString(R.string.history_details_status)

            // fill status field
            val percent = (data.orderResponse.filled.toFloat() / data.orderResponse.amount.toFloat()) * 100
            view.text_transaction_value?.setBackgroundResource(0)
            when (data.orderResponse.getStatus()) {
                OrderStatus.Filled -> {
                    // force string, bcz percent with commission
                    view.text_transaction_value?.text = FILLED_ORDER_PERCENT
                }
                OrderStatus.Cancelled -> {
                    // with template "{percent}% (Cancelled)"
                    view.text_transaction_value?.text = getString(R.string.my_orders_details_canceled_status, percent
                            .roundToInt()
                            .toString())
                }
                else -> {
                    // with template "{percent}%"
                    view.text_transaction_value?.text = percent
                            .roundToInt()
                            .toString()
                            .plus("%")
                }
            }

            view.image_transaction_type.setImageDrawable(TransactionType.EXCHANGE_TYPE.icon())

            view.text_transaction_value.makeTextHalfBold()
        }

        return view
    }

    override fun setupBody(data: MyOrderTransaction): View? {
        val view = inflater?.inflate(R.layout.content_bottom_sheet_my_orders_body, null, false)

        view?.let {
            view.text_amount_value.text = data.orderResponse.getScaledAmount(data.amountAssetInfo?.precision)
            view.text_filled_value.text = data.orderResponse.getType().directionSign
                    .plus(data.orderResponse.getScaledFilled(data.amountAssetInfo?.precision))
            view.text_price_value.text = data.orderResponse.getScaledPrice(data.amountAssetInfo?.precision, data.priceAssetInfo?.precision)
            view.text_total_value.text = data.orderResponse.getScaledTotal(data.priceAssetInfo?.precision)

            showTickerOrSimple(view.text_amount_value, view.text_amount_tag, data.amountAssetInfo)
            showTickerOrSimple(view.text_filled_value, view.text_filled_tag, data.amountAssetInfo)
            showTickerOrSimple(view.text_price_value, view.text_price_tag, data.priceAssetInfo)
            showTickerOrSimple(view.text_total_value, view.text_total_tag, data.priceAssetInfo)

            view.text_filled_value.makeTextHalfBold(true)
            view.text_total_value.makeTextHalfBold(true)
        }

        return view
    }

    private fun showTickerOrSimple(valueView: AppCompatTextView, tickerView: AppCompatTextView, assetInfo: AssetInfoResponse?) {
        if (isShowTicker(assetInfo?.id)) {
            val ticker = assetInfo?.getTokenTicker()
            if (!ticker.isNullOrBlank()) {
                tickerView.text = ticker
                tickerView.visiable()
            }
        } else {
            valueView.text = valueView.text.toString().plus(" ${assetInfo?.name}")
        }
    }

    override fun setupInfo(data: MyOrderTransaction): View? {
        val view = inflater?.inflate(R.layout.fragment_history_bottom_sheet_base_info_layout, null, false)

        view?.let {
            view.setPaddingTop(15.dp)
            // hide unused fields
            view.relative_block.gone()
            view.relative_status.gone()
            view.relative_confirmations.gone()

            setFee(data, view)

            // fill time field
            view.text_timestamp?.text = data.orderResponse.timestamp.date(Constants.DATE_TIME_PATTERN)
        }

        return view
    }

    private fun setFee(data: MyOrderTransaction, view: View) {
        if (data.orderResponse.feeAsset != null && data.orderResponse.fee != null) {

            val feeAssetBalance = find(data.orderResponse.feeAsset!!)

            if (feeAssetBalance == null) {
                showProgressBar(true)
                eventSubscriptions.add(nodeServiceManager.assetDetails(data.orderResponse.feeAsset!!)
                        .compose(RxUtil.applyObservableDefaultSchedulers())
                        .subscribe({
                            showProgressBar(false)
                            view.text_fee?.text =
                                    "${MoneyUtil.getScaledText(
                                            data.orderResponse.fee ?: 0, 
                                            it.decimals).stripZeros()} " +
                                    it.name
                        }, {
                            showProgressBar(false)
                            it.printStackTrace()
                        }))
            } else {
                view.text_fee?.text =
                        "${MoneyUtil.getScaledText(
                                data.orderResponse.fee ?: 0,
                                feeAssetBalance.getDecimals()).stripZeros()} " +
                                feeAssetBalance.getName()
            }
        } else {
            view.text_fee?.text = MoneyUtil.getScaledText(data.fee, WavesConstants.WAVES_ASSET_INFO.precision).stripZeros()
            view.text_base_info_tag.visiable()
        }
    }

    override fun setupFooter(data: MyOrderTransaction): View? {
        val view = inflater?.inflate(R.layout.content_my_orders_bottom_sheet_bottom_btns, null, false)

        view?.let {
            eventSubscriptions.add(view.image_close.clicks()
                    .throttleFirst(1500, TimeUnit.MILLISECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe {
                        dismiss()
                    })

            if (!arrayOf(OrderStatus.Cancelled, OrderStatus.Filled).contains(data.orderResponse.getStatus())) {
                view.text_cancel_order.visiable()

                eventSubscriptions.add(view.text_cancel_order.clicks()
                        .throttleFirst(1500, TimeUnit.MILLISECONDS)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe {
                            cancelOrder(data)
                        })
            }
        }

        return view
    }

    private fun cancelOrder(data: MyOrderTransaction) {
        showProgressBar(true)
        eventSubscriptions.add(matcherServiceManager.cancelOrder(data.orderResponse.id, data.amountAssetInfo?.id, data.priceAssetInfo?.id)
                .compose(RxUtil.applyObservableDefaultSchedulers())
                .subscribe({
                    showProgressBar(false)
                    cancelOrderListener?.successCancelOrder()

                    selectedItem?.orderResponse?.status = AssetPairOrderResponse.API_STATUS_CANCELLED
                    configureView()
                }, {
                    showProgressBar(false)
                    it.printStackTrace()
                }))
    }

    interface CancelOrderListener {
        fun successCancelOrder()
    }

    companion object {
        const val FILLED_ORDER_PERCENT = "100%"
    }
}