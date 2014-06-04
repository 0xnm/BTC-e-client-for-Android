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

package com.QuarkLabs.BTCeClient;

import org.jetbrains.annotations.Nullable;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

class SimpleRequest {
    /**
     * Makes simple non-authenticated request
     *
     * @param url URL of Trade API
     * @return Response of type JSONObject
     * @throws JSONException
     */
    @Nullable
    public JSONObject makeRequest(String url) throws JSONException {

        try {
            HttpURLConnection connection = (HttpURLConnection) (new URL(url)).openConnection();
            InputStream response = connection.getInputStream();
            StringBuilder sb = new StringBuilder();
            if (connection.getResponseCode() == 200) {
                BufferedReader rd = new BufferedReader(new InputStreamReader(response));
                String line;
                while ((line = rd.readLine()) != null) {
                    sb.append(line);
                }
                response.close();
                rd.close();
                return new JSONObject(sb.toString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}