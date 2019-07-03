package com.ringly.ringly.ui.dfu;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.ringly.ringly.R;
import com.ringly.ringly.bluetooth.Ring;
import com.ringly.ringly.config.Mixpanel;


public final class ChargerFragment extends Fragment implements Ring.Listener {

    public static ChargerFragment newInstance(final boolean recovery) {
        final Bundle arguments = new Bundle();
        if (recovery) arguments.putBoolean(Argument.RECOVERY.toString(), true);

        final ChargerFragment fragment = new ChargerFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    private static final String TAG = ChargerFragment.class.getCanonicalName();

    private enum Argument {
        RECOVERY,
    }


    private DfuActivity mActivity;
    private ImageView mRingBox;
    private View mProceedButton;
    private View mProgressBar;


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

        final View view = inflater.inflate(R.layout.fragment_dfu_charger, container, false);
        mActivity.onCreateView(view);

        mRingBox = (ImageView) view.findViewById(R.id.ring_box);
        mProgressBar = view.findViewById(R.id.progress_bar);

        mProceedButton = view.findViewById(R.id.proceed);
        mProceedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mActivity.onChargerDone();
            }
        });

        view.findViewById(R.id.cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mActivity.onChargerCanceled();
            }
        });

        // in recovery mode, we just assume the ring is in the charger:
        if (getArguments().getBoolean(Argument.RECOVERY.toString())) showRingInCharger();

        return view;
    }

    @Override
    public void onStart() {
        //noinspection HardCodedStringLiteral
        Log.d(TAG, "onStart");
        super.onStart();

        mActivity.addListener(this);
    }

    @Override
    public void onStop() {
        //noinspection HardCodedStringLiteral
        Log.d(TAG, "onStop");
        super.onStop();

        mActivity.removeListener(this);
    }


    ////
    //// Ring.Listener callback, registered in onStart
    ////

    @Override
    public void onUpdate(final Ring ring) {
        //noinspection HardCodedStringLiteral
        Log.d(TAG, "onUpdate");

        // TODO would be better to force a fresh read
        // because if you remove the ring from its charger right before this check,
        // then it will still seem to be connected and in the charger.

        if (ring.getChargeState().isPresent() && ring.getChargeState().get() > 0) {
            showRingInCharger();

            track(Mixpanel.Event.DFU_RING_IN_CHARGER);
        } else {
            if (ring.getChargeState().isPresent()) { // only change image once we're sure
                mRingBox.setImageLevel(0);
            }
            mProceedButton.setVisibility(View.INVISIBLE);
            mProgressBar.setVisibility(ring.isConnected() ? View.INVISIBLE : View.VISIBLE);

            track(Mixpanel.Event.DFU_REQUESTED_RING_IN_CHARGER);
        }
    }


    ////
    //// private methods
    ////

    private void showRingInCharger() {
        mRingBox.setImageLevel(1);
        mProgressBar.setVisibility(View.INVISIBLE);
        mProceedButton.setVisibility(View.VISIBLE);
    }

    private Mixpanel.Event mLastEvent;
    private void track(final Mixpanel.Event event) {
        // only track if something has changed
        if (event != mLastEvent) {
            mActivity.getMixpanel().track(event);
            mLastEvent = event;
        }
    }
}
