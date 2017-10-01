package com.QuarkLabs.BTCeClient.api;

import android.support.annotation.NonNull;

import com.google.gson.JsonObject;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AccountInfo {
    private Map<String, BigDecimal> funds;
    // here can be rights, but not used in the app
    private long transactionCount;
    private int openOrdersCount;
    private long serverTime;

    private AccountInfo() { }

    public Map<String, BigDecimal> getFunds() {
        return funds;
    }

    public long getTransactionCount() {
        return transactionCount;
    }

    public int getOpenOrdersCount() {
        return openOrdersCount;
    }

    public long getServerTime() {
        return serverTime;
    }

    @NonNull
    public static AccountInfo create(@NonNull JsonObject jsonObject) {
        AccountInfo accountInfo = new AccountInfo();
        Map<String, BigDecimal> funds = new HashMap<>();
        JsonObject fundsJson = jsonObject.getAsJsonObject("funds");
        for (String currency : fundsJson.keySet()) {
            funds.put(currency.toUpperCase(Locale.US),
                    fundsJson.get(currency).getAsBigDecimal().stripTrailingZeros());
        }
        accountInfo.funds = funds;
        accountInfo.transactionCount = jsonObject.get("transaction_count").getAsLong();
        accountInfo.openOrdersCount = jsonObject.get("open_orders").getAsInt();
        accountInfo.serverTime = jsonObject.get("server_time").getAsLong();
        return accountInfo;
    }
}
