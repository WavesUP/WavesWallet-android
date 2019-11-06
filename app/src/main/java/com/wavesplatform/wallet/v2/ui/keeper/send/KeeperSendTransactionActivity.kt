package com.wavesplatform.wallet.v2.ui.keeper.send

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import moxy.presenter.InjectPresenter
import moxy.presenter.ProvidePresenter
import com.wavesplatform.sdk.keeper.interfaces.KeeperTransactionResponse
import com.wavesplatform.sdk.model.request.node.DataTransaction
import com.wavesplatform.sdk.model.request.node.InvokeScriptTransaction
import com.wavesplatform.sdk.model.request.node.TransferTransaction
import com.wavesplatform.sdk.model.response.node.AssetsDetailsResponse
import com.wavesplatform.sdk.utils.notNull
import com.wavesplatform.wallet.R
import com.wavesplatform.wallet.v2.ui.base.view.BaseActivity
import com.wavesplatform.wallet.v2.ui.keeper.KeeperTransactionActivity
import kotlinx.android.synthetic.main.activity_keeper_send_transaction.*
import pers.victor.ext.click
import pers.victor.ext.gone
import pers.victor.ext.visiable
import javax.inject.Inject

class KeeperSendTransactionActivity : BaseActivity(), KeeperSendTransactionView {

    @Inject
    @InjectPresenter
    lateinit var presenter: KeeperSendTransactionPresenter

    @ProvidePresenter
    fun providePresenter(): KeeperSendTransactionPresenter = presenter

    override fun configLayoutRes() = R.layout.activity_keeper_send_transaction

    override fun askPassCode() = true

    override fun needToShowNetworkMessage() = true

    override fun onCreate(savedInstanceState: Bundle?) {
        translucentStatusBar = true
        overridePendingTransition(R.anim.slide_in_right, R.anim.null_animation)
        super.onCreate(savedInstanceState)
    }

    override fun onViewReady(savedInstanceState: Bundle?) {
        image_loader.show()

        presenter.transaction = intent.getParcelableExtra(KeeperTransactionActivity.KEY_INTENT_TRANSACTION)
        when (presenter.transaction) {
            is TransferTransaction -> {
                presenter.receiveAssetDetails((presenter.transaction as TransferTransaction).assetId)
            }
            is DataTransaction, is InvokeScriptTransaction -> {
                transaction_view.setTransaction(presenter.transaction!!)
            }
            else -> {
                onError(Throwable("Wrong transaction type!"))
            }
        }
        presenter.sendTransaction(presenter.transaction!!)
    }

    override fun onBackPressed() {
        presenter.transactionResponse.notNull {
            success()
        }
    }

    override fun onSuccessSend(transaction: KeeperTransactionResponse) {
        presenter.transactionResponse = transaction
        card_progress.gone()
        card_success.visiable()
        image_loader.hide()
        button_okay.click {
            success()
        }
    }

    override fun onError(error: Throwable) {
        val errorMessage = error.localizedMessage
        val data = Intent()
        data.putExtra(KeeperTransactionActivity.KEY_INTENT_RESPONSE_ERROR, errorMessage)
        setResult(Activity.RESULT_CANCELED, data)
        finish()
    }

    override fun onReceiveAssetDetails(assetDetails: AssetsDetailsResponse) {
        transaction_view.setTransaction(presenter.transaction!!, assetDetails)
    }

    private fun success() {
        presenter.transactionResponse.notNull {
            val data = Intent()
            data.putExtra(KeeperTransactionActivity.KEY_INTENT_RESPONSE_TRANSACTION, it)
            setResult(Activity.RESULT_OK, data)
            finish()
        }
    }
}