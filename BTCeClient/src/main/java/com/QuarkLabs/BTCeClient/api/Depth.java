package com.QuarkLabs.BTCeClient.api;

import android.support.annotation.NonNull;

import com.QuarkLabs.BTCeClient.utils.PairUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public final class Depth {
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

    @NonNull
    public static Depth create(@NonNull String pair, @NonNull JsonObject jsonObject) {
        Depth depth = new Depth();
        depth.pair = PairUtils.serverToLocal(pair);
        depth.asks = new ArrayList<>();
        JsonArray asks = jsonObject.getAsJsonArray("asks");
        for (int i = 0; i < asks.size(); i++) {
            depth.asks.add(PriceVolumePair.create(
                    asks.get(i).getAsJsonArray().get(0).getAsBigDecimal().stripTrailingZeros(),
                    asks.get(i).getAsJsonArray().get(1).getAsBigDecimal().stripTrailingZeros()));
        }
        depth.bids = new ArrayList<>();
        JsonArray bids = jsonObject.getAsJsonArray("bids");
        for (int i = 0; i < bids.size(); i++) {
            depth.bids.add(PriceVolumePair.create(
                    bids.get(i).getAsJsonArray().get(0).getAsBigDecimal().stripTrailingZeros(),
                    bids.get(i).getAsJsonArray().get(1).getAsBigDecimal().stripTrailingZeros()));
        }
        return depth;
    }
}
