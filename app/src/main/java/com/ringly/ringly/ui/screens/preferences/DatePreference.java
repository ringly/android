package com.ringly.ringly.ui.screens.preferences;

import android.content.Context;
import android.support.v7.preference.DialogPreference;
import android.support.v7.preference.PreferenceViewHolder;
import android.text.format.DateUtils;
import android.util.AttributeSet;

import com.google.common.base.Optional;
import com.ringly.ringly.Preferences;
import com.ringly.ringly.R;

import java.util.Calendar;

import static com.ringly.ringly.R.string.set;

public class DatePreference extends DialogPreference {
    public DatePreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        setPositiveButtonText(getContext().getString(set));
        setNegativeButtonText(getContext().getString(R.string.clear));
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        if(restorePersistedValue ) {
            if(defaultValue == null) {
                setDate(Preferences.getBirthday(getContext()));
            } else {
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis((Long) defaultValue);
                setDate(Optional.of(cal));
            }
        } else  {
            setDate(Optional.absent());
        }
    }

    public void setDate(Optional<Calendar> cal) {
        if(cal.isPresent()) {
            setSummary(DateUtils.formatDateTime(getContext(), cal.get().getTimeInMillis(),
                DateUtils.FORMAT_NUMERIC_DATE));
            if(callChangeListener(cal.get().getTimeInMillis())) {
                persistLong(cal.get().getTimeInMillis());
            }
        } else {
            setSummary("");
            persistLong(-1);
        }
    }
}
