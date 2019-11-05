/*
 * Created by Eduard Zaydel on 1/4/2019
 * Copyright © 2019 Waves Platform. All rights reserved.
 */

package com.wavesplatform.wallet.v2.ui.language.change_welcome

import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import moxy.presenter.InjectPresenter
import moxy.presenter.ProvidePresenter
import com.chad.library.adapter.base.BaseQuickAdapter
import com.wavesplatform.wallet.v2.data.model.local.Language
import com.wavesplatform.wallet.v2.data.model.local.LanguageItem
import com.wavesplatform.sdk.utils.notNull
import com.wavesplatform.wallet.R
import com.wavesplatform.wallet.v2.ui.base.view.BaseActivity
import com.wavesplatform.wallet.v2.ui.home.MainActivity
import com.wavesplatform.wallet.v2.ui.language.LanguageAdapter
import com.wavesplatform.wallet.v2.ui.language.LanguagePresenter
import com.wavesplatform.wallet.v2.ui.language.LanguageView
import com.wavesplatform.wallet.v2.util.getLocalizedString
import com.wavesplatform.wallet.v2.util.launchActivity
import kotlinx.android.synthetic.main.activity_change_language.*
import pers.victor.ext.click
import pers.victor.ext.gone
import pers.victor.ext.visiable
import javax.inject.Inject

class ChangeLanguageActivity : BaseActivity(), LanguageView {

    @Inject
    @InjectPresenter
    lateinit var presenter: LanguagePresenter

    @Inject
    lateinit var adapter: LanguageAdapter

    @ProvidePresenter
    fun providePresenter(): LanguagePresenter = presenter

    override fun configLayoutRes() = R.layout.activity_change_language

    override fun onCreate(savedInstanceState: Bundle?) {
        overridePendingTransition(R.anim.slide_in_right, R.anim.null_animation)
        super.onCreate(savedInstanceState)
    }

    override fun onViewReady(savedInstanceState: Bundle?) {
        setupToolbar(toolbar_view, true, getString(R.string.profile_language_toolbar_title), R.drawable.ic_toolbar_back_black)

        recycle_language.addOnScrollListener(object : androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: androidx.recyclerview.widget.RecyclerView, dx: Int, dy: Int) {
                appbar_layout.isSelected = recycle_language.canScrollVertically(-1)
            }
        })

        recycle_language.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        recycle_language.adapter = adapter
        adapter.changeRootPadding = true

        adapter.setNewData(presenter.getLanguages())
        markCurrentSelectedLanguage()

        adapter.onItemClickListener = BaseQuickAdapter.OnItemClickListener { adapter, view, position ->
            val item = adapter.getItem(position) as LanguageItem

            if (presenter.currentLanguagePosition == position) return@OnItemClickListener

            val languageItemByCode = Language.getLanguageItemByCode(preferencesHelper.getLanguage())
            val positionCurrent = adapter.data.indexOf(languageItemByCode)
            if (position == positionCurrent) {
                frame_button_save.gone()
            } else {
                frame_button_save.visiable()
            }

            if (presenter.currentLanguagePosition == -1) {
                // check new item
                presenter.currentLanguagePosition = position
                item.checked = true
                adapter.setData(position, item)
            } else {
                // uncheck old item
                val currentCheckedItem = adapter.getItem(presenter.currentLanguagePosition) as LanguageItem
                currentCheckedItem.checked = false
                adapter.setData(presenter.currentLanguagePosition, currentCheckedItem)

                // check new item
                presenter.currentLanguagePosition = position
                item.checked = true
                adapter.setData(position, item)
            }

            setLocalizedTextToButton(item)
        }

        button_save.click {
            val item = adapter.getItem(presenter.currentLanguagePosition)
            item.notNull {
                presenter.saveLanguage(it.language.code)
                setLanguage(Language.getLocale(it.language.code))
                launchActivity<MainActivity>(clear = true)
            }
        }
    }

    private fun setLocalizedTextToButton(item: LanguageItem) {
        button_save.text = getLocalizedString(R.string.choose_language_confirm, Language.getLocale(item.language.code))
    }

    private fun markCurrentSelectedLanguage() {
        val languageItemByCode = Language.getLanguageItemByCode(preferencesHelper.getLanguage())
        val position = adapter.data.indexOf(languageItemByCode)
        presenter.currentLanguagePosition = position
        val languageItem = adapter.getItem(position) as LanguageItem
        languageItem.checked = true
        adapter.setData(position, languageItem)
    }

    override fun onBackPressed() {
        finish()
        overridePendingTransition(R.anim.null_animation, R.anim.slide_out_right)
    }
}
