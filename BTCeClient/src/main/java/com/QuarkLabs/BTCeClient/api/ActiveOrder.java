package com.QuarkLabs.BTCeClient.api;

import android.support.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

public class ActiveOrder {
    private long id;
    @NonNull
    private String pair;
    @NonNull
    private String type;
    private double amount;
    private double rate;
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

    public double getAmount() {
        return amount;
    }

    public double getRate() {
        return rate;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public int getStatus() {
        return status;
    }

    public static ActiveOrder create(long id, @NonNull JSONObject jsonObject)
            throws JSONException {
        ActiveOrder activeOrder = new ActiveOrder();
        activeOrder.id = id;
        activeOrder.pair = jsonObject.getString("pair");
        activeOrder.type = jsonObject.getString("type");
        activeOrder.amount = jsonObject.getDouble("amount");
        activeOrder.rate = jsonObject.getDouble("rate");
        activeOrder.createdAt = jsonObject.getLong("timestamp_created");
        activeOrder.status = jsonObject.getInt("status");
        return activeOrder;
    }
}
