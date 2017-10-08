package com.QuarkLabs.BTCeClient.api;

import android.support.annotation.NonNull;

import com.QuarkLabs.BTCeClient.utils.PairUtils;
import com.google.gson.JsonObject;

import java.math.BigDecimal;

public class ActiveOrder {
    private long id;
    @NonNull
    private String pair;
    @NonNull
    private String type;
    private BigDecimal amount;
    private BigDecimal rate;
    private long createdAt;
    private int status;

    private ActiveOrder() { }

    public long getId() {
        return id;
    }

    @NonNull
    public String getPair() {
        return pair;
    }

    @NonNull
    public String getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getRate() {
        return rate;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public int getStatus() {
        return status;
    }

    @NonNull
    public static ActiveOrder create(long id, @NonNull JsonObject jsonObject) {
        ActiveOrder activeOrder = new ActiveOrder();
        activeOrder.id = id;
        activeOrder.pair = PairUtils.serverToLocal(jsonObject.get("pair").getAsString());
        activeOrder.type = jsonObject.get("type").getAsString();
        activeOrder.amount = jsonObject.get("amount").getAsBigDecimal().stripTrailingZeros();
        activeOrder.rate = jsonObject.get("rate").getAsBigDecimal().stripTrailingZeros();
        activeOrder.createdAt = jsonObject.get("timestamp_created").getAsLong();
        activeOrder.status = jsonObject.get("status").getAsInt();
        return activeOrder;
    }
}
