package com.ringly.ringly;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.common.base.Optional;
import com.ringly.ringly.bluetooth.RinglyService;

public final class BondStateReceiver extends BroadcastReceiver {
    private static final String TAG = BondStateReceiver.class.getCanonicalName();


    @Override
    public void onReceive(final Context context, final Intent intent) {
        Log.d(TAG, "onReceive: " + intent);
        String action = intent.getAction();

        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        Optional<String> address = new Preferences(context).getRingAddress();

        // only interact with "our" ring device:
        if (device != null && address.isPresent() && device.getAddress().equals(address.get())) {
            // monitor bond state to catch if/when the user revoked bonding/pairing:
            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                final int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                final int prevBondState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.ERROR);

                Log.d(TAG, String.format("*** bond state: %d, prev state: %d", bondState, prevBondState));

                // TODO: should consider comparing the previous bond state to detect **edges** in the state changes.
                // ie. do we only care when our ring transitioned from bonded -> none, or none -> bonding?
                // is bonding -> none an error state? (ie. timeout or user intervention?)

                if (bondState == BluetoothDevice.BOND_NONE) {
                    // our ring has lost its bond,
                    // ...because the user "forgot" it in Bluetooth settings,
                    // ...or because they clicked Disconnect in Preferences,
                    //    (though we remove the ring address from Preferences first and short-circuit above)
                    // ...or because *something* else failed with authentication and some Android versions
                    // and/or handsets inexplicably revoke bonding without user action.
                    // https://github.com/ringly/android/wiki/Known-Issues#re-connecting--re-encryption

                    // to work around issues where bond status is revoked unexpectedly,
                    // we ignore this and ask the devices (programmatically) reconnect / repair
                    // TODO: should we post a notification to the user reminding them to disconnect from the app preferences?
                    // if so, how to disambiguate between explicit intent and unexpected un-bonding?

                    Log.d(TAG, "*** our ring bond was revoked. reconnecting (and re-bonding) immediately...");
                    RinglyService.doReconnect(context);
                }
                else if (bondState == BluetoothDevice.BOND_BONDING) {
                    Log.d(TAG, "*** ring is bonding...");
                }
                else if (bondState == BluetoothDevice.BOND_BONDED) {
                    Log.d(TAG, "*** ring is bonded...we can continue with the connection flow...");
                    RinglyService.doBondedConnection(context);
                }
            }
        }
    }
}
