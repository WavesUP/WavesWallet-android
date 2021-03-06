/*
 * Created by Eduard Zaydel on 8/8/2019
 * Copyright © 2019 Waves Platform. All rights reserved.
 */

package com.wavesplatform.wallet.v2.data.model.local.widget

import android.content.Context
import com.wavesplatform.wallet.v2.data.Constants
import com.wavesplatform.wallet.v2.ui.widget.MarketPulseAppWidgetProvider

enum class MarketWidgetCurrency(var symbol: String) {
    USD(Constants.Fiat.USD_SYMBOL),
    EUR(Constants.Fiat.EUR_SYMBOL);

    companion object {
        private const val PREF_CURRENCY_KEY = "appwidget_currency_"

        fun getCurrency(context: Context, appWidgetId: Int): MarketWidgetCurrency {
            val prefs = context.getSharedPreferences(MarketPulseAppWidgetProvider.PREFS_NAME, 0)
            val currency = prefs.getString(PREF_CURRENCY_KEY + appWidgetId, null)
            return values().firstOrNull { it.name == currency } ?: USD
        }

        fun switchCurrency(context: Context, appWidgetId: Int) {
            val inverseCurrency =
                    when (getCurrency(context, appWidgetId)) {
                        USD -> EUR
                        EUR -> USD
                    }
            setCurrency(context, appWidgetId, inverseCurrency)
        }

        fun setCurrency(context: Context, appWidgetId: Int, currency: MarketWidgetCurrency) {
            val prefs = context.getSharedPreferences(MarketPulseAppWidgetProvider.PREFS_NAME, 0).edit()
            prefs.putString(PREF_CURRENCY_KEY + appWidgetId, currency.name)
            prefs.apply()
        }

        fun removeCurrency(context: Context, appWidgetId: Int) {
            val prefs = context.getSharedPreferences(MarketPulseAppWidgetProvider.PREFS_NAME, 0).edit()
            prefs.remove(PREF_CURRENCY_KEY + appWidgetId)
            prefs.apply()
        }
    }
}