/*
 * Created by Eduard Zaydel on 1/4/2019
 * Copyright © 2019 Waves Platform. All rights reserved.
 */

package com.wavesplatform.wallet.v2.ui.base.view

import android.app.ProgressDialog
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.AnimationUtils
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.appcompat.app.ActionBar
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.akexorcist.localizationactivity.core.LocalizationActivityDelegate
import com.akexorcist.localizationactivity.core.OnLocaleChangedListener
import com.github.pwittchen.reactivenetwork.library.rx2.ReactiveNetwork
import com.google.firebase.analytics.FirebaseAnalytics
import com.wavesplatform.sdk.WavesSdk
import com.wavesplatform.sdk.net.NetworkException
import com.wavesplatform.sdk.net.OnErrorListener
import com.wavesplatform.sdk.utils.RxUtil
import com.wavesplatform.wallet.App
import com.wavesplatform.wallet.R
import com.wavesplatform.wallet.v2.data.Events
import com.wavesplatform.wallet.v2.data.helpers.SentryHelper
import com.wavesplatform.wallet.v2.data.local.PreferencesHelper
import com.wavesplatform.wallet.v2.data.manager.ErrorManager
import com.wavesplatform.wallet.v2.data.manager.GithubServiceManager
import com.wavesplatform.wallet.v2.data.manager.base.BaseServiceManager
import com.wavesplatform.wallet.v2.data.manager.gateway.manager.CoinomatDataManager
import com.wavesplatform.wallet.v2.data.manager.gateway.manager.GatewayDataManager
import com.wavesplatform.wallet.v2.ui.auth.passcode.enter.EnterPassCodeActivity
import com.wavesplatform.wallet.v2.ui.splash.SplashActivity
import com.wavesplatform.wallet.v2.ui.welcome.WelcomeActivity
import com.wavesplatform.wallet.v2.ui.widget.MarketPulseAppWidgetProvider
import com.wavesplatform.wallet.v2.util.*
import dagger.android.AndroidInjection
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.content_no_internet_bottom_message_layout.view.*
import moxy.MvpAppCompatActivity
import org.fingerlinks.mobile.android.navigator.Navigator
import pyxis.uzuki.live.richutilskt.utils.hideKeyboard
import timber.log.Timber
import java.util.*
import javax.inject.Inject

abstract class BaseActivity : MvpAppCompatActivity(), BaseView, BaseMvpView, HasAndroidInjector, OnLocaleChangedListener {
    private val mCompositeDisposable = CompositeDisposable()
    var eventSubscriptions: CompositeDisposable = CompositeDisposable()

    lateinit var toolbar: Toolbar
    var translucentStatusBar = false
    protected var mActionBar: ActionBar? = null
    val fragmentContainer: Int
        @IdRes get() = 0

    @Inject
    lateinit var androidInjector: DispatchingAndroidInjector<Any>

    @Inject
    lateinit var mRxEventBus: RxEventBus
    @Inject
    lateinit var prefsUtil: PrefsUtil
    @Inject
    lateinit var mErrorManager: ErrorManager
    @Inject
    lateinit var dataManager: BaseServiceManager
    @Inject
    lateinit var preferencesHelper: PreferencesHelper

    private var progressDialog: ProgressDialog? = null
    private val localizationDelegate = LocalizationActivityDelegate(this)
    protected lateinit var firebaseAnalytics: FirebaseAnalytics

    private var noInternetLayout: View? = null

    private var onErrorListener: OnErrorListener? = null

