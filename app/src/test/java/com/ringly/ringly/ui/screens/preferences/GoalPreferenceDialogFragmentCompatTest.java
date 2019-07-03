package com.ringly.ringly.ui.screens.preferences;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Created by peter on 5/3/17.
 */

public class GoalPreferenceDialogFragmentCompatTest {

    @org.junit.Test
    public void testCompareVersions() {

        int min = GoalPreferenceDialogFragmentCompat.MIN_STEPS_GOAL;
        // int max = GoalPreferenceDialogFragmentCompat.MAX_STEPS_GOAL;
        int inc = GoalPreferenceDialogFragmentCompat.INCR_STEPS_GOAL;

        assertThat(
                "Min goal is 0 index",
                GoalPreferenceDialogFragmentCompat.goalToPickerIndex(min),
                is(0)
        );

        assertThat(
                "Min goal + 2 increments is 2nd index",
                GoalPreferenceDialogFragmentCompat.goalToPickerIndex(min + 2 * inc),
                is(2)
        );

        assertThat(
                "0 index is min goal",
                GoalPreferenceDialogFragmentCompat.pickerIndexToGoal(0),
                is(min)
        );

        assertThat(
                "4 index is min goal + 3 increments",
                GoalPreferenceDialogFragmentCompat.pickerIndexToGoal(3),
                is(min + 3 * inc)
        );
    }
}
