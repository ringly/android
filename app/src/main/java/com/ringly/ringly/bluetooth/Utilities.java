package com.ringly.ringly.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.util.Log;

import com.google.common.base.Optional;
import com.ringly.ringly.config.Bluetooth;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public final class Utilities {
    private Utilities() {}


    private static final String TAG = Utilities.class.getCanonicalName();

    public enum Property {
        READ(BluetoothGattCharacteristic.PROPERTY_READ),
        WRITE(BluetoothGattCharacteristic.PROPERTY_WRITE),
        NOTIFY(BluetoothGattCharacteristic.PROPERTY_NOTIFY),
        INDICATE(BluetoothGattCharacteristic.PROPERTY_INDICATE),
        ;

        @SuppressWarnings("InstanceVariableNamingConvention")
        private final int flag;
        Property(final int flag) {
            this.flag = flag;
        }
    }



    // DFU Park UUIDs
    public static final UUID PARK_DFU_SERVICE_UUID = UUID.fromString("00001530-1212-efde-1523-785feabcd123");
    public static final UUID PARK_DFU_CONTROL_POINT_CHARACTERISTIC_UUID = UUID.fromString("00001531-1212-efde-1523-785feabcd123");
    public static final UUID PARK_DFU_PACKET_CHARACTERISTIC = UUID.fromString("00001532-1212-efde-1523-785feabcd123");

    // DFU Madison UUIDs
    public static final UUID MADISON_DFU_SERVICE_UUID = UUID.fromString("a01f1540-70db-4ce5-952b-873759f85c44");
    public static final UUID MADISON_DFU_CONTROL_POINT_CHARACTERISTIC = UUID.fromString("a01f1541-70db-4ce5-952b-873759f85c44");
    public static final UUID MADISON_DFU_PACKET_CHARACTERISTIC = UUID.fromString("a01f1542-70db-4ce5-952b-873759f85c44");


    public static BluetoothManager getBluetoothManager(final Context context) {
        final BluetoothManager manager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        return manager;
    }

    public static BluetoothAdapter getBluetoothAdapter(final Context context) {
        final BluetoothManager manager = Utilities.getBluetoothManager(context);
        return manager.getAdapter();
    }

    public static BluetoothGattService newService(final int uuid16) {
        return newService(Bluetooth.uuid16(uuid16));
    }

    public static BluetoothGattService newService(final String uuid) {
        return newService(UUID.fromString(uuid));
    }

    public static BluetoothGattService newService(final UUID uuid) {
        return new BluetoothGattService(uuid, BluetoothGattService.SERVICE_TYPE_PRIMARY);
    }

    public static BluetoothGattCharacteristic newCharacteristic(
            final BluetoothGattService service, final int uuid16, final Property... properties
    ) {
        return newCharacteristic(service, Bluetooth.uuid16(uuid16), properties);
    }

    public static BluetoothGattCharacteristic newCharacteristic(
            final BluetoothGattService service, final String uuid, final Property... properties
    ) {
        return newCharacteristic(service, UUID.fromString(uuid), properties);
    }

    public static BluetoothGattCharacteristic newCharacteristic(
            final BluetoothGattService service, final UUID uuid, final Property... properties
    ) {
        boolean notify = false;
        int flags = 0;
        for (final Property property : properties) {
            if (property == Property.NOTIFY) notify = true;
            //noinspection AccessingNonPublicFieldOfAnotherObject
            flags |= property.flag;
        }

        final BluetoothGattCharacteristic characteristic
                = new BluetoothGattCharacteristic(uuid, flags, 0);

        if (notify) {
            newDescriptor(characteristic, Bluetooth.CLIENT_CHARACTERISTIC_CONFIGURATION_DESCRIPTOR_ID);
        }

        service.addCharacteristic(characteristic);
        return characteristic;
    }

    @SuppressWarnings("UnusedDeclaration")
    public static BluetoothGattDescriptor newDescriptor(
            final BluetoothGattCharacteristic characteristic, final int uuid16
    ) {
        return newDescriptor(characteristic, Bluetooth.uuid16(uuid16));
    }

    @SuppressWarnings("UnusedDeclaration")
    public static BluetoothGattDescriptor newDescriptor(
            final BluetoothGattCharacteristic characteristic, final String uuid
    ) {
        return newDescriptor(characteristic, UUID.fromString(uuid));
    }

    public static BluetoothGattDescriptor newDescriptor(
            final BluetoothGattCharacteristic characteristic, final UUID uuid
    ) {
        final BluetoothGattDescriptor descriptor = new BluetoothGattDescriptor(uuid, 0);
        characteristic.addDescriptor(descriptor);
        return descriptor;
    }

    public static Optional<BluetoothGattCharacteristic> getCharacteristic(
            final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic
    ) {
        final BluetoothGattService s = gatt.getService(characteristic.getService().getUuid());
        if (s == null) {
            //noinspection HardCodedStringLiteral
            Log.w(TAG, "device doesn't have expected service UUID " + characteristic.getService().getUuid());
            return Optional.absent();
        }

        final BluetoothGattCharacteristic c = s.getCharacteristic(characteristic.getUuid());
        if (c == null) {
            //noinspection HardCodedStringLiteral
            Log.w(TAG, "device doesn't have expected characteristic UUID " + characteristic.getUuid());
            return Optional.absent();
        }

        return Optional.of(c);
    }

    public static boolean enableNotifications(
            final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic
    ) {
        if (!gatt.setCharacteristicNotification(characteristic, true)) return false;

        final BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                Bluetooth.CLIENT_CHARACTERISTIC_CONFIGURATION_DESCRIPTOR_ID
        );

        // Set based on the properties the characteristic has.
        byte[] value =
            (characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) == 0 ?
                BluetoothGattDescriptor.ENABLE_INDICATION_VALUE :
                BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE;

        return descriptor != null
                && descriptor.setValue(value)
                && gatt.writeDescriptor(descriptor);
    }

    // adapted from http://stackoverflow.com/questions/22596951/how-to-programmatically-force-bluetooth-low-energy-service-discovery-on-android
    public static boolean refreshServiceCache(final BluetoothGatt gatt) {
        try {
            //noinspection HardCodedStringLiteral
            final Method m = gatt.getClass().getMethod("refresh");
            return (Boolean) m.invoke(gatt);
        } catch (final NoSuchMethodException e) {
            //noinspection HardCodedStringLiteral
            Log.e(TAG, "BluetoothGatt.refresh failed", e);
            return false;
        } catch (final IllegalAccessException e) {
            //noinspection HardCodedStringLiteral
            Log.e(TAG, "BluetoothGatt.refresh failed", e);
            return false;
        } catch (final InvocationTargetException e) {
            //noinspection HardCodedStringLiteral
            Log.e(TAG, "BluetoothGatt.refresh failed", e);
            return false;
        }
        // TODO once we drop API level 18, we can use multi-catch syntax
    }

    public static boolean removeBond(final BluetoothDevice device) {
        try {
            //noinspection HardCodedStringLiteral
            final Method m = device.getClass().getMethod("removeBond");
            return (Boolean) m.invoke(device);
        } catch (final NoSuchMethodException e) {
            //noinspection HardCodedStringLiteral
            Log.e(TAG, "BluetoothDevice.removeBond failed", e);
            return false;
        } catch (final IllegalAccessException e) {
            //noinspection HardCodedStringLiteral
            Log.e(TAG, "BluetoothDevice.removeBond failed", e);
            return false;
        } catch (final InvocationTargetException e) {
            //noinspection HardCodedStringLiteral
            Log.e(TAG, "BluetoothDevice.removeBond failed", e);
            return false;
        }
    }

    public static boolean cancelPairingDialog(final BluetoothDevice device) {
        try {
            //noinspection HardCodedStringLiteral
            final Method m = device.getClass().getMethod("cancelPairingUserInput");
            return (Boolean) m.invoke(device);
        } catch (IllegalAccessException e) {
            //noinspection HardCodedStringLiteral
            Log.e(TAG, "BluetoothDevice.cancelPairingUserInput failed", e);
            return false;
        } catch (InvocationTargetException e) {
            //noinspection HardCodedStringLiteral
            Log.e(TAG, "BluetoothDevice.cancelPairingUserInput failed", e);
            return false;
        } catch (NoSuchMethodException e) {
            //noinspection HardCodedStringLiteral
            Log.e(TAG, "BluetoothDevice.cancelPairingUserInput failed", e);
            return false;
        }
    }

    public static boolean deviceIsEncrypted(final BluetoothDevice device) {
        try {
            //noinspection HardCodedStringLiteral
            final Method m = device.getClass().getMethod("isEncrypted");
            return (Boolean) m.invoke(device);
        } catch (IllegalAccessException e) {
            //noinspection HardCodedStringLiteral
            Log.e(TAG, "BluetoothDevice.isEncrypted failed", e);
            return false;
        } catch (InvocationTargetException e) {
            //noinspection HardCodedStringLiteral
            Log.e(TAG, "BluetoothDevice.isEncrypted failed", e);
            return false;
        } catch (NoSuchMethodException e) {
            //noinspection HardCodedStringLiteral
            Log.d(TAG, "BluetoothDevice.isEncrypted() method not found.");
            return false;
        }
    }

    ////
    //// Activity Tracking
    ////

    // UTC timestamp in milliseconds for 2016-04-06 20:00
    public static final long START_DATE = 1459972800000L;

    /**
     * Converts a timestamp from milliseconds to the Ringly time in bytes
     *
     * @param ms - UTC timestamp in milliseconds
     * @return ms converted to a Ringly understandable 3-bytes
     */
    public static byte[] msToMinuteBytes(long ms) {
        long msSinceStart = Math.max(0, ms - START_DATE);
        int mins = (int) (msSinceStart / (60000));
        return new byte[] {
            (byte) (mins & 0xFF),
            (byte) ((mins >> 8) & 0xFF),
            (byte) ((mins >> 16) & 0xFF)
        };
    }

    /**
     * Returns the Ringly time bytes as a UTC timestamp in milliseconds
     *
     * Time Formatting (3 bytes)
     *
     * Lower 23 bits - minutes since April 6, 2016 8 PM UTC (1459972800)
     * Top bit - indicates real time or inferred time
     *
     * -   1 == real time (time is calculated from phone: control point Date time write
     *                     or Apple time service)
     * -   0 == inferred time (count is incremented by one over last saved minute count)
     *
     * @param bytes - the bytes from a Ringly device for a time
     * @return the time value as a UTC timestamp in milliseconds
     */
    public static long minuteBytesToMs(byte[] bytes) {
        long mins = (bytes[0] & 0xFF) | ((bytes[1] & 0xFF) << 8) | ((bytes[2] & 0x7F) << 16);
        return TimeUnit.MINUTES.toMillis(mins) + START_DATE;
    }
}
