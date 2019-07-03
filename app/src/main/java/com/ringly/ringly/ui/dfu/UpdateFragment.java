package com.ringly.ringly.ui.dfu;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.ringly.ringly.R;
import com.ringly.ringly.bluetooth.HardwareFamily;
import com.ringly.ringly.bluetooth.Ring;
import com.ringly.ringly.bluetooth.RinglyService;
import com.ringly.ringly.bluetooth.Utilities;
import com.ringly.ringly.config.Bluetooth;
import com.ringly.ringly.config.Command;
import com.ringly.ringly.config.Mixpanel;

import java.util.List;
import java.util.Map;

import no.nordicsemi.android.dfu.DfuBaseService;
import no.nordicsemi.android.dfu.DfuProgressListener;
import no.nordicsemi.android.dfu.DfuProgressListenerAdapter;
import no.nordicsemi.android.dfu.DfuServiceInitiator;
import no.nordicsemi.android.dfu.DfuServiceListenerHelper;
import no.nordicsemi.android.dfu.DfuSettingsConstants;


public final class UpdateFragment extends Fragment
        implements Ring.Listener, BluetoothAdapter.LeScanCallback
{

    public static UpdateFragment newInstance(final Firmwares files,
                                             final boolean recovery,
                                             final HardwareFamily hardwareFamily) {
        final Bundle arguments = files.toBundle();
        if (recovery) {
            arguments.putBoolean(Argument.RECOVERY.toString(), true);
        }

        arguments.putString(Argument.HARDWARE_FAMILY.toString(), hardwareFamily.name());

        final UpdateFragment fragment = new UpdateFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    private static final String TAG = UpdateFragment.class.getCanonicalName();

    private static final int LISTEN_TIMEOUT = 10000; // milliseconds
    private static final int SCAN_TIMEOUT = 10000; // milliseconds
    private static final int DFU_DELAY = 1000; // milliseconds
    private static final int DFU_TIMEOUT = 45_000; // milliseconds
    private static final int DFU_REFRESH_CACHE_TIMEOUT = 10_000; //milliseconds
    private static final int PROGRESS_TRANSITION_DURATION = 400; // milliseconds
    private static final int MAX_LISTEN_ATTEMPTS = 8;
    private static final int OLD_DFU_SAMSUNG_ORDER = "samsung".equals(Build.MANUFACTURER) ? 1 : 0;


    private enum Argument {
        RECOVERY,
        HARDWARE_FAMILY,
    }

    private enum FirmwareType {
        BOOTLOADER,
        APPLICATION
    }


    private DfuActivity mActivity;
    private Handler mHandler;
    private BluetoothAdapter mBluetoothAdapter;
    private TextView mProgressView;
    private TextView mStatusView;
    private Drawable mProgressBar;

    private final List<FirmwareType> mTypes = Lists.newLinkedList();
    private boolean mRecovery;
    private HardwareFamily mHardwareFamily;
    private Firmwares mFiles;
    private int mIndex = -1;
    private int mListenAttempts;
    private int mOldDfuAttempts = 0;
    private BluetoothGatt mOldDfuGatt;

    private Optional<String> mTrackingDfuVersion;

    private boolean mListening;
    private boolean mScanning;
    private boolean mUpdating;


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
    public void onCreate(final Bundle savedInstanceState) {
        //noinspection HardCodedStringLiteral
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        final Bundle arguments = getArguments();

        mFiles = Firmwares.fromBundle(arguments);
        mRecovery = arguments.getBoolean(Argument.RECOVERY.toString());
        // TODO(mad-uuids) - verify this doesn't explode on lookup
        mHardwareFamily = HardwareFamily.lookupName(arguments.getString(Argument.HARDWARE_FAMILY.toString())).get();

        if (mFiles.bootloader.isPresent()) {
            mTypes.add(FirmwareType.BOOTLOADER);
        }
        if (mFiles.application.isPresent()) {
            mTypes.add(FirmwareType.APPLICATION);
        }

        mHandler = new Handler();
        mBluetoothAdapter = Utilities.getBluetoothAdapter(getActivity());
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState
    ) {
        //noinspection HardCodedStringLiteral
        Log.d(TAG, "onCreateView");

        final View view = inflater.inflate(R.layout.fragment_dfu_update, container, false);
        mActivity.onCreateView(view);

        mProgressView = (TextView) view.findViewById(R.id.progress);
        mStatusView = (TextView) view.findViewById(R.id.status);

        ImageView progressBar = (ImageView) view.findViewById(R.id.progress_bar);
        mProgressBar = progressBar.getDrawable();

        nextUpdate();

        return view;
    }

    @Override
    public void onDestroy() {
        //noinspection HardCodedStringLiteral
        Log.d(TAG, "onDestroy");
        super.onDestroy();

        if (mListening) stopListen();
        else if (mScanning) stopScan();
        else if (mUpdating) stopDfu();
    }


    ////
    //// Ring.Listener callback, registered in startListen()
    ////

    @Override
    public void onUpdate(final Ring ring) {
        //noinspection HardCodedStringLiteral
        Log.d(TAG, "onUpdate: connected=" + ring.isConnected());
        checkListening(true);

        if (ring.isConnected()) { // wait until ring is connected to send DFU command
            stopListen();
            RinglyService.doCommand(Command.ENTER_DFU, getActivity());

            // let the command execute before we start dfu, since it disconnects RinglyService:
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    startScan();
                }
            }, DFU_DELAY);
        }
    }


    ////
    //// LeScanCallback, registered in startScan()
    ////

    @Override
    public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
        Log.d(TAG, "onLeScan: " + device + " name=" + device.getName()); // NON-NLS

        // TODO(mad-uuids) - should we check one last time that scanRecord matches recovery hw?

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (!mScanning) {
                    Log.w(TAG, "no longer scanning"); // NON-NLS
                    return;
                }

                if (device.getName() != null && device.getName().startsWith(Bluetooth.DFU_PREFIX)) {
                    stopScan();

                    final Optional<String> bootloaderVersion = Bluetooth.getBootloaderVersion(device);
                    mTrackingDfuVersion = bootloaderVersion;

                    if (bootloaderVersion.isPresent() && mTypes.get(mIndex) == FirmwareType.BOOTLOADER
                            && mFiles.bootloader.get().version.equals(bootloaderVersion.get())) {
                        // the ring already has the desired bootloader,
                        // so skip to the next update, if there is one
                        if (!beginNextUpdate()) {
                            return;
                        }
                    }

                    // TODO(mad-uuids) - there's probably a cleaner way to do this... also, should I sent absent through if it's a madison one too?
                    String address = device.getAddress();
                    for (HardwareFamily fam : HardwareFamily.values()) {
                        if (fam.dfuMacAddress.equals(address)) {
                            startDfu(Optional.<String>absent());
                            return;
                        }
                    }

                    startDfu(Optional.of(address)); // old bootloader
                }
            }
        });
    }

    private void nextUpdate() {
        if (beginNextUpdate()) {
            mListenAttempts = 0; // fresh start
            startListen();
        }
    }

    private boolean beginNextUpdate() {
        mIndex++;
        if (mIndex < mTypes.size()) {
            com.ringly.ringly.ui.Utilities.uppercaseAndKern(mProgressView, Html.fromHtml(
                    "Update <b>" + (mIndex + 1) + "</b> of <b>" + mTypes.size() + "</b>"));
            return true;
        } else {
            mActivity.onUpdateDone();
            return false;
        }
    }

    private void startListen() {
        //noinspection HardCodedStringLiteral
        Log.d(TAG, "startListen");
        checkListening(false);

        mListenAttempts++;
        if (mListenAttempts <= MAX_LISTEN_ATTEMPTS) {
            // TODO(mad-uuids) - this looks like the one place where mRecovery has a meaning...
            if (mRecovery) {
                startScan();
            } else {
                mListening = true;
                mProgressBar.setLevel(0);
                mStatusView.setText("Connecting…");
                mHandler.postDelayed(mListenTimeout, LISTEN_TIMEOUT);
                mActivity.addListener(this); // run onUpdate() until ring is connected
            }
        } else {
            mActivity.onUpdateFailed();
        }
    }

    private void stopListen() {
        //noinspection HardCodedStringLiteral
        Log.d(TAG, "stopListen");
        checkListening(true);
        mListening = false;

        mActivity.removeListener(this);
        mHandler.removeCallbacks(mListenTimeout);
    }

    private final Runnable mListenTimeout = new Runnable() {
        @Override
        public void run() {
            //noinspection HardCodedStringLiteral
            Log.d(TAG, "listen timeout");

            stopListen();
            abortDfu(); // already happens in dfu timeout, but doesn't always take…
            // TODO toggle bluetooth?
            RinglyService.doReconnect(getActivity());
            startListen();
        }
    };

    private void startScan() {
        //noinspection HardCodedStringLiteral
        Log.d(TAG, "startScan");
        checkScanning(false);
        mScanning = true;

        mStatusView.setText("Scanning…");
        mHandler.postDelayed(mScanTimeout, SCAN_TIMEOUT);
        mBluetoothAdapter.startLeScan(this);
    }

    private void stopScan() {
        //noinspection HardCodedStringLiteral
        Log.d(TAG, "stopScan");
        checkScanning(true);
        mScanning = false;

        mBluetoothAdapter.stopLeScan(this);
        mHandler.removeCallbacks(mScanTimeout);
    }

    private final Runnable mScanTimeout = new Runnable() {
        @Override
        public void run() {
            //noinspection HardCodedStringLiteral
            Log.d(TAG, "scan timeout");

            stopScan();
            startListen(); // start over
        }
    };

    private void startDfu(final Optional<String> oldAddress) {
        //noinspection HardCodedStringLiteral
        Log.d(TAG, "startDfu: " + oldAddress);
        checkUpdating(false);
        mUpdating = true;

        mStatusView.setText("Starting…");
        mActivity.getPreferences().temporaryUnsetRing(); // stop RinglyService from connecting
        RinglyService.start(getActivity()); // show RinglyService that the ring is gone

        if (oldAddress.isPresent()) {
            // old bootloader can't handle fast packets from OnePlus 2 and Nexus 5X:
            PreferenceManager.getDefaultSharedPreferences(getActivity()).edit()
                    .putString(DfuSettingsConstants.SETTINGS_NUMBER_OF_PACKETS, "4").commit();

            // since old dfu reuses ring address, we need to refresh its service cache:
            Log.d(TAG, "manufacturer=" + Build.MANUFACTURER);
            mOldDfuAttempts++;
            // potentially try both styles, but try the most likely one first:
            if (mOldDfuAttempts%2 == OLD_DFU_SAMSUNG_ORDER) {
                /*
                 * tested on Galaxy S3, S4, and S5
                 */
                Log.d(TAG, "old DFU, Samsung style");
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        final Runnable oldDfuTimeout = new Runnable() {
                            @Override
                            public void run() {
                                Log.d(TAG, "old DFU timeout");
                                startOldDfu(oldAddress.get());
                            }
                        };
                        mHandler.postDelayed(oldDfuTimeout, DFU_REFRESH_CACHE_TIMEOUT);

                        Log.d(TAG, "old DFU: connectGatt");
                        mOldDfuGatt = mBluetoothAdapter.getRemoteDevice(oldAddress.get())
                                .connectGatt(getActivity(), false, new BluetoothGattCallback() {
                                    @Override
                                    public void onConnectionStateChange(final BluetoothGatt gatt, final int status, final int newState) {
                                        Log.d(TAG, "old DFU: onConnectionStateChange status=" + status + " newState=" + newState);
                                        if (newState == BluetoothGatt.STATE_CONNECTED) {
                                            Log.d(TAG, "old DFU: refresh");
                                            Utilities.refreshServiceCache(gatt);
                                        }
                                        if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                                            mHandler.removeCallbacks(oldDfuTimeout);
                                            startOldDfu(oldAddress.get());
                                        }
                                    }

                                    @Override
                                    public void onServicesDiscovered(final BluetoothGatt gatt, final int status) {
                                        Log.d(TAG, "old DFU: onServicesDiscovered status=" + status);
                                        Log.d(TAG, "old DFU: disconnect");
                                        gatt.disconnect();
                                    }
                                });
                        mOldDfuGatt.disconnect();
                    }
                }, DFU_DELAY);
            } else {
                /*
                 * tested on Moto E and Nexus 5 and 7
                 */
                Log.d(TAG, "old DFU, non-Samsung style");
                Log.d(TAG, "old DFU: connectGatt");
                final BluetoothGatt gatt = mBluetoothAdapter.getRemoteDevice(oldAddress.get())
                        .connectGatt(getActivity(), false, new BluetoothGattCallback() {
                        });
                Log.d(TAG, "old DFU: refresh"); // NON-NLS
                Utilities.refreshServiceCache(gatt);
                int n = 0;
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG, "old DFU: discover"); // NON-NLS
                        gatt.discoverServices();
                    }
                }, ++n * DFU_DELAY);
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG, "old DFU: refresh"); // NON-NLS
                        Utilities.refreshServiceCache(gatt);
                    }
                }, ++n * DFU_DELAY);
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG, "old DFU: close"); // NON-NLS
                        gatt.close();
                    }
                }, ++n * DFU_DELAY);
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        startDfu(oldAddress.get());
                    }
                }, ++n * DFU_DELAY);
            }

        } else {
            // in case we set preferences for the old bootloader, clear them
            PreferenceManager.getDefaultSharedPreferences(getActivity()).edit()
                    .remove(DfuSettingsConstants.SETTINGS_NUMBER_OF_PACKETS).commit();
            startDfu(mHardwareFamily.dfuMacAddress);
        }
    }

    private void startOldDfu(final String address) {
        Log.d(TAG, "old DFU: close");
        mOldDfuGatt.close();

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                startDfu(address);
            }
        }, DFU_DELAY);
    }

    private void startDfu(final String address) {
        //noinspection HardCodedStringLiteral
        Log.d(TAG, "startDfu: " + address);
        checkUpdating(true);

        final Firmwares.Firmware file;
        // TODO(mad-uuids) - verify I didn't break anything related to mIndex when bootloader...
        if (mTypes.get(mIndex) == FirmwareType.BOOTLOADER) {
            file = mFiles.bootloader.get();
        } else {
            file = mFiles.application.get();
        }

        DfuServiceListenerHelper.registerProgressListener(getActivity(), mDfuProgressListener);
        new DfuServiceInitiator(address)
                .setZip(file.value)
                .setDisableNotification(true)
                .start(getActivity(), mHardwareFamily.dfuBaseServiceClass);
        startDfuTimeout();

        trackDfuWrite(Mixpanel.Event.DFU_WRITE_STARTED);
    }

    private void stopDfu() {
        //noinspection HardCodedStringLiteral
        Log.d(TAG, "stopDfu");
        checkUpdating(true);
        mUpdating = false;

        DfuServiceListenerHelper.unregisterProgressListener(getActivity(), mDfuProgressListener);
        getActivity().stopService(new Intent(getActivity(), mHardwareFamily.dfuBaseServiceClass));
        stopDfuTimeout();

        mActivity.getPreferences().restoreRing(); // let RinglyService connect again
        RinglyService.start(getActivity()); // show RinglyService that the ring is back
    }

    private void abortDfu() {
        //noinspection HardCodedStringLiteral
        Log.d(TAG, "abortDfu");

        getActivity().sendBroadcast(new Intent(DfuBaseService.BROADCAST_ACTION)
                .putExtra(DfuBaseService.EXTRA_ACTION, DfuBaseService.ACTION_ABORT));
    }

    private void startDfuTimeout() {
        //noinspection HardCodedStringLiteral
        Log.d(TAG, "startDfuTimeout");
        checkUpdating(true);

        stopDfuTimeout();
        mHandler.postDelayed(mDfuTimeout, DFU_TIMEOUT);
    }

    private void stopDfuTimeout() {
        mHandler.removeCallbacks(mDfuTimeout);
    }

    private final Runnable mDfuTimeout = new Runnable() {
        @Override
        public void run() {
            //noinspection HardCodedStringLiteral
            Log.d(TAG, "dfu timeout");

            abortDfu();
            stopDfu();
            startListen(); // start over
        }
    };

    private final DfuProgressListener mDfuProgressListener = new DfuProgressListenerAdapter() {
        @Override
        public void onProgressChanged(
                final String deviceAddress, int percent, final float speed,
                final float avgSpeed, final int currentPart, final int partsTotal
        ) {
            //noinspection HardCodedStringLiteral
            Log.d(TAG, "onProgressChanged");
            checkUpdating(true);

            if (percent == 99) percent = 100; // never actually gets to 100 :'(

            mStatusView.setText(percent + "%");

            ObjectAnimator.ofInt(mProgressBar, "level", percent * 100) // max level is 10000
                .setDuration(PROGRESS_TRANSITION_DURATION)
                .start();

            startDfuTimeout(); // reset timer
        }

        @Override
        public void onDfuCompleted(final String deviceAddress) {
            //noinspection HardCodedStringLiteral
            Log.d(TAG, "onDfuCompleted");
            checkUpdating(true);

            stopDfu();

            trackDfuWrite(Mixpanel.Event.DFU_WRITE_COMPLETED);

            nextUpdate();
        }

        @Override
        public void onError(
                final String deviceAddress, final int error, final int errorType, final String message
        ) {
            //noinspection HardCodedStringLiteral
            Log.d(TAG, "onError: " + error + " " + errorType + " " + message);
            checkUpdating(true);

            mDfuTimeout.run(); // try again
        }
    };

    private void trackDfuWrite(final Mixpanel.Event event) {
        final FirmwareType type = mTypes.get(mIndex);
        final Firmwares.Firmware file;
        if (type == FirmwareType.BOOTLOADER) file = mFiles.bootloader.get();
        else file = mFiles.application.get();

        final Map<Mixpanel.Property, Object> properties = Maps.newHashMap(ImmutableMap.<Mixpanel.Property, Object>of(
                Mixpanel.Property.INDEX, mIndex,
                Mixpanel.Property.COUNT, mTypes.size(),
                Mixpanel.Property.PACKAGE_TYPE, type.name().toLowerCase(),
                Mixpanel.Property.PACKAGE_VERSION, file.version
        ));

        if (mTrackingDfuVersion.isPresent()) {
            final String dfuVersion = mTrackingDfuVersion.get();
            properties.put(Mixpanel.Property.DFU_VERSION, dfuVersion.equals("026") ? "0.0.26" : dfuVersion);
        }

        mActivity.getMixpanel().track(event, properties);
    }

    private void checkListening(final boolean listening) {
        checkState(listening, false, false);
    }
    private void checkScanning(final boolean scanning) {
        checkState(false, scanning, false);
    }
    private void checkUpdating(final boolean updating) {
        checkState(false, false, updating);
    }
    private void checkState(final boolean listening, final boolean scanning, final boolean updating) {
        if (mListening != listening || mScanning != scanning || mUpdating != updating) {
            throw new RuntimeException("listening=" + mListening + " scanning=" + mScanning + " updating=" + mUpdating);
        }
    }
}
