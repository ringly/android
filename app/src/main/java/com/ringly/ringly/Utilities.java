package com.ringly.ringly;

import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.io.ByteStreams;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public final class Utilities {
    private Utilities() {}

    private static final String TAG = Utilities.class.getCanonicalName();

    public static final long NANOSECONDS_PER_HOUR = 60 * 60 * 1_000_000_000L;


    private static final int BYTE_MASK = 0xff;
    private static final String HEX_BYTE_FORMAT = "%02X";
    private static final String NON_DIGIT_RE = "\\D";

    public static <E extends Enum<E>> Optional<E> valueOfNullable(final Class<E> cls, final String name) {
        if (name != null) {
            try {
                return Optional.of(Enum.valueOf(cls, name));
            } catch (final IllegalArgumentException e) {
                //noinspection HardCodedStringLiteral
                Log.w(TAG, "unknown enum name", e);
            }
        }
        return Optional.absent();
    }

    public static String toHexString(final byte... bytes) {
        final List<String> hex = Lists.newLinkedList();
        for (final int b : bytes) hex.add(String.format(HEX_BYTE_FORMAT, b & BYTE_MASK));
        //noinspection MagicCharacter
        return Joiner.on(":").join(hex);
    }

    public static int compareVersions(final String version1, final String version2) {
        // TODO unit tests
        final String[] parts1 = version1.split(NON_DIGIT_RE);
        final String[] parts2 = version2.split(NON_DIGIT_RE);

        for (int i = 0; i < parts1.length || i < parts2.length; i++) {
            final int part1 = i < parts1.length && !parts1[i].equals("") ? Integer.parseInt(parts1[i]) : 0;
            final int part2 = i < parts2.length && !parts2[i].equals("") ? Integer.parseInt(parts2[i]) : 0;

            if (part1 != part2) {
                return part1 - part2;
            }
        }
        return 0;
    }

    public static File copyAssetToFile(final String name, final Context context)
            throws IOException
    {
        final InputStream in = context.getAssets().open(name);
        try {
            return copyToFile(in, name, context);
        } finally {
            in.close();
        }
    }

    public static File copyToFile(final InputStream in, final String name, final Context context)
            throws IOException
    {
        final OutputStream out = context.openFileOutput(name, 0);
        try {
            ByteStreams.copy(in, out);
        } finally {
            out.close();
        }
        return context.getFileStreamPath(name);
    }

    public static <T, S> Optional<S> bind(Optional<T> o, Function<T, Optional<S>> f) {
        return o.isPresent() ? f.apply(o.get()) : Optional.absent();
    }

    /**
     * Returns true if location needs to be enabled in order to scan for bluetooth devices.
     * @param context
     * @return whether we need to turn location on or not to scan for bluetooth devices.
     */
    public static boolean needsLocationEnabled(Context context) {
        // based on tests, while API level 23 requires requesting the location permission
        // to scan for bluetooth devices, it does not require needing it to be enabled,
        // but API level 24 does...
        //
        // Also, Build.VERSION_CODES doesn't have a NOUGAT with our current deps.
        //
        return Build.VERSION.SDK_INT >= 24 && !isLocationEnabled(context);
    }

    /**
     * Returns true if the user has location turned on.
     * @param context
     * @return whether the Location service has been enabled or not.
     */
    public static boolean isLocationEnabled(Context context) {
        // derived from http://stackoverflow.com/a/22980843/376489
        int locationMode = 0;

        try {
            locationMode = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE);
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
            return false;
        }

        return locationMode != Settings.Secure.LOCATION_MODE_OFF;
    }

    public static byte[] intToByteArrayLittleEndian(int value, int numBytes) {
        // modified from http://stackoverflow.com/a/11419863/376489
        byte[] encodedValue = new byte[numBytes];
        for (int i = numBytes - 1; i >= 0; i--) {
            encodedValue[i] = (byte) (value >> Byte.SIZE * i);
        }
        return encodedValue;
    }
}
