package com.QuarkLabs.BTCeClient.utils;

import android.app.NotificationManager;
import android.content.Context;
import android.media.RingtoneManager;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;

import com.QuarkLabs.BTCeClient.R;

public final class ContextUtils {

    private ContextUtils() { }

    /**
     * Creates app notification
     *
     * @param context Context
     * @param id      Notification id
     * @param message Message to show
     */
    public static void makeNotification(@NonNull Context context, int id,
                                        @NonNull String message) {
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_stat_bitcoin_sign)
                .setContentTitle(context.getString(R.string.app_name))
                .setColor(context.getResources().getColor(R.color.colorPrimary))
                .setContentText(message);

        mBuilder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        //noinspection ConstantConditions
        notificationManager.notify(id, mBuilder.build());
    }
}
