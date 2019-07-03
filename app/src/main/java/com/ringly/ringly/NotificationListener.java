package com.ringly.ringly;

import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import com.ringly.ringly.bluetooth.RinglyService;
import com.ringly.ringly.config.NotificationType;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import rx.subjects.PublishSubject;

public final class NotificationListener extends NotificationListenerService {

    // from android.provider.Settings, but not currently exposed
    // TODO remove when this bug is resolved: https://code.google.com/p/android/issues/detail?id=58030
    public static final String ACTION_NOTIFICATION_LISTENER_SETTINGS
            = "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS";

    // https://android.googlesource.com/platform/packages/apps/Settings/+/master/src/com/android/settings/bluetooth/BluetoothPermissionRequest.java
    public static final int PAIRING_NOTIFICATION_ID = 17301632; // android.R.drawable.stat_sys_data_bluetooth;

    private static final String TAG = NotificationListener.class.getCanonicalName();

    private final Set<String> mProcessedNotifications = new HashSet<>();

    private final PublishSubject<StatusBarNotification> mSBNSubject =
        PublishSubject.create();

    public static boolean isEnabled(Context context) {
        return NotificationManagerCompat.getEnabledListenerPackages(context)
                .contains(context.getPackageName());
    }


    @Override
    public void onCreate() {
        //noinspection HardCodedStringLiteral
        Log.d(TAG, "onCreate");

        // Group notifications with a one second debounce and notify for each distinct app
        mSBNSubject
            .window(mSBNSubject.debounce(1, TimeUnit.SECONDS))
            .flatMap(sbnobs -> sbnobs.distinct(StatusBarNotification::getPackageName))
            .subscribe(
                sbn -> {
                    for (final NotificationType type : NotificationType.values()) {
                        if (type.ids.contains(sbn.getPackageName())) {
                            RinglyService.doNotify(type, this);
                            return;
                        }
                    }

                    RinglyService.doNotify(sbn.getPackageName(), this);
                },
                err -> Log.e(TAG, "SBN debounce error", err)
            );
    }

    @Override
    public IBinder onBind(final Intent intent) {
        //noinspection HardCodedStringLiteral
        Log.d(TAG, "onBind: " + intent);

        // return to the app:
        // TODO need to check back stack or set a variable or something, otherwise this happens at startup
//        startActivity(new Intent(this, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));

        return super.onBind(intent);
    }

    @Override
    public void onNotificationPosted(final StatusBarNotification sbn) {
        //noinspection HardCodedStringLiteral
        Log.d(TAG, "onNotificationPosted: " + sbn);

        String key = key(sbn);

        if(mProcessedNotifications.contains(key)) {
            return;
        }

        mProcessedNotifications.add(key);

        mSBNSubject.onNext(sbn);

    }

    @Override
    public void onNotificationRemoved(final StatusBarNotification sbn) {
        //noinspection HardCodedStringLiteral
        Log.d(TAG, "onNotificationRemoved: " + sbn);

        mProcessedNotifications.remove(key(sbn));
    }

    private String key(StatusBarNotification sbn) {
        // Unique (as possible) id per notification
        return sbn.getUserId() + "|" + sbn.getPackageName() + "|" + sbn.getId() + "|" + sbn.getTag();
    }
}
