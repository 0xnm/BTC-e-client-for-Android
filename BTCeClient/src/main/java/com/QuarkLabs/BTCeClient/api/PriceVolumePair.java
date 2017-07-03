package com.QuarkLabs.BTCeClient.api;


public class PriceVolumePair {
    private double price;
    private double volume;

    private PriceVolumePair() { }

    public double getPrice() {
        return price;
    }

    public double getVolume() {
        return volume;
    }

    public static PriceVolumePair create(double price, double volume) {
        PriceVolumePair priceVolumePair = new PriceVolumePair();
        priceVolumePair.price = price;
        priceVolumePair.volume = volume;
        return priceVolumePair;
    }
}
