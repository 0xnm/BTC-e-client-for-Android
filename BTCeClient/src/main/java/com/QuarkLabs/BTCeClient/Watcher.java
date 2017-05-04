package com.QuarkLabs.BTCeClient;


import android.support.annotation.IntDef;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static com.QuarkLabs.BTCeClient.Watcher.PANIC_BUY;
import static com.QuarkLabs.BTCeClient.Watcher.PANIC_SELL;
import static com.QuarkLabs.BTCeClient.Watcher.STOP_LOSS;
import static com.QuarkLabs.BTCeClient.Watcher.TAKE_PROFIT;

@IntDef(value = {PANIC_BUY, PANIC_SELL, STOP_LOSS, TAKE_PROFIT})
@Target({ElementType.PARAMETER, ElementType.FIELD, ElementType.LOCAL_VARIABLE})
@Retention(RetentionPolicy.SOURCE)
public @interface Watcher {

    int PANIC_BUY = 0;
    int PANIC_SELL = 1;
    int STOP_LOSS = 2;
    int TAKE_PROFIT = 3;
}
