/*
 * Created by Eduard Zaydel on 1/4/2019
 * Copyright © 2019 Waves Platform. All rights reserved.
 */

package com.wavesplatform.wallet.v2.ui.home.profile.addresses

import android.os.Bundle
import com.arellomobile.mvp.presenter.InjectPresenter
import com.arellomobile.mvp.presenter.ProvidePresenter
import com.jakewharton.rxbinding2.view.RxView
import com.wavesplatform.wallet.App
import com.wavesplatform.wallet.R
import com.wavesplatform.sdk.model.response.node.transaction.AliasTransactionResponse
import com.wavesplatform.wallet.v2.ui.base.view.BaseActivity
import com.wavesplatform.wallet.v2.ui.home.profile.addresses.alias.AliasBottomSheetFragment
import com.wavesplatform.wallet.v2.util.copyToClipboard
import com.wavesplatform.wallet.v2.util.showSuccess
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.activity_profile_addresses_and_keys.*
import pers.victor.ext.click
import pers.victor.ext.gone
import pers.victor.ext.visiable
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class AddressesAndKeysActivity : BaseActivity(), AddressesAndKeysView {

    @Inject
    @InjectPresenter
    lateinit var presenter: AddressesAndKeysPresenter

    @ProvidePresenter
    fun providePresenter(): AddressesAndKeysPresenter = presenter

    override fun configLayoutRes(): Int = R.layout.activity_profile_addresses_and_keys

    override fun onCreate(savedInstanceState: Bundle?) {
        overridePendingTransition(R.anim.slide_in_right, R.anim.null_animation)
        super.onCreate(savedInstanceState)
    }

    override fun onViewReady(savedInstanceState: Bundle?) {
        setupToolbar(toolbar_view, true, getString(R.string.addresses_and_keys_toolbar_title), R.drawable.ic_toolbar_back_black)

        text_address.text = App.accessManager.getWallet()?.address
        text_public_key.text = App.accessManager.getWallet()?.publicKeyStr

        eventSubscriptions.add(RxView.clicks(image_address_copy)
                .throttleFirst(1500, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    image_address_copy.copyToClipboard(text_address.text.toString())
                })

        eventSubscriptions.add(RxView.clicks(image_public_key_copy)
                .throttleFirst(1500, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    image_public_key_copy.copyToClipboard(text_public_key.text.toString())
                })

        eventSubscriptions.add(RxView.clicks(image_private_key_copy)
                .throttleFirst(1500, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    image_private_key_copy.copyToClipboard(text_private_key.text.toString())
                })

        button_show.click {
            button_show.gone()
            val wallet = App.accessManager.getWallet()
            text_private_key.text = (wallet?.privateKeyStr)
            relative_private_key_block.visiable()
        }

        presenter.loadAliases()
    }

    override fun afterSuccessLoadAliases(ownAliases: MutableList<AliasTransactionResponse>) {
        if (ownAliases.isEmpty()) {
            text_alias_count.text = getString(R.string.addresses_and_keys_you_do_not_have)
        } else {
            text_alias_count.text = String.format(getString(R.string.alias_dialog_you_have), ownAliases.size)
        }
        relative_alias.click {
            val bottomSheetFragment = AliasBottomSheetFragment()
            bottomSheetFragment.configureDialog(ownAliases.isEmpty(), AliasBottomSheetFragment.FROM_PROFILE)
            bottomSheetFragment.onCreateAliasListener = object : AliasBottomSheetFragment.OnCreateAliasListener {
                override fun onSuccess(alias: AliasTransactionResponse) {
                    bottomSheetFragment.dismiss()
                    showSuccess(getString(R.string.new_alias_success_create), R.id.root)

                    ownAliases.add(alias)
                    text_alias_count.text = String.format(getString(R.string.alias_dialog_you_have), ownAliases.size)
                }
            }
            bottomSheetFragment.show(supportFragmentManager, bottomSheetFragment.tag)
        }
    }

    override fun onBackPressed() {
        finish()
        overridePendingTransition(R.anim.null_animation, R.anim.slide_out_right)
    }

    override fun needToShowNetworkMessage(): Boolean = true
}
