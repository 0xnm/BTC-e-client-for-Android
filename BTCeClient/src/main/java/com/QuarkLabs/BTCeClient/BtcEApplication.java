package com.QuarkLabs.BTCeClient;

import android.app.Application;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.QuarkLabs.BTCeClient.api.Api;
import com.QuarkLabs.BTCeClient.api.ExchangeInfo;
import com.QuarkLabs.BTCeClient.tasks.ApiResultListener;
import com.QuarkLabs.BTCeClient.tasks.GetExchangeInfoTask;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class BtcEApplication extends Application {

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

        appPreferences.addListener(new AppPreferences.Listener() {
            @Override
            public void onApiKeyChanged(@Nullable String apiKey) {
                refreshApiCredentials();
            }

            @Override
            public void onApiSecretChanged(@Nullable String apiKey) {
                refreshApiCredentials();
            }

            @Override
            public void onExchangeUrlChanged(@NonNull String exchangeUrl) {
                api.setHostUrl(exchangeUrl);
            }
        });

        SecurityManager securityManager = SecurityManager.getInstance(this);

        String apiKey = securityManager.decryptString(appPreferences.getApiKey());
        String apiSecret = securityManager.decryptString(appPreferences.getApiSecret());
        api = new Api(this, appPreferences.getExchangeUrl(), apiKey, apiSecret);

        if (appPreferences.getExchangePairs().isEmpty()) {
            // if didn't update yet, lets set hardcoded one
            Set<String> pairs = new HashSet<>();
            Collections.addAll(pairs, getResources().getStringArray(R.array.ExchangePairs));
            appPreferences.setExchangePairs(pairs);
        }

        new GetExchangeInfoTask(api, new ApiResultListener<ExchangeInfo>() {
            @Override
            public void onSuccess(@NonNull ExchangeInfo result) {
                appPreferences.setExchangePairs(PairUtils.exchangePairs(result));
            }

            @Override
            public void onError(@NonNull String error) {
                Log.e(BtcEApplication.class.getSimpleName(),
                        "Failed to update exchange info on app startup: " + error);
            }
        }).execute();
    }

    private void refreshApiCredentials() {
        SecurityManager securityManager = SecurityManager.getInstance(this);

        String apiKey = securityManager.decryptString(appPreferences.getApiKey());
        String apiSecret = securityManager.decryptString(appPreferences.getApiSecret());

        api.setCredentials(apiKey, apiSecret);
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
}
