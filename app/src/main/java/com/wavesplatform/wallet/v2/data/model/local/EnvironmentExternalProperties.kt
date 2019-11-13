/*
 * Created by Eduard Zaydel on 13/6/2019
 * Copyright © 2019 Waves Platform. All rights reserved.
 */

package com.wavesplatform.wallet.v2.data.model.local

import androidx.annotation.StringRes

data class EnvironmentExternalProperties(
        @StringRes var environmentDisplayName: Int,
        var wavesEnterpriseNetCode: Char,
        var usdId: String,
        var eurId: String,
        var matcherAddress: String
)