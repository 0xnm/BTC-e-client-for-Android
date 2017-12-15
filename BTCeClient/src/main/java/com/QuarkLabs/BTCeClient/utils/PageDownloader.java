package com.QuarkLabs.BTCeClient.utils;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.QuarkLabs.BTCeClient.WexLocale;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HttpsURLConnection;

public class PageDownloader {

    private static final String TAG = PageDownloader.class.getSimpleName();

    /**
     * Downloads page for give pair
     *
     * @param pair   Pair in local format
     * @param locale Language (one of 3 supported)
     * @return Contents of the requested page or {@code null}
     */
    @Nullable
    public String download(@NonNull String exchangeUrl,
                           @Nullable String pair,
                           @Nullable @WexLocale String locale) {
        StringBuilder content = new StringBuilder();
        BufferedReader reader = null;
        HttpsURLConnection connection = null;
        try {
            String path = "";
            if (pair != null && !pair.isEmpty()) {
                path = (isToken(pair) ? "/tokens/" : "/exchange/") + PairUtils.localToServer(pair);
            }
            URL url = new URL(exchangeUrl + path + "?old_charts=1");
            connection = (HttpsURLConnection) url.openConnection();
            connection.setConnectTimeout((int) TimeUnit.SECONDS.toMillis(5));
            connection.setReadTimeout((int) TimeUnit.SECONDS.toMillis(30));
            if (locale != null && !locale.isEmpty()) {
                connection.addRequestProperty("Cookie", "locale=" + locale);
            }

            if (connection.getResponseCode() != HttpsURLConnection.HTTP_OK) {
                return null;
            }
            reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), "UTF-8"));
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line);
            }
            return content.toString();
        } catch (IOException e) {
            Log.e(TAG, "Failed to get page", e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    Log.e(TAG, "Failed to close reader", e);
                }
            }
        }
        return null;
    }

    private boolean isToken(String pair) {
        return pair.split("_")[0].length() == 5;
    }
}
