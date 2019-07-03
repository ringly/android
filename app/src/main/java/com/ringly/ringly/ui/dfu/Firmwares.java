package com.ringly.ringly.ui.dfu;

import android.os.Bundle;
import android.util.Log;

import com.google.common.base.Optional;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Container of Firmwares.Firmware objects for:
 *
 * -   application
 * -   bootloader
 *
 * Where a Firmwares.Firmware objects represent a firmware update with:
 *
 * -   version - String - the version number of the firmware update, e.g., 2.2.0
 * -   value -  String - the API endpoint for downloading the firmware
 *
 * As a convenience this can be converted to and from a bundle, loaded from JSON,
 * and has an `Optional.isPresent()`-like `hasFirmwares()` method.
 */
public final class Firmwares {
    private static final String TAG = Firmwares.class.getCanonicalName();
    private static final String PACKAGE = Firmwares.class.getPackage().getName();

    public static final String BUNDLE_NAME = "firmwares";

    private enum Key {
        BOOTLOADER_VERSION,
        BOOTLOADER_VALUE,
        APPLICATION_VERSION,
        APPLICATION_VALUE,;

        @Override
        public String toString() {
            return PACKAGE + "." + name();
        }
    }

    public static Firmwares fromJson(final JSONArray firmwares) throws JSONException
    {
        Log.d(TAG, "fromJson: " + firmwares);
        Optional<Firmware> bootloaderUrl = Optional.absent();
        Optional<Firmware> applicationUrl = Optional.absent();
        for (int i = 0; i < firmwares.length(); i++) {
            final JSONObject firmware = firmwares.getJSONObject(i);
            if ("bootloader".equals(firmware.getString("target"))) {
                bootloaderUrl = Optional.of(
                        new Firmware(firmware.getString("version"), firmware.getString("url")));
            }
            if ("application".equals(firmware.getString("target"))) {
                applicationUrl = Optional.of(
                        new Firmware(firmware.getString("version"), firmware.getString("url")));
            }
        }
        Log.d(TAG, "firmware updates: bootloader=" + bootloaderUrl + " application=" + applicationUrl);
        return new Firmwares(bootloaderUrl, applicationUrl);
    }

    public static Firmwares fromBundle(final Bundle bundle) {
        return new Firmwares(
                Optional.fromNullable(bundle.getString(Key.BOOTLOADER_VERSION.toString())),
                Optional.fromNullable(bundle.getString(Key.BOOTLOADER_VALUE.toString())),
                Optional.fromNullable(bundle.getString(Key.APPLICATION_VERSION.toString())),
                Optional.fromNullable(bundle.getString(Key.APPLICATION_VALUE.toString())));
    }

    public static class Firmware {
        public static Optional<Firmware> of(final Optional<String> version, final Optional<String> value) {
            if (version.isPresent() != value.isPresent()) {
                throw new IllegalArgumentException("inconsistent version=" + version + " value=" + value);
            }
            if (version.isPresent()) return Optional.of(new Firmware(version.get(), value.get()));
            else return Optional.absent();
        }

        public final String version;
        public final String value;

        public Firmware(final String version, final String value) {
            this.version = version;
            this.value = value;
        }

        @Override
        public String toString() {
            return "version=" + version + " value=" + value;
        }

        public Firmware withValue(final String value) {
            return new Firmware(version, value);
        }
    }


    public final Optional<Firmware> bootloader;
    public final Optional<Firmware> application;

    public Firmwares() {
        this(Optional.<Firmware>absent(), Optional.<Firmware>absent());
    }

    public Firmwares(final Optional<Firmware> bootloader, final Optional<Firmware> application) {
        this.bootloader = bootloader;
        this.application = application;
    }

    public Firmwares(
            final Optional<String> bootloaderVersion, final Optional<String> bootloaderValue,
            final Optional<String> applicationVersion, final Optional<String> applicationValue
    ) {
        this(Firmware.of(bootloaderVersion, bootloaderValue),
                Firmware.of(applicationVersion, applicationValue));
    }

    public boolean hasFirmwares() {
        return bootloader.isPresent() || application.isPresent();
    }

    public Bundle toBundle() {
        final Bundle bundle = new Bundle();
        if (bootloader.isPresent()) {
            bundle.putString(Key.BOOTLOADER_VERSION.toString(), bootloader.get().version);
            bundle.putString(Key.BOOTLOADER_VALUE.toString(), bootloader.get().value);
        }
        if (application.isPresent()) {
            bundle.putString(Key.APPLICATION_VERSION.toString(), application.get().version);
            bundle.putString(Key.APPLICATION_VALUE.toString(), application.get().value);
        }
        return bundle;
    }

    @Override
    public String toString() {
        return "bootloader=" + bootloader + " application=" + application;
    }
}
