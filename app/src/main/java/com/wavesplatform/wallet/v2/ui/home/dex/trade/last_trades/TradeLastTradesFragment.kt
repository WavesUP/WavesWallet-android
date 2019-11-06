/*
 * Created by Eduard Zaydel on 1/4/2019
 * Copyright © 2019 Waves Platform. All rights reserved.
 */

package com.wavesplatform.wallet.v2.ui.home.dex.trade.last_trades

import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import com.arellomobile.mvp.presenter.InjectPresenter
import com.arellomobile.mvp.presenter.ProvidePresenter
import com.wavesplatform.wallet.R
import com.wavesplatform.sdk.utils.MoneyUtil
import com.wavesplatform.wallet.v2.data.Events
import com.wavesplatform.wallet.v2.data.model.remote.response.WatchMarketResponse
import com.wavesplatform.sdk.model.response.data.LastTradesResponse
import com.wavesplatform.sdk.utils.notNull
import com.wavesplatform.sdk.utils.stripZeros
import com.wavesplatform.wallet.v2.ui.base.view.BaseFragment
import com.wavesplatform.wallet.v2.ui.home.dex.trade.TradeActivity
import kotlinx.android.synthetic.main.fragment_trade_last_trades.*
import kotlinx.android.synthetic.main.content_global_server_error_layout.*
import kotlinx.android.synthetic.main.content_empty_data.view.*
import pers.victor.ext.click
import pers.victor.ext.gone
import pers.victor.ext.inflate
import pers.victor.ext.visiable
import javax.inject.Inject

class TradeLastTradesFragment : BaseFragment(), TradeLastTradesView {

    @Inject
    @InjectPresenter
    lateinit var presenter: TradeLastTradesPresenter

    @Inject
    lateinit var adapter: TradeLastTradesAdapter

    companion object {
        fun newInstance(watchMarket: WatchMarketResponse?): TradeLastTradesFragment {
            val args = Bundle()
            args.classLoader = WatchMarketResponse::class.java.classLoader
            args.putParcelable(TradeActivity.BUNDLE_MARKET, watchMarket)
            val fragment = TradeLastTradesFragment()
            fragment.arguments = args
            return fragment
        }
    }

    @ProvidePresenter
    fun providePresenter(): TradeLastTradesPresenter = presenter

    override fun configLayoutRes() = R.layout.fragment_trade_last_trades

    override fun onViewReady(savedInstanceState: Bundle?) {
        presenter.watchMarket = arguments?.getParcelable<WatchMarketResponse>(TradeActivity.BUNDLE_MARKET)

        eventSubscriptions.add(rxEventBus.filteredObservable(Events.UpdateButtonsPrice::class.java)
                .subscribe {
                    it.bidPrice.notNull {
                        text_sell_value?.text = MoneyUtil.getScaledPrice(it, presenter.watchMarket?.market?.amountAssetDecimals
                                ?: 0, presenter.watchMarket?.market?.priceAssetDecimals
                                ?: 0).stripZeros()
                    }

                    it.askPrice.notNull {
                        text_buy_value?.text = MoneyUtil.getScaledPrice(it, presenter.watchMarket?.market?.amountAssetDecimals
                                ?: 0, presenter.watchMarket?.market?.priceAssetDecimals
                                ?: 0).stripZeros()
                    }
                })

        swipe_container.setColorSchemeResources(R.color.submit400)

        presenter.watchMarket?.market.notNull {
            adapter.market = it
        }

        swipe_container.setOnRefreshListener {
            loadLastTrades()
        }

        recycle_last_trades.layoutManager = LinearLayoutManager(baseActivity)
        adapter.bindToRecyclerView(recycle_last_trades)

        linear_buy.click {
            rxEventBus.post(Events.DexOrderButtonClickEvent(true))
        }

        linear_sell.click {
            rxEventBus.post(Events.DexOrderButtonClickEvent(false))
        }

        button_retry.click {
            error_layout.gone()

            progress_bar.show()
            loadLastTrades()
        }

        loadLastTrades()
    }

    private fun loadLastTrades() {
        presenter.loadLastTrades()
    }

    private fun getEmptyView(): View {
        val view = inflate(R.layout.content_empty_data)
        view.text_empty.text = getString(R.string.last_trades_empty)
        return view
    }

    override fun afterSuccessLoadLastTrades(data: List<LastTradesResponse.DataResponse.ExchangeTransactionResponse>) {
        progress_bar.hide()
        swipe_container.isRefreshing = false
        error_layout.gone()
        adapter.setNewData(data)
        adapter.emptyView = getEmptyView()
        if (data.isNotEmpty()) {
            linear_fields_name.visiable()
        } else {
            linear_fields_name.gone()
        }
    }

    override fun afterFailedLoadLastTrades() {
        progress_bar.hide()
        swipe_container.isRefreshing = false
        if (adapter.data.isEmpty()) {
            linear_buttons.gone()
            linear_fields_name.gone()

            error_layout.visiable()
        }
    }

    override fun onNetworkConnectionChanged(networkConnected: Boolean) {
        super.onNetworkConnectionChanged(networkConnected)
        linear_buy.isClickable = networkConnected
        linear_sell.isClickable = networkConnected
        if (networkConnected) {
            linear_buy.setBackgroundResource(R.drawable.shape_btn_waves_blue_default)
            linear_sell.setBackgroundResource(R.drawable.shape_btn_red_default)
        } else {
            linear_buy.setBackgroundResource(R.drawable.shape_btn_waves_blue_disabled)
            linear_sell.setBackgroundResource(R.drawable.shape_btn_red_disabled)
        }
    }
}
