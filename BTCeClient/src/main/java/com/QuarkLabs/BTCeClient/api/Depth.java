package com.QuarkLabs.BTCeClient.api;


import android.support.annotation.NonNull;

import com.QuarkLabs.BTCeClient.PairUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class Depth {
    private String pair;
    private List<PriceVolumePair> asks;
    private List<PriceVolumePair> bids;

    private Depth() { }

    public String getPair() {
        return pair;
    }

    public List<PriceVolumePair> getAsks() {
        return asks;
    }

    public List<PriceVolumePair> getBids() {
        return bids;
    }

    public static Depth create(@NonNull String pair, @NonNull JSONObject jsonObject)
            throws JSONException {
        Depth depth = new Depth();
        depth.pair = PairUtils.serverToLocal(pair);
        depth.asks = new ArrayList<>();
        JSONArray asks = jsonObject.getJSONArray("asks");
        for (int i = 0; i < asks.length(); i++) {
            depth.asks.add(PriceVolumePair.create(asks.getJSONArray(i).getDouble(0),
                    asks.getJSONArray(i).getDouble(1)));
        }
        depth.bids = new ArrayList<>();
        JSONArray bids = jsonObject.getJSONArray("bids");
        for (int i = 0; i < bids.length(); i++) {
            depth.bids.add(PriceVolumePair.create(bids.getJSONArray(i).getDouble(0),
                    bids.getJSONArray(i).getDouble(1)));
        }
        return depth;
    }
}
