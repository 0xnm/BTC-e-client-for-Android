package com.QuarkLabs.BTCeClient.api;

import android.support.annotation.StringDef;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static com.QuarkLabs.BTCeClient.api.TradeType.BUY;
import static com.QuarkLabs.BTCeClient.api.TradeType.SELL;

@Target({ElementType.PARAMETER, ElementType.FIELD, ElementType.LOCAL_VARIABLE, ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
@StringDef(value = {SELL, BUY})
public @interface TradeType {
    String SELL = "sell";
    String BUY = "buy";
}
