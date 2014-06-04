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

import android.app.ListActivity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.RemoteViews;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class WidgetConfigure extends ListActivity {
    private int mAppWidgetId;
    private ArrayAdapter<String> mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setResult(RESULT_CANCELED);
        setTitle(getString(R.string.WidgetConfigTitle));
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            mAppWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }
        String[] currencies = getResources().getStringArray(R.array.ExchangePairs);
        mAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, currencies);
        setListAdapter(mAdapter);
        getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                SharedPreferences sharedPreferences = getSharedPreferences("widget" + mAppWidgetId, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("pair", mAdapter.getItem(position));
                editor.commit();
                Map<Integer, String> map = new HashMap<>();
                map.put(mAppWidgetId, mAdapter.getItem(position));

                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(WidgetConfigure.this);
                RemoteViews views = new RemoteViews(getPackageName(), R.layout.appwidget_layout);
                views.setTextViewText(R.id.widgetPair, mAdapter.getItem(position));
                SimpleDateFormat df = new SimpleDateFormat("EEE HH:mm");
                Calendar calendar = Calendar.getInstance();
                views.setTextViewText(R.id.widgetDate, df.format(calendar.getTime()));

                appWidgetManager.updateAppWidget(mAppWidgetId, views);

                UpdateWidgetsTask updateWidgetsTask = new UpdateWidgetsTask(WidgetConfigure.this, map);
                updateWidgetsTask.execute();

                Intent resultValue = new Intent();
                resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
                setResult(RESULT_OK, resultValue);

                finish();

            }
        });

    }
}
