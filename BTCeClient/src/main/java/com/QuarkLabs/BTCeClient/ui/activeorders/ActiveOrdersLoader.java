package com.QuarkLabs.BTCeClient.ui.activeorders;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.support.annotation.Nullable;

import com.QuarkLabs.BTCeClient.BtcEApplication;
import com.QuarkLabs.BTCeClient.api.ActiveOrder;
import com.QuarkLabs.BTCeClient.api.Api;
import com.QuarkLabs.BTCeClient.api.CallResult;

import java.util.List;

public class ActiveOrdersLoader extends AsyncTaskLoader<CallResult<List<ActiveOrder>>> {

    @Nullable
    private CallResult<List<ActiveOrder>> callResult;
    private final Api api;

    public ActiveOrdersLoader(Context context) {
        super(context);
        api = BtcEApplication.get(context).getApi();
    }

    @Override
    public CallResult<List<ActiveOrder>> loadInBackground() {
        callResult = api.getActiveOrders();
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
