/*
 * Created by Eduard Zaydel on 1/4/2019
 * Copyright © 2019 Waves Platform. All rights reserved.
 */

package com.wavesplatform.wallet.v2.ui.home.dex.trade.orderbook

import com.arellomobile.mvp.InjectViewState
import com.chad.library.adapter.base.entity.MultiItemEntity
import com.wavesplatform.wallet.v2.data.model.local.LastPriceItem
import com.wavesplatform.wallet.v2.data.model.remote.response.WatchMarketResponse
import com.wavesplatform.sdk.model.response.data.LastTradesResponse
import com.wavesplatform.sdk.model.response.matcher.OrderBookResponse
import com.wavesplatform.sdk.net.NetworkException
import com.wavesplatform.wallet.v2.data.model.local.OrderBookAskMultiItemEntity
import com.wavesplatform.wallet.v2.data.model.local.OrderBookBidMultiItemEntity
import com.wavesplatform.wallet.v2.ui.base.presenter.BasePresenter
import com.wavesplatform.sdk.utils.RxUtil
import com.wavesplatform.sdk.utils.notNull
import com.wavesplatform.wallet.v2.util.errorBody
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.functions.BiFunction
import pyxis.uzuki.live.richutilskt.utils.runOnUiThread
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@InjectViewState
class TradeOrderBookPresenter @Inject constructor() : BasePresenter<TradeOrderBookView>() {
    var watchMarket: WatchMarketResponse? = null
    var needFirstScrollToLastPrice = true
    private val subscriptions = CompositeDisposable()

    fun loadOrderBook() {
        addSubscription(
                Observable.interval(0, 10, TimeUnit.SECONDS)
                        .retry(3)
                        .flatMap {
                            val market = watchMarket?.market
                            return@flatMap Observable.zip(matcherServiceManager.loadOrderBook(
                                    market?.amountAsset!!,
                                    market.priceAsset),
                                    dataServiceManager.getLastExchangesByPair(watchMarket?.market?.amountAsset,
                                            watchMarket?.market?.priceAsset,
                                            DEFAULT_LIMIT),
                                    BiFunction { orderBook: OrderBookResponse,
                                                 lastPrice: ArrayList<LastTradesResponse.DataResponse.ExchangeTransactionResponse> ->
                                        return@BiFunction Pair(orderBook, lastPrice)
                                    })
                        }
                        .doOnError {
                            if (it is NetworkException) {
                                val errorMessage = it.errorBody()?.message ?: ""
                                if (errorMessage.isEmpty()) {
                                    runOnUiThread { viewState.afterFailedOrderbook() }
                                } else {
                                    runOnUiThread { viewState.afterFailedOrderbook(errorMessage) }
                                }
                            } else {
                                runOnUiThread { viewState.afterFailedOrderbook() }
                            }
                        }
                        .onErrorResumeNext(Observable.empty())
                        .compose(RxUtil.applyObservableDefaultSchedulers())
                        .subscribe({ pair ->
                            val result = mutableListOf<MultiItemEntity>()
                            result.addAll(getCalculatedAsks(pair.first.asks).asReversed())
                            pair.second.firstOrNull().notNull {
                                val firstAsk = pair.first.asks.firstOrNull()?.price?.toDouble()
                                val firstBid = pair.first.bids.firstOrNull()?.price?.toDouble()
                                if (firstAsk == null && firstBid == null) {
                                    // do nothing, will show empty view
                                } else if (firstAsk == null || firstBid == null) {
                                    result.add(LastPriceItem(0.0, it))
                                } else {
                                    var percent = ((firstAsk.minus(firstBid))
                                            .times(100).div(firstBid))
                                    if (percent >= 100) percent = 99.99
                                    result.add(LastPriceItem(percent, it))
                                }
                            }
                            val lastPricePosition = result.size - 1
                            result.addAll(getCalculatedBids(pair.first.bids))
                            viewState.afterSuccessOrderbook(result, lastPricePosition)
                        }, {
                            it.printStackTrace()
                            viewState.afterFailedOrderbook()
                        }))
    }

    private fun getCalculatedBids(list: List<OrderBookResponse.BidResponse>): Collection<MultiItemEntity> {
        var totalSum = 0L
        val orderBookBids = mutableListOf<OrderBookBidMultiItemEntity>()
        list.forEach { bid ->
            totalSum += bid.total
            bid.sum = totalSum
            orderBookBids.add(OrderBookBidMultiItemEntity(bid))
        }
        return orderBookBids
    }

    private fun getCalculatedAsks(list: List<OrderBookResponse.AskResponse>): List<OrderBookAskMultiItemEntity> {
        var totalSum = 0L
        val orderBookAsks = mutableListOf<OrderBookAskMultiItemEntity>()
        list.forEach { ask ->
            totalSum += ask.total
            ask.sum = totalSum
            orderBookAsks.add(OrderBookAskMultiItemEntity(ask))
        }
        return orderBookAsks
    }

    override fun onDestroy() {
        super.onDestroy()
        clearSubscriptions()
    }

    override fun addSubscription(subscription: Disposable) {
        subscriptions.add(subscription)
    }

    fun clearSubscriptions() {
        subscriptions.clear()
    }

    companion object {
        var DEFAULT_LIMIT = 1
    }
}
