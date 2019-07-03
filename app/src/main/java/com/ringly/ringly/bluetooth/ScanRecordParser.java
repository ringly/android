package com.ringly.ringly.bluetooth;

import android.os.ParcelUuid;
import android.util.Log;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static android.content.ContentValues.TAG;
import static java.util.Arrays.copyOfRange;

/**
 * ScanRecordParser
 * ================
 *
 * Introduces functionality to handle reading of `byte[] scanRecord` to identify
 * information related to DFU.
 *
 * Park and Madison peripherals use different service & characteristic UUIDs for
 * DFU, and this information is exposed in the advertising data when they are in
 * recovery mode. As a result, we should check if this is exposed to properly
 * handle recovery mode and to trigger the right update process.
 *
 * Created by peter on 10/18/16.
 */

public final class ScanRecordParser {

    public static Optional<HardwareFamily> getRecoveryModeHardwareFamily(byte[] scanRecord) {
        Map<ParcelUuid, HardwareFamily> uuidToFam = Maps.newHashMap();
        for (HardwareFamily fam : HardwareFamily.values()) {
            uuidToFam.put(new ParcelUuid(fam.recoveryModeServiceUuid), fam);
        }

        List<ParcelUuid> uuids = getSolicitedUuids(scanRecord);
        for (ParcelUuid uuid : uuids) {
            if (uuidToFam.containsKey(uuid)) {
                return Optional.of(uuidToFam.get(uuid));
            }
        }

        return Optional.absent();
    }


    ////
    //// private methods
    ////

    private static List<ParcelUuid> getSolicitedUuids(byte[] scanRecord) {
        // Check scan record to see if in recovery mode

        List<ParcelUuid> results = Lists.newArrayList();

        HashMap<Byte, byte[]> parsedScanRecord = parseScanRecord(scanRecord);

        // type == 21 has our solicited service uuids (which for now contains just the dfu if available)
        byte[] solicitedServiceUuids = parsedScanRecord.get(((byte) 0x15));

        if (solicitedServiceUuids != null && solicitedServiceUuids.length % 16 == 0) {
            for (int i = 0; i < solicitedServiceUuids.length; i += 16) {
                byte[] uuidBytes = Arrays.copyOfRange(solicitedServiceUuids, i, i + 16);
                results.add(parseUuidFrom(uuidBytes));
            }
        }

        return results;
    }

    private static HashMap<Byte, byte[]> parseScanRecord(byte[] scanRecord) {
        HashMap<Byte, byte[]> results = Maps.newHashMap();

        try {
            for (int i = 0; i < scanRecord.length; ) {
                byte length = scanRecord[i];
                if (length == 0) {
                    // TODO(mad-uuids) - Check with Ethan that once we hit a zero length it means
                    // there is nothing left in the scan record... anecdotally this matches what i'm seeing.
                    break;
                }
                byte type = scanRecord[i + 1];
                results.put(type, copyOfRange(scanRecord, i + 2, i + 1 + length));
                i = i + length + 1;
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            Log.e(TAG, "index out of bounds while reading scanRecord", e);
        }

        return results;
    }


    //
    // HACK via:
    // https://github.com/android/platform_frameworks_base/blob/2a5f608e7a5765db3b91165dac5b6280b26da26c/Android.mk
    //
    public static final ParcelUuid BASE_UUID =
            ParcelUuid.fromString("00000000-0000-1000-8000-00805F9B34FB");

    /** Length of bytes for 16 bit UUID */
    public static final int UUID_BYTES_16_BIT = 2;
    /** Length of bytes for 32 bit UUID */
    public static final int UUID_BYTES_32_BIT = 4;
    /** Length of bytes for 128 bit UUID */
    public static final int UUID_BYTES_128_BIT = 16;

    /**
     * Parse UUID from bytes. The {@code uuidBytes} can represent a 16-bit, 32-bit or 128-bit UUID,
     * but the returned UUID is always in 128-bit format.
     * Note UUID is little endian in Bluetooth.
     *
     * @param uuidBytes Byte representation of uuid.
     * @return {@link ParcelUuid} parsed from bytes.
     * @throws IllegalArgumentException If the {@code uuidBytes} cannot be parsed.
     */
    public static ParcelUuid parseUuidFrom(byte[] uuidBytes) {
        if (uuidBytes == null) {
            throw new IllegalArgumentException("uuidBytes cannot be null");
        }
        int length = uuidBytes.length;
        if (length != UUID_BYTES_16_BIT && length != UUID_BYTES_32_BIT &&
                length != UUID_BYTES_128_BIT) {
            throw new IllegalArgumentException("uuidBytes length invalid - " + length);
        }

        // Construct a 128 bit UUID.
        if (length == UUID_BYTES_128_BIT) {
            ByteBuffer buf = ByteBuffer.wrap(uuidBytes).order(ByteOrder.LITTLE_ENDIAN);
            long msb = buf.getLong(8);
            long lsb = buf.getLong(0);
            return new ParcelUuid(new UUID(msb, lsb));
        }

        // For 16 bit and 32 bit UUID we need to convert them to 128 bit value.
        // 128_bit_value = uuid * 2^96 + BASE_UUID
        long shortUuid;
        if (length == UUID_BYTES_16_BIT) {
            shortUuid = uuidBytes[0] & 0xFF;
            shortUuid += (uuidBytes[1] & 0xFF) << 8;
        } else {
            shortUuid = uuidBytes[0] & 0xFF ;
            shortUuid += (uuidBytes[1] & 0xFF) << 8;
            shortUuid += (uuidBytes[2] & 0xFF) << 16;
            shortUuid += (uuidBytes[3] & 0xFF) << 24;
        }
        long msb = BASE_UUID.getUuid().getMostSignificantBits() + (shortUuid << 32);
        long lsb = BASE_UUID.getUuid().getLeastSignificantBits();
        return new ParcelUuid(new UUID(msb, lsb));
    }
}
