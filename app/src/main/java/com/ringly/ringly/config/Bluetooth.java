package com.ringly.ringly.config;


import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;

import com.google.common.base.Optional;
import com.ringly.ringly.bluetooth.Utilities;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings({"MagicNumber", "HardCodedStringLiteral", "UnusedDeclaration"})
public final class Bluetooth {
    private Bluetooth() {}


    public static final String RINGLY = "RINGLY";

    public static final String DFU_PREFIX = "Ringly-DFU";
    public static final String PARK_DFU_ADDRESS = "C0:89:67:45:23:01";
    public static final String MADISON_DFU_ADDRESS = "F0:E0:D0:C0:B0:A0";

    // org.bluetooth.descriptor.gatt.client_characteristic_configuration
    public static final UUID CLIENT_CHARACTERISTIC_CONFIGURATION_DESCRIPTOR_ID = uuid16(0x2902);

    //
    // ### Device Information Service
    //

    // org.bluetooth.service.device_information
    public static final BluetoothGattService DEVICE_INFORMATION_SERVICE = Utilities.newService(0x180a);
    // org.bluetooth.characteristic.firmware_revision_string
    public static final BluetoothGattCharacteristic FIRMWARE_REVISION_STRING_CHARACTERISTIC = Utilities.newCharacteristic(
            DEVICE_INFORMATION_SERVICE, 0x2a26,
            Utilities.Property.READ);
    // org.bluetooth.characteristic.hardware_revision_string
    public static final BluetoothGattCharacteristic HARDWARE_REVISION_STRING_CHARACTERISTIC = Utilities.newCharacteristic(
            DEVICE_INFORMATION_SERVICE, 0x2a27,
            Utilities.Property.READ);
    // org.bluetooth.characteristic.manufacturer_name_string
    public static final BluetoothGattCharacteristic MANUFACTURER_NAME_STRING_CHARACTERISTIC = Utilities.newCharacteristic(
            DEVICE_INFORMATION_SERVICE, 0x2a29,
            Utilities.Property.READ);
    // NONSTANDARD
    public static final BluetoothGattCharacteristic BOOTLOADER_REVISION_STRING_CHARACTERISTIC = Utilities.newCharacteristic(
            DEVICE_INFORMATION_SERVICE, 0x2aab,
            Utilities.Property.READ);

    //
    // ### Battery Service
    //

    // org.bluetooth.service.battery_service
    public static final BluetoothGattService BATTERY_SERVICE = Utilities.newService(0x180f);
    // org.bluetooth.characteristic.battery_level
    public static final BluetoothGattCharacteristic BATTERY_LEVEL_CHARACTERISTIC = Utilities.newCharacteristic(
            BATTERY_SERVICE, 0x2a19,
            Utilities.Property.READ, Utilities.Property.NOTIFY);
    // non-standard?
    public static final BluetoothGattCharacteristic CHARGE_STATE_CHARACTERISTIC = Utilities.newCharacteristic(
            BATTERY_SERVICE, 0x2a1b,
            Utilities.Property.READ, Utilities.Property.NOTIFY);

    //
    // ### Ringly Service
    //
    public static final BluetoothGattService RINGLY_SERVICE
            = Utilities.newService("ebdf3d60-706f-636f-9077-0002a5d5c51b");
    // this characteristic is only available for madison devices:
    public static final BluetoothGattCharacteristic RINGLY_CLEAR_BOND_CHARACTERISTIC = Utilities.newCharacteristic(
            RINGLY_SERVICE, "ebdf000f-706f-636f-9077-0002a5d5c51b",
            Utilities.Property.READ, Utilities.Property.WRITE);
    public static final BluetoothGattCharacteristic RINGLY_RUBRIC_CHARACTERISTIC = Utilities.newCharacteristic(
            RINGLY_SERVICE, "ebdf00a0-706f-636f-9077-0002a5d5c51b",
            Utilities.Property.READ, Utilities.Property.WRITE);
    public static final BluetoothGattCharacteristic RINGLY_TAP_CHARACTERISTIC = Utilities.newCharacteristic(
            RINGLY_SERVICE, "ebdf08a0-706f-636f-9077-0002a5d5c51b",
            Utilities.Property.NOTIFY);
    public static final BluetoothGattCharacteristic RINGLY_ANCS_CHARACTERISTIC = Utilities.newCharacteristic(
            RINGLY_SERVICE, "ebdfe420-706f-636f-9077-0002a5d5c51b",
            Utilities.Property.READ, Utilities.Property.WRITE, Utilities.Property.NOTIFY);

