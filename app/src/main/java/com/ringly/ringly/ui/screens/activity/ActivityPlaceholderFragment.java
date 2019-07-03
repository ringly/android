package com.ringly.ringly.ui.screens.activity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ringly.ringly.Preferences;
import com.ringly.ringly.R;
import com.ringly.ringly.bluetooth.HardwareFamily;
import com.ringly.ringly.config.Mixpanel;
import com.ringly.ringly.config.Screen;
import com.ringly.ringly.db.Db;
import com.ringly.ringly.ui.MainActivity;
import com.ringly.ringly.ui.SetupHelper;
import com.ringly.ringly.ui.dfu.DfuActivity;
import com.ringly.ringly.ui.dfu.Firmwares;

import rx.subscriptions.CompositeSubscription;

public class ActivityPlaceholderFragment extends Fragment {
    public static final String TAG = ActivityPlaceholderFragment.class.getCanonicalName();

    private SharedPreferences.OnSharedPreferenceChangeListener mListener;
    private CompositeSubscription mSubscriptions;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSubscriptions = new CompositeSubscription();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_activity_placeholder, container, false);

        mListener = (__, pref) -> {
            if(Preferences.isActivityPref(pref)) {
                this.updateView(view);
            }
        };

        Preferences.getPreferences(getContext())
            .registerOnSharedPreferenceChangeListener(mListener);
        updateView(view);

        ((TextView) view.findViewById(R.id.text_placeholder_text))
            .setMovementMethod(LinkMovementMethod.getInstance());

        mSubscriptions.add(Db.getInstance(getContext())
                .getCount()
                .subscribe(
                        c -> {
                            if (c > 0) {
                                if (getArguments().get(MainActivity.FW_VERSION_BUNDLE) != null &&
                                        !SetupHelper.needsActivityUpdate((String) getArguments().get(MainActivity.FW_VERSION_BUNDLE))) {
                                    // run through `changeScreen` again, which sees if ACTIVITY_PLACEHOLDER can
                                    // instead switch to ACTIVITY
                                    FragmentActivity activity = getActivity();
                                    if (activity == null) {
                                        // HACK(peter) - this returned `null` in the past, before I added
                                        // the unsubscribe code in the life-cycle for this fragment, so
                                        // IDK if this will still return `null` or not, but I figured
                                        // I'd rather be safe than sorry
                                        Log.e(TAG, "getActivity() returned null inside ActivityPlaceholderFragment steps subscription");
                                    } else {
                                        activity.runOnUiThread(() -> {
                                            FragmentActivity innerActivity = getActivity();
                                            if (innerActivity == null) {
                                                // HACK(peter) - looks like getActivity can be null
                                                // inside here, even after the prior null check
                                                // outside of this thread
                                                Log.e(TAG, "inner getActivity() returned null inside ActivityPlaceholderFragment steps subscription");
                                            } else {
                                                ((MainActivity) innerActivity).changeScreen(Screen.ACTIVITY_PLACEHOLDER, R.string.activity);
                                            }
                                        });
                                    }
                                }
                            }
                        },
                        err -> Log.e(TAG, "onCreateView: Error getting count", err)
                ));

        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Preferences.getPreferences(getContext())
            .unregisterOnSharedPreferenceChangeListener(mListener);

        mSubscriptions.unsubscribe();
    }

    private void updateView(View view) {
        view.findViewById(R.id.button_update).setVisibility(View.GONE);

        Bundle bundle = getArguments();

        boolean hasFwVersion = bundle.get(MainActivity.FW_VERSION_BUNDLE) != null;
        TextView placeholderView = (TextView) view.findViewById(R.id.text_placeholder_text);

        if (Preferences.getSupportsActivity(view.getContext())) {
            // SUPPORTS_ACTIVITY preference is set (happens via RinglyService.startActivityTracking())
            //
            placeholderView.setText(R.string.get_moving);

        } else if (hasFwVersion &&
                SetupHelper.hasActivityUpdate(bundle.getString(MainActivity.FW_VERSION_BUNDLE))) {
            // We have the peripheral's FW version and it's one that *should* support activity,
            // but for some reason activity has not been started...
            //
            placeholderView.setText(R.string.reconnect_ringly_for_steps);

        } else if (hasFwVersion &&
                SetupHelper.needsActivityUpdate(bundle.getString(MainActivity.FW_VERSION_BUNDLE))) {
            // A FW update is required to support activity (version is 2.x where x < 2)
            //

            if (bundle.get(Firmwares.BUNDLE_NAME) == null ||
                    bundle.getString(HardwareFamily.BUNDLE_NAME) == null ||
                    !hasFwVersion) {
                // We do not have a firmware update available or are missing info on the Ringly
                //
                placeholderView.setText(R.string.update_needed_but_not_found);

            } else {
                // We have a firmware update available, let's ask for it!
                //
                placeholderView.setText(R.string.update_description);

                Firmwares fws = Firmwares.fromBundle(bundle.getBundle(Firmwares.BUNDLE_NAME));
                HardwareFamily hardwareFamily = HardwareFamily.valueOf(bundle.getString(HardwareFamily.BUNDLE_NAME));

                view.findViewById(R.id.button_update).setVisibility(View.VISIBLE);
                view.findViewById(R.id.button_update).setOnClickListener(v -> {
                    MainActivity activity = (MainActivity) getActivity();
                    activity.onUpdate();
                    view.findViewById(R.id.button_update).setEnabled(false);

                    DfuActivity.start(fws, hardwareFamily, MainActivity.DFU_REQUEST_CODE, getActivity());

                    activity.getMixpanel().track(Mixpanel.Event.DFU_TAPPED);
                });
            }

        } else if (!hasFwVersion || bundle.get(HardwareFamily.BUNDLE_NAME) == null) {
            // We are missing one or both of firmware version or hardware family,
            // which means we are either not connected to a Ringly or have not loaded
            // it yet.
            //
            // Let's show something that handles someone not having a Ringly or just not being
            // connected (e.g., pulling out of charger disconnects for a bit).
            //
            placeholderView.setText(R.string.steps_placeholder_unknown);

        } else if (hasFwVersion &&
                bundle.getString(HardwareFamily.BUNDLE_NAME) != null &&
                bundle.getBundle(Firmwares.BUNDLE_NAME) != null &&
                SetupHelper.needsActivityUpdate(bundle.getString(MainActivity.FW_VERSION_BUNDLE))) {
            // A FW update is required to support activity (version is 2.x where x < 2)
            //
            placeholderView.setText(R.string.update_description);

            Firmwares fws = Firmwares.fromBundle(bundle.getBundle(Firmwares.BUNDLE_NAME));
            HardwareFamily hardwareFamily = HardwareFamily.valueOf(bundle.getString(HardwareFamily.BUNDLE_NAME));

            view.findViewById(R.id.button_update).setVisibility(View.VISIBLE);
            view.findViewById(R.id.button_update).setOnClickListener(v -> {
                MainActivity activity = (MainActivity) getActivity();
                activity.onUpdate();
                view.findViewById(R.id.button_update).setEnabled(false);

                DfuActivity.start(fws, hardwareFamily, MainActivity.DFU_REQUEST_CODE, getActivity());

                activity.getMixpanel().track(Mixpanel.Event.DFU_TAPPED);
            });

        } else {
            // Looks like we have a connected device but it will never support steps, e.g.,
            // a Park ring.
            //
            placeholderView.setText(R.string.steps_placeholder_unsupported);

        }
    }
}
