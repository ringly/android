package com.ringly.ringly.ui.screens.preferences;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.codetroopers.betterpickers.calendardatepicker.CalendarDatePickerDialogFragment;
import com.codetroopers.betterpickers.timepicker.TimePickerBuilder;
import com.codetroopers.betterpickers.timepicker.TimePickerDialogFragment;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.ringly.ringly.Preferences;
import com.ringly.ringly.R;
import com.ringly.ringly.bluetooth.RinglyService;
import com.ringly.ringly.config.Mixpanel;
import com.ringly.ringly.ui.BaseActivity;
import com.ringly.ringly.ui.MainActivity;
import com.ringly.ringly.ui.login.LoginActivity;

import java.util.Calendar;
import java.util.Vector;

public final class PreferencesFragment extends PreferenceFragmentCompat {

    private static final String TAG = PreferencesFragment.class.getCanonicalName();

    // Grabbed from PreferenceFragmentCompat
    private static final String DIALOG_FRAGMENT_TAG =
        "android.support.v7.preference.PreferenceFragment.DIALOG";

    private static final String HELP_URL = "https://explore.ringly.com/setup";

    public static final String PARAMETER_REFRESH_ACTIVITY = "refreshActivity";
    public static final String PARAMETER_DISCONNECT = "disconnect";
    public static final String PARAMETER_SIGN_OUT= "signOut";

    private BaseActivity mActivity;

    private SharedPreferences.OnSharedPreferenceChangeListener mListener;


    ////
    //// Fragment methods
    ////

    @Override
    public void onAttach(final Context activity) {
        //noinspection HardCodedStringLiteral
        Log.d(TAG, "onAttach");
        mActivity = (BaseActivity) activity;
        super.onAttach(activity);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mListener =
            (sp, key) -> {
                int trackId = -1;
                if(getString(R.string.connection_light_key).equals(key)) {
                    trackId = R.string.connection_light;
                } else if(getString(R.string.disconnection_buzz_key).equals(key)) {
                    trackId = R.string.disconnection_buzz;
                } else if(getString(R.string.sleep_mode_key).equals(key)) {
                    trackId = R.string.sleep_mode;
                } else if(Preferences.HEIGHT.equals(key) || Preferences.WEIGHT.equals(key)
                        || Preferences.GOAL.equals(key) || Preferences.BIRTHDAY.equals(key) ) {
                    //If the user modified the height, weight, goal, or birthday, the app has to refresh the activity screen.
                    //Then I add this parameter in the intent, and the preference screen close, in MainActivity check if has to refresh.
                    mActivity.getIntent().putExtra(PARAMETER_REFRESH_ACTIVITY, true);
                } else if(getString(R.string.mindful_reminder_key).equals(key)) {
                    enableDisableReminderTime();
                }

                if(trackId != -1) {
                    mActivity.getMixpanel().trackSetting(trackId);
                }
            };

        getPreferenceManager().getSharedPreferences()
            .registerOnSharedPreferenceChangeListener(mListener);

        findPreference(getString(R.string.prefs_help))
            .setOnPreferenceClickListener(__ -> {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(HELP_URL));
                startActivity(browserIntent);
                return true;
            });

        if (!Preferences.getRingAddress(getContext()).isPresent()) {
             findPreference(getString(R.string.disconnect)).setVisible(false);
        }

        findPreference(getString(R.string.disconnect))
            .setOnPreferenceClickListener(__ -> {
                RinglyService.doForgetRing(mActivity);

                mActivity.getMixpanel().track(Mixpanel.Event.TAPPED_BUTTON, ImmutableMap.of(
                    Mixpanel.Property.NAME, getResources().getString(R.string.disconnect)));
                Intent data = mActivity.getIntent();
                data.putExtra(PARAMETER_DISCONNECT, true);
                mActivity.setResult(Activity.RESULT_OK, data);
                mActivity.finish();
                return true;
            });

        findPreference(getString(R.string.signout))
            .setOnPreferenceClickListener(__ -> {
                Preferences.clearAuthToken(mActivity);
                Intent intent = new Intent(getContext(), LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                ActivityCompat.startActivity(mActivity, intent, null);
                Intent data = mActivity.getIntent();
                data.putExtra(PARAMETER_SIGN_OUT, true);
                mActivity.setResult(Activity.RESULT_OK, data);
                mActivity.finish();
                return true;
            });

        //enableDisableReminderTime();

        PreferenceManager.setDefaultValues(getContext(), R.xml.preferences, false);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        getPreferenceManager().setSharedPreferencesName(Preferences.PREFERENCES_NAME);
        PreferenceManager.setDefaultValues(getContext(), Preferences.PREFERENCES_NAME,
            Context.MODE_PRIVATE, R.xml.preferences, false);
        addPreferencesFromResource(R.xml.preferences);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);

