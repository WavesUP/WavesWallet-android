/*
 * Created by Eduard Zaydel on 25/6/2019
 * Copyright © 2019 Waves Platform. All rights reserved.
 */

package com.wavesplatform.wallet.v2.data.model.local.gateway.coinomat

import com.wavesplatform.wallet.v2.data.model.local.gateway.base.GatewayDepositModel

data class CoinomatDepositArgs(var assetId: String) : GatewayDepositModel