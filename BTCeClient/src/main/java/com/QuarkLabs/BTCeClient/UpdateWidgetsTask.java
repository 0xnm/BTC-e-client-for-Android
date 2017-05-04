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

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.RemoteViews;
import com.QuarkLabs.BTCeClient.exchangeApi.App;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;

class UpdateWidgetsTask extends AsyncTask<Void, Void, JSONObject> {
    private WeakReference<Context> mContext;
    private Map<Integer, String> mMap;

    public UpdateWidgetsTask(Context context, Map<Integer, String> map) {
        mContext = new WeakReference<>(context);
        mMap = map;
    }

    @Override
    protected JSONObject doInBackground(Void... params) {
        Set<String> set = new HashSet<>();
        JSONObject response = null;
        for (int x : mMap.keySet()) {
            set.add(mMap.get(x));
        }
        try {
            response = App.getPairInfo(set.toArray(new String[set.size()]));
            response = response == null ? new JSONObject() : response;
            Context context = mContext.get();
            if (context == null) {
                return null;
            }
            DBWorker dbWorker = DBWorker.getInstance(context);
            String[] columns = {"pair", "last"};
            Cursor cursor = dbWorker.pullWidgetData(columns);
            Map<String, Double> values = new HashMap<>();
            if (cursor.moveToFirst()) {
                while (!cursor.isAfterLast()) {
                    values.put(cursor.getString(cursor.getColumnIndex(columns[0])),
                            cursor.getDouble(cursor.getColumnIndex(columns[1])));
                    cursor.moveToNext();
                }
            }
            cursor.close();
            for (@SuppressWarnings("unchecked") Iterator<String> x = response.keys(); x.hasNext(); ) {
                String pair = x.next();
                String pairInDB = pair.replace("_", "/").toUpperCase(Locale.US);
                ContentValues cv = new ContentValues(4);
                double last = response.getJSONObject(pair).getDouble("last");
                double sell = response.getJSONObject(pair).getDouble("sell");
                double buy = response.getJSONObject(pair).getDouble("buy");
                cv.put("last", last);
                cv.put("buy", buy);
                cv.put("sell", sell);
                if (values.containsKey(pairInDB)) {
                    if (last >= values.get(pairInDB)) {
                        response.getJSONObject(pair).put("color", "green");
                    } else {
                        response.getJSONObject(pair).put("color", "red");
                    }
                } else {
                    response.getJSONObject(pair).put("color", "green");
                }

                int result = dbWorker.updateWidgetData(cv,
                        pair.replace("_", "/").toUpperCase(Locale.US));
                if (result == 0) {
                    cv.put("pair", pair.replace("_", "/").toUpperCase(Locale.US));
                    dbWorker.insertToWidgetData(cv);
                }
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return response;
    }

    @Override
    protected void onPostExecute(JSONObject jsonObject) {
        if (jsonObject != null) {
            try {
                Context context = mContext.get();
                if (context == null) {
                    return;
                }
                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
                for (int x : mMap.keySet()) {
                    RemoteViews views = new RemoteViews(context.getPackageName(),
                            R.layout.appwidget_layout);
                    double price = jsonObject
                            .getJSONObject(mMap.get(x).replace("/", "_").toLowerCase(Locale.US))
                            .getDouble("last");
                    String priceString;
                    if (price > 1) {
                        priceString = (new DecimalFormat("#.##")).format(price);
                    } else {
                        priceString = String.valueOf(price);
                    }
                    views.setTextViewText(R.id.widgetCurrencyValue, priceString);
                    views.setTextViewText(R.id.widgetPair, mMap.get(x));
                    String color = jsonObject
                            .getJSONObject(mMap.get(x)
                                    .replace("/", "_")
                                    .toLowerCase(Locale.US))
                            .getString("color");
                    int colorValue = color.equals("green") ? Color.GREEN : Color.RED;
                    views.setTextColor(R.id.widgetCurrencyValue, colorValue);
                    Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                    intent.setClass(mContext.get(), WidgetProvider.class);
                    Bundle bundle = new Bundle();
                    bundle.putIntArray(AppWidgetManager.EXTRA_APPWIDGET_IDS,
                            appWidgetManager.getAppWidgetIds(new ComponentName(context,
                                    WidgetProvider.class)));
                    intent.putExtras(bundle);
                    PendingIntent pi = PendingIntent.getBroadcast(context,
                            0,
                            intent,
                            PendingIntent.FLAG_UPDATE_CURRENT);
                    views.setOnClickPendingIntent(R.id.widgetContainer, pi);
                    SimpleDateFormat df = new SimpleDateFormat("EEE HH:mm", Locale.US);
                    Calendar calendar = Calendar.getInstance();
                    views.setTextViewText(R.id.widgetDate, df.format(calendar.getTime()));
                    appWidgetManager.updateAppWidget(x, views);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}
