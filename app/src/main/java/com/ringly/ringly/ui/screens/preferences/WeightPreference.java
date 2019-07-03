package com.ringly.ringly.ui.screens.preferences;

import android.content.Context;
import android.support.v7.preference.DialogPreference;
import android.util.AttributeSet;

import com.ringly.ringly.Preferences;
import com.ringly.ringly.R;
import com.ringly.ringly.config.model.Weight;

public class WeightPreference extends DialogPreference {
    private Weight mWeight;

    public WeightPreference(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WeightPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        setDialogLayoutResource(R.layout.preference_weight);
        setPositiveButtonText(R.string.done);
        setNegativeButtonText(R.string.clear);

        setOnPreferenceChangeListener((p, v) ->  {
            if(p == WeightPreference.this && v instanceof Weight) {
                Preferences.setWeight(getContext(), (Weight) v);
                return true;
            }

            return false;
        });
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        setWeight(Preferences.getWeight(getContext()));
    }

    Weight getWeight() {
        return mWeight;
    }

    public void setWeight(Weight weight) {
        mWeight = weight;
        setSummary(mWeight.toString());
        Preferences.setWeight(getContext(), mWeight);
    }
}