    override fun androidInjector(): AndroidInjector<Any> {
        return androidInjector
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(localizationDelegate.attachBaseContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        localizationDelegate.addOnLocaleChangedListener(this)
        localizationDelegate.onCreate(savedInstanceState)
        super.onCreate(savedInstanceState)
        firebaseAnalytics = FirebaseAnalytics.getInstance(this)
        if (!translucentStatusBar) {
            setStatusBarColor(R.color.white)
            setNavigationBarColor(R.color.white)
        }
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        setContentView(configLayoutRes())
        Timber.tag(javaClass.simpleName)
        onViewReady(savedInstanceState)

        noInternetLayout = layoutInflater.inflate(R.layout.content_no_internet_bottom_message_layout, null)

        eventSubscriptions.add(ReactiveNetwork
                .observeInternetConnectivity()
                .distinctUntilChanged()
                .onErrorResumeNext(Observable.empty())
                .compose(RxUtil.applyObservableDefaultSchedulers())
                .subscribe({ connected ->
                    onNetworkConnectionChanged(connected)
                }, {
                    it.printStackTrace()
                }))

        onErrorListener = object : OnErrorListener {
            override fun onError(exception: NetworkException) {
                val retrySubject = PublishSubject.create<Events.RetryEvent>()
                mErrorManager.handleError(exception, retrySubject)
                SentryHelper.logException(exception)
            }
        }
    }

    protected fun exit() {
        launchActivity<SplashActivity>(clear = true) {
            putExtra(SplashActivity.EXIT, true)
        }
    }

    public override fun onResume() {
        super.onResume()
        localizationDelegate.onResume(this)

        askPassCodeIfNeed()
        mCompositeDisposable.add(mRxEventBus.filteredObservable(Events.ErrorEvent::class.java)
                .compose(RxUtil.applyObservableDefaultSchedulers())
                .subscribe({ errorEvent ->
                    mErrorManager.showError(this,
                            errorEvent.retrofitException, errorEvent.retrySubject)
                }, { t: Throwable? -> t?.printStackTrace() }))
    }

    private fun addErrorListener() {
        WavesSdk.service().addOnErrorListener(onErrorListener!!)
        dataManager.coinomatService = CoinomatDataManager.create(onErrorListener)
        dataManager.gatewayService = GatewayDataManager.create(onErrorListener)
        dataManager.githubService = GithubServiceManager.create(onErrorListener)
    }

    public override fun onPause() {
        mCompositeDisposable.clear()
        super.onPause()
        WavesSdk.service().removeOnErrorListener(onErrorListener!!)
        CoinomatDataManager.removeOnErrorListener()
        GatewayDataManager.removeOnErrorListener()
        GithubServiceManager.removeOnErrorListener()
    }

    override fun onDestroy() {
        eventSubscriptions.clear()
        super.onDestroy()
    }

    protected open fun askPassCode() = true

    private fun askPassCodeIfNeed() {
        val guid = App.getAccessManager().getLastLoggedInGuid()

        val notAuthenticated = !App.getAccessManager().isAuthenticated()
        val hasGuidToLogin = !TextUtils.isEmpty(guid)

        if (!MonkeyTest.isTurnedOn() && hasGuidToLogin && notAuthenticated && askPassCode()) {
            launchActivity<EnterPassCodeActivity>(
                    requestCode = EnterPassCodeActivity.REQUEST_ENTER_PASS_CODE) {
                putExtra(EnterPassCodeActivity.KEY_INTENT_GUID, guid)
            }
        }
    }

    @JvmOverloads
    inline fun setupToolbar(
            toolbar: Toolbar,
            homeEnable: Boolean = false,
            title: String = "",
            @DrawableRes icon: Int = R.drawable.ic_arrow_back_white_24dp,
            crossinline onClickListener: () -> Unit = { onBackPressed() }
    ) {
        this.toolbar = toolbar
        setSupportActionBar(this.toolbar)
        mActionBar = supportActionBar

        mActionBar?.setHomeButtonEnabled(homeEnable)
        mActionBar?.setDisplayHomeAsUpEnabled(homeEnable)

        mActionBar?.setHomeAsUpIndicator(AppCompatResources.getDrawable(this, icon))

        if (title.isNotEmpty()) mActionBar?.title = title
        else mActionBar?.title = " "

        toolbar.setNavigationOnClickListener {
            hideKeyboard()
            onClickListener()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun setTitle(title: Int) {
        super.setTitle(title)
        if (mActionBar != null)
            mActionBar!!.title = getString(title)
    }

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount > 1)
            supportFragmentManager.popBackStack()
        else
            finish()
    }

    override fun showNetworkError() {
        showSnackbar(R.string.check_connectivity_exit)
    }

    override fun showProgressBar(isShowProgress: Boolean) {
        pyxis.uzuki.live.richutilskt.utils.runOnUiThread {
            if (isShowProgress) {
                if (progressDialog == null || !progressDialog?.isShowing!!) {
                    progressDialog = ProgressDialog(this)
                    progressDialog?.setCancelable(false)
                    progressDialog?.setMessage(getString(R.string.dialog_processing))
                    progressDialog?.show()
                } else {
                    progressDialog?.show()
                }
            } else if (progressDialog != null && progressDialog!!.isShowing) {
                progressDialog?.dismiss()
                progressDialog = null
            }
        }
    }

    fun openFragment(fragmentContainer: Int, fragment: Fragment) {
        val canGoBack = Navigator.with(this).utils()
                .canGoBackToSpecificPoint(fragment::class.java.simpleName, fragmentContainer, supportFragmentManager)
        if (canGoBack) {
            Navigator.with(this)
                    .utils()
                    .goBackToSpecificPoint(fragment::class.java.simpleName)
        } else {
            Navigator.with(this)
                    .build()
                    .goTo(fragment, fragmentContainer)
                    .addToBackStack()
                    .tag(fragment::class.java.simpleName)
                    .replace()
                    .commit()
        }
    }

    protected fun setStatusBarColor(@ColorRes intColorRes: Int) {
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.statusBarColor = ContextCompat.getColor(this, intColorRes)
        } else {
            window.statusBarColor = ContextCompat.getColor(this, R.color.black)
        }
    }

