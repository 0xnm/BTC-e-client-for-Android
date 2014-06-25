/*
 * BTC-e client
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

package com.QuarkLabs.BTCeClient.exchangeApi;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONException;
import org.json.JSONObject;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class AuthRequest {
    public static String key;
    public static String secret;
    private long nonce;
    private Mac mac;
    private SecretKeySpec _key;

    public AuthRequest(long nonce, Context context) {
        SharedPreferences sh = PreferenceManager.getDefaultSharedPreferences(context);
        this.nonce = nonce;
        key = sh.getString("key", "");
        secret = sh.getString("secret", "");
    }

    /**
     * Converts byte array to HEX string
     *
     * @param array String as array of bytes
     * @return String in HEX format
     */
    private static String byteArrayToHexString(byte[] array) {

        StringBuilder hexString = new StringBuilder();
        for (byte b : array) {
            int intVal = b & 0xff;
            if (intVal < 0x10)
                hexString.append("0");
            hexString.append(Integer.toHexString(intVal));
        }
        return hexString.toString();
    }

    /**
     * Makes any request, which require authentication
     *
     * @param method    Method of Trade API
     * @param arguments Additional arguments, which can exist for this method
     * @return Response of type JSONObject
     * @throws JSONException
     */
    @Nullable
    public JSONObject makeRequest(@NotNull String method, Map<String, String> arguments) throws JSONException {

        if (key.length() == 0 || secret.length() == 0) {
            return new JSONObject("{success:0,error:'No key/secret provided'}");
        }

        if (arguments == null) {
            arguments = new HashMap<>();
        }

        arguments.put("method", method);
        arguments.put("nonce", "" + ++nonce);
        String postData = "";
        for (Iterator<Map.Entry<String, String>> it = arguments.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, String> ent = it.next();
            if (postData.length() > 0) {
                postData += "&";
            }
            postData += ent.getKey() + "=" + ent.getValue();
        }
        try {
            _key = new SecretKeySpec(secret.getBytes("UTF-8"), "HmacSHA512");
        } catch (UnsupportedEncodingException uee) {
            System.err.println("Unsupported encoding exception: " + uee.toString());
            return null;
        }

        try {
            mac = Mac.getInstance("HmacSHA512");
        } catch (NoSuchAlgorithmException nsae) {
            System.err.println("No such algorithm exception: " + nsae.toString());
            return null;
        }

        try {
            mac.init(_key);
        } catch (InvalidKeyException ike) {
            System.err.println("Invalid key exception: " + ike.toString());
            return null;
        }

        try {
            HttpURLConnection connection = (HttpURLConnection) (new URL("https://btc-e.com/tapi")).openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-type", "application/x-www-form-urlencoded");
            connection.setRequestProperty("Key", key);
            byte[] array = mac.doFinal(postData.getBytes("UTF-8"));
            connection.setRequestProperty("Sign", byteArrayToHexString(array));
            DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
            wr.writeBytes(postData);
            wr.flush();
            wr.close();
            InputStream response = connection.getInputStream();
            StringBuilder sb = new StringBuilder();
            if (connection.getResponseCode() == 200) {
                String line;
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(response));
                while ((line = bufferedReader.readLine()) != null) {
                    sb.append(line);
                }
                return new JSONObject(sb.toString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}