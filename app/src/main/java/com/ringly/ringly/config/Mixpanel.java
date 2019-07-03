package com.ringly.ringly.config;


import android.content.Context;
import android.util.Log;

import com.google.common.collect.EnumMultiset;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multiset;
import com.mixpanel.android.mpmetrics.MixpanelAPI;
import com.ringly.ringly.Preferences;
import com.ringly.ringly.R;

import org.apache.commons.lang3.text.WordUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.Map;

public class Mixpanel {

    private static final String TAG = Mixpanel.class.getCanonicalName();
    private static final String MIXPANEL_TOKEN = "YOUR-TOKEN-HERE"; // NON-NLS
    private static final int HOURLY_QUOTA = 100;


    @SuppressWarnings("HardCodedStringLiteral")
    public enum Event {
        TAPPED_BUTTON,
        VIEWED_SCREEN,
        SWITCHED_MAIN_VIEW,
        CHANGED_NOTIFICATION,
        DISABLED_NOTIFICATION,
        CHANGED_CONTACT,
        DISABLED_CONTACT,
        CHANGED_SETTING,
        NOTIFIED,
        APPLICATION_LAUNCH,
        APPLICATION_FOREGROUND,
        APPLICATION_BACKGROUND,
        NOTIFICATIONS_REQUESTED,
        NOTIFICATIONS_COMPLETED,
        NOTIFICATIONS_PERMISSION,
        AUTHENTICATION_COMPLETED,
        AUTHENTICATION_FAILED,

        DFU_BANNER_SHOWN("DFU Banner Shown"),
        DFU_TAPPED("DFU Tapped"),
        DFU_CANCELLED("DFU Cancelled"),
        DFU_DOWNLOADED("DFU Downloaded"),
        DFU_DOWNLOAD_FAILED("DFU Download Failed"),
        DFU_START("DFU Start"),
        DFU_REQUESTED_RING_IN_CHARGER("DFU Requested Ring in Charger"),
        DFU_RING_IN_CHARGER("DFU Ring in Charger"),
        DFU_WRITE_STARTED("DFU Write Started"),
        DFU_WRITE_COMPLETED("DFU Write Completed"),
        DFU_COMPLETED("DFU Completed"),
        DFU_FAILED("DFU Failed"),

        EXCEEDED_EVENT_QUOTA,
        ;

        private final String mName;
        Event() {
            mName = capitalized(this);
        }
        Event(final String name) {
            mName = name;
        }
        public String toString() {
            return mName;
        }
    }

    @SuppressWarnings("HardCodedStringLiteral")
    public enum Property {
        // super properties
        CONNECTED,
        BATTERY_LEVEL,
        CHARGE_STATE,
        FIRMWARE_REVISION,
        HARDWARE_REVISION,
        BOOTLOADER_REVISION,
        ENABLED_NOTIFICATIONS,
        ENABLED_CONTACTS,
        CUSTOMIZED_NOTIFICATIONS,
        CONNECTION_TAPS_SETTING,
        OUT_OF_RANGE_SETTING,
        SLEEP_MODE_SETTING,
        INNER_RING_SETTING,

        // per-event properties
        NAME,
        COLOR,
        VIBRATION,
        CONTACT_COLOR,
        SUPPORTED,
        ENABLED,
        SENT,
        APPLICATION,
        METHOD,
        TO,
        VIA,
        FROM,
        VALUE,
        ACCEPTED,
        RESULT,
        CODE,
        DOMAIN,

        // dfu properties
        PACKAGE_TYPE,
        INDEX,
        COUNT,
        DFU_VERSION("DFU Version"),
        PACKAGE_VERSION,
        ;

        private final String mName;
        Property() {
            mName = capitalized(this);
        }
        Property(final String name) {
            mName = name;
        }
        public String toString() {
            return mName;
        }
    }

    public enum NotificationMethod { TAP, PAN, VIBRATION, ENABLED }


    public static String capitalized(final Enum<?> e) {
        //noinspection MagicCharacter
        return WordUtils.capitalizeFully(e.name().replace('_', ' '));
    }

    public static Map<Property, String> getNotificationProperties(
            final NotificationType type, final Color color, final Vibration vibration,
            final NotificationMethod method, final Context context
    ) {
        // TODO add sent or unsent properties
        return new ImmutableMap.Builder<Property, String>()
            .put(Property.NAME, context.getResources().getString(type.nameId))
            .put(Property.COLOR, color.toString())
            .put(Property.VIBRATION, vibration.toString())
            .put(Property.METHOD, capitalized(method))
            .build();
    }

