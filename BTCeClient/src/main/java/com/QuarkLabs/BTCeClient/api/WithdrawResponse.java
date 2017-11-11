package com.QuarkLabs.BTCeClient.api;

import android.support.annotation.NonNull;

import com.google.gson.JsonObject;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class WithdrawResponse {

    private long tId;
    private BigDecimal amountSent;
    private Map<String, BigDecimal> funds;

    private WithdrawResponse() { }

    public long getTransactionId() {
        return tId;
    }

    public BigDecimal getAmountSent() {
        return amountSent;
    }

    public Map<String, BigDecimal> getFunds() {
        return funds;
    }

    @NonNull
    public static WithdrawResponse create(@NonNull JsonObject json) {
        WithdrawResponse response = new WithdrawResponse();

        response.tId = json.getAsJsonPrimitive("tId").getAsLong();
        response.amountSent = json.getAsJsonPrimitive("amountSent").getAsBigDecimal();

        Map<String, BigDecimal> funds = new HashMap<>();
        Set<String> currencies = json.getAsJsonObject("funds").keySet();
        for (String currency : currencies) {
            funds.put(currency.toUpperCase(Locale.US),
                    json.getAsJsonObject("funds").get(currency)
                            .getAsBigDecimal().stripTrailingZeros());
        }
        response.funds = funds;
        return response;
    }
}
