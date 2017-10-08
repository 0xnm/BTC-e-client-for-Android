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

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;

import java.util.concurrent.TimeUnit;

/*
 Code was taken and adapted from here:
 www.androidsnippets.com/prompt-engaged-users-to-rate-your-app-in-the-android-market-appirater
 */
public final class AppRater {
    private final static int DAYS_UNTIL_PROMPT = 3;
    private final static int LAUNCHES_UNTIL_PROMPT = 5;

    private static final String APPRATER_LOG_FILENAME = "apprater";
    private static final String DONT_SHOW_AGAIN_KEY = "dontshowagain";
    private static final String LAUNCH_COUNT_KEY = "launch_count";
    private static final String FIRST_LAUNCH_TIMESTAMP = "date_firstlaunch";

    private AppRater() {
    }

    public static void trackAppLaunch(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(APPRATER_LOG_FILENAME,
                Context.MODE_PRIVATE);
        if (prefs.getBoolean(DONT_SHOW_AGAIN_KEY, false)) {
            return;
        }

        SharedPreferences.Editor editor = prefs.edit();

        // Increment launch counter
        long launchCount = prefs.getLong(LAUNCH_COUNT_KEY, 0) + 1;
        editor.putLong(LAUNCH_COUNT_KEY, launchCount);

        // Get date of first launch
        long firstLaunchTimestamp = prefs.getLong(FIRST_LAUNCH_TIMESTAMP, 0);
        if (firstLaunchTimestamp == 0) {
            firstLaunchTimestamp = System.currentTimeMillis();
            editor.putLong(FIRST_LAUNCH_TIMESTAMP, firstLaunchTimestamp);
        }

        // Wait at least n days before opening
        if (launchCount >= LAUNCHES_UNTIL_PROMPT && (System.currentTimeMillis()
                >= firstLaunchTimestamp + TimeUnit.DAYS.toMillis(DAYS_UNTIL_PROMPT))) {
            showRateDialog(context, editor);
        }

        editor.apply();
    }

    private static void showRateDialog(@NonNull final Context context,
                                       @NonNull final SharedPreferences.Editor editor) {
        String appName = context.getString(context.getApplicationInfo().labelRes);

        final AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.apprate_title, appName))
                .setMessage(context.getString(R.string.apprate_text, appName))
                .setPositiveButton(R.string.apprate_rate_action,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                openPlayStorePage(context);
                                dialog.dismiss();
                            }
                        })
                .setNeutralButton(R.string.apprate_remind_later_button,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                editor.clear().commit();
                                dialog.dismiss();
                            }
                        })
                .setNegativeButton(R.string.apprate_dont_show_again_button,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                editor.putBoolean(DONT_SHOW_AGAIN_KEY, true);
                                editor.commit();
                                dialog.dismiss();
                            }
                        })
                .create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dif) {
                dialog.getButton(DialogInterface.BUTTON_POSITIVE)
                        .setTextColor(ColorStateList.valueOf(Color.BLUE));
            }
        });
        dialog.show();
    }

    private static void openPlayStorePage(Context context) {
        try {
            context.startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=" + BuildConfig.APPLICATION_ID)));
        } catch (ActivityNotFoundException e) {
            context.startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("http://play.google.com/store/apps/details?id="
                            + BuildConfig.APPLICATION_ID)));
        }

        context.getSharedPreferences(APPRATER_LOG_FILENAME,
                Context.MODE_PRIVATE)
                .edit()
                .putBoolean(DONT_SHOW_AGAIN_KEY, true)
                .apply();
    }

}
