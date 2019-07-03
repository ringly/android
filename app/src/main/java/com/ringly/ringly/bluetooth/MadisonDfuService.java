package com.ringly.ringly.bluetooth;

import android.app.Activity;

import com.ringly.ringly.ui.MainActivity;

import java.util.UUID;

import no.nordicsemi.android.dfu.DfuBaseService;


public class MadisonDfuService extends DfuBaseService {
    @Override
    protected Class<? extends Activity> getNotificationTarget() {
        return MainActivity.class;
    }

    @Override
    protected UUID getDfuServiceUuid() {
        return Utilities.MADISON_DFU_SERVICE_UUID;
    }

    @Override
    protected UUID getDfuControlPointUuid() {
        return Utilities.MADISON_DFU_CONTROL_POINT_CHARACTERISTIC;
    }

    @Override
    protected UUID getDfuPacketUuid() {
        return Utilities.MADISON_DFU_PACKET_CHARACTERISTIC;
    }
}
