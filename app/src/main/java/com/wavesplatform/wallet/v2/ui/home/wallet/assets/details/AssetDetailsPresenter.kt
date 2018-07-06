package com.wavesplatform.wallet.v2.ui.home.wallet.assets.details

import android.os.Bundle
import com.arellomobile.mvp.InjectViewState
import com.vicpin.krealmextensions.queryAllAsFlowable
import com.vicpin.krealmextensions.queryAllAsSingle
import com.wavesplatform.wallet.v2.data.model.remote.response.AssetBalance
import com.wavesplatform.wallet.v2.ui.base.presenter.BasePresenter
import com.wavesplatform.wallet.v2.ui.home.history.TestObject
import com.wavesplatform.wallet.v2.ui.home.history.adapter.HistoryItem
import com.wavesplatform.wallet.v2.util.RxUtil
import java.util.*
import javax.inject.Inject

@InjectViewState
class AssetDetailsPresenter @Inject constructor() : BasePresenter<AssetDetailsView>() {

    fun loadAssets() {
        addSubscription(queryAllAsSingle<AssetBalance>()
                .compose(RxUtil.applySingleDefaultSchedulers())
                .subscribe({
                    val hiddenList = it.filter({ it.isHidden }).toCollection(ArrayList())
                    val sortedToFirstFavoriteList = it.filter({ !it.isHidden }).sortedByDescending({ it.isFavorite }).toCollection(ArrayList())
                    sortedToFirstFavoriteList.addAll(hiddenList)

                    viewState.afterSuccessLoadAssets(sortedToFirstFavoriteList)
                }, {
                    it.printStackTrace()
                }))
    }

}