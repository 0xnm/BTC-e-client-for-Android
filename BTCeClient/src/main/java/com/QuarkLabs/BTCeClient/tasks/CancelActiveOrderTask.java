package com.QuarkLabs.BTCeClient.tasks;

import android.os.AsyncTask;
import android.support.annotation.NonNull;

import com.QuarkLabs.BTCeClient.api.Api;
import com.QuarkLabs.BTCeClient.api.CallResult;
import com.QuarkLabs.BTCeClient.api.CancelOrderResponse;

/**
 * AsyncTask class to cancel active order. Input: order IDs.
 */
public class CancelActiveOrderTask extends AsyncTask<Long, Void,
        CallResult<CancelOrderResponse>> {

    private final Api api;
    private final ApiResultListener<CancelOrderResponse> resultListener;

    public CancelActiveOrderTask(@NonNull Api api,
                                 @NonNull ApiResultListener<CancelOrderResponse> resultListener) {
        this.api = api;
        this.resultListener = resultListener;
    }

    @Override
    protected CallResult<CancelOrderResponse> doInBackground(Long... params) {
        long orderId = params[0];
        return api.cancelOrder(orderId);
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    protected void onPostExecute(CallResult<CancelOrderResponse> result) {
        if (result.isSuccess()) {
            resultListener.onSuccess(result.getPayload());
        } else {
            resultListener.onError(result.getError());
        }
    }
}