    protected fun setNavigationBarColor(@ColorRes intColorRes: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            window.navigationBarColor = ContextCompat.getColor(this, intColorRes)
        } else {
            window.navigationBarColor = ContextCompat.getColor(this, R.color.black)
        }
    }

    protected fun clearAndLogout() {
        App.getAccessManager().setLastLoggedInGuid("")
        App.getAccessManager().resetWallet()
        launchActivity<WelcomeActivity>(clear = true)
    }

    protected abstract fun onViewReady(savedInstanceState: Bundle?)

    override fun getApplicationContext(): Context {
        return localizationDelegate.getApplicationContext(super.getApplicationContext())
    }

    override fun getResources(): Resources {
        return localizationDelegate.getResources(super.getResources())
    }

    fun setLanguage(language: String) {
        localizationDelegate.setLanguage(this, language)
    }

    fun setLanguage(locale: Locale) {
        localizationDelegate.setLanguage(this, locale)
        MarketPulseAppWidgetProvider.updateAllWidgetsByBroadcast(this)
    }

    fun setDefaultLanguage(language: String) {
        localizationDelegate.setDefaultLanguage(language)
    }

    fun setDefaultLanguage(locale: Locale) {
        localizationDelegate.setDefaultLanguage(locale)
    }

    fun getCurrentLanguage(): Locale {
        return localizationDelegate.getLanguage(this)
    }

    override fun onBeforeLocaleChanged() {}

    override fun onAfterLocaleChanged() {}

    override fun onNetworkConnectionChanged(networkConnected: Boolean) {
        if (needToShowNetworkMessage()) {
            if (networkConnected) {
                if (noInternetLayout?.parent != null) {
                    noInternetLayout?.image_no_internet?.clearAnimation()
                    rootLayoutToShowNetworkMessage().removeView(noInternetLayout)
                }
            } else {
                if (noInternetLayout?.parent == null) {
                    noInternetLayout?.linear_no_internet_message?.setMargins(0, 0, 0, extraBottomMarginToShowNetworkMessage())
                    rootLayoutToShowNetworkMessage().addView(noInternetLayout)
                    noInternetLayout?.image_no_internet?.startAnimation(AnimationUtils.loadAnimation(this, R.anim.easy_rotate))
                }
            }
        }
    }

    fun snakeAnimationForNetworkMsg() {
        val animation = AnimationUtils.loadAnimation(this, R.anim.easy_shake_error)
        noInternetLayout?.startAnimation(animation)
    }

    open fun needToShowNetworkMessage(): Boolean = false

    open fun extraBottomMarginToShowNetworkMessage(): Int = 0

    open fun rootLayoutToShowNetworkMessage(): ViewGroup = findViewById(android.R.id.content)
}
