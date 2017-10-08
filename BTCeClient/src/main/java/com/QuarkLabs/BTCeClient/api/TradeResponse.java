package com.QuarkLabs.BTCeClient.api;

import android.support.annotation.NonNull;

import com.google.gson.JsonObject;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class TradeResponse {
    private BigDecimal received;
    private BigDecimal remains;
    private long orderId;
    private Map<String, BigDecimal> funds;

    private TradeResponse() { }

    public BigDecimal getReceived() {
        return received;
    }

    public BigDecimal getRemains() {
        return remains;
    }

    public long getOrderId() {
        return orderId;
    }

    public Map<String, BigDecimal> getFunds() {
        return funds;
    }

    @NonNull
    public static TradeResponse create(@NonNull JsonObject jsonObject) {
        TradeResponse tradeResponse = new TradeResponse();
        tradeResponse.received = jsonObject.get("received").getAsBigDecimal().stripTrailingZeros();
        tradeResponse.remains = jsonObject.get("remains").getAsBigDecimal().stripTrailingZeros();
        tradeResponse.orderId = jsonObject.get("order_id").getAsLong();
        Map<String, BigDecimal> funds = new HashMap<>();
        Set<String> currencies = jsonObject.getAsJsonObject("funds").keySet();
        for (String currency : currencies) {
            funds.put(currency.toUpperCase(Locale.US),
                    jsonObject.getAsJsonObject("funds").get(currency)
                            .getAsBigDecimal().stripTrailingZeros());
        }
        tradeResponse.funds = funds;
        return tradeResponse;
    }
}
