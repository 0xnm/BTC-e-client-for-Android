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

package com.QuarkLabs.BTCeClient.ui;

import android.app.AlarmManager;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.NotificationCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.QuarkLabs.BTCeClient.data.AppPreferences;
import com.QuarkLabs.BTCeClient.AppRater;
import com.QuarkLabs.BTCeClient.BtcEApplication;
import com.QuarkLabs.BTCeClient.ui.history.ListType;
import com.QuarkLabs.BTCeClient.MainHost;
import com.QuarkLabs.BTCeClient.R;
import com.QuarkLabs.BTCeClient.ui.activeorders.ActiveOrdersFragment;
import com.QuarkLabs.BTCeClient.ui.charts.ChartsFragment;
import com.QuarkLabs.BTCeClient.ui.help.HelpFragment;
import com.QuarkLabs.BTCeClient.ui.history.HistoryFragment;
import com.QuarkLabs.BTCeClient.ui.terminal.HomeFragment;
import com.QuarkLabs.BTCeClient.ui.watchers.WatchersFragment;
import com.QuarkLabs.BTCeClient.ui.depth.OrdersBookFragment;
import com.QuarkLabs.BTCeClient.ui.settings.SettingsFragment;
import com.QuarkLabs.BTCeClient.interfaces.ActivityCallbacks;
import com.QuarkLabs.BTCeClient.services.CheckTickersService;
import com.QuarkLabs.BTCeClient.ui.chat.ChatFragment;
import com.andrognito.pinlockview.IndicatorDots;
import com.andrognito.pinlockview.PinLockListener;
import com.andrognito.pinlockview.PinLockView;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity
        implements ActivityCallbacks, MainHost {

    private static final String SHOULD_SHOW_PIN_VIEW_KEY = "SHOULD_SHOW_PIN_VIEW";
    private static final int PIN_MAX_ATTEMPTS = 5;

    private boolean isAlarmSet;

    @NonNull
    private final HomeFragment homeFragment = new HomeFragment();
    @Nullable
    private OrdersBookFragment ordersBookFragment;

    private DrawerLayout drawerLayout;
    private ListView drawerList;
    private ActionBarDrawerToggle drawerToggle;
    private String[] drawerListItems;
    private final Handler uiHandler = new Handler();

    private PinLockView pinLockView;
    private View pinContainer;
    private TextView pinTitleView;
    private TextView pinAttemptsLeftView;

    @Nullable
    private Runnable displayTask;

    private AppPreferences appPreferences;
    private final PreferencesListener preferencesListener = new PreferencesListener();
    private boolean inPinSetupMode;

    private boolean shouldShowPinView;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        appPreferences = BtcEApplication.get(this).getAppPreferences();
        AppRater.trackAppLaunch(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }

        appPreferences.addListener(preferencesListener);

        isAlarmSet = appPreferences.isPeriodicCheckEnabled();
        if (isAlarmSet) {
            setRecurringAlarm(Integer.parseInt(appPreferences.getCheckPeriodMillis()));
        }

        drawerListItems = getResources().getStringArray(R.array.NavSections);
        drawerList = (ListView) findViewById(R.id.left_drawer);
        drawerList.setAdapter(new ArrayAdapter<>(this,
                R.layout.drawer_list_item, drawerListItems));
        drawerList.setOnItemClickListener((parent, view, position, id) ->
                displayItem(position, true));
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawerLayout != null) {
            drawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
            drawerToggle = new ActionBarDrawerToggle(this,
                    drawerLayout,
                    R.string.app_name,
                    R.string.app_name);
            drawerLayout.setDrawerListener(drawerToggle);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        pinLockView = (PinLockView) findViewById(R.id.pin_lock_view);
        pinContainer = findViewById(R.id.pin_container);
        pinTitleView = (TextView) findViewById(R.id.pin_title);
        pinAttemptsLeftView = (TextView) findViewById(R.id.pin_attempts_left);

        pinLockView.attachIndicatorDots((IndicatorDots) findViewById(R.id.indicator_dots));

        if (savedInstanceState == null) {
            displayItem(0, false);
            shouldShowPinView = appPreferences.isPinProtectionEnabled();
        } else {
            shouldShowPinView = savedInstanceState.getBoolean(SHOULD_SHOW_PIN_VIEW_KEY);
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (drawerLayout != null) {
            drawerToggle.syncState();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (drawerLayout != null) {
            drawerToggle.onConfigurationChanged(newConfig);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        displayItem(0, false);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (shouldShowPinView) {
            int attempts = appPreferences.getPinAttempts();
            showPinView(attempts == PIN_MAX_ATTEMPTS ? R.string.pin_max_attempts
                    : R.string.enter_pin, new PinLockListener() {
                @Override
                public void onComplete(String pin) {
                    if (pin.equals(appPreferences.getPin())
                            && appPreferences.getPinAttempts() != PIN_MAX_ATTEMPTS) {
                        appPreferences.setPinAttempts(0);
                        hidePinView();
                    } else {
                        pinLockView.resetPinLockView();
                        if (appPreferences.getPinAttempts() == PIN_MAX_ATTEMPTS) {
                            pinTitleView.setText(R.string.pin_max_attempts);
                        } else {
                            appPreferences.setPinAttempts(appPreferences.getPinAttempts() + 1);
                        }
                        pinAttemptsLeftView.setText(
                                getString(R.string.pin_attempts_left,
                                        PIN_MAX_ATTEMPTS - appPreferences.getPinAttempts()));
                    }
                }

                @Override
                public void onEmpty() {
                    // not interested
                }

                @Override
                public void onPinChange(int pinLength, String intermediatePin) {
                    // not interested
                }
            }, false);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        uiHandler.removeCallbacks(displayTask);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (isChangingConfigurations()) {
            shouldShowPinView = pinContainer.getVisibility()
                    == View.VISIBLE && appPreferences.isPinProtectionEnabled();
        } else {
            shouldShowPinView = appPreferences.isPinProtectionEnabled();
        }
        outState.putBoolean(SHOULD_SHOW_PIN_VIEW_KEY, shouldShowPinView);
    }

    @Override
    protected void onDestroy() {
        appPreferences.removeListener(preferencesListener);
        super.onDestroy();
    }

    /**
     * Displays selected fragment
     *
     * @param position Position at the list (0-based)
     */
    private void displayItem(final int position, boolean fromDrawer) {
        Fragment fragment = null;
        final FragmentManager fragmentManager = getFragmentManager();
        switch (position) {
            case 0:
                fragment = homeFragment;
                fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                break;
            case 1:
                if (ordersBookFragment == null) {
                    ordersBookFragment = new OrdersBookFragment();
                }
                fragment = ordersBookFragment;
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
                fragment = new ChatFragment();
                break;
            case 7:
                fragment = new SettingsFragment();
                break;
            case 8:
                fragment = new WatchersFragment();
                break;
            case 9:
                fragment = new HelpFragment();
                break;
            default:
                break;
        }
        final Fragment fr = fragment;
        if (fr != null) {
            displayTask = () -> {
                FragmentTransaction transaction = fragmentManager.beginTransaction()
                        .setCustomAnimations(R.animator.fade_in, R.animator.fade_out)
                        .replace(R.id.content_frame, fr);
                if (position != 0) {
                    //name of fragment = position
                    transaction.addToBackStack(String.valueOf(position));
                }
                transaction.commit();
                setTitle(drawerListItems[position]);
                drawerList.setItemChecked(position, true);
                drawerList.setSelection(position);
            };
            if (fromDrawer) {
                //delay in msecs
                int delay = 250;
                //post delayed for smooth behaviour
                uiHandler.postDelayed(displayTask, delay);
            } else {
                displayTask.run();
            }
            if (drawerLayout != null && drawerLayout.isDrawerOpen(drawerList)) {
                drawerLayout.closeDrawer(drawerList);
            }
        }
    }

    @Override
    public void setupPinView(@NonNull PinLockListener listener) {
        inPinSetupMode = true;
        showPinView(R.string.enter_pin, listener, true);
    }

    @Override
    public void hidePinView() {
        inPinSetupMode = false;
        pinContainer.setVisibility(View.GONE);
        pinLockView.setPinLockListener(null);
        pinLockView.resetPinLockView();
    }

    public void showPinView(@StringRes int titleId, @Nullable PinLockListener listener,
                            boolean isSetup) {
        if (isSetup) {
            pinAttemptsLeftView.setVisibility(View.GONE);
        } else {
            pinAttemptsLeftView.setVisibility(View.VISIBLE);
        }
        pinAttemptsLeftView.setText(getString(R.string.pin_attempts_left,
                PIN_MAX_ATTEMPTS - appPreferences.getPinAttempts()));
        pinTitleView.setText(titleId);
        pinContainer.setVisibility(View.VISIBLE);
        pinLockView.setPinLockListener(listener);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        if (drawerLayout != null) {
            return drawerToggle.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Sets up alarm used for periodic check
     *
     * @param msecs Checking period
     */
    private void setRecurringAlarm(long msecs) {
        PendingIntent pendingIntent = pendingIntentForRecurringCheck();
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        //noinspection ConstantConditions
        alarmManager.cancel(pendingIntent);
        alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(5), msecs, pendingIntent);
        isAlarmSet = true;
    }

    private PendingIntent pendingIntentForRecurringCheck() {
        return PendingIntent.getService(this, 0,
                new Intent(this, CheckTickersService.class), PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @Override
    public void onBackPressed() {
        FragmentManager fm = getFragmentManager();
        if (inPinSetupMode) {
            hidePinView();
            Fragment currentFragment = fm.findFragmentById(R.id.content_frame);
            if (currentFragment instanceof SettingsFragment) {
                ((SettingsFragment) currentFragment).disablePinSetupMode();
            }
        } else if (pinContainer.getVisibility() == View.VISIBLE) {
            finish();
        } else {
            super.onBackPressed();
            int switchToPosition = 0;
            if (fm.getBackStackEntryCount() != 0) {
                String stackName = fm.getBackStackEntryAt(fm.getBackStackEntryCount() - 1)
                        .getName();
                switchToPosition = Integer.parseInt(stackName);
            }
            drawerList.setItemChecked(switchToPosition, true);
            drawerList.setSelection(switchToPosition);
            setTitle(drawerListItems[switchToPosition]);
        }
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
        //noinspection ConstantConditions
        mNotificationManager.notify(id, mBuilder.build());
    }

    @Override
    public void openTradingSection(@NonNull String pair, @NonNull BigDecimal price) {
        homeFragment.addShowTradingTask(pair, price);
        displayItem(0, false);
    }

    private final class PreferencesListener extends AppPreferences.Listener {
        @Override
        public void onCheckStatus(boolean isEnabled, @Nullable String periodMillis) {
            isAlarmSet = isEnabled;
            if (isAlarmSet) {
                setRecurringAlarm(Integer.parseInt(periodMillis));
            } else {
                AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
                //noinspection ConstantConditions
                alarmManager.cancel(pendingIntentForRecurringCheck());
            }
        }
    }
}