package com.QuarkLabs.BTCeClient.tasks;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import com.QuarkLabs.BTCeClient.api.TradeType;
import com.QuarkLabs.BTCeClient.ui.terminal.HomeFragment;

public final class TradeRequest implements Parcelable {
    @NonNull
    @TradeType
    private final String type;
    @NonNull
    private final String tradeAmount;
    @NonNull
    private final String tradeCurrency;
    @NonNull
    private final String tradePrice;
    @NonNull
    private final String tradePriceCurrency;

    public TradeRequest(@NonNull @TradeType String type,
                        @NonNull String tradeAmount, @NonNull String tradeCurrency,
                        @NonNull String tradePrice, @NonNull String tradePriceCurrency) {
        this.type = type;
        this.tradeAmount = tradeAmount;
        this.tradeCurrency = tradeCurrency;
        this.tradePrice = tradePrice;
        this.tradePriceCurrency = tradePriceCurrency;
    }

    protected TradeRequest(Parcel in) {
        //noinspection WrongConstant
        type = in.readString();
        tradeAmount = in.readString();
        tradeCurrency = in.readString();
        tradePrice = in.readString();
        tradePriceCurrency = in.readString();
    }

    @NonNull
    public String getType() {
        return type;
    }

    @NonNull
    public String getTradeAmount() {
        return tradeAmount;
    }

    @NonNull
    public String getTradeCurrency() {
        return tradeCurrency;
    }

    @NonNull
    public String getTradePrice() {
        return tradePrice;
    }

    @NonNull
    public String getTradePriceCurrency() {
        return tradePriceCurrency;
    }

    public static final Creator<TradeRequest> CREATOR = new Creator<TradeRequest>() {
        @Override
        public TradeRequest createFromParcel(Parcel in) {
            return new TradeRequest(in);
        }

        @Override
        public TradeRequest[] newArray(int size) {
            return new TradeRequest[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(type);
        dest.writeString(tradeAmount);
        dest.writeString(tradeCurrency);
        dest.writeString(tradePrice);
        dest.writeString(tradePriceCurrency);
    }
}
