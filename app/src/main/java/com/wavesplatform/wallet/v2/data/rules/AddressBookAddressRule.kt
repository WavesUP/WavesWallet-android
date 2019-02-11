package com.wavesplatform.wallet.v2.data.rules

import android.support.annotation.StringRes
import com.vicpin.krealmextensions.queryFirst
import com.wavesplatform.wallet.v2.data.model.db.AddressBookUserDb

import io.github.anderscheow.validator.rules.BaseRule

class AddressBookAddressRule : BaseRule {

    constructor() : super("Value must not be empty") {}

    constructor(@StringRes errorRes: Int) : super(errorRes) {}

    constructor(errorMessage: String) : super(errorMessage) {}

    override fun validate(value: Any?): Boolean {
        if (value == null) {
            throw NullPointerException()
        }

        if (value is String) {
            val user = queryFirst<AddressBookUserDb> { equalTo("address", value) }
            return user == null
        }

        throw ClassCastException("Required String value")
    }
}