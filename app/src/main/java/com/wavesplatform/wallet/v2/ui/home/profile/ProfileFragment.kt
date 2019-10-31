/*
 * Created by Eduard Zaydel on 1/4/2019
 * Copyright © 2019 Waves Platform. All rights reserved.
 */

package com.wavesplatform.wallet.v2.ui.home.profile

import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.view.*
import com.arellomobile.mvp.presenter.InjectPresenter
import com.arellomobile.mvp.presenter.ProvidePresenter
import com.novoda.simplechromecustomtabs.SimpleChromeCustomTabs
import com.wavesplatform.sdk.utils.notNull
import com.wavesplatform.wallet.App
import com.wavesplatform.wallet.BuildConfig
import com.wavesplatform.wallet.R
import com.wavesplatform.wallet.v2.data.Constants
import com.wavesplatform.wallet.v2.data.Events
import com.wavesplatform.wallet.v2.data.analytics.AnalyticEvents
import com.wavesplatform.wallet.v2.data.analytics.analytics
import com.wavesplatform.wallet.v2.data.manager.NodeServiceManager
import com.wavesplatform.wallet.v2.data.model.local.Language
import com.wavesplatform.wallet.v2.ui.auth.fingerprint.FingerprintAuthDialogFragment
import com.wavesplatform.wallet.v2.ui.auth.new_account.NewAccountActivity
import com.wavesplatform.wallet.v2.ui.auth.passcode.create.CreatePassCodeActivity
import com.wavesplatform.wallet.v2.ui.auth.passcode.enter.EnterPassCodeActivity
import com.wavesplatform.wallet.v2.ui.base.view.BaseFragment
import com.wavesplatform.wallet.v2.ui.home.MainActivity
import com.wavesplatform.wallet.v2.ui.home.profile.address_book.AddressBookActivity
import com.wavesplatform.wallet.v2.ui.home.profile.addresses.AddressesAndKeysActivity
import com.wavesplatform.wallet.v2.ui.home.profile.backup.BackupPhraseActivity
import com.wavesplatform.wallet.v2.ui.home.profile.change_password.ChangePasswordActivity
import com.wavesplatform.wallet.v2.ui.home.profile.network.NetworkActivity
import com.wavesplatform.wallet.v2.ui.home.profile.settings.DevOptionsActivity
import com.wavesplatform.wallet.v2.ui.language.change_welcome.ChangeLanguageActivity
import com.wavesplatform.wallet.v2.ui.welcome.WelcomeActivity
import com.wavesplatform.wallet.v2.util.*
import kotlinx.android.synthetic.main.fragment_profile.*
import pers.victor.ext.click
import pers.victor.ext.finish
import pers.victor.ext.telephonyManager
import pers.victor.ext.visiable
import javax.inject.Inject

class ProfileFragment : BaseFragment(), ProfileView {

    @Inject
    @InjectPresenter
    lateinit var presenter: ProfilePresenter
    @Inject
    lateinit var nodeServiceManager: NodeServiceManager
    private var onElevationAppBarChangeListener: MainActivity.OnElevationAppBarChangeListener? = null

    @ProvidePresenter
    fun providePresenter(): ProfilePresenter = presenter

    override fun configLayoutRes(): Int = R.layout.fragment_profile

