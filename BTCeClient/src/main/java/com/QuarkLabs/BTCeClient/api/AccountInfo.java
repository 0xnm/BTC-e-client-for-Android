package com.QuarkLabs.BTCeClient.api;


import android.support.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

public class AccountInfo {
    private Map<String, Double> funds;
    // here can be rights, but not used in the app
    private long transactionCount;
    private int openOrdersCount;
    private long serverTime;

    private AccountInfo() { }

    public Map<String, Double> getFunds() {
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

    public static AccountInfo create(@NonNull JSONObject jsonObject) throws JSONException {
        AccountInfo accountInfo = new AccountInfo();
        Map<String, Double> funds = new HashMap<>();
        Iterator<String> fundsIterator = jsonObject.getJSONObject("funds").keys();
        while (fundsIterator.hasNext()) {
            String fund = fundsIterator.next();
            funds.put(fund.toUpperCase(Locale.US),
                    jsonObject.getJSONObject("funds").getDouble(fund));
        }
        accountInfo.funds = funds;
        accountInfo.transactionCount = jsonObject.optLong("transaction_count");
        accountInfo.openOrdersCount = jsonObject.optInt("open_orders");
        accountInfo.serverTime = jsonObject.optInt("server_time");
        return accountInfo;
    }
}
