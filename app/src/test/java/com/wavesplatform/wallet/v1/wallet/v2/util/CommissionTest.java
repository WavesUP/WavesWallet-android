package com.wavesplatform.wallet.v1.wallet.v2.util;

import com.wavesplatform.sdk.model.request.node.BaseTransaction;
import com.wavesplatform.wallet.v2.data.model.service.configs.GlobalTransactionCommissionResponse;
import com.wavesplatform.wallet.v2.util.TransactionCommissionUtil;

import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class CommissionTest {

    @Test
    public void checkCommissions() {
        GlobalTransactionCommissionResponse commission = new GlobalTransactionCommissionResponse();

        GlobalTransactionCommissionResponse.ParamsResponse params = new GlobalTransactionCommissionResponse.ParamsResponse();
        params.setTransactionType(BaseTransaction.TRANSFER);
        params.setSmartAccount(true);
        params.setSmartAsset(false);
        assertEquals(500000L, TransactionCommissionUtil.Companion.countCommission(commission, params));

        params = new GlobalTransactionCommissionResponse.ParamsResponse();
        params.setTransactionType(BaseTransaction.TRANSFER);
        params.setSmartAccount(false);
        params.setSmartAsset(false);
        assertEquals(100000L, TransactionCommissionUtil.Companion.countCommission(commission, params));

        params = new GlobalTransactionCommissionResponse.ParamsResponse();
        params.setTransactionType(BaseTransaction.TRANSFER);
        params.setSmartAccount(true);
        params.setSmartAsset(true);
        assertEquals(900000L, TransactionCommissionUtil.Companion.countCommission(commission, params));

        params = new GlobalTransactionCommissionResponse.ParamsResponse();
        params.setTransactionType(BaseTransaction.ISSUE);
        params.setSmartAccount(false);
        assertEquals(100000000L, TransactionCommissionUtil.Companion.countCommission(commission, params));

        params = new GlobalTransactionCommissionResponse.ParamsResponse();
        params.setTransactionType(BaseTransaction.ISSUE);
        params.setSmartAccount(true);
        assertEquals(100400000L, TransactionCommissionUtil.Companion.countCommission(commission, params));

        params = new GlobalTransactionCommissionResponse.ParamsResponse();
        params.setTransactionType(BaseTransaction.REISSUE);
        params.setSmartAccount(true);
        assertEquals(100400000L, TransactionCommissionUtil.Companion.countCommission(commission, params));

        params = new GlobalTransactionCommissionResponse.ParamsResponse();
        params.setTransactionType(BaseTransaction.REISSUE);
        params.setSmartAccount(false);
        assertEquals(100000000L, TransactionCommissionUtil.Companion.countCommission(commission, params));

        params = new GlobalTransactionCommissionResponse.ParamsResponse();
        params.setTransactionType(BaseTransaction.BURN);
        params.setSmartAccount(false);
        params.setSmartAsset(false);
        assertEquals(100000L, TransactionCommissionUtil.Companion.countCommission(commission, params));

        params = new GlobalTransactionCommissionResponse.ParamsResponse();
        params.setTransactionType(BaseTransaction.BURN);
        params.setSmartAccount(true);
        params.setSmartAsset(false);
        assertEquals(500000L, TransactionCommissionUtil.Companion.countCommission(commission, params));

        params = new GlobalTransactionCommissionResponse.ParamsResponse();
        params.setTransactionType(BaseTransaction.BURN);
        params.setSmartAccount(true);
        params.setSmartAsset(true);
        assertEquals(900000L, TransactionCommissionUtil.Companion.countCommission(commission, params));

        params = new GlobalTransactionCommissionResponse.ParamsResponse();
        params.setTransactionType(BaseTransaction.CREATE_LEASING);
        params.setSmartAccount(false);
        params.setSmartAsset(true);
        assertEquals(100000L, TransactionCommissionUtil.Companion.countCommission(commission, params));

        params = new GlobalTransactionCommissionResponse.ParamsResponse();
        params.setTransactionType(BaseTransaction.CREATE_LEASING);
        params.setSmartAccount(true);
        params.setSmartAsset(true);
        assertEquals(500000L, TransactionCommissionUtil.Companion.countCommission(commission, params));

        params = new GlobalTransactionCommissionResponse.ParamsResponse();
        params.setTransactionType(BaseTransaction.CANCEL_LEASING);
        params.setSmartAccount(false);
        params.setSmartAsset(true);
        assertEquals(100000L, TransactionCommissionUtil.Companion.countCommission(commission, params));

        params = new GlobalTransactionCommissionResponse.ParamsResponse();
        params.setTransactionType(BaseTransaction.CANCEL_LEASING);
        params.setSmartAccount(true);
        params.setSmartAsset(true);
        assertEquals(500000L, TransactionCommissionUtil.Companion.countCommission(commission, params));

        params = new GlobalTransactionCommissionResponse.ParamsResponse();
        params.setTransactionType(BaseTransaction.CREATE_ALIAS);
        params.setSmartAccount(false);
        params.setSmartAsset(true);
        assertEquals(100000L, TransactionCommissionUtil.Companion.countCommission(commission, params));

        params = new GlobalTransactionCommissionResponse.ParamsResponse();
        params.setTransactionType(BaseTransaction.CREATE_ALIAS);
        params.setSmartAccount(true);
        params.setSmartAsset(true);
        assertEquals(500000L, TransactionCommissionUtil.Companion.countCommission(commission, params));

        params = new GlobalTransactionCommissionResponse.ParamsResponse();
        params.setTransactionType(BaseTransaction.MASS_TRANSFER);
        params.setSmartAccount(false);
        params.setSmartAsset(false);
        params.setTransfersCount(1);
        assertEquals(200000L, TransactionCommissionUtil.Companion.countCommission(commission, params));

        params = new GlobalTransactionCommissionResponse.ParamsResponse();
        params.setTransactionType(BaseTransaction.MASS_TRANSFER);
        params.setSmartAccount(false);
        params.setSmartAsset(true);
        params.setTransfersCount(1);
        assertEquals(600000L, TransactionCommissionUtil.Companion.countCommission(commission, params));

        params = new GlobalTransactionCommissionResponse.ParamsResponse();
        params.setTransactionType(BaseTransaction.MASS_TRANSFER);
        params.setSmartAccount(true);
        params.setSmartAsset(true);
        params.setTransfersCount(1);
        assertEquals(1000000L, TransactionCommissionUtil.Companion.countCommission(commission, params));

        params = new GlobalTransactionCommissionResponse.ParamsResponse();
        params.setTransactionType(BaseTransaction.MASS_TRANSFER);
        params.setSmartAccount(false);
        params.setSmartAsset(false);
        params.setTransfersCount(2);
        assertEquals(200000L, TransactionCommissionUtil.Companion.countCommission(commission, params));

        params = new GlobalTransactionCommissionResponse.ParamsResponse();
        params.setTransactionType(BaseTransaction.MASS_TRANSFER);
        params.setSmartAccount(false);
        params.setSmartAsset(true);
        params.setTransfersCount(2);
        assertEquals(600000L, TransactionCommissionUtil.Companion.countCommission(commission, params));

        params = new GlobalTransactionCommissionResponse.ParamsResponse();
        params.setTransactionType(BaseTransaction.MASS_TRANSFER);
        params.setSmartAccount(true);
        params.setSmartAsset(true);
        params.setTransfersCount(2);
        assertEquals(1000000L, TransactionCommissionUtil.Companion.countCommission(commission, params));

        params = new GlobalTransactionCommissionResponse.ParamsResponse();
        params.setTransactionType(BaseTransaction.MASS_TRANSFER);
        params.setSmartAccount(false);
        params.setSmartAsset(false);
        params.setTransfersCount(3);
        assertEquals(300000L, TransactionCommissionUtil.Companion.countCommission(commission, params));

        params = new GlobalTransactionCommissionResponse.ParamsResponse();
        params.setTransactionType(BaseTransaction.MASS_TRANSFER);
        params.setSmartAccount(false);
        params.setSmartAsset(true);
        params.setTransfersCount(3);
        assertEquals(700000L, TransactionCommissionUtil.Companion.countCommission(commission, params));

        params = new GlobalTransactionCommissionResponse.ParamsResponse();
        params.setTransactionType(BaseTransaction.MASS_TRANSFER);
        params.setSmartAccount(true);
        params.setSmartAsset(true);
        params.setTransfersCount(3);
        assertEquals(1100000L, TransactionCommissionUtil.Companion.countCommission(commission, params));

        params = new GlobalTransactionCommissionResponse.ParamsResponse();
        params.setTransactionType(BaseTransaction.DATA);
        params.setSmartAccount(false);
        params.setSmartAsset(true);
        params.setBytesCount(1025);
        assertEquals(200000L, TransactionCommissionUtil.Companion.countCommission(commission, params));

        params = new GlobalTransactionCommissionResponse.ParamsResponse();
        params.setTransactionType(BaseTransaction.DATA);
        params.setSmartAccount(true);
        params.setSmartAsset(true);
        params.setBytesCount(2049);
        assertEquals(700000L, TransactionCommissionUtil.Companion.countCommission(commission, params));

        params = new GlobalTransactionCommissionResponse.ParamsResponse();
        params.setTransactionType(BaseTransaction.ADDRESS_SCRIPT);
        params.setSmartAccount(false);
        params.setSmartAsset(true);
        assertEquals(1000000L, TransactionCommissionUtil.Companion.countCommission(commission, params));

        params = new GlobalTransactionCommissionResponse.ParamsResponse();
        params.setTransactionType(BaseTransaction.ADDRESS_SCRIPT);
        params.setSmartAccount(true);
        params.setSmartAsset(true);
        assertEquals(1400000L, TransactionCommissionUtil.Companion.countCommission(commission, params));

        params = new GlobalTransactionCommissionResponse.ParamsResponse();
        params.setTransactionType(BaseTransaction.SPONSORSHIP);
        params.setSmartAccount(false);
        params.setSmartAsset(true);
        assertEquals(100000000L, TransactionCommissionUtil.Companion.countCommission(commission, params));

        params = new GlobalTransactionCommissionResponse.ParamsResponse();
        params.setTransactionType(BaseTransaction.SPONSORSHIP);
        params.setSmartAccount(true);
        params.setSmartAsset(true);
        assertEquals(100400000L, TransactionCommissionUtil.Companion.countCommission(commission, params));

        params = new GlobalTransactionCommissionResponse.ParamsResponse();
        params.setTransactionType(BaseTransaction.ASSET_SCRIPT);
        params.setSmartAccount(false);
        params.setSmartAsset(true);
        assertEquals(100000000L, TransactionCommissionUtil.Companion.countCommission(commission, params));

        params = new GlobalTransactionCommissionResponse.ParamsResponse();
        params.setTransactionType(BaseTransaction.ASSET_SCRIPT);
        params.setSmartAccount(true);
        params.setSmartAsset(true);
        assertEquals(100400000L, TransactionCommissionUtil.Companion.countCommission(commission, params));
    }
}
