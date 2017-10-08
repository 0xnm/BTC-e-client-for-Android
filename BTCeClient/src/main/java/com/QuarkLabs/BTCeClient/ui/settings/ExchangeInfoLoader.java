package com.QuarkLabs.BTCeClient.ui.settings;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.support.annotation.Nullable;

import com.QuarkLabs.BTCeClient.BtcEApplication;
import com.QuarkLabs.BTCeClient.api.CallResult;
import com.QuarkLabs.BTCeClient.api.ExchangeInfo;

public class ExchangeInfoLoader extends AsyncTaskLoader<CallResult<ExchangeInfo>> {
    @Nullable
    private CallResult<ExchangeInfo> callResult;

    public ExchangeInfoLoader(Context context) {
        super(context);
    }

    @Override
    public CallResult<ExchangeInfo> loadInBackground() {
        callResult = BtcEApplication.get(getContext()).getApi().getExchangeInfo();
        return callResult;
    }

    @Override
    protected void onStartLoading() {
        if (callResult != null) {
            deliverResult(callResult);
        }
        if (takeContentChanged() || callResult == null) {
            forceLoad();
        }
    }
}
