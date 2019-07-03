package com.ringly.ringly.config;

import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.CharStreams;
import com.ringly.ringly.BuildConfig;
import com.ringly.ringly.Preferences;
import com.ringly.ringly.bluetooth.Ring;
import com.ringly.ringly.config.model.DeviceSnapshotResponse;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import rx.Observable;
import rx.schedulers.Schedulers;

public class Api {

    private static final String TAG = Api.class.getCanonicalName();

    private static final Map<String, String> HEADERS = ImmutableMap.of(
            "X-APPTOKEN", "YOUR-TOKEN-HERE",
            "X-PLATFORM", "Android"
    );
    private static final String AUTH_TOKEN_HEADER = "Authorization";
    private static final String AUTH_TOKEN_PRE = "Token ";
    private static final String PHONE_ID_HEADER = "X-UUID";
    private static final String ROOT_URL = "https://api.ringly.com/v0/";
    private static final String FIRMWARE_PATH = "firmware/";

    private final String mPhoneId;

    private final LoginService mLoginService;
    private final GuidedMeditationsService mGuidedMeditationsService;

    public Api(final Context context) {
        mPhoneId = Settings.Secure.getString(context.getContentResolver(),
                Settings.Secure.ANDROID_ID);

        HttpLoggingInterceptor logger = new HttpLoggingInterceptor();
        logger.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
            .addInterceptor(new HeaderInterceptor())
            .addInterceptor(new AuthTokenInterceptor(context))
            .addInterceptor(logger)
            .build();

        mLoginService = new Retrofit.Builder()
            .baseUrl(ROOT_URL)
            .client(client)
            .addCallAdapterFactory(RxJavaCallAdapterFactory.createWithScheduler(Schedulers.io()))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(LoginService.class);

        mGuidedMeditationsService = new Retrofit.Builder()
            .baseUrl(ROOT_URL)
            .client(client)
            .addCallAdapterFactory(RxJavaCallAdapterFactory.createWithScheduler(Schedulers.io()))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GuidedMeditationsService.class);
    }

    public JSONArray getFirmwares(
            final Optional<String> hardware, final Optional<String> bootloader, final Optional<String> application,
            final boolean all, final boolean force
    ) throws IOException, JSONException {
        // HACK TODO(mad-uuids) - strip test details from fw app version
        String applicationRaw = application.orNull();
        if (applicationRaw != null) {
            applicationRaw = Splitter.on('-').split(applicationRaw).iterator().next();
        }
        Log.d(TAG, "getFirmwares(hardware=" + hardware.orNull() + ", bootloader=" + bootloader.orNull() + ", " +
            "application=" + applicationRaw + ", all=" + all + ", force=" + force + ")");
        final Map<String, String> parameters = Maps.newHashMap();
        parameters.put("android_application", BuildConfig.VERSION_NAME);

        if (hardware.isPresent()) {
            parameters.put("hardware", hardware.get());
        }
        if (bootloader.isPresent()) {
            parameters.put("bootloader", bootloader.get());
        }
        if (applicationRaw != null) {
            parameters.put("application", applicationRaw);
        }
        if (all) {
            parameters.put("all", "true");
        }
        if (force) {
            parameters.put("force", "true");
        }

        final HttpURLConnection connection = openConnection(FIRMWARE_PATH, parameters);
        if (connection.getResponseCode() != 200) {
            throw new IOException("API returned response code" + connection.getResponseCode());
        }

        final InputStream in = connection.getInputStream();
        // TODO once we drop API Level 18, use try-with-resources:
        //noinspection TryFinallyCanBeTryWithResources
        try {
            // TODO actually check what charset we're receiving:
            return new JSONArray(CharStreams.toString(new InputStreamReader(in, Charsets.UTF_8)));
        } finally {
            in.close();
        }
    }

    public void addHeaders(final HttpURLConnection connection) {
        for (final Map.Entry<String, String> header : HEADERS.entrySet()) {
            connection.setRequestProperty(header.getKey(), header.getValue());
        }
        connection.setRequestProperty(PHONE_ID_HEADER, mPhoneId);
    }

    public LoginService getLoginService() {
        return mLoginService;
    }

    public GuidedMeditationsService getGuidedMeditationsService() {
        return mGuidedMeditationsService;
    }

    public Observable<DeviceSnapshotResponse> registerDevice(Ring ring, String uuid) {
        return mLoginService.registerRing(
            ring.getGatt().getDevice().getAddress(),
            1,
            uuid,
            Build.VERSION.RELEASE,
            BuildConfig.VERSION_NAME,
            ring.getFirmwareRevision().or(""),
            "unimplemented",
            ring.getBootloaderRevision().or(""),
            ring.getHardwareRevision().or(""),
            ring.getGatt().getDevice().getName(),
            Build.MANUFACTURER + " " + Build.MODEL
        );
    }

    /////////////////////////////////////////////
    // Private methods

    private HttpURLConnection openConnection(
            final String path, final Map<String, String> parameters
    ) throws IOException {
        final List<String> queries = Lists.newLinkedList();
        for (final Map.Entry<String, String> parameter : parameters.entrySet()) {
            // TODO URL-encode
            queries.add(parameter.getKey() + "=" + parameter.getValue());
        }
        final String query = queries.isEmpty() ? "" : "?" + Joiner.on("&").join(queries);
        final URL url = new URL(ROOT_URL + path + query);

        final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        addHeaders(connection);
        return connection;
    }

    /////////////////////////////////////////////
    // Retrofit interceptors

    private static class HeaderInterceptor implements Interceptor {
        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();
            Request.Builder reqp = request.newBuilder();

            for(final Map.Entry<String, String> header : HEADERS.entrySet()) {
                reqp.addHeader(header.getKey(), header.getValue());
            }

            return chain.proceed(reqp.build());
        }
    }

    private static class AuthTokenInterceptor implements Interceptor {
        Context mContext;

        public AuthTokenInterceptor(Context context) {
            mContext = context.getApplicationContext();
        }

        @Override
        public Response intercept(Chain chain) throws IOException {
            Optional<Request> req =
                Preferences.getAuthToken(mContext)
                    .transform(
                        at -> chain.request()
                            .newBuilder()
                            .addHeader(AUTH_TOKEN_HEADER, AUTH_TOKEN_PRE + at)
                            .build()
                    );

            // Interceptor::proceed could be unpure, so we compute the Optional here
            return chain.proceed(req.or(chain.request()));
        }
    }

}