    //
    // ### Ringly Logging Service
    //
    public static final BluetoothGattService RINGLY_LOGGING_SERVICE
            = Utilities.newService("d2b1ad60-604f-11e4-8460-0002a5d5c51b");
    public static final BluetoothGattCharacteristic RINGLY_QUERIED_INFO_CHARACTERISTIC = Utilities.newCharacteristic(
            RINGLY_LOGGING_SERVICE, "d2b11c60-604f-11e4-8460-0002a5d5c51b",
            Utilities.Property.NOTIFY);
    public static final BluetoothGattCharacteristic RINGLY_FLASH_LOG_CHARACTERISTIC = Utilities.newCharacteristic(
            RINGLY_LOGGING_SERVICE, "d2b14a20-604f-11e4-8460-0002a5d5c51b",
            Utilities.Property.READ);
    public static final BluetoothGattCharacteristic RINGLY_LOGGING_REQUEST_CHARACTERISTIC = Utilities.newCharacteristic(
            RINGLY_LOGGING_SERVICE, "d2b1af60-604f-11e4-8460-0002a5d5c51b",
            Utilities.Property.READ, Utilities.Property.WRITE);

    public static final BluetoothGattService ACTIVITY_TRACKING =
        Utilities.newService("7bb5e345-3359-48fd-b6f6-4fc86056ac70");
    public static final BluetoothGattCharacteristic ACTIVITY_TRACKING_CONTROL_CHARACTERISTIC =
        Utilities.newCharacteristic(ACTIVITY_TRACKING, "7bb5cafe-3359-48fd-b6f6-4fc86056ac70",
            Utilities.Property.READ, Utilities.Property.WRITE);
    public static final BluetoothGattCharacteristic ACTIVITY_TRACKING_DATA_CHARACTERISTIC =
        Utilities.newCharacteristic(ACTIVITY_TRACKING, "7bb5feed-3359-48fd-b6f6-4fc86056ac70",
            Utilities.Property.READ, Utilities.Property.INDICATE);
    public static final BluetoothGattCharacteristic ACCELEROMETER_DATA_CHARACTERISTIC =
        Utilities.newCharacteristic(ACTIVITY_TRACKING, "7bb50911-3359-48fd-b6f6-4fc86056ac70",
            Utilities.Property.READ, Utilities.Property.NOTIFY);

    public static UUID uuid16(final int uuid16) {
        return UUID.fromString(String.format("0000%04x-0000-1000-8000-00805f9b34fb", uuid16));
    }

    private static final Pattern RINGLY_PATTERN
            = Pattern.compile('^' + RINGLY, Pattern.CASE_INSENSITIVE);
    public static boolean isRingly(final BluetoothDevice device) {
        return device.getName() != null && RINGLY_PATTERN.matcher(device.getName()).find();
    }

    private static final Pattern DFU_VERSION_PATTERN
            = Pattern.compile(Bluetooth.DFU_PREFIX + "\\((.*?)\\)"); // NON-NLS
    public static Optional<String> getBootloaderVersion(final BluetoothDevice device) {
        // TODO(mad-uuids) - looking at the name is dodgy
        if (device.getName() != null) {
            final Matcher m = DFU_VERSION_PATTERN.matcher(device.getName());
            if (m.find()) {
                return Optional.of(m.group(1));
            }
        }
        return Optional.absent();
    }
}
