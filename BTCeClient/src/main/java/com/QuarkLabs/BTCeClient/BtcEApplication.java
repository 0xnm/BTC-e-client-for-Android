package com.QuarkLabs.BTCeClient;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;

import com.QuarkLabs.BTCeClient.api.Api;

public class BtcEApplication extends Application implements
        SharedPreferences.OnSharedPreferenceChangeListener {

    private Api api;
    private AppPreferences appPreferences;

    @Override
    public void onCreate() {
        super.onCreate();
        appPreferences = new AppPreferences(this);
        /* // in Russia btc-e.com is blocked, so need to use mirror
        if ("RU".equalsIgnoreCase(getResources().getConfiguration().locale.getCountry())
                && !defaultPreferences.contains(SettingsFragment.KEY_USE_MIRROR)) {
            // commit instead of apply, because need to make sure changes are effective before
            // continuing
            defaultPreferences.edit()
                    .putBoolean(SettingsFragment.KEY_USE_MIRROR, true)
                    .apply();
        }*/

        appPreferences.registerOnSharedPreferenceChangeListener(this);

        SecurityManager securityManager = SecurityManager.getInstance(this);

        String apiKey = securityManager.decryptString(appPreferences.getApiKey());
        String apiSecret = securityManager.decryptString(appPreferences.getApiSecret());
        api = new Api(this, getHostUrl(), apiKey, apiSecret);
    }

    private void refreshApiCredentials() {
        SecurityManager securityManager = SecurityManager.getInstance(this);

        String apiKey = securityManager.decryptString(appPreferences.getApiKey());
        String apiSecret = securityManager.decryptString(appPreferences.getApiSecret());

        api.setCredentials(apiKey, apiSecret);
    }

    public String getHostUrl() {
        return "https://wex.nz";
    }

    @NonNull
    public Api getApi() {
        return api;
    }

    @NonNull
    public AppPreferences getAppPreferences() {
        return appPreferences;
    }

    public static BtcEApplication get(@NonNull Context context) {
        return (BtcEApplication) context.getApplicationContext();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (AppPreferences.KEY_API_KEY.equals(key)
                || AppPreferences.KEY_API_SECRET.equals(key)) {
            refreshApiCredentials();
        } else if (AppPreferences.KEY_USE_MIRROR.equals(key)) {
            api.setHostUrl(getHostUrl());
        }
    }
}
