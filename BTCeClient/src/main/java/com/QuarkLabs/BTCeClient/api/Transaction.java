package com.QuarkLabs.BTCeClient.api;

import android.support.annotation.NonNull;

import com.google.gson.JsonObject;

import java.math.BigDecimal;
import java.util.Locale;

public class Transaction {
    private long id;
    private int type;
    private BigDecimal amount;
    private String currency;
    private String description;
    private int status;
    private long timestamp;

    private Transaction() { }

    @NonNull
    public long getId() {
        return id;
    }

    public int getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getDescription() {
        return description;
    }

    public int getStatus() {
        return status;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @NonNull
    public static Transaction create(long id, @NonNull JsonObject jsonObject) {
        Transaction transaction = new Transaction();
        transaction.id = id;
        transaction.currency = jsonObject.get("currency").getAsString().toUpperCase(Locale.US);
        transaction.type = jsonObject.get("type").getAsInt();
        transaction.amount = jsonObject.get("amount").getAsBigDecimal().stripTrailingZeros();
        transaction.description = jsonObject.get("desc").getAsString();
        transaction.timestamp = jsonObject.get("timestamp").getAsLong();
        transaction.status = jsonObject.get("status").getAsInt();
        return transaction;
    }
}
