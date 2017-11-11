package com.QuarkLabs.BTCeClient.tasks;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.widget.Toast;

import com.QuarkLabs.BTCeClient.BtcEApplication;
import com.QuarkLabs.BTCeClient.ConstantHolder;
import com.QuarkLabs.BTCeClient.R;
import com.QuarkLabs.BTCeClient.api.Api;
import com.QuarkLabs.BTCeClient.api.CallResult;
import com.QuarkLabs.BTCeClient.api.TradeResponse;
import com.QuarkLabs.BTCeClient.ui.terminal.HomeFragment;
import com.QuarkLabs.BTCeClient.utils.ContextUtils;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Map;

/**
 * AsyncTask to register trade request on the exchange
 */
public class RegisterTradeRequestTask extends AsyncTask<TradeRequest, Void,
        CallResult<TradeResponse>> implements UnregistrableTask {

    @SuppressLint("StaticFieldLeak")
    @NonNull
    private final Context appContext;
    @NonNull
    private final Api api;
    @Nullable
    private ApiResultListener<TradeResponse> resultListener;

    public RegisterTradeRequestTask(@NonNull Context context,
                                    @NonNull Api api,
                                    @NonNull ApiResultListener<TradeResponse> resultListener) {
        this.appContext = context.getApplicationContext();
        this.api = api;
        this.resultListener = resultListener;
    }

    @Override
    protected CallResult<TradeResponse> doInBackground(TradeRequest... params) {
        TradeRequest tradeRequest = params[0];
        String tradeAction = tradeRequest.getType();
        String pair = tradeRequest.getTradeCurrency().toLowerCase(Locale.US)
                + "_" + tradeRequest.getTradePriceCurrency().toLowerCase(Locale.US);
        return api.trade(pair, tradeAction, tradeRequest.getTradePrice(),
                tradeRequest.getTradeAmount());
    }

    @Override
    protected void onPostExecute(@NonNull CallResult<TradeResponse> callResult) {
        String message;
        if (callResult.isSuccess()) {
            message = appContext.getString(R.string.order_successfully_added);
            //noinspection ConstantConditions
            Map<String, BigDecimal> funds = callResult.getPayload().getFunds();
            BtcEApplication.get(appContext).getInMemoryStorage().setFunds(funds);
            if (resultListener != null) {
                resultListener.onSuccess(callResult.getPayload());
            }
        } else {
            message = callResult.getError();
            if (resultListener != null) {
                //noinspection ConstantConditions
                resultListener.onError(message);
            }
        }
        //noinspection ConstantConditions
        ContextUtils.makeNotification(appContext,
                ConstantHolder.TRADE_REGISTERED_NOTIF_ID, message);
    }

    @Override
    public void unregisterListener() {
        resultListener = null;
    }
}
