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

package com.QuarkLabs.BTCeClient.tasks;

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

import com.QuarkLabs.BTCeClient.BtcEApplication;
import com.QuarkLabs.BTCeClient.DBWorker;
import com.QuarkLabs.BTCeClient.R;
import com.QuarkLabs.BTCeClient.WidgetProvider;
import com.QuarkLabs.BTCeClient.api.Api;
import com.QuarkLabs.BTCeClient.api.CallResult;
import com.QuarkLabs.BTCeClient.api.Ticker;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class UpdateWidgetsTask extends AsyncTask<Void, Void,
        Map<String, UpdateWidgetsTask.Status>> {
    private final Context appContext;
    private Api api;
    private DBWorker dbWorker;
    private Map<Integer, String> pairWidgets;

    /**
     * Creates new instance
     *
     * @param context     Context
     * @param pairWidgets Map of widgetId - pair
     */
    public UpdateWidgetsTask(Context context, Map<Integer, String> pairWidgets) {
        appContext = context.getApplicationContext();
        api = BtcEApplication.get(appContext).getApi();
        dbWorker = DBWorker.getInstance(appContext);
        this.pairWidgets = pairWidgets;
    }

    @Override
    protected Map<String, UpdateWidgetsTask.Status> doInBackground(Void... params) {
        Set<String> pairs = new HashSet<>();
        for (int x : pairWidgets.keySet()) {
            pairs.add(pairWidgets.get(x));
        }

        CallResult<List<Ticker>> result = api.getPairInfo(new ArrayList<>(pairs));

        if (!result.isSuccess()) {
            return Collections.emptyMap();
        }

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

        Map<String, Status> statuses = new HashMap<>();
        for (Ticker ticker : result.getPayload()) {
            String pair = ticker.getPair();
            String pairInDb = pair.replace("_", "/").toUpperCase(Locale.US);
            ContentValues cv = new ContentValues(4);
            double last = ticker.getLast();
            double sell = ticker.getSell();
            double buy = ticker.getBuy();
            cv.put("last", last);
            cv.put("buy", buy);
            cv.put("sell", sell);

            Status status = new Status();
            status.ticker = ticker;

            if (values.containsKey(pairInDb)) {
                if (last >= values.get(pairInDb)) {
                    status.color = Color.GREEN;
                } else {
                    status.color = Color.RED;
                }
            } else {
                status.color = Color.GREEN;
            }

            statuses.put(pair, status);

            int changedCount = dbWorker.updateWidgetData(cv,
                    pair.replace("_", "/").toUpperCase(Locale.US));
            if (changedCount == 0) {
                cv.put("pair", pair.replace("_", "/").toUpperCase(Locale.US));
                dbWorker.insertToWidgetData(cv);
            }
        }
        return statuses;
    }

    @Override
    protected void onPostExecute(Map<String, Status> statuses) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(appContext);
        for (int widgetId : pairWidgets.keySet()) {
            RemoteViews views = new RemoteViews(appContext.getPackageName(),
                    R.layout.appwidget_layout);
            Status status = statuses.get(pairWidgets.get(widgetId)
                    .replace("/", "_").toLowerCase(Locale.US));
            if (status == null) {
                continue;
            }
            double price = status.ticker.getLast();
            String priceString;
            if (price > 1) {
                priceString = (new DecimalFormat("#.##")).format(price);
            } else {
                priceString = String.valueOf(price);
            }
            views.setTextViewText(R.id.widgetCurrencyValue, priceString);
            views.setTextViewText(R.id.widgetPair, pairWidgets.get(widgetId));

            int colorValue = status.color;
            views.setTextColor(R.id.widgetCurrencyValue, colorValue);
            Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            intent.setClass(appContext, WidgetProvider.class);
            Bundle bundle = new Bundle();
            bundle.putIntArray(AppWidgetManager.EXTRA_APPWIDGET_IDS,
                    appWidgetManager.getAppWidgetIds(new ComponentName(appContext,
                            WidgetProvider.class)));
            intent.putExtras(bundle);
            PendingIntent pi = PendingIntent.getBroadcast(appContext,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            views.setOnClickPendingIntent(R.id.widgetContainer, pi);
            SimpleDateFormat df = new SimpleDateFormat("EEE HH:mm", Locale.US);
            Calendar calendar = Calendar.getInstance();
            views.setTextViewText(R.id.widgetDate, df.format(calendar.getTime()));
            appWidgetManager.updateAppWidget(widgetId, views);
        }
    }

    static final class Status {
        int color;
        Ticker ticker;
    }
}
