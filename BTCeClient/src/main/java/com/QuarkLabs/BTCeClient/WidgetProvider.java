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

package com.QuarkLabs.BTCeClient;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.SharedPreferences;

import com.QuarkLabs.BTCeClient.tasks.UpdateWidgetsTask;

import java.util.HashMap;
import java.util.Map;

public class WidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Map<Integer, String> pairWidgets = new HashMap<>();

        for (int id : appWidgetIds) {
            SharedPreferences sharedPreferences = context.getSharedPreferences("widget" + id,
                    Context.MODE_PRIVATE);
            String pair = sharedPreferences.getString("pair", "");
            if (pair.length() != 0) {
                pairWidgets.put(id, pair);
            }
        }

        UpdateWidgetsTask updateWidgetsTask = new UpdateWidgetsTask(context, pairWidgets);
        updateWidgetsTask.execute();
    }
}
