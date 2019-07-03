package com.ringly.ringly.bluetooth;


import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.ringly.ringly.config.Mixpanel;

@SuppressWarnings({"PublicField", "InstanceVariableNamingConvention"})
public class Ring {

    public interface Listener {
        void onUpdate(Ring ring);
    }

    private final BluetoothGatt mGatt;
    private final Listener mListener;
    private final Mixpanel mMixpanel;

    private Optional<BluetoothGattCharacteristic> mRinglyRubricCharacteristic = Optional.absent();
    private boolean mConnected = false;
    private boolean mHasBootloaderRevision;
    private Optional<Integer> mBatteryLevel = Optional.absent();
    private Optional<Integer> mChargeState = Optional.absent();
    private Optional<String> mFirmwareRevision = Optional.absent();
    private Optional<String> mHardwareRevision = Optional.absent();
    private Optional<String> mBootloaderRevision = Optional.absent();

    public Ring(final BluetoothGatt gatt, final Listener listener, final Mixpanel mixpanel) {
        mGatt = gatt;
        mListener = listener;
        mMixpanel = mixpanel;
    }

    public BluetoothGatt getGatt() {
        return mGatt;
    }

    public Optional<BluetoothGattCharacteristic> getRinglyRubricCharacteristic() {
        return mRinglyRubricCharacteristic;
    }

    public void setRinglyRubricCharacteristic(final Optional<BluetoothGattCharacteristic> value) {
        mRinglyRubricCharacteristic = value;
    }

    public boolean isConnected() {
        return mConnected;
    }

    public void setConnected(final boolean value) {
        if (!value) { // reset all values on disconnect
            setBatteryLevel(Optional.<Integer>absent());
            setChargeState(Optional.<Integer>absent());
            setFirmwareRevision(Optional.<String>absent());
            setHardwareRevision(Optional.<String>absent());
            setBootloaderRevision(Optional.<String>absent());
        }

        mConnected = value;
        update(Mixpanel.Property.CONNECTED, Optional.of(value));
    }

    public boolean hasBootloaderRevision() {
        return mHasBootloaderRevision;
    }

    public void setHasBootloaderRevision(final boolean value) {
        mHasBootloaderRevision = value;
    }

    public Optional<Integer> getBatteryLevel() {
        return mBatteryLevel;
    }

    public void setBatteryLevel(final Optional<Integer> value) {
        if (!mConnected) return; // TODO why do we need this?
        mBatteryLevel = value;
        update(Mixpanel.Property.BATTERY_LEVEL, value);
    }

    public Optional<Integer> getChargeState() {
        return mChargeState;
    }

    public void setChargeState(final Optional<Integer> value) {
        if (!mConnected) return; // TODO why do we need this?
        mChargeState = value;
        update(Mixpanel.Property.CHARGE_STATE, value);
    }

    public Optional<String> getFirmwareRevision() {
        return mFirmwareRevision;
    }

    public void setFirmwareRevision(final Optional<String> value) {
        if (!mConnected) return; // TODO why do we need this?
        mFirmwareRevision = value;
        update(Mixpanel.Property.FIRMWARE_REVISION, value);
    }

    public Optional<String> getHardwareRevision() {
        return mHardwareRevision;
    }

    public void setHardwareRevision(final Optional<String> value) {
        if (!mConnected) return; // TODO why do we need this?
        mHardwareRevision = value;
        update(Mixpanel.Property.HARDWARE_REVISION, value);
    }

    public Optional<String> getBootloaderRevision() {
        return mBootloaderRevision;
    }

    public void setBootloaderRevision(final Optional<String> value) {
        if (!mConnected) return; // TODO why do we need this?
        mBootloaderRevision = value;
        update(Mixpanel.Property.BOOTLOADER_REVISION, value);
    }

    private void update(final Mixpanel.Property property, final Optional<?> value) {
        mListener.onUpdate(this);

        if (value.isPresent()) {
            mMixpanel.registerSuperProperties(ImmutableMap.of(property, value.get()));
        }
        else mMixpanel.unregisterSuperProperty(property);
    }
}
