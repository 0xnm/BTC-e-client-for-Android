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

import android.support.annotation.Nullable;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.TimeUnit;

class GuestApi {

    private static final String TAG = GuestApi.class.getSimpleName();

    /**
     * Makes simple non-authenticated request
     *
     * @param urlString URL of Trade API
     * @return Response of type JSONObject
     * @throws JSONException Thrown in case of unexpected payload
     */
    @Nullable
    JSONObject call(String urlString) throws JSONException {

        HttpURLConnection connection = null;
        BufferedReader rd = null;
        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout((int) TimeUnit.SECONDS.toMillis(5));
            connection.setReadTimeout((int) TimeUnit.SECONDS.toMillis(30));
            InputStream response = connection.getInputStream();
            StringBuilder sb = new StringBuilder();
            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                rd = new BufferedReader(new InputStreamReader(response));
                String line;
                while ((line = rd.readLine()) != null) {
                    sb.append(line);
                }
                return new JSONObject(sb.toString());
            }
        } catch (IOException e) {
            logException(e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
            if (rd != null) {
                try {
                    rd.close();
                } catch (IOException e) {
                    logException(e);
                }
            }
        }
        return null;
    }

    private void logException(Exception exception) {
        Log.e(TAG, "Exception while using public API", exception);
    }
}