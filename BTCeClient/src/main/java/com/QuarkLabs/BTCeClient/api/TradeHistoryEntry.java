package com.QuarkLabs.BTCeClient.api;

import android.support.annotation.NonNull;

import com.QuarkLabs.BTCeClient.PairUtils;
import com.google.gson.JsonObject;

import java.math.BigDecimal;

public class TradeHistoryEntry {
    private long id;
    private String pair;
    private String type;
    private BigDecimal amount;
    private BigDecimal rate;
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

    public BigDecimal getAmount() {
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

    public BigDecimal getRate() {
        return rate;
    }

    @NonNull
    public static TradeHistoryEntry create(long id, @NonNull JsonObject jsonObject) {
        TradeHistoryEntry trade = new TradeHistoryEntry();
        trade.id = id;
        trade.pair = PairUtils.serverToLocal(jsonObject.get("pair").getAsString());
        trade.type = jsonObject.get("type").getAsString();
        trade.amount = jsonObject.get("amount").getAsBigDecimal().stripTrailingZeros();
        trade.rate = jsonObject.get("rate").getAsBigDecimal().stripTrailingZeros();
        trade.timestamp = jsonObject.get("timestamp").getAsLong();
        trade.orderId = jsonObject.get("order_id").getAsLong();
        trade.isYourOrder = jsonObject.get("is_your_order").getAsInt() == 1;
        return trade;
    }
}
