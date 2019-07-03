package com.ringly.ringly.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.ringly.ringly.Preferences;
import com.ringly.ringly.bluetooth.Ring;
import com.ringly.ringly.bluetooth.RinglyService;
import com.ringly.ringly.config.Api;
import com.ringly.ringly.config.Mixpanel;

import java.util.Set;

public class BaseActivity extends AppCompatActivity implements ServiceConnection {

    private static final String TAG = BaseActivity.class.getCanonicalName();

    private static final String GOTHAM_BOOK_ASSET = "Gotham-Book.otf";


    private final Set<Ring.Listener> mListeners = Sets.newHashSet();

    // TODO make these private and use getters:
    protected Preferences mPreferences;
    protected Typeface mGothamBook;
    protected Mixpanel mMixpanel;

    private Optional<RinglyService> mService = Optional.absent();


    ////
    //// Activity methods
    ////

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        //noinspection HardCodedStringLiteral
        Log.d(TAG, "onCreate: " + savedInstanceState);
        super.onCreate(savedInstanceState);

        mPreferences = new Preferences(this);
        mGothamBook = Typeface.createFromAsset(getAssets(), GOTHAM_BOOK_ASSET);
        mMixpanel = new Mixpanel(this);
    }

    @Override
    protected void onDestroy() {
        //noinspection HardCodedStringLiteral
        Log.d(TAG, "onDestroy");

        mMixpanel.flush();

        super.onDestroy();
    }


    ////
    //// ServiceConnection callbacks
    ////

    @Override
    public void onServiceConnected(final ComponentName name, final IBinder binder) {
        //noinspection HardCodedStringLiteral
        Log.d(TAG, "onServiceConnected: " + name);

        final RinglyService.LocalBinder localBinder = (RinglyService.LocalBinder) binder;
        final RinglyService service = localBinder.getService();

        if (mService.isPresent()) {
            final RinglyService oldService = mService.get();

            if (service == oldService) {
                Log.d(TAG, "already connected to service id=" + service.hashCode()); // NON-NLS
                return; // already bound to this service
            }

            Log.w(TAG, "overwriting connection to service id=" + oldService.hashCode()); // NON-NLS
            // remove listeners from old service before adding to new:
            for (final Ring.Listener l : mListeners) oldService.removeListener(l);
        }

        Log.d(TAG, "connecting to service id=" + service.hashCode()); // NON-NLS
        mService = Optional.of(service);

        // copy list before iterating, in case a callback adds or removes listeners:
        for (final Ring.Listener l : Lists.newLinkedList(mListeners)) service.addListener(l);
    }

    @Override
    public void onServiceDisconnected(final ComponentName name) {
        //noinspection HardCodedStringLiteral
        Log.d(TAG, "onServiceDisconnected: " + name);

        if (mService.isPresent()) {
            Log.d(TAG, "disconnecting from service id=" + mService.get().hashCode()); // NON-NLS
            for (final Ring.Listener l : mListeners) mService.get().removeListener(l);
            mService = Optional.absent();
        } else Log.w(TAG, "disconnecting from nothing…"); // NON-NLS
    }


    ////
    //// shared methods for fragments
    ////

    @SuppressWarnings("ChainOfInstanceofChecks")
    public void onCreateView(final View view) {
        setDefaultTypeface(view);
    }

    public void setDefaultTypeface(final View view) {
        if (view instanceof ViewGroup) {
            final ViewGroup parent = (ViewGroup) view;
            for (int i = 0; i < parent.getChildCount(); i++) onCreateView(parent.getChildAt(i));
        } else if (view instanceof TextView) {
            final TextView text = (TextView) view;

            final Typeface typeface = text.getTypeface();
            text.setTypeface(mGothamBook, typeface != null ? typeface.getStyle() : Typeface.NORMAL);
        }
    }

    public void addListener(final Ring.Listener listener) {
        //noinspection HardCodedStringLiteral
        Log.d(TAG, "addListener: n=" + mListeners.size());

        if (mListeners.isEmpty()) { // our first listener, so bind to the service
            //noinspection HardCodedStringLiteral
            Log.d(TAG, "binding…");
            bindService(new Intent(this, RinglyService.class), this, Context.BIND_AUTO_CREATE);
        }

        if (!mListeners.add(listener)) throw new RuntimeException("duplicate listener");
        if (mService.isPresent()) mService.get().addListener(listener);
    }

    public void removeListener(final Ring.Listener listener) {
        //noinspection HardCodedStringLiteral
        Log.d(TAG, "removeListener: n=" + mListeners.size());

        if (!mListeners.remove(listener)) throw new RuntimeException("unknown listener");
        if (mService.isPresent()) mService.get().removeListener(listener);

        if (mListeners.isEmpty()) { // no listeners, so unbind from service
            //noinspection HardCodedStringLiteral
            Log.d(TAG, "unbinding…");
            unbindService(this);
        }
    }

    public Preferences getPreferences() {
        return mPreferences;
    }

    public Mixpanel getMixpanel() {
        return mMixpanel;
    }
}
