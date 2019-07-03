package com.ringly.ringly.ui.screens.preferences;

import android.content.Context;
import android.support.v7.preference.DialogPreference;
import android.support.v7.preference.PreferenceViewHolder;
import android.text.format.DateUtils;
import android.util.AttributeSet;

import com.google.common.base.Optional;
import com.ringly.ringly.Preferences;
import com.ringly.ringly.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;



/**
 * HourPreference
 * Reminder is not implemented. It's not been used yet.
 */
public class HourPreference extends DialogPreference {

    public static final int DEFAULT_HOUR = 11;
    public static final int DEFAULT_MINUTES = 0;

    public HourPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setTime(Preferences.getReminderHour(getContext()), Preferences.getReminderMinutes(getContext()));
        setPositiveButtonText(getContext().getString(R.string.set));
        setNegativeButtonText(getContext().getString(R.string.clear));
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
    }


    public void setTime(int hour, int minutes) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minutes);
        SimpleDateFormat sdf = new SimpleDateFormat("h:mm a");
        setSummary(sdf.format(calendar.getTime()));
        Preferences.setReminderHour(getContext(), hour);
        Preferences.setReminderMinutes(getContext(), minutes);
    }
}
