package com.ringly.ringly.ui.screens.preferences;

import android.content.Context;
import android.support.annotation.StringRes;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.TextView;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.ringly.ringly.BuildConfig;
import com.ringly.ringly.Preferences;
import com.ringly.ringly.R;
import com.ringly.ringly.bluetooth.Ring;

import java.util.List;

import static android.content.ContentValues.TAG;

public class RingPreference extends Preference implements Ring.Listener {
    private TextView mVersions;
    private String mVersionText;

    public RingPreference(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RingPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setLayoutResource(R.layout.preference_ring);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        @StringRes int connection = R.string.no_ring_connected;
        if (Preferences.getRingName(getContext()).isPresent()) {
            com.ringly.ringly.ui.Utilities.uppercaseAndKern((TextView) holder.findViewById(R.id.ring_id),
                Preferences.getRingName(getContext()).get());
            connection = R.string.ring_connected;
        }

        final TextView connectionView = (TextView) holder.findViewById(R.id.ring_connection);
        connectionView.setText(connection);

        mVersions = (TextView) holder.findViewById(R.id.versions);
        if(mVersionText != null) {
            mVersions.setText(mVersionText);
        }
    }

    @Override
    public void onUpdate(final Ring ring) {
        //noinspection HardCodedStringLiteral
        Log.d(TAG, "onUpdate");

        final List<String> versions = Lists.newLinkedList();
        versions.add("sw " + BuildConfig.VERSION_NAME);
        if (ring.getFirmwareRevision().isPresent()) {
            String version = "fw " + ring.getFirmwareRevision().get();
            if (ring.getBootloaderRevision().isPresent()) {
                version += " (" + ring.getBootloaderRevision().get() + ")";
            }
            versions.add(version);
        }
        if (ring.getHardwareRevision().isPresent()) {
            versions.add("hw " + ring.getHardwareRevision().get());
        }


        mVersionText = Joiner.on(" – ").join(versions);

        if(mVersions != null) {
            mVersions.setText(Joiner.on(" – ").join(versions));
        }
    }
}
