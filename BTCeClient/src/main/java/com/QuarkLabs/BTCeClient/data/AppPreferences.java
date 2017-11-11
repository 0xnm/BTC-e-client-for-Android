package com.QuarkLabs.BTCeClient.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.QuarkLabs.BTCeClient.utils.PairUtils;
import com.QuarkLabs.BTCeClient.R;
import com.QuarkLabs.BTCeClient.WexLocale;
import com.QuarkLabs.BTCeClient.utils.SecurityManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AppPreferences implements SharedPreferences.OnSharedPreferenceChangeListener {

    // should be strictly aligned with key in preferences.xml
    private final String keyApiKey;
    private final String keyApiSecret;
    private final String keyCheckEnabled;
    private final String keyCheckPeriod;
    private final String keyExchangeUrl;
    private final String keyLinkifyChat;
    private final String keyDontShowZeroFunds;
    private final String keyPinProtection;

    private final String defaultExchangeUrl;

    private static final String KEY_USE_OLD_CHARTS = "use_btce_charts";

    private static final String KEY_CHARTS_TO_DISPLAY = "ChartsToDisplay";
    private static final String KEY_PAIRS_TO_DISPLAY = "PairsToDisplay";

    private static final String KEY_EXCHANGE_PAIRS = "EXCHANGE_PAIRS";
    private static final String KEY_CHAT_LOCALE = "CHAT_LOCALE";

    private static final String PIN = "PIN";
    private static final String PIN_ATTEMPTS = "PIN_ATTEMPTS";

    private final Set<Listener> listeners = Collections.synchronizedSet(new HashSet<Listener>());

    @NonNull
    private final SharedPreferences preferences;
    @NonNull
    private final SecurityManager securityManager;

    public AppPreferences(@NonNull Context context) {
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences.registerOnSharedPreferenceChangeListener(this);
        keyApiKey = context.getString(R.string.settings_key_api_key);
        keyApiSecret = context.getString(R.string.settings_key_api_secret);
        keyCheckEnabled = context.getString(R.string.settings_key_check_enabled);
        keyCheckPeriod = context.getString(R.string.settings_key_check_period);
        keyExchangeUrl = context.getString(R.string.settings_key_exchange_url);
        keyLinkifyChat = context.getString(R.string.settings_key_linkify_chat);
        keyDontShowZeroFunds = context.getString(R.string.settings_key_dont_show_zero_funds);
        keyPinProtection = context.getString(R.string.settings_key_enable_pin);

        securityManager = SecurityManager.getInstance(context);

        defaultExchangeUrl = context.getString(R.string.settings_exchange_url_default);
    }

    @NonNull
    public String getApiKey() {
        return preferences.getString(keyApiKey, "");
    }

    @NonNull
    public String getApiSecret() {
        return preferences.getString(keyApiSecret, "");
    }

    public void eraseApiKeyAndSecret() {
        preferences.edit().remove(keyApiKey).remove(keyApiSecret).apply();
    }

    @NonNull
    public String getCheckPeriodMillis() {
        return preferences.getString(keyCheckPeriod, "60000");
    }

    public boolean isShowOldCharts() {
        return preferences.getBoolean(KEY_USE_OLD_CHARTS, false);
    }

    /**
     * Get charts to display, that are currently supported by exchange.
     *
     * @return Charts to display, that are currently supported by exchange.
     */
    @NonNull
    public List<String> getChartsToDisplay() {
        Set<String> supportedPairs = getExchangePairsInternal();
        Set<String> chartsToDisplay = preferences
                .getStringSet(KEY_CHARTS_TO_DISPLAY, new HashSet<>());

        List<String> supportedChartsToDisplay = new ArrayList<>();

        for (String pair : chartsToDisplay) {
            if (supportedPairs.contains(pair)) {
                supportedChartsToDisplay.add(pair);
            }
        }

        Collections.sort(supportedChartsToDisplay, PairUtils.CURRENCY_COMPARATOR);
        return supportedChartsToDisplay;
    }

    public void setChartsToDisplay(@NonNull Set<String> charts) {
        preferences.edit()
                .putStringSet(KEY_CHARTS_TO_DISPLAY, charts)
                .apply();
    }

    /**
     * Get tickers to display on the home screen (as cards), that are currently supported by
     * exchange.
     *
     * @return Tickers to display on the home screen (as cards), that are currently supported by
     * exchange.
     */
    @NonNull
    public List<String> getPairsToDisplay() {
        Set<String> supportedPairs = getExchangePairsInternal();
        Set<String> pairsToDisplay = preferences
                .getStringSet(KEY_PAIRS_TO_DISPLAY, new HashSet<>());

        List<String> supportedPairsToDisplay = new ArrayList<>();

        for (String pair : pairsToDisplay) {
            if (supportedPairs.contains(pair)) {
                supportedPairsToDisplay.add(pair);
            }
        }

        Collections.sort(supportedPairsToDisplay, PairUtils.CURRENCY_COMPARATOR);
        return supportedPairsToDisplay;
    }

    public void setPairsToDisplay(@NonNull Collection<String> pairs) {
        preferences.edit()
                .putStringSet(KEY_PAIRS_TO_DISPLAY, new HashSet<>(pairs))
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
    public void setExchangePairs(@NonNull Collection<String> pairs) {
        preferences.edit()
                .putStringSet(KEY_EXCHANGE_PAIRS, new HashSet<>(pairs))
                .apply();
    }

    /**
     * Gets pairs supported by exchange
     *
     * @return Pairs supported by exchange
     */
    @NonNull
    public List<String> getExchangePairs() {
        List<String> exchangePairs = new ArrayList<>(getExchangePairsInternal());
        Collections.sort(exchangePairs, PairUtils.CURRENCY_COMPARATOR);
        return exchangePairs;
    }

    @NonNull
    private Set<String> getExchangePairsInternal() {
        return preferences.getStringSet(KEY_EXCHANGE_PAIRS, new HashSet<>());
    }

    @NonNull
    public List<String> getExchangeCurrencies() {
        Set<String> pairs = getExchangePairsInternal();
        Set<String> currenciesSet = new HashSet<>();

        for (String pair : pairs) {
            String[] pairCurrencies = pair.split("/");
            currenciesSet.add(pairCurrencies[0]);
            currenciesSet.add(pairCurrencies[1]);
        }

        List<String> currencies = new ArrayList<>(currenciesSet);
        Collections.sort(currencies, PairUtils.CURRENCY_COMPARATOR);
        return currencies;
    }

    public void addListener(@NonNull Listener listener) {
        this.listeners.add(listener);
    }

    public void removeListener(@NonNull Listener listener) {
        this.listeners.remove(listener);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        for (Listener listener : listeners) {
            if (keyApiKey.equals(key)) {
                listener.onApiKeyChanged(getApiKey());
            } else if (keyApiSecret.equals(key)) {
                listener.onApiSecretChanged(getApiSecret());
            } else if (keyExchangeUrl.equals(key)) {
                listener.onExchangeUrlChanged(getExchangeUrl());
            } else if (keyCheckEnabled.equals(key) || keyCheckPeriod.equals(key)) {
                listener.onCheckStatus(isPeriodicCheckEnabled(), getCheckPeriodMillis());
            }
        }
    }

    @NonNull
    public String getExchangeUrl() {
        return preferences.getString(keyExchangeUrl, defaultExchangeUrl);
    }

    @WexLocale
    @NonNull
    public String getChatLocale() {
        //noinspection WrongConstant
        return preferences.getString(KEY_CHAT_LOCALE, WexLocale.EN);
    }

    public void setChatLocale(@NonNull @WexLocale String locale) {
        preferences.edit()
                .putString(KEY_CHAT_LOCALE, locale)
                .apply();
    }

    public boolean isLinkifyChat() {
        return preferences.getBoolean(keyLinkifyChat, false);
    }

    public boolean isDontShowZeroFunds() {
        return preferences.getBoolean(keyDontShowZeroFunds, false);
    }

    public boolean isPinProtectionEnabled() {
        return preferences.getBoolean(keyPinProtection, false);
    }

    public void setPinProtectionEnabled(boolean isEnabled) {
        preferences.edit().putBoolean(keyPinProtection, isEnabled).apply();
    }

    @NonNull
    public String getPin() {
        String pin = preferences.getString(PIN, "");
        return "".equals(pin) ? "" : securityManager.decryptString(pin);
    }

    public void setPin(@NonNull String pin) {
        preferences.edit().putString(PIN,
                "".equals(pin) ? pin : securityManager.encryptString(pin)).apply();
    }

    public int getPinAttempts() {
        return preferences.getInt(PIN_ATTEMPTS, 0);
    }

    public void setPinAttempts(int attempts) {
        preferences.edit().putInt(PIN_ATTEMPTS, attempts).apply();
    }

    public static abstract class Listener {
        public void onApiKeyChanged(@Nullable String apiKey) {
            // do nothing
        }

        public void onApiSecretChanged(@Nullable String apiKey) {
            // do nothing
        }

        public void onCheckStatus(boolean isEnabled, @Nullable String periodMillis) {
            // do nothing
        }

        public void onExchangeUrlChanged(@NonNull String apiUrl) {
            // do nothing
        }

    }
}
