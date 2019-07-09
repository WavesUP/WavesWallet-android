/*
 * Created by Eduard Zaydel on 1/4/2019
 * Copyright © 2019 Waves Platform. All rights reserved.
 */

package com.wavesplatform.wallet.v2.ui.home.wallet.leasing

import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.wavesplatform.sdk.utils.WavesConstants
import com.wavesplatform.sdk.model.response.node.HistoryTransactionResponse
import com.wavesplatform.sdk.utils.MoneyUtil
import com.wavesplatform.sdk.utils.stripZeros
import com.wavesplatform.wallet.R
import com.wavesplatform.wallet.v2.util.icon
import com.wavesplatform.wallet.v2.util.makeTextHalfBold
import com.wavesplatform.wallet.v2.util.transactionType
import kotlinx.android.synthetic.main.item_history.view.*
import javax.inject.Inject

class LeasingActiveAdapter @Inject constructor() : BaseQuickAdapter<HistoryTransactionResponse, BaseViewHolder>(R.layout.item_history, null) {

    override fun convert(helper: BaseViewHolder, item: HistoryTransactionResponse) {
        helper.setGone(R.id.text_tag, true)
                .setText(R.id.text_tag, WavesConstants.WAVES_ASSET_INFO.name)

        helper.itemView.image_transaction.setImageDrawable(item.transactionType().icon())
        helper.itemView.text_transaction_name.text = mContext.getString(item.transactionType().title)
        helper.itemView.text_transaction_value.text = MoneyUtil.getScaledText(item.amount, item.asset).stripZeros()
        helper.itemView.text_transaction_value.makeTextHalfBold()
    }
}
