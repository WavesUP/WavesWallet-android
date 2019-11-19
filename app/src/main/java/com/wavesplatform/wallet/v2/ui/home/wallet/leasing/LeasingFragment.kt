/*
 * Created by Eduard Zaydel on 1/4/2019
 * Copyright © 2019 Waves Platform. All rights reserved.
 */

package com.wavesplatform.wallet.v2.ui.home.wallet.leasing

import android.os.Bundle
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.LinearLayoutManager
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import moxy.presenter.InjectPresenter
import moxy.presenter.ProvidePresenter
import com.chad.library.adapter.base.BaseQuickAdapter
import com.ethanhua.skeleton.Skeleton
import com.ethanhua.skeleton.ViewSkeletonScreen
import com.wavesplatform.wallet.R
import com.wavesplatform.wallet.v2.data.Events
import com.wavesplatform.wallet.v2.data.analytics.AnalyticEvents
import com.wavesplatform.wallet.v2.data.analytics.analytics
import com.wavesplatform.wallet.v2.data.model.local.HistoryTab
import com.wavesplatform.sdk.model.response.node.AssetBalanceResponse
import com.wavesplatform.sdk.model.response.node.HistoryTransactionResponse
import com.wavesplatform.sdk.utils.notNull
import com.wavesplatform.wallet.v2.ui.base.view.BaseFragment
import com.wavesplatform.wallet.v2.ui.home.MainActivity
import com.wavesplatform.wallet.v2.ui.home.history.HistoryActivity
import com.wavesplatform.wallet.v2.ui.home.history.HistoryFragment
import com.wavesplatform.wallet.v2.ui.home.history.details.HistoryDetailsBottomSheetFragment
import com.wavesplatform.wallet.v2.ui.home.history.tab.HistoryTabFragment
import com.wavesplatform.wallet.v2.ui.home.wallet.WalletTabShadowListener
import com.wavesplatform.wallet.v2.ui.home.wallet.address.MyAddressQRActivity
import com.wavesplatform.wallet.v2.ui.home.wallet.leasing.start.StartLeasingActivity
import com.wavesplatform.wallet.v2.util.launchActivity
import com.wavesplatform.wallet.v2.util.makeTextHalfBold
import kotlinx.android.synthetic.main.fragment_leasing.*
import pers.victor.ext.click
import pers.victor.ext.gone
import pers.victor.ext.visiable
import javax.inject.Inject

class LeasingFragment : BaseFragment(), LeasingView, WalletTabShadowListener {

    @Inject
    @InjectPresenter
    lateinit var presenter: LeasingPresenter

    @ProvidePresenter
    fun providePresenter(): LeasingPresenter = presenter

    @Inject
    lateinit var adapterActiveAdapter: LeasingActiveAdapter
    private var skeletonScreen: ViewSkeletonScreen? = null
    var elevationAppBarChangeListener: MainActivity.OnElevationAppBarChangeListener? = null

    override fun configLayoutRes(): Int = R.layout.fragment_leasing

