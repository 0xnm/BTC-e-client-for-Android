package com.QuarkLabs.BTCeClient.tasks;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.QuarkLabs.BTCeClient.BtcEApplication;
import com.QuarkLabs.BTCeClient.ConstantHolder;
import com.QuarkLabs.BTCeClient.R;
import com.QuarkLabs.BTCeClient.api.AccountInfo;
import com.QuarkLabs.BTCeClient.api.Api;
import com.QuarkLabs.BTCeClient.api.CallResult;
import com.QuarkLabs.BTCeClient.utils.ContextUtils;

import java.math.BigDecimal;
import java.util.Map;

/**
 * AsyncTask to update funds
 */
public class UpdateFundsTask extends AsyncTask<Void, Void, CallResult<AccountInfo>>
        implements UnregistrableTask {

    @SuppressLint("StaticFieldLeak")
    @NonNull
    private final Context appContext;
    @NonNull
    private final Api api;
    @Nullable
    private ApiResultListener<AccountInfo> resultListener;

    public UpdateFundsTask(@NonNull Context appContext,
                           @NonNull Api api,
                           @NonNull ApiResultListener<AccountInfo> resultListener) {
        this.appContext = appContext;
        this.api = api;
        this.resultListener = resultListener;
    }

    @Override
    protected CallResult<AccountInfo> doInBackground(Void... params) {
        return api.getAccountInfo();
    }

    @Override
    protected void onPostExecute(CallResult<AccountInfo> result) {
        String notificationText;
        if (result.isSuccess()) {
            notificationText = appContext.getString(R.string.FundsInfoUpdatedtext);
            //noinspection ConstantConditions
            Map<String, BigDecimal> funds = result.getPayload().getFunds();
            BtcEApplication.get(appContext).getInMemoryStorage().setFunds(funds);
            if (resultListener != null) {
                resultListener.onSuccess(result.getPayload());
            }
        } else {
            notificationText = result.getError();
            if (resultListener != null) {
                //noinspection ConstantConditions
                resultListener.onError(result.getError());
            }
        }
        //noinspection ConstantConditions
        ContextUtils.makeNotification(appContext, ConstantHolder.ACCOUNT_INFO_NOTIF_ID,
                notificationText);
    }

    @Override
    public void unregisterListener() {
        resultListener = null;
    }
}
