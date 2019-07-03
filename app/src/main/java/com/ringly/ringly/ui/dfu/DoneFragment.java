package com.ringly.ringly.ui.dfu;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ringly.ringly.R;


public final class DoneFragment extends Fragment  {

    private static final String TAG = DoneFragment.class.getCanonicalName();


    private DfuActivity mActivity;


    ////
    //// Fragment methods
    ////

    @Override
    public void onAttach(final Activity activity) {
        //noinspection HardCodedStringLiteral
        Log.d(TAG, "onAttach");
        super.onAttach(activity);

        mActivity = (DfuActivity) activity;
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState
    ) {
        //noinspection HardCodedStringLiteral
        Log.d(TAG, "onCreateView");

        final View view = inflater.inflate(R.layout.fragment_dfu_done, container, false);
        mActivity.onCreateView(view);

        view.findViewById(R.id.proceed).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View _) {
                mActivity.onDone();
            }
        });

        return view;
    }
}
