/*
 * WEX client
 *     Copyright (C) 2014  QuarkDev Solutions <quarkdev.solutions@gmail.com>
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.QuarkLabs.BTCeClient.api;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringDef;
import android.util.Log;

import com.QuarkLabs.BTCeClient.BuildConfig;
import com.QuarkLabs.BTCeClient.R;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.QuarkLabs.BTCeClient.api.AuthApi.TradeMethod.ACTIVE_ORDERS;
import static com.QuarkLabs.BTCeClient.api.AuthApi.TradeMethod.CANCEL_ORDER;
import static com.QuarkLabs.BTCeClient.api.AuthApi.TradeMethod.GET_INFO;
import static com.QuarkLabs.BTCeClient.api.AuthApi.TradeMethod.TRADE;
import static com.QuarkLabs.BTCeClient.api.AuthApi.TradeMethod.TRADE_HISTORY;
import static com.QuarkLabs.BTCeClient.api.AuthApi.TradeMethod.TRANSACTIONS_HISTORY;
import static com.QuarkLabs.BTCeClient.api.AuthApi.TradeMethod.WITHDRAW;

class AuthApi {

    private static final JsonParser JSON_PARSER = new JsonParser();
    private static final String TAG = AuthApi.class.getSimpleName();

    private final String key;
    private final String secret;
    @NonNull
    private final Context appContext;
    private long nonce;

    private String hostUrl;

    @StringDef(value = {GET_INFO, TRANSACTIONS_HISTORY, TRADE_HISTORY,
            ACTIVE_ORDERS, TRADE, CANCEL_ORDER, WITHDRAW})
    @Target({ElementType.PARAMETER, ElementType.FIELD, ElementType.LOCAL_VARIABLE})
    @Retention(RetentionPolicy.SOURCE)
    @interface TradeMethod {
        String GET_INFO = "getInfo";
        String TRANSACTIONS_HISTORY = "TransHistory";
        String TRADE_HISTORY = "TradeHistory";
        String ACTIVE_ORDERS = "ActiveOrders";
        String TRADE = "Trade";
        String CANCEL_ORDER = "CancelOrder";
        String WITHDRAW = "WithdrawCoin";
    }

    AuthApi(@NonNull Context appContext, long nonce,
            @NonNull String key, @NonNull String secret, String hostUrl) {
        this.appContext = appContext;
        this.nonce = nonce;
        this.key = key;
        this.secret = secret;
        this.hostUrl = hostUrl;
    }

    /**
     * Converts byte array to HEX string
     *
     * @param array String as array of bytes
     * @return String in HEX format
     */
    private String byteArrayToHexString(byte[] array) {

        StringBuilder hexString = new StringBuilder();
        for (byte b : array) {
            int intVal = b & 0xff;
            if (intVal < 0x10) {
                hexString.append('0');
            }
            hexString.append(Integer.toHexString(intVal));
        }
        return hexString.toString();
    }

    void setHostUrl(String hostUrl) {
        this.hostUrl = hostUrl;
    }

    /**
     * Makes any request, which require authentication
     *
     * @param method     Method of Trade API
     * @param parameters Additional arguments, which can exist for this method
     * @return Response of type {@link JsonObject}
     */
    @Nullable
    JsonObject makeRequest(@TradeMethod @NonNull String method,
                           @Nullable Map<String, String> parameters) {

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Sending request to " + method
                    + (parameters != null ? ", with parameters: " + parameters.toString() : ""));
        }

        if (key.length() == 0 || secret.length() == 0) {
            return JSON_PARSER.parse("{success:0,error:'"
                    + appContext.getString(R.string.no_key_secret_error) + "'}").getAsJsonObject();
        }

        if (parameters == null) {
            parameters = new HashMap<>();
        }

        parameters.put("method", method);
        parameters.put("nonce", "" + ++nonce);
        String postData = "";
        for (Map.Entry<String, String> ent : parameters.entrySet()) {
            if (postData.length() > 0) {
                postData += "&";
            }
            postData += ent.getKey() + "=" + ent.getValue();
        }

        final SecretKeySpec secretKeySpec;
        try {
            secretKeySpec = new SecretKeySpec(secret.getBytes("UTF-8"), "HmacSHA512");
        } catch (UnsupportedEncodingException uee) {
            Log.e(TAG, "Unsupported encoding exception", uee);
            return null;
        }

        final Mac mac;
        try {
            mac = Mac.getInstance("HmacSHA512");
        } catch (NoSuchAlgorithmException nsae) {
            Log.e(TAG, "No such algorithm exception", nsae);
            return null;
        }

        try {
            mac.init(secretKeySpec);
        } catch (InvalidKeyException ike) {
            Log.e(TAG, "Invalid key exception", ike);
            return null;
        }

        HttpURLConnection connection = null;
        BufferedReader bufferedReader = null;
        DataOutputStream wr = null;
        //noinspection TryWithIdenticalCatches
        try {
            connection = (HttpURLConnection) (new URL(hostUrl + "/tapi"))
                    .openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-type", "application/x-www-form-urlencoded");
            connection.setRequestProperty("Key", key);
            connection.setConnectTimeout((int) TimeUnit.SECONDS.toMillis(5));
            connection.setReadTimeout((int) TimeUnit.SECONDS.toMillis(30));
            byte[] array = mac.doFinal(postData.getBytes("UTF-8"));
            connection.setRequestProperty("Sign", byteArrayToHexString(array));
            wr = new DataOutputStream(connection.getOutputStream());
            wr.writeBytes(postData);
            wr.flush();
            InputStream response = connection.getInputStream();
            StringBuilder sb = new StringBuilder();
            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                String line;
                bufferedReader = new BufferedReader(new InputStreamReader(response, "UTF-8"));
                while ((line = bufferedReader.readLine()) != null) {
                    sb.append(line);
                }
                return JSON_PARSER.parse(sb.toString()).getAsJsonObject();
            }
        } catch (IOException e) {
            logException(e);
        } catch (JsonParseException jpe) {
            logException(jpe);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException e) {
                    logException(e);
                }
            }
            if (wr != null) {
                try {
                    wr.close();
                } catch (IOException e) {
                    logException(e);
                }
            }
        }

        return null;
    }

    private void logException(Exception exception) {
        Log.e(TAG, "Exception while using trade API", exception);
    }
}