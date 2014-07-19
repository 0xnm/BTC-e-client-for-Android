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
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ListView;
import com.QuarkLabs.BTCeClient.exchangeApi.App;
import com.QuarkLabs.BTCeClient.fragments.*;
import com.QuarkLabs.BTCeClient.interfaces.ActivityCallbacks;


public class MainActivity extends Activity
        implements ActivityCallbacks, SharedPreferences.OnSharedPreferenceChangeListener {

    public static App app;
    private static AlarmManager alarmManager;
    private static boolean alarmSet;
    private static PendingIntent pendingIntent;
    private HomeFragment mHomeFragment = new HomeFragment();
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private ActionBarDrawerToggle mDrawerToggle;
    private String[] mDrawerListItems;
    private boolean isDrawerLocked = false;

    /**
     * Displays selected fragment
     *
     * @param position Position at the list (0-based)
     */
    private void displayItem(final int position) {
        Fragment fragment = null;
        final FragmentManager fragmentManager = getFragmentManager();
        switch (position) {
            case 0:
                fragment = mHomeFragment;
                fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                break;
            case 1:
                fragment = new OrdersBookFragment();
                break;
            case 2:
                fragment = new ActiveOrdersFragment();
                break;
            case 3:
                fragment = HistoryFragment.newInstance(ListType.Trades);
                break;
            case 4:
                fragment = HistoryFragment.newInstance(ListType.Transactions);
                break;
            case 5:
                fragment = new ChartsFragment();
                break;
            case 6:
                fragment = new SettingsFragment();
                break;
            case 7:
                fragment = new NotifiersFragment();
                break;
            case 8:
                fragment = new HelpFragment();
                break;
            default:
                break;
        }
        final Fragment fr = fragment;
        if (fr != null) {
            //delay in msecs
            int delay = 250;
            //post delayed for smooth behaviour
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {

                    FragmentTransaction transaction = fragmentManager.beginTransaction()
                            .replace(R.id.content_frame, fr)
                            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
                    if (position != 0) {
                        transaction.addToBackStack(String.valueOf(position)); //name of fragment = position
                    }

                    transaction.commit();
                    setTitle(mDrawerListItems[position]);
                }
            }, delay);
            mDrawerList.setItemChecked(position, true);
            mDrawerList.setSelection(position);
            if (!isDrawerLocked) {
                mDrawerLayout.closeDrawer(mDrawerList);
            }
        }
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        AppRater.app_launched(this);
        BitmapDrawable bg = (BitmapDrawable) getResources().getDrawable(R.drawable.bg_striped);
        bg.setTileModeXY(Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setBackgroundDrawable(bg);
        }

        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        onVersionUpdate(sharedPreferences);

        alarmSet = sharedPreferences.getBoolean(SettingsFragment.KEY_CHECK_ENABLED, true);
        if (alarmSet) {
            setRecurringAlarm(Integer.parseInt(
                    sharedPreferences.getString(SettingsFragment.KEY_CHECK_PERIOD, "60000")));
        }

        mDrawerListItems = getResources().getStringArray(R.array.NavSections);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        FrameLayout contentFrame = (FrameLayout) findViewById(R.id.content_frame);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);
        mDrawerList.setAdapter(new ArrayAdapter<>(this, R.layout.drawer_list_item, mDrawerListItems));
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, Gravity.START);
        if (((ViewGroup.MarginLayoutParams) contentFrame.getLayoutParams()).leftMargin ==
                (int) getResources().getDimension(R.dimen.drawer_size)) {
            mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_OPEN, mDrawerList);
            mDrawerLayout.setScrimColor(Color.TRANSPARENT);
            mDrawerLayout.setFocusableInTouchMode(false);
            isDrawerLocked = true;
        }

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

        if (!isDrawerLocked) {
            mDrawerLayout.setDrawerListener(mDrawerToggle);
            // enabling action bar app icon and behaving it as toggle button
            getActionBar().setDisplayHomeAsUpEnabled(true);
            getActionBar().setHomeButtonEnabled(true);
        }

        if (savedInstanceState == null) {
            displayItem(0);
        }

    }

    private void onVersionUpdate(final SharedPreferences sharedPreferences) {
        final String keyToCheck = "newSecuritySystemShown";
        boolean needNotify = sharedPreferences.getBoolean(keyToCheck, false);
        if (needNotify) {
            PreferenceManager.setDefaultValues(this, R.xml.preferences, true);
            String messageTitle = "API key security system";
            String message = "New security system added with this update. " +
                    "Please enter your key and secret once again in Settings section. " +
                    "Now they will be stored in encrypted state, " +
                    "it will save them from being compromised even if device is rooted.";
            new AlertDialog.Builder(this)
                    .setNeutralButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putBoolean(keyToCheck, true);
                            editor.commit();
                        }
                    })
                    .setTitle(messageTitle)
                    .setMessage(message)
                    .show();
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
        return mDrawerToggle.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);
    }

    /**
     * Sets up alarm used for periodic check
     *
     * @param msecs Checking period
     */
    public void setRecurringAlarm(long msecs) {
        Intent downloader = new Intent(this, StartServiceReceiver.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
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
    public void onBackPressed() {
        super.onBackPressed();
        int switchToPosition = 0;
        FragmentManager fm = getFragmentManager();
        if (fm.getBackStackEntryCount() != 0) {
            String stackName = fm.getBackStackEntryAt(fm.getBackStackEntryCount() - 1).getName();
            switchToPosition = Integer.parseInt(stackName);
        }
        mDrawerList.setItemChecked(switchToPosition, true);
        mDrawerList.setSelection(switchToPosition);
        setTitle(mDrawerListItems[switchToPosition]);
    }

    @Override
    public void makeNotification(int id, String message) {
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_stat_bitcoin_sign)
                .setContentTitle(getResources().getString(R.string.app_name))
                .setContentText(message);

        mBuilder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(id, mBuilder.build());
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(SettingsFragment.KEY_CHECK_ENABLED)) {
            alarmSet = sharedPreferences.getBoolean(SettingsFragment.KEY_CHECK_ENABLED, false);
            if (alarmSet) {
                setRecurringAlarm(Integer
                        .parseInt(sharedPreferences.getString(SettingsFragment.KEY_CHECK_PERIOD, "60000")));
            } else {
                alarmManager.cancel(pendingIntent);
            }
        } else if (key.equals(SettingsFragment.KEY_CHECK_PERIOD)) {
            setRecurringAlarm(Integer.parseInt(sharedPreferences.getString(key, "60000")));
        }
        if (key.equals(SettingsFragment.KEY_API_KEY) || key.equals(SettingsFragment.KEY_API_SECRET)) {
            app = new App(this);
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