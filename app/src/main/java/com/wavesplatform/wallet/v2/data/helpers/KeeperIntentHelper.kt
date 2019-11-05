/*
 * Created by Eduard Zaydel on 30/8/2019
 * Copyright © 2019 Waves Platform. All rights reserved.
 */

package com.wavesplatform.wallet.v2.data.helpers

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import com.wavesplatform.sdk.keeper.interfaces.Keeper
import com.wavesplatform.sdk.keeper.model.KeeperIntentResult
import com.wavesplatform.wallet.v2.ui.auth.choose_account.ChooseAccountActivity
import com.wavesplatform.wallet.v2.util.launchActivity

object KeeperIntentHelper {
    private const val BUNDLE_KEEPER_RESULT = "key_intent_keeper_result"

    fun exitToRootWithResult(activity: Activity, keeperIntentResult: KeeperIntentResult) {
        activity.launchActivity<ChooseAccountActivity> {
            val bundle = Bundle().apply {
                putParcelable(BUNDLE_KEEPER_RESULT, keeperIntentResult)
            }
            putExtras(bundle)
        }
    }

    fun parseIntentResult(intent: Intent): KeeperIntentResult? {
        return if (intent.hasExtra(BUNDLE_KEEPER_RESULT)) {
            intent.getParcelableExtra(BUNDLE_KEEPER_RESULT)
        } else {
            null
        }
    }

    fun exitToDAppWithResult(activity: FragmentActivity,
                             keeperIntentResult: KeeperIntentResult,
                             keeper: Keeper) {
        keeper.finishProcess(activity, keeperIntentResult)
    }
}