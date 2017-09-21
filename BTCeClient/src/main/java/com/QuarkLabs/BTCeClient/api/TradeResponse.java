package com.QuarkLabs.BTCeClient.api;

import android.support.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

public class TradeResponse {
    private double received;
    private double remains;
    private long orderId;
    private Map<String, Double> funds;

    private TradeResponse() { }

    public double getReceived() {
        return received;
    }

    public double getRemains() {
        return remains;
    }

    public long getOrderId() {
        return orderId;
    }

    public Map<String, Double> getFunds() {
        return funds;
    }

    public static TradeResponse create(@NonNull JSONObject jsonObject) throws JSONException {
        TradeResponse tradeResponse = new TradeResponse();
        tradeResponse.received = jsonObject.getDouble("received");
        tradeResponse.remains = jsonObject.getDouble("remains");
        tradeResponse.orderId = jsonObject.getLong("order_id");
        Map<String, Double> funds = new HashMap<>();
        Iterator<String> fundsIterator = jsonObject.getJSONObject("funds").keys();
        while (fundsIterator.hasNext()) {
            String fund = fundsIterator.next();
            funds.put(fund.toUpperCase(Locale.US),
                    jsonObject.getJSONObject("funds").getDouble(fund));
        }
        tradeResponse.funds = funds;
        return tradeResponse;
    }
}
