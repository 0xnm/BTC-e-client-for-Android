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

package com.QuarkLabs.BTCeClient.services;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;

import com.QuarkLabs.BTCeClient.data.AppPreferences;
import com.QuarkLabs.BTCeClient.BtcEApplication;
import com.QuarkLabs.BTCeClient.ConstantHolder;
import com.QuarkLabs.BTCeClient.data.DBWorker;
import com.QuarkLabs.BTCeClient.data.InMemoryStorage;
import com.QuarkLabs.BTCeClient.ui.MainActivity;
import com.QuarkLabs.BTCeClient.utils.PairUtils;
import com.QuarkLabs.BTCeClient.R;
import com.QuarkLabs.BTCeClient.Watcher;
import com.QuarkLabs.BTCeClient.api.Api;
import com.QuarkLabs.BTCeClient.api.CallResult;
import com.QuarkLabs.BTCeClient.api.Ticker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CheckTickersService extends IntentService {

    public CheckTickersService() {
        super(CheckTickersService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        final Api api = BtcEApplication.get(this).getApi();
        final AppPreferences appPreferences = BtcEApplication.get(this).getAppPreferences();
        final InMemoryStorage inMemoryStorage = BtcEApplication.get(this).getInMemoryStorage();

        Set<String> pairsToCheck = new HashSet<>();
        List<String> dashboardPairs = appPreferences.getPairsToDisplay();

        pairsToCheck.addAll(dashboardPairs);
        pairsToCheck.addAll(watcherPairs());

        if (pairsToCheck.isEmpty()) {
            sendUpdateFailedBroadcast();
            return;
        }

        if (networkInfo == null || !networkInfo.isConnected()) {
            sendUpdateFailedBroadcast();
            return;
        }

        CallResult<List<Ticker>> result = api.getPairInfo(pairsToCheck);

        if (!result.isSuccess()) {
            sendUpdateFailedBroadcast();
            return;
        }

        List<Ticker> tickers = result.getPayload();

        List<NotificationMessage> messages =
                createNotificationMessages(tickers, inMemoryStorage.getLatestData());

        for (NotificationMessage message : messages) {
            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

            Notification notification = new NotificationCompat.Builder(this)
                    .setContentTitle(getString(R.string.app_name))
                    .setSmallIcon(R.drawable.ic_stat_bitcoin_sign)
                    .setColor(getResources().getColor(R.color.colorPrimary))
                    .setSound(
                            RingtoneManager.getDefaultUri(
                                    RingtoneManager.TYPE_NOTIFICATION))
                    .setContentTitle(message.title)
                    .setContentText(message.details)
                    .setTicker(message.title)
                    .setContentIntent(
                            PendingIntent.getActivity(
                                    this, 0, new Intent(this, MainActivity.class),
                                    PendingIntent.FLAG_UPDATE_CURRENT)
                    )
                    .setAutoCancel(true)
                    .build();

            notificationManager.notify((int) System.currentTimeMillis(), notification);
        }

        Map<String, Ticker> newData = new HashMap<>();
        for (Ticker ticker : tickers) {
            newData.put(ticker.getPair(), ticker);
        }

        inMemoryStorage.saveTickers(newData);
        LocalBroadcastManager.getInstance(this)
                .sendBroadcast(new Intent(ConstantHolder.UPDATE_TICKERS_ACTION));
    }

    private void sendUpdateFailedBroadcast() {
        LocalBroadcastManager.getInstance(this)
                .sendBroadcast(new Intent(ConstantHolder.UPDATE_TICKERS_FAILED_ACTION));
    }

    private List<String> watcherPairs() {
        Cursor watchersCursor = DBWorker.getInstance(this).getNotifiers();
        List<String> watcherPairs = new ArrayList<>();
        watchersCursor.moveToFirst();
        while (!watchersCursor.isAfterLast()) {
            String pair = watchersCursor.getString(
                    watchersCursor.getColumnIndex(DBWorker.NOTIFIERS_PAIR_COLUMN));
            if (PairUtils.isSupportedPair(this, pair)) {
                watcherPairs.add(pair);
            }
            watchersCursor.moveToNext();
        }
        watchersCursor.close();
        return watcherPairs;
    }

    /**
     * Processes new data, adds notifications if any
     *
     * @param tickers New tickers data
     * @param oldData Old tickers data
     * @return Notification messages
     */
    @NonNull
    private List<NotificationMessage> createNotificationMessages(
            @NonNull List<Ticker> tickers, @NonNull Map<String, Ticker> oldData) {

        Map<String, List<WatcherDescriptor>> watchersByPair = watchersByPair();

        List<NotificationMessage> messages = new ArrayList<>();

        for (Ticker ticker : tickers) {
            String pair = ticker.getPair();

            if (!watchersByPair.containsKey(pair)) {
                continue;
            }
            double newValue = ticker.getLast().doubleValue();

            for (WatcherDescriptor watcher : watchersByPair.get(pair)) {
                @Watcher int watcherType = watcher.type;
                switch (watcherType) {
                    case Watcher.PANIC_BUY:
                        if (oldData.containsKey(pair)) {
                            double oldValue = oldData.get(pair).getLast().doubleValue();
                            float percent = watcher.value;
                            if (oldValue != 0 && newValue > ((1 + (percent / 100)) * oldValue)) {
                                messages.add(createPanicBuyMessage(pair, percent));
                            }
                        }
                        break;
                    case Watcher.PANIC_SELL:
                        if (oldData.containsKey(pair)) {
                            double oldValue = oldData.get(pair).getLast().doubleValue();
                            float percent = watcher.value;
                            if (oldValue != 0 && newValue < ((1 - (percent / 100)) * oldValue)) {
                                messages.add(createPanicSellMessage(pair, percent));
                            }
                        }
                        break;
                    case Watcher.STOP_LOSS:
                        if (newValue != 0 && newValue < watcher.value) {
                            messages.add(createStopLossMessage(pair, watcher.value));
                        }
                        break;
                    case Watcher.TAKE_PROFIT:
                        if (newValue != 0 && newValue > watcher.value) {
                            messages.add(createTakeProfitMessage(pair, watcher.value));
                        }
                        break;
                    default:
                        break;
                }
            }
        }
        return messages;
    }

    @NonNull
    private Map<String, List<WatcherDescriptor>> watchersByPair() {
        Cursor watchers = DBWorker.getInstance(this).getNotifiers();
        watchers.moveToFirst();
        Map<String, List<WatcherDescriptor>> watchersByPair = new HashMap<>();
        while (!watchers.isAfterLast()) {
            String pair = watchers.getString(
                    watchers.getColumnIndex(DBWorker.NOTIFIERS_PAIR_COLUMN));
            if (!watchersByPair.containsKey(pair)) {
                watchersByPair.put(pair, new ArrayList<WatcherDescriptor>());
            }

            List<WatcherDescriptor> watchersForPair = watchersByPair.get(pair);
            WatcherDescriptor watcherDescriptor = new WatcherDescriptor();

            //noinspection WrongConstant
            watcherDescriptor.type = watchers.getInt(
                    watchers.getColumnIndex(DBWorker.NOTIFIERS_TYPE_COLUMN));
            watcherDescriptor.value = watchers.getFloat(
                    watchers.getColumnIndex(DBWorker.NOTIFIERS_VALUE_COLUMN));
            watchersForPair.add(watcherDescriptor);

            watchers.moveToNext();
        }

        watchers.close();
        return watchersByPair;
    }

    private NotificationMessage createStopLossMessage(String pair, float value) {
        NotificationMessage message = new NotificationMessage();
        message.title = getString(R.string.watcher_alarm,
                getString(R.string.watcher_stop_loss), pair);
        message.details = getString(R.string.watcher_alarm_stop_loss_details,
                pair, String.valueOf(value), pair.split("/")[1]);
        return message;
    }

    private NotificationMessage createTakeProfitMessage(String pair, float value) {
        NotificationMessage message = new NotificationMessage();
        message.title = getString(R.string.watcher_alarm,
                getString(R.string.watcher_take_profit), pair);
        message.details = getString(R.string.watcher_alarm_take_profit_details,
                pair, String.valueOf(value), pair.split("/")[1]);
        return message;
    }

    private NotificationMessage createPanicBuyMessage(String pair, float value) {
        NotificationMessage message = new NotificationMessage();
        message.title = getString(R.string.watcher_alarm,
                getString(R.string.watcher_panic_buy), pair);
        message.details = getString(R.string.watcher_alarm_panic_buy_details,
                pair, String.valueOf(value));
        return message;
    }

    private NotificationMessage createPanicSellMessage(String pair, float value) {
        NotificationMessage message = new NotificationMessage();
        message.title = getString(R.string.watcher_alarm,
                getString(R.string.watcher_panic_sell), pair);
        message.details = getString(R.string.watcher_alarm_panic_sell_details,
                pair, String.valueOf(value));
        return message;
    }

    private static final class NotificationMessage {
        String title;
        String details;
    }

    private static final class WatcherDescriptor {
        @Watcher
        int type;
        float value;
    }
}
