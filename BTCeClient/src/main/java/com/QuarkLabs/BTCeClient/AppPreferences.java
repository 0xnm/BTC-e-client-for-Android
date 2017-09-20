package com.QuarkLabs.BTCeClient;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;

import java.util.HashSet;
import java.util.Set;

public class AppPreferences {

    // should be strictly aligned with key in preferences.xml
    public final String keyApiKey;
    public final String keyApiSecret;
    public final String keyCheckEnabled;
    public final String keyCheckPeriod;

    private static final String KEY_USE_MIRROR = "use_mirror";
    private static final String KEY_USE_OLD_CHARTS = "use_btce_charts";

    private static final String KEY_CHARTS_TO_DISPLAY = "ChartsToDisplay";
    private static final String KEY_PAIRS_TO_DISPLAY = "PairsToDisplay";

    private static final String KEY_EXCHANGE_PAIRS = "EXCHANGE_PAIRS";

    @NonNull
    private final SharedPreferences preferences;

    AppPreferences(@NonNull Context context) {
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        keyApiKey = context.getString(R.string.settings_key_api_key);
        keyApiSecret = context.getString(R.string.settings_key_api_secret);
        keyCheckEnabled = context.getString(R.string.settings_key_check_enabled);
        keyCheckPeriod = context.getString(R.string.settings_key_check_period);
    }

    public void registerOnSharedPreferenceChangeListener(
            SharedPreferences.OnSharedPreferenceChangeListener listener) {
        preferences.registerOnSharedPreferenceChangeListener(listener);
    }

    public void unregisterOnSharedPreferenceChangeListener(
            SharedPreferences.OnSharedPreferenceChangeListener listener) {
        preferences.unregisterOnSharedPreferenceChangeListener(listener);
    }

    @NonNull
    public String getApiKey() {
        return preferences.getString(keyApiKey, "");
    }

    @NonNull
    public String getApiSecret() {
        return preferences.getString(keyApiSecret, "");
    }

    @NonNull
    public String getCheckPeriodMillis() {
        return preferences.getString(keyCheckPeriod, "60000");
    }

    public boolean isShowOldCharts() {
        return preferences.getBoolean(KEY_USE_OLD_CHARTS, false);
    }

    @NonNull
    public Set<String> getChartsToDisplay() {
        return preferences.getStringSet(KEY_CHARTS_TO_DISPLAY, new HashSet<String>());
    }

    public void setChartsToDisplay(@NonNull Set<String> charts) {
        preferences.edit()
                .putStringSet(KEY_CHARTS_TO_DISPLAY, charts)
                .apply();
    }

    @NonNull
    public Set<String> getPairsToDisplay() {
        return preferences.getStringSet(KEY_PAIRS_TO_DISPLAY, new HashSet<String>());
    }

    public void setPairsToDisplay(@NonNull Set<String> pairs) {
        preferences.edit()
                .putStringSet(KEY_PAIRS_TO_DISPLAY, pairs)
                .apply();
    }

    public boolean isPeriodicCheckEnabled() {
        return preferences.getBoolean(keyCheckEnabled, false);
    }

    /**
     * Sets pairs supported by exchange
     *
     * @param pairs Pair supported by exchange
     */
    public void setExchangePairs(@NonNull Set<String> pairs) {
        preferences.edit()
                .putStringSet(KEY_EXCHANGE_PAIRS, pairs)
                .apply();
    }

    /**
     * Gets pairs supported by exchange
     *
     * @return Pairs supported by exchange
     */
    @NonNull
    public Set<String> getExchangePairs() {
        return preferences.getStringSet(KEY_EXCHANGE_PAIRS, new HashSet<String>());
    }

    @NonNull
    public Set<String> getExchangeCurrencies() {
        Set<String> pairs = getExchangePairs();
        Set<String> currencies = new HashSet<>();

        for (String pair : pairs) {
            String[] pairCurrencies = pair.split("/");
            currencies.add(pairCurrencies[0]);
            currencies.add(pairCurrencies[1]);
        }

        return currencies;
    }
}
