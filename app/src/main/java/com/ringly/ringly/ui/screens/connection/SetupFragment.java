package com.ringly.ringly.ui.screens.connection;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.common.collect.ImmutableMap;
import com.ringly.ringly.NotificationListener;
import com.ringly.ringly.R;
import com.ringly.ringly.config.Mixpanel;
import com.ringly.ringly.config.Screen;
import com.ringly.ringly.ui.MainActivity;
import com.ringly.ringly.ui.SetupHelper;
import com.ringly.ringly.ui.Utilities;


public final class SetupFragment extends Fragment {

    private static final String TAG = SetupFragment.class.getCanonicalName();

    private static final int REQUEST_PHONE_STATE = 1;
    private static final int REQUEST_RECEIVE_SMS = 2;
    private static final int REQUEST_RECEIVE_MMS = 3;

    private MainActivity mActivity;


    ////
    //// Fragment methods
    ////

    @Override
    public void onAttach(final Context context) {
        //noinspection HardCodedStringLiteral
        Log.d(TAG, "onAttach");
        super.onAttach(context);

        mActivity = (MainActivity) context;
    }

    @Override
    public void onStart() {
        super.onStart();

        mActivity.getMixpanel().track(Mixpanel.Event.NOTIFICATIONS_REQUESTED);
    }

    @Override
    public View onCreateView(
            final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState
    ) {
        //noinspection HardCodedStringLiteral
        Log.d(TAG, "onCreateView");

        final View view = inflater.inflate(R.layout.fragment_setup, container, false);
        mActivity.onCreateView(view);

        Utilities.uppercaseAndKern((TextView) view.findViewById(R.id.ringly_main));

        view.findViewById(R.id.proceed).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                requestPermissions();
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        //noinspection HardCodedStringLiteral
        Log.d(TAG, "onResume");
        super.onResume();

        // if they enabled it, move on, otherwise stick around:
        // (if they don't want to enable they need to explicitly skip)
        // TODO: Allow explicitly skipping.
        if (SetupHelper.canNotify(mActivity)) {
            mActivity.changeScreen(Screen.CONNECTION, R.string.post_setup_notifications);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(permissions.length > 0 &&
            ActivityCompat.shouldShowRequestPermissionRationale(mActivity, permissions[0])) {
            new AlertDialog.Builder(mActivity)
                .setMessage(R.string.permissions_denied)
                .setPositiveButton(R.string.enable, (__, ___) -> requestPermissions())
                .create()
                .show();

            mActivity.getMixpanel().track(Mixpanel.Event.NOTIFICATIONS_PERMISSION,
                ImmutableMap.of(Mixpanel.Property.RESULT, R.string.denied));
        } else {
            mActivity.getMixpanel().track(Mixpanel.Event.NOTIFICATIONS_PERMISSION,
                ImmutableMap.of(Mixpanel.Property.RESULT, R.string.accepted));

            requestPermissions();
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        mActivity.getMixpanel().track(Mixpanel.Event.NOTIFICATIONS_COMPLETED,
            ImmutableMap.of(Mixpanel.Property.ACCEPTED, SetupHelper.canNotify(getContext())));
    }

    private void requestPermissions() {
        if (checkPhoneStatePermissions(REQUEST_PHONE_STATE) &&
            checkSmsPermissions(REQUEST_RECEIVE_SMS) &&  checkMmsPermissions(REQUEST_RECEIVE_MMS) &&
            !SetupHelper.notificationsEnabled(getContext())) {
            startActivity(new Intent(NotificationListener.ACTION_NOTIFICATION_LISTENER_SETTINGS));
        }
    }

    private boolean checkPhoneStatePermissions(int reqId) {
        if(ContextCompat.checkSelfPermission(mActivity, Manifest.permission.READ_PHONE_STATE) !=
            PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_PHONE_STATE}, reqId);
            return false;
        }

        return true;
    }

    private boolean checkSmsPermissions(int reqId) {
        if(ContextCompat.checkSelfPermission(mActivity, Manifest.permission.RECEIVE_SMS) !=
            PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(mActivity, Manifest.permission.READ_SMS) !=
                PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.READ_SMS,

            }, reqId);
            return false;
        }

        return true;
    }

    private boolean checkMmsPermissions(int reqId) {
        if(ContextCompat.checkSelfPermission(mActivity, Manifest.permission.RECEIVE_MMS) !=
                PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{
                    Manifest.permission.RECEIVE_MMS
            }, reqId);
            return false;
        }

        return true;
    }
}
