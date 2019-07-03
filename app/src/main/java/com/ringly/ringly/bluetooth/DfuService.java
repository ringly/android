package com.ringly.ringly.bluetooth;

import android.app.Activity;

import com.ringly.ringly.ui.MainActivity;

import no.nordicsemi.android.dfu.DfuBaseService;

// TODO(mad-uuids) - rename this file to ParkDfuService to be more explicit

public class DfuService extends DfuBaseService {
    @Override
    protected Class<? extends Activity> getNotificationTarget() {
        return MainActivity.class;
    }
}
