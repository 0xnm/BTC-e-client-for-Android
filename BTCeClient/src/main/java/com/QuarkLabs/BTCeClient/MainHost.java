package com.QuarkLabs.BTCeClient;

import android.support.annotation.NonNull;

import com.andrognito.pinlockview.PinLockListener;

import java.math.BigDecimal;

public interface MainHost {

    void openTradingSection(@NonNull String pair, @NonNull BigDecimal price);
    void setupPinView(@NonNull PinLockListener listener);
    void hidePinView();
}
