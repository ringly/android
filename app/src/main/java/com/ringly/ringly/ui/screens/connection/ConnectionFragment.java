package com.ringly.ringly.ui.screens.connection;


import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.common.base.Optional;
import com.ringly.ringly.R;
import com.ringly.ringly.bluetooth.Ring;
import com.ringly.ringly.bluetooth.RingName;
import com.ringly.ringly.bluetooth.RinglyService;
import com.ringly.ringly.config.Command;
import com.ringly.ringly.config.RingType;
import com.ringly.ringly.ui.MainActivity;
import com.ringly.ringly.ui.Utilities;

import static android.view.View.GONE;


public class ConnectionFragment extends Fragment implements Ring.Listener {

    private static final String TAG = ConnectionFragment.class.getCanonicalName();

    private static final String RING_NAME_FORMAT = "“%s”";
    private static final int LOW_BATTERY_PERCENTAGE = 25;
    private static final int FULL_BATTERY_PERCENTAGE = 75;


    private MainActivity mActivity;


    ////
    //// Fragment methods
    ////

    @Override
    public void onAttach(final Activity activity) {
        //noinspection HardCodedStringLiteral
        Log.d(TAG, "onAttach");
        super.onAttach(activity);

        mActivity = (MainActivity) activity;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        //noinspection HardCodedStringLiteral
        Log.d(TAG, "onCreate: " + savedInstanceState);
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(
            final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState
    ) {
        //noinspection HardCodedStringLiteral
        Log.d(TAG, "onCreateView: " + savedInstanceState);

        final View view = inflater.inflate(R.layout.fragment_connection, container, false);
        mActivity.onCreateView(view);
        Utilities.uppercaseAndKern((TextView) view.findViewById(R.id.ringly_main));

        view.findViewById(R.id.ring_photo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                v.startAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.shake));
                View ring = view.findViewById(R.id.ring_glow);
                View bracelet = view.findViewById(R.id.bracelet_glow);
                if(ring.getVisibility() != GONE) {
                    ring.startAnimation(
                        AnimationUtils.loadAnimation(getActivity(), R.anim.glow));
                } else if(bracelet.getVisibility() != GONE) {
                    view.findViewById(R.id.bracelet_glow).startAnimation(
                        AnimationUtils.loadAnimation(getActivity(), R.anim.glow));
                } else {
                    view.findViewById(R.id.ringly_go_glow).startAnimation(
                            AnimationUtils.loadAnimation(getActivity(), R.anim.glow));
                }
                RinglyService.doCommand(Command.BUZZ, getActivity());
            }
        });

        return view;
    }

    @Override
    public void onStart() {
        //noinspection HardCodedStringLiteral
        Log.d(TAG, "onStart");
        super.onStart();

        mActivity.addListener(this);
    }

    @Override
    public void onStop() {
        //noinspection HardCodedStringLiteral
        Log.d(TAG, "onStop");
        super.onStop();

        mActivity.removeListener(this);
    }


    ////
    //// Ring.Listener callback, registered in onStart()
    ////

    @Override
    public void onUpdate(final Ring ring) {
        //noinspection HardCodedStringLiteral
        Log.d(TAG, "onUpdate");

        final View view = getView();
        if (view == null) {
            //noinspection HardCodedStringLiteral
            Log.w(TAG, "null view!");
            return;
        }

        final Optional<String> rawName = mActivity.getPreferences().getRingName();
        if (rawName.isPresent()) {
            final Optional<RingName> name = RingName.fromString(rawName.get());
            if (name.isPresent()) {
                final String shortName = name.get().type;
                Optional<RingType> type;
                try {
                    type = Optional.of(RingType.valueOf(
                            Character.isDigit(shortName.charAt(0)) ? "_" + shortName : shortName));
                } catch (final IllegalArgumentException ignored) {
                    type = Optional.of(RingType.DAYD);
                    //if its a unknown model set DAYD as model
                    //noinspection HardCodedStringLiteral
                    Log.w(TAG, "unknown ring type: " + name.get().type);
                }

                if (type.isPresent()) {
                    final ImageView ringPhoto = (ImageView) view.findViewById(R.id.ring_photo);
                    ringPhoto.setImageResource(type.get().photoId);
                    ringPhoto.setVisibility(View.VISIBLE);

                    if(type.get().deviceType == RingType.DeviceType.RING) {
                        view.findViewById(R.id.bracelet_glow).setVisibility(GONE);
                        view.findViewById(R.id.ringly_go_glow).setVisibility(GONE);
                    } else if (type.get().deviceType == RingType.DeviceType.BRACELET) {
                        view.findViewById(R.id.ring_glow).setVisibility(GONE);
                        view.findViewById(R.id.ringly_go_glow).setVisibility(GONE);
                    } else if (type.get().deviceType == RingType.DeviceType.RINGLY_GO) {
                        view.findViewById(R.id.ring_glow).setVisibility(GONE);
                        view.findViewById(R.id.bracelet_glow).setVisibility(GONE);
                    }

                    Utilities.uppercaseAndKern((TextView) view.findViewById(R.id.ring_type),
                            String.format(RING_NAME_FORMAT, getResources().getString(type.get().nameId))
                    );

                }
            } else {
                //noinspection HardCodedStringLiteral
                Log.w(TAG, "couldn't parse ring name: " + rawName);
            }
        } else {
            //noinspection HardCodedStringLiteral
            Log.w(TAG, "null ring name");
        }

        Utilities.uppercaseAndKern((TextView) view.findViewById(R.id.connection),
                // XXX: hack to defer displaying "Connected" until after initial characteristic reads are complete.
                // should probably improve when the Ring.isConnected property is set to `true`
                (ring.isConnected() && ring.getBatteryLevel().isPresent()) ? R.string.connected : R.string.not_connected
        );

        final View battery = view.findViewById(R.id.footer);
        if (ring.getBatteryLevel().isPresent()) {
            final ImageView batteryLevel = (ImageView) view.findViewById(R.id.battery_level);
            final int percent = ring.getBatteryLevel().get();
            @DrawableRes final int iconId;
            if (percent < LOW_BATTERY_PERCENTAGE) iconId = R.drawable.battery_low;
            else if (percent < FULL_BATTERY_PERCENTAGE) iconId = R.drawable.battery_half;
            else iconId = R.drawable.battery_full;
            batteryLevel.setImageResource(iconId);

            @StringRes int id = R.string.not_charging;
            if (ring.getChargeState().isPresent()) {
                final int chargeState = ring.getChargeState().get();
                if (chargeState == 1) id = R.string.charging;
                else if (chargeState == 2) id = R.string.charged;
                else if (chargeState != 0) {
                    //noinspection HardCodedStringLiteral
                    Log.w(TAG, "unknown charge state: " + chargeState);
                }
            }
            Utilities.uppercaseAndKern((TextView) view.findViewById(R.id.charge_state), id);

            battery.setVisibility(View.VISIBLE);
        }  else battery.setVisibility(View.INVISIBLE);
    }
}
