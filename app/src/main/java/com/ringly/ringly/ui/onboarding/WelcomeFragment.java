package com.ringly.ringly.ui.onboarding;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ringly.ringly.R;

/**
 * Created by lindaliu on 1/8/16.
 */
public class WelcomeFragment extends Fragment {

    private OnboardingActivity mActivity;

    @Override
    public void onAttach(final Activity activity) {
        //noinspection HardCodedStringLiteral
        //Log.d(TAG, "onAttach");
        super.onAttach(activity);

        mActivity = (OnboardingActivity) activity;
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState
    ) {

        final View view = inflater.inflate(R.layout.fragment_onbd_welcome, container, false);

        mActivity.onCreateView(view);

        return view;
    }
}
