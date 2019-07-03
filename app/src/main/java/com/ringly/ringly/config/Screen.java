package com.ringly.ringly.config;

import android.support.annotation.DrawableRes;
import android.support.annotation.IdRes;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;

import com.ringly.ringly.R;
import com.ringly.ringly.ui.screens.mindfulness.MindfulnessFragment;

import com.ringly.ringly.ui.screens.activity.ActivityFragment;
import com.ringly.ringly.ui.screens.activity.ActivityPlaceholderFragment;
import com.ringly.ringly.ui.screens.connection.ConnectionFragment;
import com.ringly.ringly.ui.screens.ContactsFragment;
import com.ringly.ringly.ui.screens.NotificationsFragment;
import com.ringly.ringly.ui.screens.preferences.PreferencesFragment;
import com.ringly.ringly.ui.screens.connection.IntroFragment;
import com.ringly.ringly.ui.screens.connection.SetupFragment;

@SuppressWarnings({"InstanceVariableNamingConvention", "PublicField"})
public enum Screen {
    CONNECT(R.drawable.connect_icon, R.string.connect, R.string.connection_title,
        R.id.nav_item_connection, IntroFragment.class),
    SETUP(R.drawable.connect_icon, R.string.setup_notifications, R.string.connection_title,
        R.id.nav_item_connection, SetupFragment.class),
    CONNECTION(R.drawable.connect_icon, R.string.connection, R.string.connection_title,
        R.id.nav_item_connection, ConnectionFragment.class),
    NOTIFICATIONS(R.drawable.screens_notifications, R.string.notifications,
        R.string.notifications_title, R.id.nav_item_notifications, NotificationsFragment.class),
    CONTACTS(R.drawable.contacts_icon, R.string.contacts, R.string.contacts_title,
        R.id.nav_item_contacts, ContactsFragment.class),
    ACTIVITY(R.drawable.activity_icon, R.string.activity, R.string.activity_title,
        R.id.nav_item_activity, ActivityFragment.class),
    ACTIVITY_PLACEHOLDER(R.drawable.activity_icon, R.string.activity, R.string.activity_title,
        R.id.nav_item_activity, ActivityPlaceholderFragment.class),
    PREFERENCES(R.drawable.screens_profile, R.string.preferences, R.string.preferences_title,
        R.id.nav_item_preferences, PreferencesFragment.class),
    MINDFULNESS(R.drawable.meditation_icon, R.string.mindfulness, R.string.mindfulness_title,
        R.id.nav_item_mindfulness, MindfulnessFragment.class),
    ;

    @DrawableRes public final int iconId;
    @StringRes public final int nameId;
    @StringRes public final int actionbarTitle;
    @IdRes public final int navId;
    public final Class<? extends Fragment> cls;
    Screen(
        @DrawableRes final int iconId, @StringRes final int nameId,
        @StringRes final int actionbarTitle, int navId, final Class<? extends Fragment> cls
    ) {
        this.iconId = iconId;
        this.nameId = nameId;
        this.actionbarTitle = actionbarTitle;
        this.navId = navId;
        this.cls = cls;
    }
}
