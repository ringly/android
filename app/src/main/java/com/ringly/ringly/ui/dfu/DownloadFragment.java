package com.ringly.ringly.ui.dfu;

import android.app.Activity;
import android.app.Fragment;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.ringly.ringly.R;
import com.ringly.ringly.RinglyApp;
import com.ringly.ringly.Utilities;
import com.ringly.ringly.bluetooth.HardwareFamily;

import org.json.JSONException;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;


public final class DownloadFragment extends Fragment  {

    public static DownloadFragment newInstance(final Firmwares urls,
                                               final boolean recovery,
                                               final HardwareFamily hardwareFamily) {
        final Bundle arguments = urls.toBundle();

        if (recovery) {
            arguments.putBoolean(Argument.RECOVERY.toString(), true);
        }

        arguments.putString(Argument.HARDWARE_FAMILY.toString(), hardwareFamily.name());

        final DownloadFragment fragment = new DownloadFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    private static final String TAG = DownloadFragment.class.getCanonicalName();

    private enum Argument {
        RECOVERY,
        HARDWARE_FAMILY,
    }

    private static String getFile(final URL url) {
        return Iterables.getLast(Splitter.on("/").split(url.getPath()));
    }


    private DfuActivity mActivity;


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
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState
    ) {
        //noinspection HardCodedStringLiteral
        Log.d(TAG, "onCreateView");

        final View view = inflater.inflate(R.layout.fragment_dfu_download, container, false);
        mActivity.onCreateView(view);

        final Bundle arguments = getArguments();

        final boolean recovery = arguments.getBoolean(Argument.RECOVERY.toString(), false);

        // TODO(mad-uuids) - any chance this lookup could also fail?
        final HardwareFamily hardwareFamily = HardwareFamily.lookupName(
                arguments.getString(Argument.HARDWARE_FAMILY.toString())
        ).get();

        if (recovery) {
            findRecoveryUpdates(hardwareFamily);
        } else {
            download(Firmwares.fromBundle(arguments));
        }

        return view;
    }


    ////
    //// private methods
    ////

    private void findRecoveryUpdates(final HardwareFamily recoveryHardwareFamily) {
        Log.d(TAG, "findRecoveryUpdates");
        new AsyncTask<Void, Void, Firmwares>() {
            @Override
            protected Firmwares doInBackground(final Void... params) {
                try {
                    return Firmwares.fromJson(RinglyApp.getInstance().getApi().getFirmwares(
                            Optional.of(recoveryHardwareFamily.defaultVersion), // hardware
                            Optional.<String>absent(), // bootloader
                            Optional.<String>absent(), // app
                            false, // all
                            true // force - bypass any feature gating (only b/c recovery mode)
                    ));
                } catch(IOException|JSONException e) {
                    Log.e(TAG, "failed to fetch firmware updates", e);
                    return new Firmwares();
                }
            }

            @Override
            protected void onPostExecute(final Firmwares urls) {
                if (urls.hasFirmwares()) {
                    download(urls);
                } else {
                    mActivity.onDownloadFailed();
                }
            }
        }.execute();
    }

    private void download(final Firmwares urls) {
        new AsyncTask<Void, Integer, Firmwares>() {
            @Override
            protected Firmwares doInBackground(final Void... params) {
                Optional<Firmwares.Firmware> bootloaderFile = Optional.absent();
                Optional<Firmwares.Firmware> applicationFile = Optional.absent();
                try {
                    if (urls.bootloader.isPresent()) {
                        bootloaderFile = Optional.of(doDownload(urls.bootloader.get()));
                    }
                    if (urls.application.isPresent()) {
                        applicationFile = Optional.of(doDownload(urls.application.get()));
                    }
                } catch (final IOException e) {
                    Log.e(TAG, "firmware download failed", e); // NON-NLS
                    return new Firmwares();
                }

                return new Firmwares(bootloaderFile, applicationFile);
            }

            @Override
            protected void onPostExecute(final Firmwares paths) {
                if (paths.hasFirmwares()) {
                    mActivity.onDownloadDone(paths);
                } else {
                    mActivity.onDownloadFailed();
                }
            }
        }.execute();
    }

    private Firmwares.Firmware doDownload(final Firmwares.Firmware firmwareUrl) throws IOException {
        final URL url = new URL(firmwareUrl.value);

        // TODO skip download if file already exists (and has correct checksum/size?)

        final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        RinglyApp.getInstance().getApi().addHeaders(connection);

        final InputStream in = connection.getInputStream();
        try {
            return firmwareUrl.withValue(Utilities.copyToFile(in, getFile(url), getActivity()).getPath());
        } finally {
            in.close();
        }
    }
}
