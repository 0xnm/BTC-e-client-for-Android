package com.QuarkLabs.BTCeClient.api;

import android.support.annotation.NonNull;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class ExchangeInfo {
    private long serverTime;
    private List<ExchangePairInfo> pairs = new ArrayList<>();

    private ExchangeInfo() { }

    public long getServerTime() {
        return serverTime;
    }

    public List<ExchangePairInfo> getPairs() {
        return pairs;
    }

    @NonNull
    public static ExchangeInfo create(@NonNull JsonObject json) {
        ExchangeInfo exchangeInfo = new ExchangeInfo();
        exchangeInfo.serverTime = json.get("server_time").getAsLong();
        JsonObject pairsJson = json.getAsJsonObject("pairs");
        for (String pair : pairsJson.keySet()) {
            exchangeInfo.pairs.add(ExchangePairInfo.create(pair,
                    pairsJson.getAsJsonObject(pair)));
        }
        return exchangeInfo;
    }
}
