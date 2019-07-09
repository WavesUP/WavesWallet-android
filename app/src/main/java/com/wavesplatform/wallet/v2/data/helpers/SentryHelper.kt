/*
 * Created by Eduard Zaydel on 1/4/2019
 * Copyright © 2019 Waves Platform. All rights reserved.
 */

package com.wavesplatform.wallet.v2.data.helpers

import com.wavesplatform.sdk.net.NetworkException
import com.wavesplatform.wallet.BuildConfig
import com.wavesplatform.wallet.v2.data.model.local.NetworkType
import com.wavesplatform.wallet.v2.util.clone
import io.sentry.Sentry
import io.sentry.event.Event
import io.sentry.event.EventBuilder
import io.sentry.event.interfaces.ExceptionInterface
import pers.victor.ext.app
import pyxis.uzuki.live.richutilskt.utils.checkNetwork
import java.util.*

class SentryHelper {

    companion object {
        private const val NOT_FOUND_HTTP_CODE = 404
        private const val TAG_HTTP_CODE = "http.error"
        private const val TAG_NETWORK_TYPE = "network.type"

        fun logException(exception: Exception) {
            if (exception is NetworkException) {
                if (exception.kind == NetworkException.Kind.NETWORK || exception.response?.code() == NOT_FOUND_HTTP_CODE) {
                    // ignore this events to make less spam to Sentry
                } else {
                    Sentry.capture(EventBuilder()
                            .withTimestamp(Date())
                            .withTag(TAG_HTTP_CODE, exception.response?.code()?.toString())
                            .withTag(TAG_NETWORK_TYPE, NetworkType.getByType(app.checkNetwork())?.typeName)
                            .withLevel(Event.Level.ERROR)
                            .withSentryInterface(ExceptionInterface(exception))
                            .withRelease(BuildConfig.VERSION_NAME)
                            .withMessage(formatSentryMessage(exception)))
                }
            } else {
                Sentry.capture(exception)
            }
        }

        private fun formatSentryMessage(retrofitException: NetworkException): String {
            return "Error: ${retrofitException.kind.name}\n" +
                    "Url: ${retrofitException.url}\n" +
                    "Code: ${retrofitException.response?.code()}\n" +
                    "Response: ${retrofitException.response?.errorBody()?.clone()?.string()}"
        }
    }
}