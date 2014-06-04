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

import android.app.*;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import com.QuarkLabs.BTCeClient.fragments.*;


public class MyActivity extends Activity {
    public static TickersStorage tickersStorage = new TickersStorage();
    public static AlarmManager alarmManager;
    public static boolean alarmSet;
    public static PendingIntent pendingIntent;
    public static App app;
    private final HomeFragment mHomeFragment = new HomeFragment();
    private final ChartsFragment mChartsFragment = new ChartsFragment();
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private ActionBarDrawerToggle mDrawerToggle;

    /**
     * Display selected fragment
     *
     * @param position
     */
    private void displayItem(int position) {
        String tag = "";
        Fragment fragment = null;
        final FragmentManager fragmentManager = getFragmentManager();
        switch (position) {
            case 0:
                fragment = mHomeFragment;
                tag = "0";
                fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                break;
            case 1:
                fragment = new OrdersBookFragment();
                tag = "1";
                break;
            case 2:
                fragment = new ActiveOrdersFragment();
                tag = "2";
                break;
            case 3:
                fragment = new TradeHistoryFragment();
                tag = "3";
                break;
            case 4:
                fragment = new TransHistoryFragment();
                tag = "4";
                break;
            // storing ChartsFragment in memory due to big size of charts
            case 5:
                fragment = mChartsFragment;
                tag = "5";
                break;
            case 6:
                fragment = new SettingsFragment();
                tag = "6";
                break;
            case 7:
                fragment = new NotifiersFragment();
                tag = "7";
                break;
            case 8:
                fragment = new HelpFragment();
                tag = "8";
                break;
            default:
                break;
        }
        final Fragment fr = fragment;
        final String tag1 = tag;
        if (fr != null) {
            //post delayed for smooth behaviour
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {

                    fragmentManager.beginTransaction()
                            .replace(R.id.content_frame, fr)
                            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                            .addToBackStack(tag1)
                            .commit();
                }
            }, 250);
            mDrawerList.setItemChecked(position, true);
            mDrawerList.setSelection(position);
            mDrawerLayout.closeDrawer(mDrawerList);

        }
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppRater.app_launched(this);

        BitmapDrawable bg = (BitmapDrawable) getResources().getDrawable(R.drawable.bg_striped);
        bg.setTileModeXY(Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
        getActionBar().setBackgroundDrawable(bg);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        alarmSet = sharedPreferences.getBoolean("periodicalCheckEnabled", false);
        if (alarmSet) {
            setRecurringAlarm(sharedPreferences.getLong("periodForChecking", 30000));
        }

        setContentView(R.layout.main);
        String[] navSections = getResources().getStringArray(R.array.NavSections);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);
        mDrawerList.setAdapter(new ArrayAdapter<>(this, R.layout.drawer_list_item, navSections));
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());
        app = new App(this);

        mDrawerToggle = new ActionBarDrawerToggle(this,
                mDrawerLayout,
                R.drawable.ic_drawer, //nav menu toggle icon
                R.string.app_name, // nav drawer open - description for accessibility
                R.string.app_name // nav drawer close - description for accessibility
        ) {
            public void onDrawerClosed(View view) {

                // calling onPrepareOptionsMenu() to show action bar icons
                invalidateOptionsMenu();
            }

            public void onDrawerOpened(View drawerView) {

                // calling onPrepareOptionsMenu() to hide action bar icons
                invalidateOptionsMenu();
            }
        };

        mDrawerLayout.setDrawerListener(mDrawerToggle);
        // enabling action bar app icon and behaving it as toggle button
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction().add(R.id.content_frame, mHomeFragment).commit();
        }

    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        // Handle your other action bar items...

        return super.onOptionsItemSelected(item);
    }

    /**
     * Sets up alarm used for periodic check
     *
     * @param msecs Checking period
     */
    public void setRecurringAlarm(long msecs) {
        Intent downloader = new Intent(this, StartServiceReceiver.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        pendingIntent = PendingIntent.getBroadcast(this, 0, downloader, PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);
        alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + 5000,
                msecs,
                pendingIntent);
        alarmSet = true;
    }

    @Override
    protected void onDestroy() {
        if (alarmSet) {
            alarmManager.cancel(pendingIntent);
        }
        if (isFinishing()) {
            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                    .setSmallIcon(R.drawable.ic_launcher)
                    .setContentTitle(getResources().getString(R.string.app_name))
                    .setContentText("App was closed by system. Please start it again if needed");

            mBuilder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
            NotificationManager mNotificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.notify(100500, mBuilder.build());
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        FragmentManager fm = getFragmentManager();
        if (fm.getBackStackEntryCount() != 0) {
            String stackName = fm.getBackStackEntryAt(fm.getBackStackEntryCount() - 1).getName();
            mDrawerList.setItemChecked(Integer.parseInt(stackName), true);
            mDrawerList.setSelection(Integer.parseInt(stackName));
        }
    }

    /**
     * Listener for NavigationDrawer navigation
     */
    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            displayItem(position);
        }
    }

}