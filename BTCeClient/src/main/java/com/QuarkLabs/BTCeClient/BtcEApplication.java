package com.QuarkLabs.BTCeClient;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import com.QuarkLabs.BTCeClient.fragments.SettingsFragment;

public class BtcEApplication extends Application {
    private static Context appContext;

    @Override
    public void onCreate() {
        super.onCreate();
        appContext = getApplicationContext();
    }

    public static String getHostUrl() {
        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(appContext);
        return sharedPreferences.getBoolean(SettingsFragment.KEY_USE_MIRROR, false)
                ? "https://btc-e.nz" : "https://btc-e.com";
    }
}
