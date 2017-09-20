package com.QuarkLabs.BTCeClient.tasks;

import android.os.AsyncTask;
import android.support.annotation.NonNull;

import com.QuarkLabs.BTCeClient.api.Api;
import com.QuarkLabs.BTCeClient.api.CallResult;
import com.QuarkLabs.BTCeClient.api.ExchangeInfo;

public class GetExchangeInfoTask extends AsyncTask<Void, Void, CallResult<ExchangeInfo>> {

    @NonNull
    private final Api api;
    @NonNull
    private final ApiResultListener<ExchangeInfo> resultListener;

    public GetExchangeInfoTask(@NonNull Api api,
                               @NonNull ApiResultListener<ExchangeInfo> resultListener) {
        this.api = api;
        this.resultListener = resultListener;
    }

    @Override
    protected CallResult<ExchangeInfo> doInBackground(Void... params) {
        return api.getExchangeInfo();
    }

    @Override
    protected void onPostExecute(CallResult<ExchangeInfo> result) {
        if (result.isSuccess()) {
            //noinspection ConstantConditions
            resultListener.onSuccess(result.getPayload());
        } else {
            //noinspection ConstantConditions
            resultListener.onError(result.getError());
        }
    }
}
