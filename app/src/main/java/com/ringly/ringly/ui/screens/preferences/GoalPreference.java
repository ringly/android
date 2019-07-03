package com.ringly.ringly.ui.screens.preferences;

import android.content.Context;
import android.support.v7.preference.DialogPreference;
import android.util.AttributeSet;
import com.ringly.ringly.Preferences;
import com.ringly.ringly.R;

import java.text.NumberFormat;

/**
 * GoalPreference
 */
public class GoalPreference extends DialogPreference {
    private int mGoal;

    public GoalPreference(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GoalPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        setDialogLayoutResource(R.layout.preference_goal);

        setPositiveButtonText(R.string.done);
        setNegativeButtonText(R.string.clear);
        setDefaultValue(10000); //I set here becuase in custompreference doesn't work in xml
        setOnPreferenceChangeListener((p, v) ->  {
            if(v instanceof Integer) {
                Preferences.setGoal(getContext(), (Integer) v);
                return true;
            }

            return false;
        });
    }


    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        if (restorePersistedValue) {
            setGoal(Preferences.getGoal(getContext()));
        } else if (defaultValue!= null ) {
            setGoal((Integer)defaultValue);
        }
    }

    protected int getGoal() {
        return mGoal;
    }

    protected void setGoal(int goal) {
        mGoal = goal;
        NumberFormat nb = NumberFormat.getInstance();
        setSummary(nb.format(goal));
        Preferences.setGoal(getContext(), mGoal);
    }
}
