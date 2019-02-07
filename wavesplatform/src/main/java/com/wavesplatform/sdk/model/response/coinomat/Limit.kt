package com.wavesplatform.sdk.model.response.coinomat

import com.google.gson.annotations.SerializedName

data class Limit(
        @SerializedName("min") var min: String? = null,
        @SerializedName("max") var max: String? = null)