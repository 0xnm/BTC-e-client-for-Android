package com.QuarkLabs.BTCeClient.loaders;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.support.annotation.Nullable;

import com.QuarkLabs.BTCeClient.BtcEApplication;
import com.QuarkLabs.BTCeClient.api.ActiveOrder;
import com.QuarkLabs.BTCeClient.api.CallResult;

import java.util.List;

public class ActiveOrdersLoader extends AsyncTaskLoader<CallResult<List<ActiveOrder>>> {

    @Nullable
    private CallResult<List<ActiveOrder>> callResult;
    private final Context appContext;

    public ActiveOrdersLoader(Context context) {
        super(context);
        appContext = context.getApplicationContext();
    }

    @Override
    public CallResult<List<ActiveOrder>> loadInBackground() {
        callResult = BtcEApplication.get(appContext).getApi().getActiveOrders();
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
