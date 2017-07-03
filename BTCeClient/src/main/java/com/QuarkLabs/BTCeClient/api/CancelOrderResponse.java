package com.QuarkLabs.BTCeClient.api;

import android.support.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


public class CancelOrderResponse {

    @NonNull
    private long orderId;
    @NonNull
    private Map<String, Double> funds;

    private CancelOrderResponse() { }

    @NonNull
    public long getOrderId() {
        return orderId;
    }

    @NonNull
    public Map<String, Double> getFunds() {
        return funds;
    }

    public static CancelOrderResponse create(@NonNull JSONObject payload) throws JSONException {
        CancelOrderResponse result = new CancelOrderResponse();

        result.orderId = payload.getLong("order_id");
        JSONObject fundsJson = payload.getJSONObject("funds");
        Iterator<String> currenciesIterator = fundsJson.keys();
        Map<String, Double> funds = new HashMap<>();
        while (currenciesIterator.hasNext()) {
            String currency = currenciesIterator.next();
            funds.put(currency, fundsJson.getDouble(currency));
        }
        result.funds = funds;
        return result;
    }

}
