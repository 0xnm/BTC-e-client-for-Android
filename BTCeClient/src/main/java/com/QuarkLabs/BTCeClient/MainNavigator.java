package com.QuarkLabs.BTCeClient;

import android.support.annotation.NonNull;

import java.math.BigDecimal;

public interface MainNavigator {
    void openTradingSection(@NonNull String pair, @NonNull BigDecimal price);
}
