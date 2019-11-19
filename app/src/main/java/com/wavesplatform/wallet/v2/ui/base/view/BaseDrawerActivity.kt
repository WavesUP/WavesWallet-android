/*
 * Created by Eduard Zaydel on 1/4/2019
 * Copyright © 2019 Waves Platform. All rights reserved.
 */

package com.wavesplatform.wallet.v2.ui.base.view

import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.novoda.simplechromecustomtabs.SimpleChromeCustomTabs
import com.wavesplatform.wallet.App
import com.wavesplatform.wallet.R
import com.wavesplatform.wallet.v2.data.Constants
import com.wavesplatform.wallet.v2.data.analytics.AnalyticEvents
import com.wavesplatform.wallet.v2.data.analytics.analytics
import com.wavesplatform.wallet.v2.ui.welcome.WelcomeActivity
import com.wavesplatform.wallet.v2.util.launchActivity
import com.wavesplatform.wallet.v2.util.openUrlWithChromeTab
import com.yarolegovich.slidingrootnav.SlidingRootNav
import com.yarolegovich.slidingrootnav.SlidingRootNavBuilder
import com.yarolegovich.slidingrootnav.callback.DragStateListener
import kotlinx.android.synthetic.main.content_menu_left_drawer.view.*
import pers.victor.ext.*

abstract class BaseDrawerActivity : BaseActivity() {

    lateinit var slidingRootNav: SlidingRootNav
    private var view: View? = null
    private var drawerIcon = findDrawable(R.drawable.ic_toolbar_menu)
    private var counter = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        slidingRootNav = SlidingRootNavBuilder(this)
                .withDragDistance(px2dp(screenWidth) - dp2px(18))
                .withRootViewScale(0.87f)
                .withRootViewElevation(10)
                .withToolbarMenuToggle(toolbar)
                .addDragStateListener(object : DragStateListener {
                    override fun onDragEnd(isMenuOpened: Boolean) {
                        if (isMenuOpened) {
                            analytics.trackEvent(AnalyticEvents.WavesMenuPageEvent)
                            view?.visiable()
                        } else {
                            view?.gone()
                        }
                    }

                    override fun onDragStart() {
                    }
                })
                .addDragListener { progress ->
                    slidingRootNav.layout.linear_drawer.scaleX = 1.5f - (progress / 2)
                    slidingRootNav.layout.linear_drawer.scaleY = 1.5f - (progress / 2)

                    if (progress > 0.02) {
                        if (window.statusBarColor != R.color.white)
                            setStatusBarColor(R.color.white)
                    } else {
                        if (window.statusBarColor != R.color.basic50)
                            setStatusBarColor(R.color.basic50)
                    }
                }
                .withMenuOpened(false)
                .withSavedState(savedInstanceState)
                .withMenuLayout(R.layout.content_menu_left_drawer)
                .inject()

        toolbar.navigationIcon = drawerIcon

        createCloseView()
        slidingRootNav.layout.linear_drawer.scaleX = 1.5f
        slidingRootNav.layout.linear_drawer.scaleY = 1.5f
        slidingRootNav.layout.text_site.paintFlags = slidingRootNav.layout.text_site.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        slidingRootNav.layout.text_site.click {
            analytics.trackEvent(AnalyticEvents.WavesMenuForumEvent)
            openUrlWithChromeTab(Constants.URL_WAVES_FORUM)
        }
        slidingRootNav.layout.text_whitepaper.click {
            analytics.trackEvent(AnalyticEvents.WavesMenuWhitepaperEvent)
            openUrlWithIntent(Constants.URL_WHITEPAPER)
        }
        slidingRootNav.layout.text_terms.click {
            analytics.trackEvent(AnalyticEvents.WavesMenuTermsAndConditionsEvent)
            openUrlWithIntent(Constants.URL_TERMS)
        }
        slidingRootNav.layout.text_support.click {
            analytics.trackEvent(AnalyticEvents.WavesMenuSupportEvent)
            openUrlWithChromeTab(Constants.SUPPORT_SITE)
        }
        slidingRootNav.layout.image_discord.click {
            analytics.trackEvent(AnalyticEvents.WavesMenuDiscordEvent)
            openDiscord(Constants.URL_DISCORD)
        }
        slidingRootNav.layout.image_reddit.click {
            analytics.trackEvent(AnalyticEvents.WavesMenuRedditEvent)
            openReddit(Constants.URL_REDDIT)
        }
        slidingRootNav.layout.image_github.click {
            analytics.trackEvent(AnalyticEvents.WavesMenuGithubEvent)
            openUrlWithChromeTab(Constants.URL_GITHUB)
        }
        slidingRootNav.layout.image_telegram.click {
            analytics.trackEvent(AnalyticEvents.WavesMenuTelegramEvent)
            openTelegram(Constants.ACC_TELEGRAM)
        }
        slidingRootNav.layout.image_twitter.click {
            analytics.trackEvent(AnalyticEvents.WavesMenuTwitterEvent)
            openTwitter(Constants.ACC_TWITTER)
        }

