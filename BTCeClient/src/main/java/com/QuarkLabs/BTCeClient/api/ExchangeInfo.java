package com.QuarkLabs.BTCeClient.api;

import android.support.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ExchangeInfo {
    private long serverTime;
    private Map<String, ExchangePairInfo> pairs = new HashMap<>();

    private ExchangeInfo() { }

    public long getServerTime() {
        return serverTime;
    }

    public Map<String, ExchangePairInfo> getPairs() {
        return pairs;
    }

    public static ExchangeInfo create(@NonNull JSONObject json) throws JSONException {
        ExchangeInfo exchangeInfo = new ExchangeInfo();
        exchangeInfo.serverTime = json.getLong("server_time");
        JSONObject pairsJson = json.getJSONObject("pairs");
        Iterator<String> pairKeysIterator = pairsJson.keys();
        while (pairKeysIterator.hasNext()) {
            String pair = pairKeysIterator.next();
            exchangeInfo.pairs.put(pair, ExchangePairInfo.create(pairsJson.getJSONObject(pair)));
        }
        return exchangeInfo;
    }
}
