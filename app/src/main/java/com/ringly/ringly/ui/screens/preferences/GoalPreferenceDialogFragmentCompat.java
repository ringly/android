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
 *  GoalPreferenceDialogFragmentCompat
 */
public class GoalPreferenceDialogFragmentCompat extends PreferenceDialogFragmentCompat {
    protected static final int MIN_STEPS_GOAL = 5_000;
    protected static final int MAX_STEPS_GOAL = 20_000;
    protected static final int INCR_STEPS_GOAL = 500;

    private int mGoal;
    private NumberPicker numberPicker;

    public static GoalPreferenceDialogFragmentCompat newInstance(String key) {
        final GoalPreferenceDialogFragmentCompat
            fragment = new GoalPreferenceDialogFragmentCompat();
        final Bundle b = new Bundle(1);
        b.putString(ARG_KEY, key);
        fragment.setArguments(b);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            mGoal = getGoalPreference().getGoal();
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
        // and not a valid multiple of INCR_STEPS_GOAL, then round down to the nearest
        int pickerGoal = mGoal;
        if (pickerGoal < MIN_STEPS_GOAL) {
            pickerGoal = MIN_STEPS_GOAL;
        } else if (pickerGoal > MAX_STEPS_GOAL) {
            pickerGoal = MAX_STEPS_GOAL;
        } else {
            pickerGoal = (pickerGoal / INCR_STEPS_GOAL) * INCR_STEPS_GOAL;
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
            if (getGoalPreference().callChangeListener(mGoal)) {
                (getGoalPreference()).setGoal(mGoal);
            }
        }
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
    }

    private String[] createGoalsValues() {
        ArrayList<String> values = new ArrayList<String>();
        NumberFormat nb = NumberFormat.getInstance();
        for (int i=MIN_STEPS_GOAL; i <= MAX_STEPS_GOAL; i += INCR_STEPS_GOAL) {
            values.add(nb.format(i));
        }
        return values.toArray(new String[]{});
    }

    protected static int goalToPickerIndex(int goal) {
        return (goal - MIN_STEPS_GOAL) / INCR_STEPS_GOAL;
    }

    protected static int pickerIndexToGoal(int pickerIndex) {
        return (pickerIndex * INCR_STEPS_GOAL) + MIN_STEPS_GOAL;
    }

    private GoalPreference getGoalPreference() {
        return (GoalPreference) getPreference();
    }
}
