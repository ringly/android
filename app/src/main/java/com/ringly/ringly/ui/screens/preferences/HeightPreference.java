package com.ringly.ringly.ui.screens.preferences;

import android.content.Context;
import android.support.v7.preference.DialogPreference;
import android.util.AttributeSet;

import com.ringly.ringly.Preferences;
import com.ringly.ringly.R;
import com.ringly.ringly.config.model.Height;

public class HeightPreference extends DialogPreference {
    private Height mHeight;

    public HeightPreference(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HeightPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        setDialogLayoutResource(R.layout.preference_height);
        setPositiveButtonText(R.string.done);
        setNegativeButtonText(R.string.clear);

        setOnPreferenceChangeListener((p, v) ->  {
            if(v instanceof Height) {
                Preferences.setHeight(getContext(), (Height) v);
                return true;
            }

            return false;
        });
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        setHeight(Preferences.getHeight(getContext()));
    }

    Height getHeight() {
        return mHeight;
    }

    public void setHeight(Height height) {
        mHeight = height;
        setSummary(mHeight.toString());
        Preferences.setHeight(getContext(), mHeight);
    }
}
