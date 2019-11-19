package com.wavesplatform.wallet.v2.ui.home.profile.settings

import android.os.Bundle
import com.wavesplatform.wallet.App
import moxy.presenter.InjectPresenter
import moxy.presenter.ProvidePresenter
import com.wavesplatform.wallet.R
import com.wavesplatform.wallet.v2.ui.base.view.BaseActivity
import com.wavesplatform.wallet.v2.util.EnvironmentManager
import kotlinx.android.synthetic.main.activity_dev_options.*
import com.wavesplatform.wallet.v2.util.PrefsUtil
import pers.victor.ext.click
import javax.inject.Inject

class DevOptionsActivity : BaseActivity(), DevOptionsView {

    @Inject
    @InjectPresenter
    lateinit var presenter: DevOptionsPresenter

    @ProvidePresenter
    fun providePresenter(): DevOptionsPresenter = presenter

    override fun configLayoutRes() = R.layout.activity_dev_options

    override fun onViewReady(savedInstanceState: Bundle?) {
        setupToolbar(toolbar_view, true, "Dev options", R.drawable.ic_toolbar_back_black)

        useTestConfigSwitch.isChecked = presenter.preferenceHelper.useTest
        useTestConfigSwitch.setOnCheckedChangeListener { _, isChecked ->
            presenter.preferenceHelper.useTest = isChecked
            EnvironmentManager.restartApp(App.appContext)
        }

        useTestNewsSwitch.isChecked = presenter.preferenceHelper.useTestNews
        useTestNewsSwitch.setOnCheckedChangeListener { _, isChecked ->
            presenter.preferenceHelper.useTestNews = isChecked
        }

        resetShowedNewsButton.click {
            prefsUtil.removeGlobalValue(PrefsUtil.SHOWED_NEWS_IDS)
        }
    }
}