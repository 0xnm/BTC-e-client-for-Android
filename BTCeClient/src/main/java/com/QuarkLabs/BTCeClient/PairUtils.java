package com.QuarkLabs.BTCeClient;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.*;

public final class PairUtils {

    private PairUtils() {
    }

    public static List<String> getTickersToDisplayThatSupported(Context context) {
        Set<String> supportedPairs = supportedPairs(context);

        SharedPreferences sh = PreferenceManager.getDefaultSharedPreferences(context);
        Set<String> pairsToDisplay = sh.getStringSet("PairsToDisplay", new HashSet<String>());

        List<String> supportedPairsToDisplay = new ArrayList<>();

        for (String pair : pairsToDisplay) {
            if (supportedPairs.contains(pair)) {
                supportedPairsToDisplay.add(pair);
            }
        }

        return supportedPairsToDisplay;
    }

    public static List<String> getChartsToDisplayThatSupported(Context context) {
        Set<String> supportedPairs = supportedPairs(context);

        SharedPreferences sh = PreferenceManager.getDefaultSharedPreferences(context);
        Set<String> pairsToDisplay = sh.getStringSet("ChartsToDisplay", new HashSet<String>());

        List<String> supportedChartsToDisplay = new ArrayList<>();

        for (String pair : pairsToDisplay) {
            if (supportedPairs.contains(pair)) {
                supportedChartsToDisplay.add(pair);
            }
        }

        return supportedChartsToDisplay;
    }

    private static Set<String> supportedPairs(Context context) {
        Set<String> supportedPairs = new HashSet<>();
        Collections.addAll(supportedPairs, context.getResources().getStringArray(R.array.ExchangePairs));
        return supportedPairs;
    }


}
