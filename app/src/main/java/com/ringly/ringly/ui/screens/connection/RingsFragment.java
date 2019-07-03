package com.ringly.ringly.ui.screens.connection;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.ringly.ringly.R;
import com.ringly.ringly.bluetooth.HardwareFamily;
import com.ringly.ringly.bluetooth.ScanRecordParser;
import com.ringly.ringly.bluetooth.Utilities;
import com.ringly.ringly.config.Bluetooth;
import com.ringly.ringly.ui.MainActivity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;


public final class RingsFragment extends ListFragment implements BluetoothAdapter.LeScanCallback {

    public static RingsFragment newInstance(final Iterable<Ring> rings) {
        final ArrayList<String> addresses = Lists.newArrayList();
        final ArrayList<Integer> rssis = Lists.newArrayList();
        final ArrayList<String> hwFams = Lists.newArrayList();
        for (final Ring ring : rings) {
            addresses.add(ring.bluetoothDevice.getAddress());
            rssis.add(ring.rssi);
            hwFams.add(ring.recoveryHardwareFamily.isPresent() ?
                    ring.recoveryHardwareFamily.get().name() :
                    null);
        }

        final Bundle arguments = new Bundle();
        arguments.putStringArrayList(Argument.ADDRESSES.toString(), addresses);
        arguments.putIntegerArrayList(Argument.RSSIS.toString(), rssis);
        arguments.putStringArrayList(Argument.RECOVERY_HW_FAMS.toString(), hwFams);

        final RingsFragment fragment = new RingsFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    private static final String TAG = RingsFragment.class.getCanonicalName();

    private enum Argument {
        ADDRESSES,
        RSSIS,
        RECOVERY_HW_FAMS,
    }

    private static final int SCAN_DURATION = 2000; // milliseconds

    /**
     * RingsFragment.Ring
     * ==================
     *
     * Wrapper around BluetoothDevice to to also store rssi and hardware family.
     *
     */
    @SuppressWarnings({"PublicField", "InstanceVariableNamingConvention"})
    public static final class Ring {
        public final BluetoothDevice bluetoothDevice;
        public final int rssi;
        public final Optional<HardwareFamily> recoveryHardwareFamily;

        public Ring(final BluetoothDevice bluetoothDevice,
                    final int rssi,
                    final Optional<HardwareFamily> optionalHardwareFamily) {
            this.bluetoothDevice = bluetoothDevice;
            this.rssi = rssi;
            this.recoveryHardwareFamily = optionalHardwareFamily;
        }

        @SuppressWarnings("UnnecessaryThis")
        @Override
        public boolean equals(final Object o) {
            if (!(o instanceof Ring)) return false;
            final Ring that = (Ring) o;
            return this.bluetoothDevice.equals(that.bluetoothDevice);
        }

        @Override
        public int hashCode() {
            return bluetoothDevice.hashCode();
        }
    }


    private final class RingsAdapter extends BaseAdapter {
        private final List<Ring> mRings = Lists.newArrayList();

        RingsAdapter(final Collection<Ring> rings) {
            mRings.addAll(rings); // TODO sort these
        }

        public void add(final Ring ring) {
            if (mRings.contains(ring)) return;

            int i;
            // skip past initial rings with stronger RSSI until we find our spot:
            //noinspection StatementWithEmptyBody
            for (i = 0; i < mRings.size() && mRings.get(i).rssi >= ring.rssi; i++);
            mRings.add(i, ring);

            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mRings.size();
        }

        @Override
        public Ring getItem(final int position) {
            return mRings.get(position);
        }

        @Override
        public long getItemId(final int position) {
            return 0;
        }

        @Override
        public View getView(final int position, final View convertView, final ViewGroup parent) {
            final View view;
            if (convertView == null) {
                view = getActivity().getLayoutInflater()
                        .inflate(R.layout.listitem_rings, parent, false);
                mActivity.onCreateView(view);
            } else view = convertView;

            com.ringly.ringly.ui.Utilities.uppercaseAndKern((TextView) view.findViewById(R.id.name),
                    getItem(position).bluetoothDevice.getName());

            return view;
        }
    }


    private boolean mScanning = false;

    private MainActivity mActivity;
    private BluetoothAdapter mBluetoothAdapter;
    private Handler mHandler;
    private RingsAdapter mAdapter;


    ////
    //// ListFragment methods
    ////

    @Override
    public void onAttach(final Context activity) {
        //noinspection HardCodedStringLiteral
        Log.d(TAG, "onAttach");
        super.onAttach(activity);

        mActivity = (MainActivity) activity;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        //noinspection HardCodedStringLiteral
        Log.d(TAG, "onCreate: " + savedInstanceState);
        super.onCreate(savedInstanceState);

        mBluetoothAdapter = Utilities.getBluetoothAdapter(getActivity());
        mHandler = new Handler();

        // TODO(mad-uuids)(fam) - we are reconstructing the Rings?
        final Bundle arguments = getArguments();
        final List<String> addresses = arguments.getStringArrayList(Argument.ADDRESSES.toString());
        final List<Integer> rssis = arguments.getIntegerArrayList(Argument.RSSIS.toString());
        final List<String> recoveryFams = arguments.getStringArrayList(Argument.RECOVERY_HW_FAMS.toString());

        final Set<Ring> rings = Sets.newHashSet();
        for (int i = 0; i < addresses.size(); i++) {
            rings.add(new Ring(
                    mBluetoothAdapter.getRemoteDevice(addresses.get(i)),
                    rssis.get(i),
                    HardwareFamily.lookupName(recoveryFams.get(i))
            ));
        }

        mAdapter = new RingsAdapter(rings);

        setListAdapter(mAdapter);
    }

    @SuppressWarnings("RefusedBequest")
    @Override
    public View onCreateView(
            @NonNull final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState
    ) {
        //noinspection HardCodedStringLiteral
        Log.d(TAG, "onCreateView: " + savedInstanceState);

        final View view = inflater.inflate(R.layout.fragment_rings, container, false);
        mActivity.onCreateView(view);
        com.ringly.ringly.ui.Utilities.uppercaseAndKern((TextView)
            view.findViewById(R.id.ringly_main));
        return view;
    }

    @Override
    public void onResume() {
        //noinspection HardCodedStringLiteral
        Log.d(TAG, "onResume");
        super.onResume();

        startLeScan();
    }

    @Override
    public void onListItemClick(final ListView l, final View v, final int position, final long id) {
        //noinspection HardCodedStringLiteral
        Log.d(TAG, "onListItemClick: " + position);

        mActivity.onSelectRing(mAdapter.getItem(position));
    }

    @Override
    public void onPause() {
        //noinspection HardCodedStringLiteral
        Log.d(TAG, "onPause");
        super.onPause();

        stopLeScan();
    }

    @Override
    public void onDestroy() {
        //noinspection HardCodedStringLiteral
        Log.d(TAG, "onDestroy");

        mHandler.removeCallbacks(mRestartLeScan);

        super.onDestroy();
    }

    ////
    //// BluetoothAdapter.LeScanCallback callback, registered in onResume()
    ////

    @Override
    public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
        //noinspection HardCodedStringLiteral
        Log.d(TAG, "onLeScan: " + device);

        // TODO(mad-uuids)(fam) - check scanRecord for recovery uuids?
        final Optional<HardwareFamily> recoveryHardwareFamily
                = ScanRecordParser.getRecoveryModeHardwareFamily(scanRecord);

        if (Bluetooth.isRingly(device)) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mAdapter.add(new Ring(device, rssi, recoveryHardwareFamily));
                }
            });
        }
    }


    ////
    //// private methods
    ////

    private final Runnable mRestartLeScan = new Runnable() {
        @Override
        public void run() {
            stopLeScan();
            startLeScan();
        }
    };

    private void startLeScan() {
        if (mScanning) return;
        mScanning = true;

        // have to use deprecated method to support API < 21
        //noinspection deprecation
        mBluetoothAdapter.startLeScan(this);

        mHandler.postDelayed(mRestartLeScan, SCAN_DURATION);
    }

    private void stopLeScan() {
        if (!mScanning) return;
        mScanning = false;

        // have to use deprecated method to support API < 21
        //noinspection deprecation
        mBluetoothAdapter.stopLeScan(this);

        mHandler.removeCallbacks(mRestartLeScan);
    }
}
