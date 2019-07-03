package com.ringly.ringly.bluetooth;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothProfile;
import android.os.Handler;
import android.util.Log;

import com.google.common.collect.Lists;

import java.util.Queue;

public final class GattCallback extends BluetoothGattCallback {

    private static final String TAG = GattCallback.class.getCanonicalName();

    private static final int TIMEOUT = 4000; // milliseconds. was 500 ms. increased to allow time for re-encryption.

    public interface Listener {
        void onConnect(BluetoothGatt gatt);
        void onDisconnect(BluetoothGatt gatt);
        void onRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic);
        void onReadRssi(BluetoothGatt gatt, int rssi);
        void onFailure(BluetoothGatt gatt);
    }


    private final Handler mHandler = new Handler();
    private final Queue<Runnable> mQueue = Lists.newLinkedList();
    private final Listener mListener;

    private boolean mRunning = false;


    public GattCallback(final Listener listener) {
        mListener = listener;
    }

    public void performReadOrWrite(final Runnable r) {
        // TODO accept success and failure callbacks, though it's tricky with timeouts
        if (mRunning) {
            mQueue.add(r);
            //noinspection HardCodedStringLiteral
            Log.d(TAG, "queue grew to: " + mQueue.size());
        } else run(r); // no wait. no, wait.
    }


    ////
    //// private methods
    ////

    private final Runnable mTimer = new Runnable() {
        @Override
        public void run() {
            //noinspection HardCodedStringLiteral
            Log.w(TAG, "BLE read or write timed out");
            abortAll();
        }
    };

    private void run(final Runnable r) {
        mRunning = true;
        r.run();
        mHandler.postDelayed(mTimer, TIMEOUT);
    }

    private void finishedReadOrWrite() {
        mHandler.removeCallbacks(mTimer);
        mRunning = false; // nothing is currently running

        Runnable queuedRunnable = mQueue.poll();

        if (queuedRunnable != null) {
            run(queuedRunnable);
            //noinspection HardCodedStringLiteral
            Log.d(TAG, "queue shrank to: " + mQueue.size());
        }
    }

    // XXX: hack, this was private. trying something to force-clear the queue during (re)connection.
    public void abortAll() {
        mHandler.removeCallbacks(mTimer);
        mRunning = false;

        if (!mQueue.isEmpty()) {
            //noinspection HardCodedStringLiteral
            Log.d(TAG, "queue cleared from: " + mQueue.size());
            mQueue.clear();
        }
    }


    ////
    //// BluetoothGattCallback callbacks
    ////

    @Override
    public void onConnectionStateChange(final BluetoothGatt gatt, final int status, final int newState) {
        //noinspection HardCodedStringLiteral
        Log.d(TAG, "onConnectionStateChange, status: " + status);

        if (status == 133 /* GATT_ERROR */) {
            Log.w(TAG, "*** received GATT_ERROR (133). calling onFailure()...");
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    abortAll();
                    mListener.onFailure(gatt);
                }
            });
            return;
        }

        // TODO status?
        if (newState == BluetoothProfile.STATE_CONNECTED) {
            //noinspection HardCodedStringLiteral
            Log.d(TAG, String.format("onConnectionStateChange(%d, STATE_CONNECTED)", status));

            if (!gatt.discoverServices()) {
                throw new RuntimeException("discoverServices returned false");
            }
        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            //noinspection HardCodedStringLiteral
            Log.d(TAG, String.format("onConnectionStateChange(%d, STATE_DISCONNECTED)", status));

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    abortAll();
                    mListener.onDisconnect(gatt);
                }
            });
        }
    }

    @Override
    public void onServicesDiscovered(final BluetoothGatt gatt, final int status) {
        //noinspection HardCodedStringLiteral
        Log.d(TAG, "onServicesDiscovered, status: " + status);

        if (status != BluetoothGatt.GATT_SUCCESS /* 133 is GATT_ERROR */) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Log.w(TAG, "onServicesDiscovered did not receive GATT_SUCCESS. failing...");
                    abortAll();
                    mListener.onFailure(gatt);
                }
            });
            return;
        }

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                abortAll();
                mListener.onConnect(gatt);
            }
        });
    }

    @Override
    public void onCharacteristicChanged(
            final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic
    ) {
        //noinspection HardCodedStringLiteral
        Log.d(TAG, "onCharacteristicChanged");

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mListener.onRead(gatt, characteristic);
            }
        });
    }

    @Override
    public void onReadRemoteRssi(final BluetoothGatt gatt, final int rssi, final int status) {
        //noinspection HardCodedStringLiteral
        Log.d(TAG, "onReadRemoteRssi");

        // TODO status?
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mListener.onReadRssi(gatt, rssi);
            }
        });
    }


    ////
    //// read/write callbacks, which allow us to advance the read/write queue
    ////

    @Override
    public void onCharacteristicRead(
            final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, final int status
    ) {
        //noinspection HardCodedStringLiteral
        Log.d(TAG, "onCharacteristicRead, status: " + status);

        // TODO status?
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                finishedReadOrWrite();
                mListener.onRead(gatt, characteristic);
            }
        });
    }

    @Override
    public void onCharacteristicWrite(
            final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, final int status
    ) {
        //noinspection HardCodedStringLiteral
        Log.d(TAG, "onCharacteristicWrite, status: " + status);

        // TODO status?
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                finishedReadOrWrite();
            }
        });
    }

    @Override
    public void onDescriptorRead(
            final BluetoothGatt gatt, final BluetoothGattDescriptor descriptor, final int status
    ) {
        //noinspection HardCodedStringLiteral
        Log.d(TAG, "onDescriptorRead, status: " + status);

        // TODO status?
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                finishedReadOrWrite();
            }
        });
    }

    @Override
    public void onDescriptorWrite(
            final BluetoothGatt gatt, final BluetoothGattDescriptor descriptor, final int status
    ) {
        //noinspection HardCodedStringLiteral
        Log.d(TAG, "onDescriptorWrite, status: " + status);

        // TODO status?
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                finishedReadOrWrite();
            }
        });
    }
}
