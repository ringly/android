package com.ringly.ringly.config;

import com.ringly.ringly.config.model.AuthToken;
import com.ringly.ringly.config.model.DeviceSnapshotResponse;
import com.ringly.ringly.config.model.User;

import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;
import rx.Observable;

public interface LoginService {

    @FormUrlEncoded
    @POST("api-token-auth")
    Observable<AuthToken> authorize(@Field("username") String username,
                                    @Field("password") String password);

    @FormUrlEncoded
    @POST("users")
    Observable<User> createUser(@Field("email") String email,
                                @Field("password") String password,
                                @Field("receive_updates") boolean receiveUpdates);


    @FormUrlEncoded
    @POST("users/device-snapshot")
    Observable<DeviceSnapshotResponse> registerRing(@Field("mac_address") String mac_address,
                                                    @Field("operating_system") int os,
                                                    @Field("uuid") String uuid,
                                                    @Field("os_version") String osVersion,
                                                    @Field("client_version") String clientVersion,
                                                    @Field("application_version") String appVersion,
                                                    @Field("softdevice_version") String softDeviceVersion,
                                                    @Field("bootloader_version") String bootloaderVersion,
                                                    @Field("hardware_version") String hardware_version,
                                                    @Field("ring_name") String ringName,
                                                    @Field("model") String model);
}
