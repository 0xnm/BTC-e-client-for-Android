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

package com.QuarkLabs.BTCeClient.services;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import com.QuarkLabs.BTCeClient.ConstantHolder;
import com.QuarkLabs.BTCeClient.DBWorker;
import com.QuarkLabs.BTCeClient.MyActivity;
import com.QuarkLabs.BTCeClient.R;
import com.QuarkLabs.BTCeClient.exchangeApi.SimpleRequest;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;

public class CheckTickersService extends IntentService {
    public static final String BASE_URL = "https://btc-e.com/api/3/ticker/";
    private final static int PANIC_BUY_TYPE = 0;
    private final static int PANIC_SELL_TYPE = 1;
    private final static int STOP_LOSS_TYPE = 2;
    private final static int TAKE_PROFIT_TYPE = 3;

    public CheckTickersService() {
        super("checkTickers");
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        SharedPreferences sh = PreferenceManager.getDefaultSharedPreferences(this);
        Set<String> x = sh.getStringSet("PairsToDisplay", new HashSet<String>());
        String[] pairs = x.toArray(new String[x.size()]);
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        String url = BASE_URL;
        for (String xx : pairs) {
            url += xx.replace("/", "_").toLowerCase(Locale.US) + "-";
        }
        SimpleRequest reqSim = new SimpleRequest();

        if (networkInfo != null && networkInfo.isConnected()) {
            JSONObject data = null;
            try {
                data = reqSim.makeRequest(url);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            if (data != null && data.optInt("success", 1) != 0) {

                String message = checkNotifiers(data, MyActivity.tickersStorage.loadAllValues());

                if (message.length() != 0) {
                    NotificationManager notificationManager =
                            (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                    NotificationCompat.Builder nb = new NotificationCompat.Builder(this)
                            .setContentTitle(getResources().getString(R.string.app_name))
                            .setSmallIcon(R.drawable.ic_launcher)
                            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                            .setContentText(message.substring(0, message.length() - 2));
                    notificationManager.notify(ConstantHolder.ALARM_NOTIF_ID, nb.build());

                }
                for (Iterator<String> iterator = data.keys(); iterator.hasNext(); ) {
                    String key = iterator.next();
                    String keyForData = key.replace("_", "/").toUpperCase(Locale.US);
                    MyActivity.tickersStorage.saveEntry(keyForData, data.optJSONObject(key));
                    MyActivity.tickersStorage.saveAll(data);

                }
                LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent("UpdateTickers"));

            }
        } else {
            new Handler().post(new Runnable() {
                @Override
                public void run() {
                    //Toast.makeText(CheckTickersService.this, "Unable to fetch data", Toast.LENGTH_SHORT).show();
                }
            });

        }
    }

    /**
     * Processes new data, adds notifications if any
     *
     * @param newData JSONObject with new tickers data
     * @param oldData JSONObject with old tickers data
     * @return String with all notifications
     */
    private String checkNotifiers(JSONObject newData, JSONObject oldData) {

        DBWorker dbWorker = DBWorker.getInstance(this);
        Cursor cursor = dbWorker.getNotifiers();

        StringBuilder stringBuilder = new StringBuilder();
        for (Iterator<String> iterator = newData.keys(); iterator.hasNext(); ) {
            cursor.moveToFirst();
            String key = iterator.next();
            if (oldData != null && oldData.has(key)) {
                double oldValue = oldData.optJSONObject(key).optDouble("last");
                double newValue = newData.optJSONObject(key).optDouble("last");
                while (!cursor.isAfterLast()) {
                    boolean pairMatched = key.replace("_", "/")
                            .toUpperCase(Locale.US)
                            .equals(cursor.getString(cursor.getColumnIndex("Pair")));
                    if (pairMatched) {
                        float percent;
                        switch (cursor.getInt(cursor.getColumnIndex("Type"))) {
                            case PANIC_BUY_TYPE:
                                percent = cursor.getFloat(cursor.getColumnIndex("Value")) / 100;
                                if (newValue > ((1 + percent) * oldValue)) {
                                    stringBuilder.append("Panic Buy for ").append(key.replace("_", "/")
                                            .toUpperCase(Locale.US)).append("; ");
                                }
                                break;
                            case PANIC_SELL_TYPE:
                                percent = cursor.getFloat(cursor.getColumnIndex("Value")) / 100;
                                if (newValue < ((1 - percent) * oldValue)) {
                                    stringBuilder.append("Panic Sell for ").append(key.replace("_", "/")
                                            .toUpperCase(Locale.US)).append("; ");
                                }
                                break;
                            case STOP_LOSS_TYPE:
                                if (newValue < cursor.getFloat(cursor.getColumnIndex("Value"))) {
                                    stringBuilder.append("Stop Loss for ").append(key.replace("_", "/")
                                            .toUpperCase(Locale.US)).append("; ");
                                }
                                break;
                            case TAKE_PROFIT_TYPE:
                                if (newValue > cursor.getFloat(cursor.getColumnIndex("Value"))) {
                                    stringBuilder.append("Take Profit for ").append(key.replace("_", "/")
                                            .toUpperCase(Locale.US)).append("; ");
                                }
                                break;
                            default:
                                break;
                        }
                    }
                    cursor.moveToNext();
                }
            }
        }
        cursor.close();
        return stringBuilder.toString();
    }


}
