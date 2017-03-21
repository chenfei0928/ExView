package com.chenfei.exview.internal;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;

import com.chenfei.exview.R;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.JELLY_BEAN;

public final class ExViewInternals {
    public static void showNotification(Context context, CharSequence contentTitle,
                                        CharSequence contentText, PendingIntent pendingIntent, int notificationId) {
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        Notification notification;
        Notification.Builder builder = new Notification.Builder(context) //
                .setSmallIcon(R.drawable.exview_notification)
                .setWhen(System.currentTimeMillis())
                .setContentTitle(contentTitle)
                .setContentText(contentText)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);
        if (SDK_INT < JELLY_BEAN) {
            notification = builder.getNotification();
        } else {
            notification = builder.build();
        }
        notificationManager.notify(notificationId, notification);
    }

    public static Executor newSingleThreadExecutor(String threadName) {
        return Executors.newSingleThreadExecutor(new ExViewSingleThreadFactory(threadName));
    }

    private ExViewInternals() {
        throw new AssertionError();
    }
}
