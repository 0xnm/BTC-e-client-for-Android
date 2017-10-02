package com.QuarkLabs.BTCeClient.api;

import android.support.annotation.NonNull;

import java.math.BigDecimal;

public class PriceVolumePair {
    private BigDecimal price;
    private BigDecimal volume;

    private PriceVolumePair() { }

    public BigDecimal getPrice() {
        return price;
    }

    public BigDecimal getVolume() {
        return volume;
    }

    @NonNull
    public static PriceVolumePair create(BigDecimal price, BigDecimal volume) {
        PriceVolumePair priceVolumePair = new PriceVolumePair();
        priceVolumePair.price = price;
        priceVolumePair.volume = volume;
        return priceVolumePair;
    }
}
