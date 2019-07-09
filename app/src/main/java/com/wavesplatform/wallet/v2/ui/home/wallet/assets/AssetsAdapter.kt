/*
 * Created by Eduard Zaydel on 1/4/2019
 * Copyright © 2019 Waves Platform. All rights reserved.
 */

package com.wavesplatform.wallet.v2.ui.home.wallet.assets

import android.support.v7.widget.RecyclerView
import android.view.View
import com.chad.library.adapter.base.BaseMultiItemQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.chad.library.adapter.base.entity.MultiItemEntity
import com.oushangfeng.pinnedsectionitemdecoration.utils.FullSpanUtil
import com.wavesplatform.sdk.utils.getScaledAmount
import com.wavesplatform.wallet.App
import com.wavesplatform.wallet.R
import com.wavesplatform.wallet.v2.data.model.local.AssetBalanceMultiItemEntity
import com.wavesplatform.wallet.v2.data.model.local.WalletSectionItem
import com.wavesplatform.wallet.v2.util.makeTextHalfBold
import com.wavesplatform.wallet.v2.util.setMargins
import kotlinx.android.synthetic.main.item_wallet_asset.view.*
import kotlinx.android.synthetic.main.item_wallet_header.view.*
import pers.victor.ext.click
import pers.victor.ext.dp2px
import javax.inject.Inject

class AssetsAdapter @Inject constructor() :
        BaseMultiItemQuickAdapter<MultiItemEntity, BaseViewHolder>(null) {

    var scrollToHeaderListener: ScrollToHeaderListener? = null

    companion object {
        const val TYPE_HEADER = 0
        const val TYPE_ASSET = 1
        const val TYPE_HIDDEN_ASSET = 2
        const val TYPE_SPAM_ASSET = 3
        const val TYPE_SEARCH = 4
    }

    init {
        addItemType(TYPE_HEADER, R.layout.item_wallet_header)
        addItemType(TYPE_ASSET, R.layout.item_wallet_asset)
        addItemType(TYPE_HIDDEN_ASSET, R.layout.item_wallet_asset)
        addItemType(TYPE_SPAM_ASSET, R.layout.item_wallet_asset)
        addItemType(TYPE_SEARCH, R.layout.item_wallet_asset_search)
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        FullSpanUtil.onAttachedToRecyclerView(recyclerView, this, TYPE_HEADER)
    }

    override fun onViewAttachedToWindow(holder: BaseViewHolder) {
        super.onViewAttachedToWindow(holder)
        FullSpanUtil.onViewAttachedToWindow(holder, this, TYPE_HEADER)
    }

    override fun convert(helper: BaseViewHolder, item: MultiItemEntity) {
        when (helper.itemViewType) {
            TYPE_HEADER -> {
                val item = item as WalletSectionItem
                helper.setText(R.id.text_header, item.header)
                if (item.isExpanded) {
                    helper.itemView.image_arrow.rotation = 0f
                } else {
                    helper.itemView.image_arrow.rotation = 180f
                }

                helper.itemView.click {
                    val pos = helper.adapterPosition
                    if (item.isExpanded) {
                        collapse(pos, true)
                    } else {
                        expand(pos, true)
                        scrollToHeaderListener?.scrollToHeader(helper.adapterPosition, helper.itemView)
                    }
                }
            }
            TYPE_ASSET, TYPE_HIDDEN_ASSET, TYPE_SPAM_ASSET -> {
                try {
                    if (data[helper.adapterPosition + 1].itemType == TYPE_HEADER) {
                        helper.itemView.card_asset.setMargins(bottom = dp2px(18))
                    } else {
                        helper.itemView.card_asset.setMargins(bottom = dp2px(6))
                    }
                } catch (e: Throwable) {
                    helper.itemView.card_asset.setMargins(bottom = dp2px(6))
                    e.printStackTrace()
                }

                val item = item as AssetBalanceMultiItemEntity
                helper.setText(R.id.text_asset_name, item.getName())
                        .setText(R.id.text_asset_value, getScaledAmount(
                                item.getAvailableBalance(), item.getDecimals()))
                        .setGone(R.id.image_favourite, item.isFavorite)
                        .setGone(R.id.text_my_asset, item.issueTransaction?.sender
                                == App.getAccessManager().getWallet()?.address)
                        .setGone(R.id.text_tag_spam, item.isSpam)

                helper.itemView.image_asset_icon.setAsset(item)
                helper.itemView.text_asset_value.makeTextHalfBold()
            }
        }
    }


    interface ScrollToHeaderListener {
        fun scrollToHeader(position: Int, itemView: View)
    }
}
