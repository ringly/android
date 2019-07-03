package com.ringly.ringly.config;

/**
 * Created by lindaliu on 1/13/16.
 *
 * Possible notification modes for an app, since it is now possible for a
 * notification to have color/vibration settings but be turned off.
 *
 * NotificationMode is set in the NotificationsFragment, which loops through
 * the apps. If it sees UNSET, it will check if the app has a set color/vibration.
 * If it does, it will set the mode to ENABLED, else DISABLED.
 */
public enum NotificationMode {
    /*
     Notifications are on
     */
    ENABLED,
    /*
    Notifications are off
     */
    DISABLED,
    /*
    This is the default value that getNotificationMode returns, which is
    used for migrating existing users. The app will behave as before as
    this case.
     */
    UNSET,

    REMOVED
}