    public static Map<Property, ?> getUnsupportedNotificationProperties(String name) {
        return getNotificationProperties(false, false, false, name);
    }
    public static Map<Property, ?> getDisabledNotificationProperties(String name) {
        return getNotificationProperties(true, false, false, name);
    }
    public static Map<Property, ?> getUnsentNotificationProperties(String name) {
        return getNotificationProperties(true, true, false, name);
    }
    public static Map<Property, ?> getSentNotificationProperties(String name) {
        return getNotificationProperties(true, true, true, name);
    }
    private static Map<Property, ?> getNotificationProperties(
            final boolean supported, final boolean enabled, final boolean sent, final String name
    ) {
        return ImmutableMap.of(
                Property.SUPPORTED, supported,
                Property.ENABLED, enabled,
                Property.SENT, sent,
                Property.APPLICATION, name
        );
    }


    private static JSONObject toJson(final Map<Property, ?> properties) {
        final JSONObject json = new JSONObject();
        for (final Map.Entry<Property, ?> entry : properties.entrySet()) {
            try {
                json.put(entry.getKey().toString(), entry.getValue());
            } catch (final JSONException e) {
                //noinspection HardCodedStringLiteral
                Log.wtf(TAG, e);
            }
        }
        return json;
    }


    @SuppressWarnings("StaticNonFinalField")
    private static long sCurrentHour = 0;
    private static final Multiset<Event> sEventCounts = EnumMultiset.create(Event.class);

    private static int countEvent(final Event event) {
        final long currentHour
                = System.nanoTime() / com.ringly.ringly.Utilities.NANOSECONDS_PER_HOUR;
        //noinspection SynchronizationOnStaticField
        synchronized (sEventCounts) {
            if (sCurrentHour != currentHour) {
                sEventCounts.clear();
                //noinspection AssignmentToStaticFieldFromInstanceMethod
                sCurrentHour = currentHour;
            }
            return sEventCounts.add(event, 1) + 1;
        }
    }


    private final Context mContext;
    private final MixpanelAPI mMixpanel;
    private final Preferences mPreferences;

    public Mixpanel(final Context context) {
        mContext = context;
        mMixpanel = MixpanelAPI.getInstance(context, MIXPANEL_TOKEN);
        mPreferences = new Preferences(context);
    }

    public void registerSuperProperties(final Map<Property, ?> properties) {
        mMixpanel.registerSuperProperties(toJson(properties));
    }

    public void unregisterSuperProperty(final Property key) {
        mMixpanel.unregisterSuperProperty(key.toString());
    }

    public void track(final Event event) {
        track(event, Collections.<Property, Object>emptyMap());
    }

    public void track(final Event event, final Map<Property, ?> properties) {
        final int count = countEvent(event);

        if (count <= HOURLY_QUOTA) {
            final String name = event.toString();
            final JSONObject json = toJson(properties);
            mMixpanel.track(name, json);

            //noinspection HardCodedStringLiteral
            Log.d(TAG, "tracked event: " + name + " with properties " + json);
        } else if (count == HOURLY_QUOTA + 1) { // we just exceeded the quota
            track(Event.EXCEEDED_EVENT_QUOTA, ImmutableMap.of(Property.NAME, event.toString()));
        }
    }

    public void trackSetting(final int nameId) {
        updateSettingSuperProperties();
        boolean value;
        switch (nameId) {
            case R.string.inner_ring:
                value = mPreferences.getInnerRing();
                break;
            case R.string.connection_light:
                value = mPreferences.getConnectionLight();
                break;
            case R.string.disconnection_buzz:
                value = mPreferences.getDisconnectionBuzz();
                break;
            case R.string.sleep_mode:
                value = mPreferences.getSleepMode();
                break;
            default:
                return;
        }

        track(Event.CHANGED_SETTING, ImmutableMap.of(
                Property.NAME, mContext.getResources().getString(nameId),
                Property.VALUE, value
        ));
    }

    public void flush() {
        mMixpanel.flush();
    }

    public void updateNotificationSuperProperties() {
        int n = 0;
        boolean customized = false;
        for (final NotificationType type : NotificationType.values()) {
            final Color color = mPreferences.getNotificationColor(type);
            final Vibration vibration = mPreferences.getNotificationVibration(type);
            if (color != Color.NONE || vibration != Vibration.NONE) n++;
            if (color != type.defaultColor || vibration != type.defaultVibration) customized = true;
        }

        registerSuperProperties(ImmutableMap.of(
                Property.ENABLED_NOTIFICATIONS, n,
                Property.CUSTOMIZED_NOTIFICATIONS, customized
        ));
    }

    public void updateContactSuperProperties() {
        registerSuperProperties(ImmutableMap.of(
                Property.ENABLED_CONTACTS, mPreferences.getContacts().size()
        ));
    }

    public void updateSettingSuperProperties() {
        registerSuperProperties(ImmutableMap.of(
                Property.CONNECTION_TAPS_SETTING, mPreferences.getConnectionLight(),
                Property.OUT_OF_RANGE_SETTING, mPreferences.getDisconnectionBuzz(),
                Property.SLEEP_MODE_SETTING, mPreferences.getSleepMode(),
                Property.INNER_RING_SETTING, mPreferences.getInnerRing()
        ));
    }
}