        TypedValue tv = new TypedValue();
        if (getActivity().getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
            v.setPadding(0,
                TypedValue.complexToDimensionPixelSize(tv.data, getResources().getDisplayMetrics()),
                0,
                0 );
        }

        v.post(() -> mActivity.addListener((RingPreference) findPreference(getString(R.string.ring_key))));

        return v;
    }

    @Override
    public void onDisplayPreferenceDialog(final Preference preference) {
        DialogFragment fragment;
        if (preference instanceof WeightPreference) {
            fragment = WeightPreferenceDialogFragmentCompat.newInstance(preference.getKey());
            fragment.setTargetFragment(this, 0);
            fragment.show(getFragmentManager(), DIALOG_FRAGMENT_TAG);
        } else if (preference instanceof HeightPreference) {
            fragment = HeightPreferenceDialogFragmentCompat.newInstance(preference.getKey());
            fragment.setTargetFragment(this, 0);
            fragment.show(getFragmentManager(), DIALOG_FRAGMENT_TAG);
        } else if (preference instanceof GoalPreference) {
            fragment = GoalPreferenceDialogFragmentCompat.newInstance(preference.getKey());
            fragment.setTargetFragment(this, 0);
            fragment.show(getFragmentManager(), DIALOG_FRAGMENT_TAG);

        } else if (preference instanceof MindfulMinutesGoalPreference) {
            fragment = MindfulMinutesGoalPreferenceDialogFragmentCompat.newInstance(preference.getKey());
            fragment.setTargetFragment(this, 0);
            fragment.show(getFragmentManager(), DIALOG_FRAGMENT_TAG);

        } else if (preference instanceof DatePreference) {
            Calendar defaultCal = Preferences.getBirthday(getContext()).or(Calendar.getInstance());
            CalendarDatePickerDialogFragment calFragment =
                new CalendarDatePickerDialogFragment()
                    .setOnDateSetListener(((dialog, year, monthOfYear, dayOfMonth) -> {
                        Calendar cal = Calendar.getInstance();
                        cal.set(year, monthOfYear, dayOfMonth);
                        ((DatePreference) preference).setDate(Optional.of(cal));
                    }))
                    .setPreselectedDate(defaultCal.get(Calendar.YEAR),
                        defaultCal.get(Calendar.MONTH), defaultCal.get(Calendar.DAY_OF_MONTH));
            calFragment.show(getFragmentManager(), DIALOG_FRAGMENT_TAG);

        } else if (preference instanceof HourPreference) {
            TimePickerDialogFragment timePickerDialogFragment = new TimePickerDialogFragment();
            TimePickerDialogFragment.TimePickerDialogHandler handler = new TimePickerDialogFragment.TimePickerDialogHandler() {
                @Override
                public void onDialogTimeSet(int reference, int hourOfDay, int minute) {
                    ((HourPreference) preference).setTime(hourOfDay, minute);
                }
            };
            Vector<TimePickerDialogFragment.TimePickerDialogHandler> vector = new Vector<TimePickerDialogFragment.TimePickerDialogHandler>();
            vector.add(handler);
            timePickerDialogFragment.setTimePickerDialogHandlers(vector);
            timePickerDialogFragment.show(getFragmentManager(), DIALOG_FRAGMENT_TAG);

        }  else {
            super.onDisplayPreferenceDialog(preference);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        getPreferenceManager().getSharedPreferences()
            .unregisterOnSharedPreferenceChangeListener(mListener);

        mActivity.removeListener((RingPreference) findPreference(getString(R.string.ring_key)));
    }

    /**
     * Reminder is not implemented. It's not been used yet.
     * Enable or disable Reminder Time preferences after the user select or deselect Daily Reminder,
     */
    private void enableDisableReminderTime() {
        boolean enabled = Preferences.getDailyReminder(getContext());
        findPreference(getString(R.string.mindful_reminder_time_hour_key)).setEnabled(enabled);
    }
}
