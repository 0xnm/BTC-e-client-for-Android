package com.QuarkLabs.BTCeClient.api;

import android.support.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

public class Transaction {
    private long id;
    private int type;
    private double amount;
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

    public double getAmount() {
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

    public static Transaction create(long id, @NonNull JSONObject jsonObject)
            throws JSONException {
        Transaction transaction = new Transaction();
        transaction.id = id;
        transaction.currency = jsonObject.getString("currency").toUpperCase(Locale.US);
        transaction.type = jsonObject.getInt("type");
        transaction.amount = jsonObject.getDouble("amount");
        transaction.description = jsonObject.getString("desc");
        transaction.timestamp = jsonObject.getLong("timestamp");
        transaction.status = jsonObject.getInt("status");
        return transaction;
    }
}
