/*
 * Created by Eduard Zaydel on 1/4/2019
 * Copyright © 2019 Waves Platform. All rights reserved.
 */

package com.wavesplatform.wallet.v2.ui.home.quick_action.send.fee

import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import moxy.presenter.InjectPresenter
import moxy.presenter.ProvidePresenter
import com.chad.library.adapter.base.BaseQuickAdapter
import com.wavesplatform.sdk.utils.WavesConstants
import com.wavesplatform.sdk.model.response.node.AssetBalanceResponse
import com.wavesplatform.sdk.utils.MoneyUtil
import com.wavesplatform.wallet.R
import com.wavesplatform.wallet.v2.data.model.local.SponsoredAssetItem
import com.wavesplatform.wallet.v2.ui.base.view.BaseSuperBottomSheetDialogFragment
import kotlinx.android.synthetic.main.fragment_sponsored_fee_bottom_sheet_dialog.view.*
import pyxis.uzuki.live.richutilskt.utils.runDelayed
import timber.log.Timber
import javax.inject.Inject

class SponsoredFeeBottomSheetFragment : BaseSuperBottomSheetDialogFragment(), SponsoredFeeDetailsView {
    private var rootView: View? = null

    private var selectedAssetId: String? = null
    private var exchange: Boolean = false
    private var amountAssetId: String? = null
    private var priceAssetId: String? = null
    private var wavesFee: Long = WavesConstants.WAVES_MIN_FEE

    var onSelectedAssetListener: SponsoredAssetSelectedListener? = null

    @Inject
    @InjectPresenter
    lateinit var presenter: SponsoredFeeDetailsPresenter

    @Inject
    lateinit var adapter: SponsoredFeeAdapter

    @ProvidePresenter
    fun providePresenter(): SponsoredFeeDetailsPresenter = presenter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        rootView = inflater.inflate(R.layout.fragment_sponsored_fee_bottom_sheet_dialog, container, false)

        rootView?.recycle_sponsored_fee_assets?.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireActivity())
        rootView?.recycle_sponsored_fee_assets?.addOnScrollListener(object : androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: androidx.recyclerview.widget.RecyclerView, dx: Int, dy: Int) {
                rootView?.appbar_layout?.isSelected = rootView?.recycle_sponsored_fee_assets!!.canScrollVertically(-1)
            }
        })

        presenter.wavesFee = wavesFee
        adapter.currentAssetId = selectedAssetId

        adapter.bindToRecyclerView(rootView?.recycle_sponsored_fee_assets)
        adapter.onItemClickListener = BaseQuickAdapter.OnItemClickListener { adapter, view, position ->
            val item = this.adapter.getItem(position) as SponsoredAssetItem

            if (item.isActive) {
                val oldCheckedPosition = this.adapter.data.indexOfFirst { it.assetBalance.assetId == this.adapter.currentAssetId }

                this.adapter.currentAssetId = item.assetBalance.assetId
                onSelectedAssetListener?.onSelected(item.assetBalance, MoneyUtil.getUnscaledValue(item.fee, item.assetBalance))

                adapter.notifyItemChanged(oldCheckedPosition)
                adapter.notifyItemChanged(position)

                runDelayed(250) {
                    dialog?.dismiss()
                }
            }
        }

        if (exchange) {
            presenter.loadExchangeCommission(amountAssetId, priceAssetId) {
                rootView?.image_loader?.hide()
                adapter.setNewData(it)
            }
        } else {
            presenter.loadSponsoredAssets {
                rootView?.image_loader?.hide()
                adapter.setNewData(it)
            }
        }

        return rootView
    }

    fun configureData(selectedAssetId: String, wavesFee: Long,
                      exchange: Boolean = false,
                      amountAssetId: String? = null,
                      priceAssetId: String? = null) {
        this.wavesFee = wavesFee
        this.selectedAssetId = selectedAssetId
        this.exchange = exchange
        this.amountAssetId = amountAssetId
        this.priceAssetId = priceAssetId
    }

    override fun onDestroyView() {
        rootView?.image_loader?.hide()
        super.onDestroyView()
    }

    interface SponsoredAssetSelectedListener {
        fun onSelected(asset: AssetBalanceResponse, fee: Long)
    }
}