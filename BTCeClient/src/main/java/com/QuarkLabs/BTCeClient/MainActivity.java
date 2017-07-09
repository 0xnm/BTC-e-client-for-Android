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

import android.app.AlarmManager;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.QuarkLabs.BTCeClient.fragments.ActiveOrdersFragment;
import com.QuarkLabs.BTCeClient.fragments.ChartsFragment;
import com.QuarkLabs.BTCeClient.fragments.HelpFragment;
import com.QuarkLabs.BTCeClient.fragments.HistoryFragment;
import com.QuarkLabs.BTCeClient.fragments.HomeFragment;
import com.QuarkLabs.BTCeClient.fragments.NotifiersFragment;
import com.QuarkLabs.BTCeClient.fragments.OrdersBookFragment;
import com.QuarkLabs.BTCeClient.fragments.SettingsFragment;
import com.QuarkLabs.BTCeClient.interfaces.ActivityCallbacks;

import java.util.concurrent.TimeUnit;


public class MainActivity extends AppCompatActivity
        implements ActivityCallbacks, SharedPreferences.OnSharedPreferenceChangeListener {

    private boolean isAlarmSet;

    private HomeFragment mHomeFragment = new HomeFragment();
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private ActionBarDrawerToggle mDrawerToggle;
    private String[] mDrawerListItems;

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
                            .setCustomAnimations(R.animator.fade_in, R.animator.fade_out)
                            .replace(R.id.content_frame, fr);
                    if (position != 0) {
                        transaction.addToBackStack(String.valueOf(position)); //name of fragment = position
                    }
                    transaction.commit();
                    setTitle(mDrawerListItems[position]);
                }
            }, delay);
            mDrawerList.setItemChecked(position, true);
            mDrawerList.setSelection(position);
            if (mDrawerLayout != null && mDrawerLayout.isDrawerOpen(mDrawerList)) {
                mDrawerLayout.closeDrawer(mDrawerList);
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        displayItem(0);
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        AppRater.trackAppLaunch(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }
        getSupportActionBar().setElevation(20);

        final SharedPreferences sharedPreferences
                = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);

        isAlarmSet = sharedPreferences.getBoolean(SettingsFragment.KEY_CHECK_ENABLED, true);
        if (isAlarmSet) {
            setRecurringAlarm(Integer.parseInt(
                    sharedPreferences.getString(SettingsFragment.KEY_CHECK_PERIOD, "60000")));
        }

        mDrawerListItems = getResources().getStringArray(R.array.NavSections);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);
        mDrawerList.setAdapter(new ArrayAdapter<>(this,
                R.layout.drawer_list_item, mDrawerListItems));
        mDrawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                displayItem(position);
            }
        });
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (mDrawerLayout != null) {
            mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
            mDrawerToggle = new ActionBarDrawerToggle(this,
                    mDrawerLayout,
                    R.string.app_name,
                    R.string.app_name);
            mDrawerLayout.setDrawerListener(mDrawerToggle);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        if (savedInstanceState == null) {
            displayItem(0);
        }

        showWhatsNewOldCharts();

    }

    private void showWhatsNewOldCharts() {
        final String key = "whats_new_old_charts_shown";
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (!prefs.contains(key)) {
            new AlertDialog.Builder(this)
                    .setMessage(R.string.whats_new_old_charts)
                    .setPositiveButton(android.R.string.ok, null)
                    .setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            prefs.edit()
                                    .putBoolean(key, true)
                                    .apply();
                        }
                    })
                    .show();
        }
    }


    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (mDrawerLayout != null) {
            mDrawerToggle.syncState();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (mDrawerLayout != null) {
            mDrawerToggle.onConfigurationChanged(newConfig);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        if (mDrawerLayout != null) {
            return mDrawerToggle.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Sets up alarm used for periodic check
     *
     * @param msecs Checking period
     */
    public void setRecurringAlarm(long msecs) {
        PendingIntent pendingIntent = pendingIntentForRecurringCheck();
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);
        alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(5), msecs, pendingIntent);
        isAlarmSet = true;
    }

    private PendingIntent pendingIntentForRecurringCheck() {
        return PendingIntent.getBroadcast(this, 0,
                new Intent(this, StartServiceReceiver.class), PendingIntent.FLAG_UPDATE_CURRENT);
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
                .setContentTitle(getString(R.string.app_name))
                .setColor(getResources().getColor(R.color.colorPrimary))
                .setContentText(message);

        mBuilder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(id, mBuilder.build());
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (SettingsFragment.KEY_CHECK_ENABLED.equals(key)) {
            isAlarmSet = sharedPreferences.getBoolean(SettingsFragment.KEY_CHECK_ENABLED, false);
            if (isAlarmSet) {
                setRecurringAlarm(Integer
                        .parseInt(sharedPreferences
                                .getString(SettingsFragment.KEY_CHECK_PERIOD, "60000")));
            } else {
                AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
                alarmManager.cancel(pendingIntentForRecurringCheck());
            }
        } else if (SettingsFragment.KEY_CHECK_PERIOD.equals(key)) {
            setRecurringAlarm(Integer.parseInt(sharedPreferences.getString(key, "60000")));
        }
    }
}