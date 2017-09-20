package com.QuarkLabs.BTCeClient;

import android.content.Context;
import android.support.annotation.NonNull;

import com.QuarkLabs.BTCeClient.api.ExchangeInfo;
import com.QuarkLabs.BTCeClient.api.ExchangePairInfo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class PairUtils {

    private static final String LOCAL_PAIR_DELIMITER = "/";
    private static final String SERVER_PAIR_DELIMITER = "_";

    private PairUtils() {
    }

    @NonNull
    public static List<String> getTickersToDisplayThatSupported(@NonNull Context context) {
        Set<String> supportedPairs = supportedPairs(context);

        Set<String> pairsToDisplay = BtcEApplication.get(context)
                .getAppPreferences().getPairsToDisplay();

        List<String> supportedPairsToDisplay = new ArrayList<>();

        for (String pair : pairsToDisplay) {
            if (supportedPairs.contains(pair)) {
                supportedPairsToDisplay.add(pair);
            }
        }

        return supportedPairsToDisplay;
    }

    @NonNull
    public static List<String> getChartsToDisplayThatSupported(@NonNull Context context) {
        Set<String> supportedPairs = supportedPairs(context);

        Set<String> pairsToDisplay = BtcEApplication.get(context)
                .getAppPreferences().getChartsToDisplay();

        List<String> supportedChartsToDisplay = new ArrayList<>();

        for (String pair : pairsToDisplay) {
            if (supportedPairs.contains(pair)) {
                supportedChartsToDisplay.add(pair);
            }
        }

        return supportedChartsToDisplay;
    }

    @NonNull
    public static String localToServer(@NonNull String pair) {
        return pair.replace(LOCAL_PAIR_DELIMITER, SERVER_PAIR_DELIMITER).toLowerCase(Locale.US);
    }

    @NonNull
    public static String serverToLocal(@NonNull String pair) {
        return pair.replace(SERVER_PAIR_DELIMITER, LOCAL_PAIR_DELIMITER).toUpperCase(Locale.US);
    }

    public static boolean isSupportedPair(@NonNull Context context, @NonNull String pair) {
        return supportedPairs(context).contains(pair);
    }

    private static Set<String> supportedPairs(@NonNull Context context) {
        return BtcEApplication.get(context).getAppPreferences().getExchangePairs();
    }

    @NonNull
    public static Set<String> exchangePairs(@NonNull ExchangeInfo exchangeInfo) {
        Set<String> pairs = new HashSet<>();
        for (ExchangePairInfo pairInfo : exchangeInfo.getPairs()) {
            pairs.add(PairUtils.serverToLocal(pairInfo.getPair()));
        }
        return pairs;
    }
}