    override fun onViewReady(savedInstanceState: Bundle?) {
        eventSubscriptions.add(rxEventBus.filteredObservable(Events.ScrollToTopEvent::class.java)
                .subscribe {
                    if (it.position == MainActivity.WALLET_SCREEN) {
                        nested_scroll_view.smoothScrollTo(0, 0)
                    }
                })

        eventSubscriptions.add(rxEventBus.filteredObservable(Events.NeedUpdateHistoryScreen::class.java)
                .subscribe {
                    presenter.getActiveLeasing()
                })

        eventSubscriptions.add(rxEventBus.filteredObservable(Events.UpdateListOfActiveTransaction::class.java)
                .subscribe {
                    adapterActiveAdapter.remove(it.position)

                    if (adapterActiveAdapter.data.isEmpty()) {
                        linear_active_leasing.gone()
                    } else {
                        linear_active_leasing.visiable()
                    }

                    text_active_leasing.text = getString(R.string.wallet_leasing_active_now, adapterActiveAdapter.data.size.toString())
                })

        swipe_container.setColorSchemeResources(R.color.submit400)
        swipe_container.setOnRefreshListener {
            presenter.getActiveLeasing()
        }

        card_view_history.click {
            launchActivity<HistoryActivity> {
                val bundle = Bundle().apply {
                    val tabs = arrayListOf(
                            HistoryTab(HistoryTabFragment.leasing_all, getString(R.string.history_all)),
                            HistoryTab(HistoryTabFragment.leasing_active_now, getString(R.string.history_active_now)),
                            HistoryTab(HistoryTabFragment.leasing_canceled, getString(R.string.history_canceled)))
                    putParcelableArrayList(HistoryFragment.BUNDLE_TABS, tabs)
                }
                putExtras(bundle)
            }
        }

        container_quick_note.click {
            if (expandable_layout_hidden.isExpanded) {
                expandable_layout_hidden.collapse()
                image_arrowup.animate()
                        .rotation(180f)
                        .setDuration(500)
                        .start()
            } else {
                expandable_layout_hidden.expand()
                image_arrowup.animate()
                        .rotation(0f)
                        .setDuration(500)
                        .withEndAction {
                            nested_scroll_view.fullScroll(View.FOCUS_DOWN)
                        }
                        .start()
            }
        }

        text_active_leasing.click {
            if (expandable_layout_active_leasing.isExpanded) {
                expandable_layout_active_leasing.collapse()
                image_active_leasing.animate()
                        .rotation(180f)
                        .setDuration(500)
                        .start()
            } else {
                expandable_layout_active_leasing.expand()
                image_active_leasing.animate()
                        .rotation(0f)
                        .setDuration(500)
                        .withEndAction {
                            nested_scroll_view.smoothScrollTo(0, (linear_active_leasing.y.toInt() + relative_active_leasing_title_container.y.toInt()) - nested_scroll_view.scrollY)
                        }
                        .start()
            }
        }

        recycle_active_leasing.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(baseActivity)
        recycle_active_leasing.adapter = adapterActiveAdapter
        recycle_active_leasing.isNestedScrollingEnabled = false

        adapterActiveAdapter.onItemClickListener = BaseQuickAdapter.OnItemClickListener { adapter, view, position ->
            val historyItem = adapter.getItem(position) as HistoryTransactionResponse

            val bottomSheetFragment = HistoryDetailsBottomSheetFragment()

            bottomSheetFragment.configureData(historyItem, position)
            bottomSheetFragment.show(requireFragmentManager(), bottomSheetFragment::class.java.simpleName)
        }

        skeletonScreen = Skeleton.bind(root)
                .shimmer(true)
                .color(R.color.basic100)
                .load(R.layout.skeleton_leasing_layout)
                .show()

        nested_scroll_view.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { nestedScrollView, p1, p2, p3, p4 ->
            presenter.enableElevation = nestedScrollView.computeVerticalScrollOffset() > 4
            elevationAppBarChangeListener?.onChange(presenter.enableElevation)
        })

        presenter.getActiveLeasing()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.menu_leasing, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_your_address -> {
                analytics.trackEvent(AnalyticEvents.WalletQRCardEvent)
                launchActivity<MyAddressQRActivity>()
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun showBalances(wavesAsset: AssetBalanceResponse) {
        skeletonScreen?.hide()
        if (wavesAsset.getAvailableBalance() > 0) {
            linear_details_balances.visiable()
        } else {
            linear_details_balances.gone()
        }

        swipe_container.isRefreshing = false
        text_available_balance.text = wavesAsset.getDisplayAvailableBalance()
        text_available_balance.makeTextHalfBold()
        text_leased.text = wavesAsset.getDisplayLeasedBalance()
        text_total.text = wavesAsset.getDisplayTotalBalance()
        val wavesBalance = wavesAsset.getAvailableBalance()
        if (wavesBalance != 0L) {
            progress_of_leasing.progress =
                    ((wavesAsset.leasedBalance ?: 0 * 100) / wavesBalance).toInt()
        } else {
            progress_of_leasing.progress = 0
        }

        button_start_lease.click {
            analytics.trackEvent(AnalyticEvents.LeasingStartTapEvent)
            launchActivity<StartLeasingActivity> {
                val bundle = Bundle()
                bundle.putLong(StartLeasingActivity.BUNDLE_WAVES, wavesAsset.getAvailableBalance())
                putExtras(bundle)
            }
        }
    }

    override fun showActiveLeasingTransaction(transactions: List<HistoryTransactionResponse>) {
        skeletonScreen?.hide()
        swipe_container.isRefreshing = false
        if (transactions.isEmpty()) {
            linear_active_leasing.gone()
        } else {
            linear_active_leasing.visiable()
        }
        adapterActiveAdapter.setNewData(transactions)
        text_active_leasing.text = getString(R.string.wallet_leasing_active_now, transactions.size.toString())
    }

    override fun onNetworkConnectionChanged(networkConnected: Boolean) {
        super.onNetworkConnectionChanged(networkConnected)
        button_start_lease.notNull {
            it.isEnabled = networkConnected
        }
    }

    override fun afterFailedLoadLeasing() {
        swipe_container.isRefreshing = false
    }

    override fun isShadowEnable(): Boolean {
        return if (::presenter.isInitialized) presenter.enableElevation
        else false
    }

    companion object {

        /**
         * @return LeasingFragment instance
         * */
        fun newInstance(): LeasingFragment {
            return LeasingFragment()
        }
    }
}
