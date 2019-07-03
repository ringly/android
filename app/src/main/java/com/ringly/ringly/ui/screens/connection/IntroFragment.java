package com.ringly.ringly.ui.screens.connection;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.ringly.ringly.R;
import com.ringly.ringly.bluetooth.HardwareFamily;
import com.ringly.ringly.bluetooth.ScanRecordParser;
import com.ringly.ringly.bluetooth.Utilities;
import com.ringly.ringly.config.Bluetooth;
import com.ringly.ringly.config.Mixpanel;
import com.ringly.ringly.ui.MainActivity;

import java.util.Set;

import static com.ringly.ringly.Utilities.isLocationEnabled;
import static com.ringly.ringly.Utilities.needsLocationEnabled;


public final class IntroFragment extends Fragment implements BluetoothAdapter.LeScanCallback {

    private static final String TAG = IntroFragment.class.getCanonicalName();

    private static final int LOCATION_REQUEST_CODE = 1;

    private static final int ENABLE_LOCATION_REQUEST_CODE = 2;

    private static final int SCAN_DURATION = 5000; // milliseconds


    private final Set<RingsFragment.Ring> mRings = Sets.newHashSet();

    private MainActivity mActivity;
    private BluetoothAdapter mBluetoothAdapter;
    private Handler mHandler;

    private Button mConnect;


    ////
    //// Fragment methods
    ////


    @Override
    public void onCreate(final Bundle savedInstanceState) {
        //noinspection HardCodedStringLiteral
        Log.d(TAG, "onCreate: " + savedInstanceState);
        super.onCreate(savedInstanceState);

        mBluetoothAdapter = Utilities.getBluetoothAdapter(getActivity());
        mHandler = new Handler();
    }

    @Override
    public void onAttach(final Context activity) {
        //noinspection HardCodedStringLiteral
        Log.d(TAG, "onAttach");
        super.onAttach(activity);

        mActivity = (MainActivity) activity;
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        //noinspection HardCodedStringLiteral
        Log.d(TAG, "onCreateView");

        final View view = inflater.inflate(R.layout.fragment_intro, container, false);
        com.ringly.ringly.ui.Utilities.uppercaseAndKern(
            (TextView) view.findViewById(R.id.ringly_main));

        mConnect = (Button) view.findViewById(R.id.proceed);
        mConnect.setOnClickListener(v -> {
            startLeScan();
            mConnect.setEnabled(false); // don't click twice
            mConnect.setText(R.string.searching);

            mActivity.getMixpanel().track(Mixpanel.Event.TAPPED_BUTTON, ImmutableMap.of(
                    Mixpanel.Property.NAME, getResources().getString(R.string.connect)));
        });

        return view;
    }

    @Override
    public void onDestroy() {
        //noinspection HardCodedStringLiteral
        Log.d(TAG, "onDestroy");

        mHandler.removeCallbacks(mTimeout);

        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if(requestCode == LOCATION_REQUEST_CODE) {
            if(grantResults.length >  0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLeScan();
            } else if(permissions.length > 0 &&
            ActivityCompat.shouldShowRequestPermissionRationale(mActivity, permissions[0])) {
                new AlertDialog.Builder(mActivity)
                    .setMessage(R.string.location_denied)
                    .setPositiveButton(R.string.enable, (__, ___) ->
                        requestPermissions(new String[]{ Manifest.permission.ACCESS_COARSE_LOCATION },
                            LOCATION_REQUEST_CODE))
                    .setOnDismissListener(__ -> resetConnectButton())
                    .create()
                    .show();

            } else {
                resetConnectButton();
            }
        }
    }

    private void requestLocation() {
        new AlertDialog.Builder(mActivity)
            .setMessage(R.string.location_needs_enabling)
            .setPositiveButton(R.string.enable, (__, ___) -> {
                Intent enableLocationIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivityForResult(enableLocationIntent, ENABLE_LOCATION_REQUEST_CODE);
            })
            .setOnDismissListener(__ -> resetConnectButton())
            .create()
            .show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ENABLE_LOCATION_REQUEST_CODE) {
            boolean needsLocationEnabled = needsLocationEnabled(mActivity);
            Log.d(TAG, "[onActivityResult] requestCode=" + requestCode +
                    ", resultCode=" + resultCode +
                    ", needsLocationEnabled=" + needsLocationEnabled);
            if (needsLocationEnabled) {
                // just keep requesting, while testing it I see that I can safely keep
                // trying and if the user ever wants to stop, she can hit the back button
                // from the AlertDialog (which appears via `requestLocation()`).
                requestLocation();
            } else {
                startLeScan();
            }
        }
    }

    ////
    //// BluetoothAdapter.LeScanCallback callback, registered in onClick()
    ////

    @Override
    public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
        //noinspection HardCodedStringLiteral
        Log.d(TAG, "onLeScan: " + device);

        // TODO(mad-uuids)(fam) - verify check scanRecord for recovery uuids
        final Optional<HardwareFamily> recoveryHardwareFamily =
                ScanRecordParser.getRecoveryModeHardwareFamily(scanRecord);

        if (Bluetooth.isRingly(device)) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mRings.add(new RingsFragment.Ring(device, rssi, recoveryHardwareFamily));
                    if (mRings.size() > 1) {
                        done();
                    }
                }
            });
        }
    }


    ////
    //// private methods
    ////

    private final Runnable mTimeout = new Runnable() {
        @Override
        public void run() {
            if (mRings.isEmpty()) {
                stopLeScan();
                final View view = getView();
                if (view == null) return;

                Toast.makeText(getActivity(), R.string.no_rings_found, Toast.LENGTH_SHORT).show();
                resetConnectButton();
            } else done();
        }
    };

    private void resetConnectButton() {
        mConnect.setText(R.string.connect);
        mConnect.setEnabled(true);
    }

    private void done() {
        mHandler.removeCallbacks(mTimeout);
        stopLeScan();

        mActivity.onRingsFound(mRings);
    }

    @SuppressWarnings("MethodOnlyUsedFromInnerClass")
    private void startLeScan() {
        //noinspection deprecation
        // For 23+ we need location permissions to scan bluetooth
        if (ContextCompat.checkSelfPermission(mActivity, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{ Manifest.permission.ACCESS_COARSE_LOCATION },
                    LOCATION_REQUEST_CODE);
        }
        else if (needsLocationEnabled(mActivity)) {
            requestLocation();
        }
        else {
            mBluetoothAdapter.startLeScan(this);
            mHandler.postDelayed(mTimeout, SCAN_DURATION);
        }
    }

    private void stopLeScan() {
        //noinspection deprecation
        mBluetoothAdapter.stopLeScan(this);
    }
}
