package com.QuarkLabs.BTCeClient.api;

import android.support.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
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

    public static ExchangeInfo create(@NonNull JSONObject json) throws JSONException {
        ExchangeInfo exchangeInfo = new ExchangeInfo();
        exchangeInfo.serverTime = json.getLong("server_time");
        JSONObject pairsJson = json.getJSONObject("pairs");
        Iterator<String> pairKeysIterator = pairsJson.keys();
        while (pairKeysIterator.hasNext()) {
            String pair = pairKeysIterator.next();
            exchangeInfo.pairs.add(ExchangePairInfo.create(pair,
                    pairsJson.getJSONObject(pair)));
        }
        return exchangeInfo;
    }
}
