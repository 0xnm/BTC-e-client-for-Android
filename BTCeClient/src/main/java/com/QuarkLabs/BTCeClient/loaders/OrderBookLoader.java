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

package com.QuarkLabs.BTCeClient.loaders;

import android.content.AsyncTaskLoader;
import android.content.Context;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Locale;

public class OrderBookLoader extends AsyncTaskLoader<JSONObject> {
    private final String mPair;
    private JSONObject mData;


    public OrderBookLoader(Context context, String pair) {
        super(context);
        mPair = pair;
    }

    @Override
    public JSONObject loadInBackground() {
        String urlString = "https://btc-e.com/api/2/" + mPair.toLowerCase(Locale.US).replace("/", "_") + "/depth";
        String out = "";
        try {
            URL url = new URL(urlString);
            URLConnection connection = url.openConnection();
            InputStream stream = connection.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            String line;
            while ((line = reader.readLine()) != null) {
                out += line;
            }
            mData = new JSONObject(out);
            stream.close();
            reader.close();
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
        return mData;
    }

    @Override
    protected void onStartLoading() {
        if (mData != null) {
            deliverResult(mData);
        }
        if (takeContentChanged() || mData == null) {
            forceLoad();
        }
    }
}
