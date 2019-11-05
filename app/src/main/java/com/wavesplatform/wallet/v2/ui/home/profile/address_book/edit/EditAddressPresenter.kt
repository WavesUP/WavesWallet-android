/*
 * Created by Eduard Zaydel on 1/4/2019
 * Copyright © 2019 Waves Platform. All rights reserved.
 */

package com.wavesplatform.wallet.v2.ui.home.profile.address_book.edit

import moxy.InjectViewState
import com.vicpin.krealmextensions.delete
import com.vicpin.krealmextensions.queryFirst
import com.vicpin.krealmextensions.save
import com.wavesplatform.wallet.v2.data.model.db.userdb.AddressBookUserDb
import com.wavesplatform.wallet.v2.ui.base.presenter.BasePresenter
import javax.inject.Inject

@InjectViewState
class EditAddressPresenter @Inject constructor() : BasePresenter<EditAddressView>() {
    var nameFieldValid = true
    var addressFieldValid = true
    var addressBookUser: AddressBookUserDb? = null

    fun isAllFieldsValid(): Boolean {
        return nameFieldValid && addressFieldValid
    }

    fun deleteAddress() {
        delete<AddressBookUserDb> { equalTo("address", addressBookUser?.address) }
        viewState.successDeleteAddress()
    }

    fun editAddress(address: String, name: String) {
        val oldAddress = queryFirst<AddressBookUserDb> { equalTo("address", addressBookUser?.address) }
        if (addressBookUser?.address == address) {
            oldAddress?.let {
                oldAddress.name = name
                oldAddress?.save()
                viewState.successEditAddress(oldAddress)
            }
        } else {
            delete<AddressBookUserDb> { equalTo("address", addressBookUser?.address) }
            val addressBookUser = AddressBookUserDb(address, name)
            addressBookUser.save()
            viewState.successEditAddress(addressBookUser)
        }
    }
}
