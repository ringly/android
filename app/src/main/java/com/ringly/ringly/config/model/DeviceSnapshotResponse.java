package com.ringly.ringly.config.model;

import com.google.gson.annotations.SerializedName;

public class DeviceSnapshotResponse {
    @SerializedName("device_created")
    public final boolean deviceCreated;

    @SerializedName("device_id")
    public final int deviceId;

    @SerializedName("snapshot_id")
    public final int snapshotId;

    @SerializedName("snapshot_created")
    public final boolean snapshotCreated;

    public DeviceSnapshotResponse(boolean deviceCreated, int deviceId, int snapshotId, boolean snapshotCreated) {
        this.deviceCreated = deviceCreated;
        this.deviceId = deviceId;
        this.snapshotId = snapshotId;
        this.snapshotCreated = snapshotCreated;
    }
}
