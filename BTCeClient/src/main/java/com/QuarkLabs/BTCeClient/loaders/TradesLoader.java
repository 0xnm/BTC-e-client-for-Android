package com.QuarkLabs.BTCeClient.loaders;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.support.annotation.NonNull;

import com.QuarkLabs.BTCeClient.BtcEApplication;
import com.QuarkLabs.BTCeClient.api.CallResult;
import com.QuarkLabs.BTCeClient.api.TradeHistoryEntry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TradesLoader extends AsyncTaskLoader<CallResult<List<TradeHistoryEntry>>> {

    private CallResult<List<TradeHistoryEntry>> callResult;
    private final Context appContext;
    @NonNull
    private final String fromDate;
    @NonNull
    private final String toDate;

    public TradesLoader(@NonNull Context context, @NonNull String fromDate,
                        @NonNull String toDate) {
        super(context);
        appContext = context.getApplicationContext();
        this.fromDate = fromDate;
        this.toDate = toDate;
    }

    @Override
    public CallResult<List<TradeHistoryEntry>> loadInBackground() {
        Map<String, String> parameters = new HashMap<>();
        //TODO should be fixed
        parameters.put("since", "0");
        parameters.put("end", toDate);
        callResult = BtcEApplication.get(appContext).getApi().getTradeHistory(parameters);
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
