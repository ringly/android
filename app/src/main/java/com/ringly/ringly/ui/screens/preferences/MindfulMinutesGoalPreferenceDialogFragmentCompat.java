package com.ringly.ringly.ui.screens.preferences;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.preference.PreferenceDialogFragmentCompat;
import android.view.View;
import android.widget.NumberPicker;

import com.ringly.ringly.R;

import java.text.NumberFormat;
import java.util.ArrayList;

/**
 *  MindfulMinutesGoalPreferenceDialogFragmentCompat
 */
public class MindfulMinutesGoalPreferenceDialogFragmentCompat extends PreferenceDialogFragmentCompat {
    protected static final int MIN_MINUTES_GOAL = 5;
    protected static final int MAX_MINUTES_GOAL = 30;
    protected static final int INCR_MINUTES_GOAL = 1;
    public static final int DEFAULT_MINUTES_GOAL = 5;

    private int mGoal;
    private NumberPicker numberPicker;

    public static MindfulMinutesGoalPreferenceDialogFragmentCompat newInstance(String key) {
        final MindfulMinutesGoalPreferenceDialogFragmentCompat
            fragment = new MindfulMinutesGoalPreferenceDialogFragmentCompat();
        final Bundle b = new Bundle(1);
        b.putString(ARG_KEY, key);
        fragment.setArguments(b);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            mGoal = getMindfulMinutesGoalPreference().getGoal();
        }
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        numberPicker = (NumberPicker) view.findViewById(R.id.goal_picker);
        String[] goalsValues = createGoalsValues();
        numberPicker.setDisplayedValues(goalsValues);
        numberPicker.setMinValue(0);
        numberPicker.setMaxValue(goalsValues.length - 1);
        numberPicker.setWrapSelectorWheel(false);

        // need to set a value that matches the number picker. If it's from an old app version
        // and not a valid multiple of INCR_MINUTES_GOAL, then round down to the nearest
        int pickerGoal = mGoal;
        if (pickerGoal < MIN_MINUTES_GOAL) {
            pickerGoal = MIN_MINUTES_GOAL;
        } else if (pickerGoal > MAX_MINUTES_GOAL) {
            pickerGoal = MAX_MINUTES_GOAL;
        } else {
            pickerGoal = (pickerGoal / INCR_MINUTES_GOAL) * INCR_MINUTES_GOAL;
        }
        numberPicker.setValue(goalToPickerIndex(pickerGoal));
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        super.onClick(dialog, which);
        if (which == DialogInterface.BUTTON_POSITIVE) {
            if (numberPicker != null) {
                int value = numberPicker.getValue();
                mGoal = pickerIndexToGoal(value);
            }
            if (getMindfulMinutesGoalPreference().callChangeListener(mGoal)) {
                (getMindfulMinutesGoalPreference()).setGoal(mGoal);
            }
        }
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
    }

    private String[] createGoalsValues() {
        ArrayList<String> values = new ArrayList<String>();
        NumberFormat nb = NumberFormat.getInstance();
        for (int i = MIN_MINUTES_GOAL; i <= MAX_MINUTES_GOAL; i += INCR_MINUTES_GOAL) {
            values.add(nb.format(i));
        }
        return values.toArray(new String[]{});
    }

    protected static int goalToPickerIndex(int goal) {
        return (goal - MIN_MINUTES_GOAL) / INCR_MINUTES_GOAL;
    }

    protected static int pickerIndexToGoal(int pickerIndex) {
        return (pickerIndex * INCR_MINUTES_GOAL) + MIN_MINUTES_GOAL;
    }

    private MindfulMinutesGoalPreference getMindfulMinutesGoalPreference() {
        return (MindfulMinutesGoalPreference) getPreference();
    }
}
