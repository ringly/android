package com.ringly.ringly;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.text.format.DateUtils;

import com.google.common.base.Optional;
import com.ringly.ringly.config.Color;
import com.ringly.ringly.config.NotificationMode;
import com.ringly.ringly.config.NotificationType;
import com.ringly.ringly.config.Vibration;
import com.ringly.ringly.config.model.Height;
import com.ringly.ringly.config.model.Weight;
import com.ringly.ringly.ui.screens.preferences.HourPreference;
import com.ringly.ringly.ui.screens.preferences.MindfulMinutesGoalPreferenceDialogFragmentCompat;

import java.util.Calendar;
import java.util.Collections;
import java.util.Set;

public final class Preferences {
    public static final String PREFERENCES_NAME = "preferences";
    private static final String CONTACT_PREFERENCES_NAME = "contacts";

    private static final String AUTH_TOKEN = "authToken";
    private static final String USER_EMAIL = "email";
    private static final String INNER_RING_KEY = "innerRing";
    private static final String NOTIFICATION_INSTRUCTIONS = "showInstructions";
    private static final String NOTIFICATION_ONBOARDING = "notificationOnboarding";
    private static final String ONBOARDING = "showOnboarding";
    private static final String RING_ADDRESS_KEY = "ringAddress";
    private static final String RING_NAME_KEY = "ringName";
    public static final String GOAL = "goal";
    private static final String TEMPORARY_RING_ADDRESS_KEY = "temporaryRingAddress";
    private static final String LAST_ACTIVITY_UPDATE = "lastActivityUpdate";
    public static final String BIRTHDAY = "birthday";

    private static final String SUPPORTS_ACTIVITY = "supportsActivity";

    public static final String HEIGHT = "height";
    private static final String HEIGHT_UNIT = "heightUnit";
    public static final String WEIGHT = "weight";
    private static final String WEIGHT_UNIT = "weightUnit";

    private static final String COLOR_KEY_SUFFIX = "_color";
    private static final String VIBRATION_KEY_SUFFIX = "_vibration";
    private static final String MODE_KEY_SUFFIX = "_mode";


    private final Context mContext;
    private final SharedPreferences mPreferences;
    private final SharedPreferences mContactPreferences;

