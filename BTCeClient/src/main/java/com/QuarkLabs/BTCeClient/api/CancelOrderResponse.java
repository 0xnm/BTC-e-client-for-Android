package com.QuarkLabs.BTCeClient.api;

import android.support.annotation.NonNull;

import com.google.gson.JsonObject;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class CancelOrderResponse {

    private long orderId;
    @NonNull
    private Map<String, BigDecimal> funds;

    private CancelOrderResponse() { }

    public long getOrderId() {
        return orderId;
    }

    @NonNull
    public Map<String, BigDecimal> getFunds() {
        return funds;
    }

    @NonNull
    public static CancelOrderResponse create(@NonNull JsonObject payload) {
        CancelOrderResponse result = new CancelOrderResponse();

        result.orderId = payload.get("order_id").getAsLong();
        JsonObject fundsJson = payload.getAsJsonObject("funds");
        Set<String> currencies = fundsJson.keySet();
        Map<String, BigDecimal> funds = new HashMap<>();
        for (String currency : currencies) {
            funds.put(currency.toUpperCase(Locale.US),
                    fundsJson.get(currency).getAsBigDecimal().stripTrailingZeros());
        }
        result.funds = funds;
        return result;
    }

}
