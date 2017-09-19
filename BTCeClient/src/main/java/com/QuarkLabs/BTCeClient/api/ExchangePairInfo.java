package com.QuarkLabs.BTCeClient.api;

import android.support.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

public class ExchangePairInfo {
    private String pair;
    private int decimalPlaces;
    private double minPrice;
    private double maxPrice;
    private double minAmount;
    private boolean hidden;
    private double fee;

    private ExchangePairInfo() { }

    public String getPair() {
        return pair;
    }

    public int getDecimalPlaces() {
        return decimalPlaces;
    }

    public double getMinPrice() {
        return minPrice;
    }

    public double getMaxPrice() {
        return maxPrice;
    }

    public double getMinAmount() {
        return minAmount;
    }

    public boolean isHidden() {
        return hidden;
    }

    public double getFee() {
        return fee;
    }

    public static ExchangePairInfo create(@NonNull String pair,
                                          @NonNull JSONObject jsonObject) throws JSONException {
        ExchangePairInfo pairInfo = new ExchangePairInfo();
        pairInfo.pair = pair;
        pairInfo.decimalPlaces = jsonObject.getInt("decimal_places");
        pairInfo.minPrice = jsonObject.getDouble("min_price");
        pairInfo.maxPrice = jsonObject.getDouble("max_price");
        pairInfo.decimalPlaces = jsonObject.getInt("decimal_places");
        pairInfo.minAmount = jsonObject.getDouble("min_amount");
        pairInfo.hidden = jsonObject.getInt("hidden") == 1;
        pairInfo.fee = jsonObject.getDouble("fee");
        return pairInfo;
    }
}
