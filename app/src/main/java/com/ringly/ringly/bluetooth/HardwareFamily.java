package com.ringly.ringly.bluetooth;

import android.bluetooth.BluetoothGattService;
import android.util.Log;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.ringly.ringly.config.Bluetooth;

import java.util.Map;
import java.util.UUID;

import no.nordicsemi.android.dfu.DfuBaseService;

/**
 * Created by peter on 10/18/16.
 */
public enum HardwareFamily {
    PARK("V00", DfuService.class, Utilities.PARK_DFU_SERVICE_UUID, Bluetooth.PARK_DFU_ADDRESS),
    MADISON("V000", MadisonDfuService.class, Utilities.MADISON_DFU_SERVICE_UUID, Bluetooth.MADISON_DFU_ADDRESS),
    ;

    private static final String TAG = Utilities.class.getCanonicalName();
    public static final String BUNDLE_NAME = "hardwareFamily";

    // NOTE - this mapping of hardware version to HardwareFamily means we have to keep this file up
    // to date if we want to support new HW versions for DFU. This also happens in the iOS project
    // currently:
    // https://github.com/ringly/ios/blob/feature/future/RinglyDFU/RinglyDFU/DFUControllerMode.swift#L100
    //
    private static final Map<String, HardwareFamily> HW_TO_ENUM = ImmutableMap.<String, HardwareFamily>builder()
            .put("v08.19", PARK)
            .put("v08.20ca", PARK)
            .put("v08.21", PARK)
            .put("v09.01cn", PARK)
            .put("v08.21ca", PARK)
            .put("8.21", PARK)
            .put("8.22", PARK)
            .put("8.27", PARK)
            .put("9.01", PARK)
            .put("v09.01ca", PARK)
            .put("v08.31ca", PARK)
            .put("8.31", PARK)
            .put("9.12 aec", PARK)
            .put("9.12aec", PARK)
            .put("9.12", PARK)
            .put("v00", PARK)
            .put("v08", PARK)
            .put("v000", MADISON)
            .build();

    public final String defaultVersion;
    public final Class<? extends DfuBaseService> dfuBaseServiceClass;
    public final UUID recoveryModeServiceUuid;
    public final String dfuMacAddress;

    HardwareFamily(final String defaultVersion,
                   final Class<? extends DfuBaseService> dfuBaseServiceClass,
                   final UUID recoveryModeServiceUuid,
                   final String dfuMacAddress) {
        this.defaultVersion = defaultVersion;
        this.dfuBaseServiceClass = dfuBaseServiceClass;
        this.recoveryModeServiceUuid = recoveryModeServiceUuid;
        this.dfuMacAddress = dfuMacAddress;
    }

    public static Optional<HardwareFamily> lookupName(final String name) {
        if (name == null) {
            return Optional.absent();
        }
        try {
            return Optional.of(HardwareFamily.valueOf(name));
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "hardware family not found for: '" + name + "'", e);
            return Optional.absent();
        }
    }

    public static Optional<HardwareFamily> lookupHardware(final String hwVersion) {
        return Optional.fromNullable(HW_TO_ENUM.get(hwVersion.toLowerCase()));
    }
}