        becomeDeveloper()
    }

    private fun becomeDeveloper() {
        slidingRootNav.layout.image_logo.click {
            counter++
            if (counter > 3) {
                if (preferencesHelper.isDeveloper()) {
                    Toast.makeText(this, getString(R.string.developer_already), Toast.LENGTH_LONG).show()
                } else {
                    if (counter == 10) {
                        counter = 0
                        Toast.makeText(this, getString(R.string.developer_success), Toast.LENGTH_LONG).show()
                        preferencesHelper.setDeveloper(true)

                        App.accessManager.resetWallet()
                        App.accessManager.setLastLoggedInGuid("")
                        finish()
                        launchActivity<WelcomeActivity>(clear = true)
                    } else {
                        Toast.makeText(this, getString(R.string.developer_in_progress, 10 - counter), Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun createCloseView() {
        view = View(this@BaseDrawerActivity)

        val params: ViewGroup.LayoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, // This will define text view width
                ViewGroup.LayoutParams.MATCH_PARENT // This will define text view height
        )
        view?.layoutParams = params
        view?.setBackgroundColor(Color.TRANSPARENT)
        view?.click {
            slidingRootNav.closeMenu(true)
        }
        slidingRootNav.layout.findViewById<ViewGroup>(R.id.root).addView(view)

        view?.gone()
    }

    fun openUrlWithIntent(url: String) {
        if (isNetworkConnected()) {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(browserIntent)
        } else {
            showNetworkError()
        }
    }

    fun openFacebook(url: String) {
        var uri = Uri.parse(url)
        try {
            val applicationInfo = packageManager.getApplicationInfo("com.facebook.katana", 0)
            if (applicationInfo.enabled) {
                // http://stackoverflow.com/a/24547437/1048340
                uri = Uri.parse("fb://facewebmodal/f?href=$url")
            }
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (ignored: Exception) {
            openUrlWithChromeTab(url)
        }
    }

    fun openTwitter(url: String) {
        var intent: Intent? = null
        try {
            // get the Twitter app if possible
            this.packageManager.getPackageInfo("com.twitter.android", 0)
            intent = Intent(Intent.ACTION_VIEW, Uri.parse("twitter://user?user_id=$url"))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            this.startActivity(intent)
        } catch (e: Exception) {
            // no Twitter app, revert to browser
            openUrlWithChromeTab(Constants.URL_TWITTER)
        }
    }

    fun openDiscord(url: String) {
        var intent: Intent? = null
        try {
            this.packageManager.getPackageInfo("com.discord", 0)
            intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            this.startActivity(intent)
        } catch (e: Exception) {
            openUrlWithChromeTab(Constants.URL_DISCORD)
        }
    }

    fun openReddit(url: String) {
        var intent: Intent? = null
        try {
            this.packageManager.getPackageInfo("com.reddit.frontpage", 0)
            intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            this.startActivity(intent)
        } catch (e: Exception) {
            openUrlWithChromeTab(url)
        }
    }

    fun openTelegram(url: String) {
        var intent: Intent? = null
        try {
            this.packageManager.getPackageInfo("org.telegram.messenger", 0)
            intent = Intent(Intent.ACTION_VIEW, Uri.parse("tg://resolve?domain=$url"))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        } catch (e: Exception) {
            try {
                this.packageManager.getPackageInfo("org.thunderdog.challegram", 0)
                intent = Intent(Intent.ACTION_VIEW, Uri.parse("tg://resolve?domain=$url"))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            } catch (e: Exception) {
                openUrlWithChromeTab(Constants.URL_TELEGRAM)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        SimpleChromeCustomTabs.getInstance().connectTo(this)
    }

    override fun onPause() {
        SimpleChromeCustomTabs.getInstance().disconnectFrom(this)
        super.onPause()
    }

    fun changeDrawerMenuIcon(icon: Drawable?) {
        drawerIcon = icon
    }
}