    public Preferences(final Context context) {
        mContext = context;
        mPreferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        mContactPreferences
                = context.getSharedPreferences(CONTACT_PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    public Optional<String> getRingAddress() {
        return Optional.fromNullable(mPreferences.getString(RING_ADDRESS_KEY, null));
    }

    public Optional<String> getRingName() {
        return Optional.fromNullable(mPreferences.getString(RING_NAME_KEY, null));
    }

    public void setRing(final String address, final String name) {
        mPreferences.edit().putString(RING_ADDRESS_KEY, address).putString(RING_NAME_KEY, name)
                .apply();
    }

    public void unsetRing() {
        mPreferences.edit()
            .remove(RING_ADDRESS_KEY)
            .remove(RING_NAME_KEY)
            .remove(SUPPORTS_ACTIVITY)
            .apply();
    }

    public void temporaryUnsetRing() {
        final Optional<String> address = getRingAddress();
        if (address.isPresent()) {
            mPreferences.edit().putString(TEMPORARY_RING_ADDRESS_KEY, address.get())
                    .remove(RING_ADDRESS_KEY).apply();
        }
    }

    public void restoreRing() {
        final String address = mPreferences.getString(TEMPORARY_RING_ADDRESS_KEY, null);
        if (address != null) {
            mPreferences.edit().putString(RING_ADDRESS_KEY, address)
                    .remove(TEMPORARY_RING_ADDRESS_KEY).apply();
        }
    }

    public Color getNotificationColor(final NotificationType type) {
        return Utilities.valueOfNullable(
                Color.class, mPreferences.getString(type + COLOR_KEY_SUFFIX, null)
        ).or(type.defaultColor);
    }

    public void setNotificationColor(final NotificationType type, final Color color) {
        mPreferences.edit().putString(type + COLOR_KEY_SUFFIX, color.toString()).apply();
    }

    public Vibration getNotificationVibration(final NotificationType type) {
        return Utilities.valueOfNullable(
                Vibration.class, mPreferences.getString(type + VIBRATION_KEY_SUFFIX, null)
        ).or(type.defaultVibration);
    }

    public void setNotificationVibration(final NotificationType type, final Vibration vibration) {
        mPreferences.edit().putString(type + VIBRATION_KEY_SUFFIX, vibration.toString()).apply();
    }

    public NotificationMode getNotificationMode(final NotificationType type) {
        return Utilities.valueOfNullable(
                NotificationMode.class, mPreferences.getString(type + MODE_KEY_SUFFIX, null)
        ).or(NotificationMode.UNSET);
    }

    public void setNotificationMode(final NotificationType type, final NotificationMode mode) {
        mPreferences.edit().putString(type + MODE_KEY_SUFFIX, mode.toString()).apply();
    }

    public boolean getConnectionLight() {
        return mPreferences.getBoolean(mContext.getString(R.string.connection_light_key), false);
    }

    public boolean getShowInstructions() {
        return mPreferences.getBoolean(NOTIFICATION_INSTRUCTIONS, false);
    }

    public void setShowInstructions(final boolean showInstructions) {
        mPreferences.edit().putBoolean(NOTIFICATION_INSTRUCTIONS, showInstructions).apply();
    }

    public boolean getShowOnboarding() {
        return mPreferences.getBoolean(ONBOARDING, true);
    }

    public void setShowOnboarding(final boolean showOnboarding) {
        mPreferences.edit().putBoolean(ONBOARDING, showOnboarding).apply();
    }

    public boolean getDisconnectionBuzz() {
        return mPreferences.getBoolean(mContext.getString(R.string.disconnection_buzz_key), false);
    }

    public boolean getSleepMode() {
        return mPreferences.getBoolean(mContext.getString(R.string.sleep_mode_key), true);
    }

    public boolean getInnerRing() {
        return mPreferences.getBoolean(INNER_RING_KEY, false);
    }

    public void setInnerRing(final boolean innerRing) {
        mPreferences.edit().putBoolean(INNER_RING_KEY, innerRing).apply();
    }


    public Set<String> getContacts() {
        return Collections.unmodifiableSet(mContactPreferences.getAll().keySet());
    }

    public void removeContact(final String contact) {
        mContactPreferences.edit().remove(contact).apply();
    }

    public Optional<Color> getContactColor(final String contact) {
        return Utilities.valueOfNullable(Color.class, mContactPreferences.getString(contact, null));
    }

    public void setContactColor(final String contact, final Color color) {
        mContactPreferences.edit().putString(contact, color.toString()).apply();
    }

    //////////////////////////////////////////
    // Static methods
    // NOTE: This is in general cleaner as it allows access wherever there's context and lets the
    // framework manage singletons.

    public static Optional<String> getRingName(Context context) {
        return Optional.fromNullable(getPreferences(context).getString(RING_NAME_KEY, null));
    }

    public static Optional<String> getAuthToken(Context context) {
        return Optional.fromNullable(
            getPreferences(context).getString(AUTH_TOKEN, null)
        );
    }

    public static void setAuthToken(Context context, @NonNull final String token) {
        getPreferences(context)
            .edit()
            .putString(AUTH_TOKEN, token)
            .apply();
    }

    public static void clearAuthToken(Context context) {
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(AUTH_TOKEN)
            .apply();
    }

    public static Optional<String> getUserEmail(Context context) {
        return Optional.fromNullable(
            getPreferences(context).getString(USER_EMAIL, null)
        );
    }

    public static void setUserEmail(Context context, @NonNull final String email) {
        getPreferences(context)
            .edit()
            .putString(USER_EMAIL, email)
            .apply();
    }

    public static boolean isOnboarded(Context context) {
        return getPreferences(context).contains(ONBOARDING);
    }

    public static Optional<String> getRingAddress(Context context) {
        return Optional.fromNullable(
            getPreferences(context).getString(RING_ADDRESS_KEY, null)
        );
    }

    public static SharedPreferences getPreferences(Context context) {
        return context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    public static boolean isNotificationOnboarded(Context context) {
        return getPreferences(context).contains(NOTIFICATION_ONBOARDING);
    }

    public static void completeNotificationOnboarding(Context context) {
        getPreferences(context).edit().putBoolean(NOTIFICATION_ONBOARDING, true).apply();
    }

    public static void setWeight(Context context, Weight weight) {
        getPreferences(context).edit()
            .putFloat(WEIGHT, weight.value)
            .putInt(WEIGHT_UNIT, weight.unit.ordinal())
            .apply();
    }

    public static Weight getWeight(Context context) {
        return new Weight(
            getPreferences(context).getFloat(WEIGHT, -1.0f),
            Weight.WeightUnit.values()[getPreferences(context).getInt(WEIGHT_UNIT, 0)]
        );
    }

    public static void setHeight(Context context, Height height) {
        getPreferences(context).edit()
            .putInt(HEIGHT, height.value)
            .putInt(HEIGHT_UNIT, height.unit.ordinal())
            .apply();
    }

    public static Height getHeight(Context context) {
        return new Height(
            getPreferences(context).getInt(HEIGHT, -1),
            Height.HeightUnit.values()[getPreferences(context).getInt(HEIGHT_UNIT, 0)]
        );
    }

    public static Integer getGoal(Context context) {
        return getPreferences(context).getInt(GOAL, 10000);
    }

    public static void setGoal(Context context, int goal) {
        getPreferences(context).edit().putInt(GOAL, goal).apply();
    }

    public static Integer getMindfulMinutesGoal(Context context) {
        return getPreferences(context).getInt(context.getString(R.string.mindful_minutes_goal_key), MindfulMinutesGoalPreferenceDialogFragmentCompat.DEFAULT_MINUTES_GOAL);
    }

    public static void setMindfulMinutesGoal(Context context, int goal) {
        getPreferences(context).edit().putInt(context.getString(R.string.mindful_minutes_goal_key), goal).apply();
    }

    public static int getMindfulMinutesCount(Context context) {
        resetMinutesCount(context);
        int seconds = getPreferences(context).getInt(context.getString(R.string.mindful_minutes_count_key), 0);
        int minutes =seconds/ 60;
        int remainder = seconds % 60;
        if (remainder > 10) {
            minutes ++;
        }
        return minutes;
    }

    private static void resetMinutesCount(Context context) {
        SharedPreferences preferences = getPreferences(context);
        long millis = preferences.getLong(context.getString(R.string.mindful_minutes_reset_key), 0);
        if (!DateUtils.isToday(millis)) {
            preferences.edit().putInt(context.getString(R.string.mindful_minutes_count_key), 0);
            preferences.edit().putLong(context.getString(R.string.mindful_minutes_reset_key),
                    System.currentTimeMillis());
        }
    }

    public static void addMindfulMinutesCount(Context context, int count) {
        int actualValue =  getPreferences(context).getInt(context.getString(R.string.mindful_minutes_count_key), 0);
        getPreferences(context).edit().putInt(context.getString(R.string.mindful_minutes_count_key), actualValue+count).apply();
    }

    //Reminder is not implemented. It's not been used yet.
    public static boolean getDailyReminder(Context context) {
        return getPreferences(context).getBoolean(context.getString(R.string.mindful_reminder_key), false);
    }

    //Reminder is not implemented. It's not been used yet.
    public static int getReminderHour(Context context) {
        return getPreferences(context).getInt(context.getString(R.string.mindful_reminder_time_hour_key), HourPreference.DEFAULT_HOUR);
    }

    //Reminder is not implemented. It's not been used yet.
    public static int getReminderMinutes(Context context) {
        return getPreferences(context).getInt(context.getString(R.string.mindful_reminder_time_minutes_key), HourPreference.DEFAULT_MINUTES);
    }

    //Reminder is not implemented. It's not been used yet.
    public static void setReminderHour(Context context, int hour) {
        getPreferences(context).edit().putInt(context.getString(R.string.mindful_reminder_time_hour_key), hour).apply();
    }

    //Reminder is not implemented. It's not been used yet.
    public static void setReminderMinutes(Context context, int hour) {
        getPreferences(context).edit().putInt(context.getString(R.string.mindful_reminder_time_minutes_key), hour).apply();
    }

    public static Optional<Calendar> getBirthday(Context context) {
        long l = getPreferences(context).getLong(BIRTHDAY, -1);
        if(l == -1) {
            return Optional.absent();
        }

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(l);
        return Optional.of(cal);
    }

    public static boolean getSupportsActivity(Context context) {
        return getPreferences(context).getBoolean(SUPPORTS_ACTIVITY, false);
    }

    public static void setRingSupportsActivity(Context context, boolean supports) {
        getPreferences(context).edit().putBoolean(SUPPORTS_ACTIVITY, supports).apply();
    }

    public static boolean isActivityPref(String pref) {
        return SUPPORTS_ACTIVITY.equals(pref);
    }

    public static boolean isRingSetting(Context context, String pref) {
        return context.getString(R.string.connection_light_key).equals(pref) ||
            context.getString(R.string.disconnection_buzz_key).equals(pref) ||
            context.getString(R.string.sleep_mode_key).equals(pref);
    }
}
