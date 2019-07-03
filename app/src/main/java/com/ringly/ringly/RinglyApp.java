package com.ringly.ringly;

import android.app.Application;
import android.support.multidex.MultiDexApplication;

import com.ringly.ringly.config.Api;
import com.ringly.ringly.config.GuidedMeditationsCache;

/**
 * Created by Monica on 6/20/2017.
 */

public class RinglyApp extends MultiDexApplication {

    //I moved this from BaseActivity becuase I need only one instance for all app, not one for each Activity
    private Api mApi;

    private static RinglyApp sInstance = null;

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;
        mApi = new Api(this);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        GuidedMeditationsCache.getInstance().cancelDownloadingTasks();
    }

    public static RinglyApp getInstance() {
        //This will never happen
        if (sInstance == null) {
            throw new IllegalStateException();
        }
        return sInstance;
    }

    public Api getApi() {
        return mApi;
    }
}
