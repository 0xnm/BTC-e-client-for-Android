package com.QuarkLabs.BTCeClient.api;

import android.support.annotation.NonNull;

import com.QuarkLabs.BTCeClient.utils.PairUtils;
import com.google.gson.JsonObject;

import java.math.BigDecimal;

public class ExchangePairInfo {
    private String pair;
    private int decimalPlaces;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private BigDecimal minAmount;
    private boolean hidden;
    private BigDecimal fee;

    private ExchangePairInfo() { }

    public String getPair() {
        return pair;
    }

    public int getDecimalPlaces() {
        return decimalPlaces;
    }

    public BigDecimal getMinPrice() {
        return minPrice;
    }

    public BigDecimal getMaxPrice() {
        return maxPrice;
    }

    public BigDecimal getMinAmount() {
        return minAmount;
    }

    public boolean isHidden() {
        return hidden;
    }

    public BigDecimal getFee() {
        return fee;
    }

    @NonNull
    public static ExchangePairInfo create(@NonNull String pair,
                                          @NonNull JsonObject jsonObject) {
        ExchangePairInfo pairInfo = new ExchangePairInfo();
        pairInfo.pair = PairUtils.serverToLocal(pair);
        pairInfo.decimalPlaces = jsonObject.get("decimal_places").getAsInt();
        pairInfo.minPrice = jsonObject.get("min_price").getAsBigDecimal().stripTrailingZeros();
        pairInfo.maxPrice = jsonObject.get("max_price").getAsBigDecimal().stripTrailingZeros();
        pairInfo.decimalPlaces = jsonObject.get("decimal_places").getAsInt();
        pairInfo.minAmount = jsonObject.get("min_amount").getAsBigDecimal().stripTrailingZeros();
        pairInfo.hidden = jsonObject.get("hidden").getAsInt() == 1;
        pairInfo.fee = jsonObject.get("fee").getAsBigDecimal().stripTrailingZeros();
        return pairInfo;
    }
}