    override fun onViewReady(savedInstanceState: Bundle?) {
        eventSubscriptions.add(rxEventBus.filteredObservable(Events.ScrollToTopEvent::class.java)
                .subscribe {
                    if (it.position == MainActivity.PROFILE_SCREEN) {
                        root_scrollView.scrollTo(0, 0)
                    }
                })

        card_address_book.click {
            analytics.trackEvent(AnalyticEvents.ProfileAddressBookPageEvent)
            launchActivity<AddressBookActivity> { }
        }
        card_addresses_and_keys.click {
            analytics.trackEvent(AnalyticEvents.ProfileAddressAndKeysEvent)
            launchActivity<AddressesAndKeysActivity> { }
        }
        card_backup_phrase.click {
            analytics.trackEvent(AnalyticEvents.ProfileBackupPhraseEvent)
            launchActivity<BackupPhraseActivity> {
                putExtra(KEY_INTENT_SET_BACKUP, true)
            }
        }
        card_language.click {
            analytics.trackEvent(AnalyticEvents.ProfileLanguageEvent)
            launchActivity<ChangeLanguageActivity>()
        }
        card_change_password.click {
            analytics.trackEvent(AnalyticEvents.ProfileChangePasswordEvent)
            launchActivity<ChangePasswordActivity>(requestCode = REQUEST_CHANGE_PASSWORD) { }
        }
        card_change_passcode.click {
            analytics.trackEvent(AnalyticEvents.ProfileChangePasscodeEvent)
            launchActivity<EnterPassCodeActivity>(
                    requestCode = REQUEST_ENTER_PASS_CODE_FOR_CHANGE)
        }
        card_network.click {
            analytics.trackEvent(AnalyticEvents.ProfileNetworkEvent)
            launchActivity<NetworkActivity> { }
        }
        card_support.click {
            analytics.trackEvent(AnalyticEvents.ProfileSupportEvent)
            openUrlWithChromeTab(Constants.SUPPORT_SITE)
        }
        card_rate_app.click {
            analytics.trackEvent(AnalyticEvents.ProfileRateAppEvent)
            openAppInPlayMarket()
        }
        card_feedback.click {
            analytics.trackEvent(AnalyticEvents.ProfileFeedbackEvent)
            sendFeedbackToSupport()
        }

        if (BuildConfig.DEBUG) {
            settings.visiable()
            settings.click {
                launchActivity<DevOptionsActivity>()
            }
        }

        button_delete_account.click {
            val alertDialog = AlertDialog.Builder(baseActivity).create()
            alertDialog.setTitle(getString(R.string.profile_general_delete_account_dialog_title))
            alertDialog.setMessage(getString(R.string.profile_general_delete_account_dialog_description))
            if (App.accessManager.isCurrentAccountBackupSkipped()) {
                alertDialog.setView(LayoutInflater.from(baseActivity)
                        .inflate(R.layout.content_delete_account_warning_layout, null))
            }
            alertDialog.setButton(AlertDialog.BUTTON_POSITIVE,
                    getString(R.string.profile_general_delete_account_dialog_delete)) { dialog, _ ->
                if (!MonkeyTest.isTurnedOn()) {
                    analytics.trackEvent(AnalyticEvents.ProfileDeleteAccountEvent)

                    App.accessManager.deleteCurrentWavesWallet()

                    presenter.prefsUtil.logOut()
                    restartApp()
                }
                dialog.dismiss()
            }
            alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.profile_general_delete_account_dialog_cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            alertDialog.show()
            alertDialog.makeStyled()
        }

        button_logout.click {
            if (!MonkeyTest.isTurnedOn()) {
                analytics.trackEvent(AnalyticEvents.ProfileLogoutDownEvent)
                logout()
            }
        }

        initFingerPrintControl()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            root_scrollView.setOnScrollChangeListener { _, _, scrollY, _, _ ->
                onElevationAppBarChangeListener.notNull {
                    presenter.hideShadow = scrollY == 0
                    onElevationAppBarChangeListener?.onChange(presenter.hideShadow)
                }
            }
        }
    }

    override fun onNetworkConnectionChanged(networkConnected: Boolean) {
        super.onNetworkConnectionChanged(networkConnected)
        if (networkConnected) {
            frame_network.alpha = Constants.View.ENABLE_VIEW
            frame_change_passcode.alpha = Constants.View.ENABLE_VIEW
            frame_change_password.alpha = Constants.View.ENABLE_VIEW
            frame_fingerprint.alpha = Constants.View.ENABLE_VIEW
            card_network.isClickable = true
            fingerprint_switch.isClickable = true
            card_change_passcode.isClickable = true
            card_fingerprint.isClickable = true
            card_change_password.isClickable = true
        } else {
            frame_network.alpha = Constants.View.DISABLE_VIEW
            frame_change_passcode.alpha = Constants.View.DISABLE_VIEW
            frame_change_password.alpha = Constants.View.DISABLE_VIEW
            frame_fingerprint.alpha = Constants.View.DISABLE_VIEW
            card_network.isClickable = false
            fingerprint_switch.isClickable = false
            card_change_passcode.isClickable = false
            card_fingerprint.isClickable = false
            card_change_password.isClickable = false
        }
    }

    private fun checkBackUp() {
        if (App.accessManager.isCurrentAccountBackupSkipped()) {
            skip_backup_indicator.setBackgroundColor(ContextCompat
                    .getColor(context!!, R.color.error500))
            skip_backup_indicator_image.setImageDrawable(ContextCompat
                    .getDrawable(context!!, R.drawable.ic_info_error_500))
        } else {
            skip_backup_indicator.setBackgroundColor(ContextCompat
                    .getColor(context!!, R.color.success400))
            skip_backup_indicator_image.setImageDrawable(ContextCompat
                    .getDrawable(context!!, R.drawable.ic_check_18_success_400))
        }

        textView_version.text = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
        textView_height.text = presenter.preferenceHelper.currentBlocksHeight.toString()
    }

    override fun onStart() {
        super.onStart()
        checkBackUp()
    }

    private fun sendFeedbackToSupport() {
        val emailIntent = Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                "mailto", Constants.SUPPORT_EMAIL, null))
        emailIntent.putExtra(Intent.EXTRA_TEXT, formatDeviceInfo())
        startActivity(Intent.createChooser(emailIntent, getString(R.string.common_feedback_title)))
    }

    private fun formatDeviceInfo(): String? {
        return "\n\n" +
                "${getString(R.string.profile_general_feedback_body_extra_title)}\n" +
                "${getString(R.string.profile_general_feedback_body_extra_os_version, Build.VERSION.RELEASE)}\n" +
                "${getString(R.string.profile_general_feedback_body_extra_app_version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE.toString())}\n" +
                "${getString(R.string.profile_general_feedback_body_extra_device_model, getDeviceName())}\n" +
                "${getString(R.string.profile_general_feedback_body_extra_language,
                        presenter.preferenceHelper.getLanguage())}\n" +
                "${getString(R.string.profile_general_feedback_body_extra_carrier, telephonyManager.networkOperatorName)}\n" +
                "${getString(R.string.profile_general_feedback_body_extra_device_id, getDeviceId())}\n"
    }

    private fun openAppInPlayMarket() {
        val uri = Uri.parse("market://details?id=" + Constants.PRODUCATION_PACKAGE_NAME)
        val myAppLinkToMarket = Intent(Intent.ACTION_VIEW, uri)
        try {
            startActivity(myAppLinkToMarket)
        } catch (e: ActivityNotFoundException) {
            showError(R.string.common_market_error, R.id.root_scrollView)
        }
    }

    private fun initFingerPrintControl() {
        if (FingerprintAuthDialogFragment.isAvailable(context!!)) {
            fingerprint_switch.setOnCheckedChangeListener(null)
            val guid = App.accessManager.getLoggedInGuid()
            fingerprint_switch.isChecked = App.accessManager.isUseFingerPrint(guid)
            fingerprint_switch.setOnCheckedChangeListener { _, enable ->
                launchActivity<EnterPassCodeActivity>(
                        requestCode = REQUEST_ENTER_PASS_CODE_FOR_FINGERPRINT) {
                    if (enable) {
                        putExtra(EnterPassCodeActivity.KEY_INTENT_PROCESS_SET_FINGERPRINT, true)
                    }
                }
            }
        } else {
            card_fingerprint.visibility = View.GONE
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            applyElevation()
        }
    }

    override fun onResume() {
        super.onResume()
        setCurrentLangFlag()
        initFingerPrintControl()
        SimpleChromeCustomTabs.getInstance().connectTo(baseActivity)
    }

    override fun onPause() {
        SimpleChromeCustomTabs.getInstance().disconnectFrom(baseActivity)
        super.onPause()
    }

    private fun setCurrentLangFlag() {
        val languageItemByCode = Language.getLanguageItemByCode(presenter.preferenceHelper.getLanguage())
        image_language_flag.setImageResource(languageItemByCode.language.image)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.menu_profile, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.action_logout -> {
                if (!MonkeyTest.isTurnedOn()) {
                    analytics.trackEvent(AnalyticEvents.ProfileLogoutUpEvent)
                    logout()
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun logout() {
        App.accessManager.resetWallet()
        App.accessManager.setLastLoggedInGuid("")
        finish()
        launchActivity<WelcomeActivity>(clear = true)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {

            REQUEST_ENTER_PASS_CODE_FOR_FINGERPRINT -> {
                if (resultCode == Constants.RESULT_OK) {
                    setFingerprint(App.accessManager.getLoggedInGuid(),
                            data!!.extras.getString(EnterPassCodeActivity.KEY_INTENT_PASS_CODE))
                } else {
                    initFingerPrintControl()
                }
            }

            REQUEST_ENTER_PASS_CODE_FOR_CHANGE -> {
                if (resultCode == Constants.RESULT_OK) {
                    val passCode = data!!.extras.getString(EnterPassCodeActivity.KEY_INTENT_PASS_CODE)
                    val password = data.extras.getString(NewAccountActivity.KEY_INTENT_PASSWORD)
                    launchActivity<CreatePassCodeActivity> {
                        putExtra(CreatePassCodeActivity.KEY_INTENT_PROCESS_CHANGE_PASS_CODE, true)
                        putExtra(EnterPassCodeActivity.KEY_INTENT_GUID,
                                App.accessManager.getLoggedInGuid())
                        putExtra(NewAccountActivity.KEY_INTENT_PASSWORD, password)
                        putExtra(EnterPassCodeActivity.KEY_INTENT_PASS_CODE, passCode)
                    }
                }
            }

            REQUEST_CHANGE_PASSWORD -> {
                if (resultCode == Constants.RESULT_OK) {
                    showSuccess(R.string.change_password_success, R.id.root_scrollView)
                }
            }
        }
    }

    private fun setFingerprint(guid: String, passCode: String) {
        if (App.accessManager.isUseFingerPrint(guid)) {
            App.accessManager.setUseFingerPrint(guid, false)
        } else {
            val fingerprintDialog = FingerprintAuthDialogFragment.newInstance(guid, passCode)
            fingerprintDialog.isCancelable = false
            fingerprintDialog.show(requireActivity().supportFragmentManager, "fingerprintDialog")
            fingerprintDialog.setFingerPrintDialogListener(
                    object : FingerprintAuthDialogFragment.FingerPrintDialogListener {
                        override fun onSuccessRecognizedFingerprint() {
                            App.accessManager.setUseFingerPrint(guid,
                                    !App.accessManager.isUseFingerPrint(guid))
                            initFingerPrintControl()
                        }

                        override fun onCancelButtonClicked(dialog: Dialog) {
                            dialog.dismiss()
                            initFingerPrintControl()
                        }

                        override fun onFingerprintLocked(message: String) {
                            initFingerPrintControl()
                        }

                        override fun onShowErrorMessage(message: String) {
                            showError(message, R.id.content)
                            fingerprintDialog.dismiss()
                        }
                    })
        }
    }

    fun setOnElevationChangeListener(listener: MainActivity.OnElevationAppBarChangeListener) {
        this.onElevationAppBarChangeListener = listener
    }

    private fun applyElevation() {
        onElevationAppBarChangeListener?.let {
            onElevationAppBarChangeListener?.onChange(presenter.hideShadow)
        }
    }

    companion object {

        fun newInstance(): ProfileFragment {
            return ProfileFragment()
        }

        const val KEY_INTENT_SET_BACKUP = "intent_set_backup"
        const val REQUEST_ENTER_PASS_CODE_FOR_CHANGE = 5551
        const val REQUEST_ENTER_PASS_CODE_FOR_FINGERPRINT = 5552
        const val REQUEST_CHANGE_PASSWORD = 5553
    }
}