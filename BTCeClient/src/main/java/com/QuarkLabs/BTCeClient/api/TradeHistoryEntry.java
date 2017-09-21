package com.QuarkLabs.BTCeClient.api;

import android.support.annotation.NonNull;

import com.QuarkLabs.BTCeClient.PairUtils;

import org.json.JSONException;
import org.json.JSONObject;

public class TradeHistoryEntry {
    private long id;
    private String pair;
    private String type;
    private double amount;
    private double rate;
    private long orderId;
    private boolean isYourOrder;
    private long timestamp;

    private TradeHistoryEntry() { }

    public long getId() {
        return id;
    }

    public String getPair() {
        return pair;
    }

    public String getType() {
        return type;
    }

    public double getAmount() {
        return amount;
    }

    public long getOrderId() {
        return orderId;
    }

    public boolean isYourOrder() {
        return isYourOrder;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public double getRate() {
        return rate;
    }

    public static TradeHistoryEntry create(long id, @NonNull JSONObject jsonObject)
            throws JSONException {
        TradeHistoryEntry trade = new TradeHistoryEntry();
        trade.id = id;
        trade.pair = PairUtils.serverToLocal(jsonObject.getString("pair"));
        trade.type = jsonObject.getString("type");
        trade.amount = jsonObject.getDouble("amount");
        trade.rate = jsonObject.getDouble("rate");
        trade.timestamp = jsonObject.getLong("timestamp");
        trade.orderId = jsonObject.getLong("order_id");
        trade.isYourOrder = jsonObject.getInt("is_your_order") == 1;
        return trade;
    }
}
