package com.QuarkLabs.BTCeClient;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;

import com.QuarkLabs.BTCeClient.fragments.SettingsFragment;

import java.util.HashSet;
import java.util.Set;

public class AppPreferences {

    // should be strictly aligned with key in preferences.xml
    public static final String KEY_API_KEY = "API_Key";
    public static final String KEY_API_SECRET = "API_Secret";
    public static final String KEY_CHECK_ENABLED = "check_enabled";
    public static final String KEY_CHECK_PERIOD = "check_period";
    public static final String KEY_USE_MIRROR = "use_mirror";
    public static final String KEY_USE_OLD_CHARTS = "use_btce_charts";

    public static final String KEY_CHARTS_TO_DISPLAY = "ChartsToDisplay";
    public static final String KEY_PAIRS_TO_DISPLAY = "PairsToDisplay";

    @NonNull
    private final SharedPreferences preferences;

    public AppPreferences(@NonNull Context context) {
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
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
        return preferences.getString(KEY_API_KEY, "");
    }

    @NonNull
    public String getApiSecret() {
        return preferences.getString(KEY_API_SECRET, "");
    }

    @NonNull
    public String getCheckPeriodMillis() {
        return preferences.getString(KEY_CHECK_PERIOD, "60000");
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
        return preferences.getBoolean(KEY_CHECK_ENABLED, false);
    }
}
