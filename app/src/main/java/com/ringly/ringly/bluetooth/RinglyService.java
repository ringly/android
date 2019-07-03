package com.ringly.ringly.bluetooth;

import android.Manifest;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.provider.ContactsContract;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.google.common.base.Optional;
import com.google.common.collect.Sets;
import com.google.common.primitives.Bytes;
import com.google.common.util.concurrent.Runnables;
import com.ringly.ringly.BuildConfig;
import com.ringly.ringly.Preferences;
import com.ringly.ringly.config.Bluetooth;
import com.ringly.ringly.config.Color;
import com.ringly.ringly.config.Command;
import com.ringly.ringly.config.Mixpanel;
import com.ringly.ringly.config.NotificationMode;
import com.ringly.ringly.config.NotificationType;
import com.ringly.ringly.config.Vibration;
import com.ringly.ringly.db.Db;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

import static com.ringly.ringly.Utilities.intToByteArrayLittleEndian;

public final class RinglyService extends Service implements GattCallback.Listener, Ring.Listener {

    private static final String TAG = RinglyService.class.getCanonicalName();
    private static final String PACKAGE = RinglyService.class.getPackage().getName();

    private enum Name {
        COMMAND_ACTION,
        NOTIFY_ACTION,
        RECONNECT_ACTION,
        FORCE_RECONNECT_ACTION,
        CLEAR_BOND_ACTION,
        BONDED_CONNECTION_ACTION,
        ACTIVITY_UPDATE,

        TYPE_EXTRA,
        PHONE_NUMBER_EXTRA,
        MODE_EXTRA,
        BUNDLE_EXTRA,
        TARGET_ADDRESS_EXTRA,
        ;

        @Override
        public String toString() {
            //noinspection MagicCharacter
            return PACKAGE + '.' + name();
        }
    }

    private static final int MIN_RECONNECT_DELAY = 10_000; // milliseconds
    private static final int MAX_RECONNECT_DELAY = 10 * 60_000; // milliseconds
    private static final int MAX_BONDED_CONNECTION_ATTEMPTS = 3; // somewhat arbitrary
    private static final int ACTIVITY_TRACKING_CONNECT_DELAY = 10_000; //this eases radio comm on device during connection which is already quite heavy

    public enum Mode {
        NORMAL,
        TEST_NOTIFICATION_COLOR,
        TEST_CONTACT_COLOR,
        TEST_VIBRATION,
    }

    public static void start(final Context context) {
        context.startService(new Intent(context, RinglyService.class));
    }

    public static void doReconnect(final Context context) {
        context.startService(
                new Intent(Name.RECONNECT_ACTION.toString(), null, context, RinglyService.class)
        );
    }

    public static void doForceReconnect(final Context context) {
        context.startService(
            new Intent(Name.FORCE_RECONNECT_ACTION.toString(), null, context, RinglyService.class)
        );
    }

    public static void doClearBond(final Context context) {
        final Preferences preferences = new Preferences(context);
        final Optional<String> targetAddress = preferences.getRingAddress(); // capture the current address since the disconnect flow clears the ring address.

        final Intent clearBondIntent = new Intent(Name.CLEAR_BOND_ACTION.toString(), null, context, RinglyService.class);
        if (targetAddress.isPresent()) {
            clearBondIntent.putExtra(Name.TARGET_ADDRESS_EXTRA.toString(), targetAddress.get());
        }

        context.startService(clearBondIntent);
    }

    public static void doBondedConnection(final Context context) {
        final Intent bondedConnectionIntent = new Intent(Name.BONDED_CONNECTION_ACTION.toString(), null, context, RinglyService.class);
        context.startService(bondedConnectionIntent);
    }

    public static void doForgetRing(final Context context) {
        RinglyService.doClearBond(context);
        final Preferences preferences = new Preferences(context);
        preferences.unsetRing();
        RinglyService.start(context);
    }

    public static void doUpdateActivity(final Context context) {
        final Intent updateActivityIntent = new Intent(Name.ACTIVITY_UPDATE.toString(), null, context, RinglyService.class);
        context.startService(updateActivityIntent);
    }

    public static void doCommand(final Command command, final Context context) {
        context.startService(
                new Intent(Name.COMMAND_ACTION.toString(), null, context, RinglyService.class)
                        .putExtra(Name.TYPE_EXTRA.toString(), command.toString())
        );
    }

    /**
     * This form doesn't cause any notification, but tracks a
     * Notified[Supported=false] event if the ring is connected.
     */
    public static void doNotify(final String string, final Context context) {
        doNotify(Optional.<NotificationType>absent(), Optional.<String>absent(), Optional.of(string), Mode.NORMAL, context);
    }

    public static void doNotify(final NotificationType type, final Context context) {
        doNotify(type, Mode.NORMAL, context);
    }
    public static void doNotify(
            final NotificationType type, final Optional<String> phoneNumber, final Context context
    ) {
        doNotify(type, phoneNumber, Mode.NORMAL, context);
    }
    public static void doNotify(
            final NotificationType type, final Mode mode, final Context context
    ) {
        doNotify(type, Optional.<String>absent(), mode, context);
    }
    public static void doNotify(
            final NotificationType type, final Optional<String> phoneNumber, final Mode mode,
            final Context context
    ) {
        doNotify(Optional.of(type), phoneNumber, Optional.<String>absent(), mode, context);
    }
    private static void doNotify(
            final Optional<NotificationType> type, final Optional<String> phoneNumber,
            final Optional<String> bundleId, final Mode mode, final Context context
    ) {
        final Intent intent
                = new Intent(Name.NOTIFY_ACTION.toString(), null, context, RinglyService.class);

        if (type.isPresent()) intent.putExtra(Name.TYPE_EXTRA.toString(), type.get().toString());
        if (phoneNumber.isPresent()) intent.putExtra(Name.PHONE_NUMBER_EXTRA.toString(), phoneNumber.get());
        if (bundleId.isPresent()) intent.putExtra(Name.BUNDLE_EXTRA.toString(), bundleId.get());
        if (mode != Mode.NORMAL) intent.putExtra(Name.MODE_EXTRA.toString(), mode.toString());

        context.startService(intent);
    }


    public final class LocalBinder extends Binder {
        public RinglyService getService() {
            return RinglyService.this;
        }
    }


    private final IBinder mBinder = new LocalBinder();
    private final Set<Ring.Listener> mListeners = Sets.newHashSet();

    private Optional<Ring> mRing = Optional.absent();

    private GattCallback mGattCallback;
    private Handler mHandler;
    private int mReconnectDelay;
    private boolean mReconnecting = false;
    private Preferences mPreferences;
    private Mixpanel mMixpanel;
    private int mBondedConnectionAttemptCount = 0;
    private CompositeSubscription mSubscriptions;

    ////
    //// public methods
    ////

    public void addListener(final Ring.Listener listener) {
        //noinspection HardCodedStringLiteral
        Log.d(TAG, "addListener: n=" + mListeners.size() + " id=" + hashCode());

        if (!mListeners.add(listener)) {
            throw new RuntimeException("duplicate listener");
        }

        if (mRing.isPresent()) {
            listener.onUpdate(mRing.get());
        }
    }

    public void removeListener(final Ring.Listener listener) {
        //noinspection HardCodedStringLiteral
        Log.d(TAG, "removeListener: n=" + mListeners.size() + " id=" + hashCode());

        if (!mListeners.remove(listener)) {
            throw new RuntimeException("unknown listener");
        }
    }


    ////
    //// Service methods
    ////

    @Override
    public void onCreate() {
        //noinspection HardCodedStringLiteral
        Log.d(TAG, "onCreate id=" + hashCode());

        mGattCallback = new GattCallback(this);
        mHandler = new Handler();
        mPreferences = new Preferences(this);
        mMixpanel = new Mixpanel(this);
        mSubscriptions = new CompositeSubscription();
        mSubscriptions.add(
            Observable.interval(20, TimeUnit.MINUTES, Schedulers.computation())
                .subscribe(__ -> updateActivity())
        );
    }

    @SuppressWarnings("RefusedBequest")
    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {
        //TODO(mad-uuids) implement in onHandleIntent instead of here http://stackoverflow.com/questions/15755785/method-onhandleintent-does-not-get-called
        //noinspection HardCodedStringLiteral
        Log.d(TAG, "onStartCommand: id=" + hashCode() + " intent=" + intent);

        final boolean reconnect = (intent != null && Name.RECONNECT_ACTION.toString().equals(intent.getAction()));
        final Optional<String> targetAddress = Optional.fromNullable(intent == null ? null : intent.getStringExtra(Name.TARGET_ADDRESS_EXTRA.toString()));
        final Optional<String> address = targetAddress.isPresent() ? targetAddress : mPreferences.getRingAddress();

        if (address.isPresent()) {
            connect(address.get(), reconnect);
        }
        else {
            disconnect();
        }
        // TODO log unusual intent/address combos?

        if (mRing.isPresent()) {
            if (intent != null && intent.getAction() != null) {
                if (Name.COMMAND_ACTION.toString().equals(intent.getAction())) doCommand(intent);
                else if (Name.NOTIFY_ACTION.toString().equals(intent.getAction())) doNotify(intent);
                else if (Name.CLEAR_BOND_ACTION.toString().equals(intent.getAction())) { doClearBond(intent); }
                else if (Name.BONDED_CONNECTION_ACTION.toString().equals(intent.getAction())) { completeConnectionSetup(mRing.get()); }
                else if (Name.ACTIVITY_UPDATE.toString().equals(intent.getAction())) {
                    updateActivity();
                } else if (Name.FORCE_RECONNECT_ACTION.toString().equals(intent.getAction())) {
                    closeGattAndStartReconnect(mRing.get().getGatt());
                } else if (!reconnect) {
                    //noinspection HardCodedStringLiteral
                    Log.w(TAG, "unknown action: " + intent.getAction());
                }
            }
        } else {
            Log.d(TAG, "no selected ring; committing suicide id=" + hashCode()); //NON-NLS
            stopSelf(startId); // if there's no chosen ring, we may as well die
        }

        return START_STICKY;
    }

    @Override
    public IBinder onBind(final Intent intent) {
        //noinspection HardCodedStringLiteral
        Log.d(TAG, "onBind: " + intent);

        return mBinder;
    }

    @Override
    public void onDestroy() {
        //noinspection HardCodedStringLiteral
        Log.d(TAG, "onDestroy id=" + hashCode());

        disconnect();
        mMixpanel.flush();
        mSubscriptions.unsubscribe();
    }


    ////
    //// GattCallback.Listener callbacks, registered in onCreate()
    ////

    @Override
    public void onConnect(final BluetoothGatt gatt) {
        //noinspection HardCodedStringLiteral
        Log.d(TAG, "onConnect");

        if (!isSelectedDevice(gatt)) return; // TODO log this?
        final Ring ring = mRing.get();

        stopReconnect();

        ring.setRinglyRubricCharacteristic(
                Utilities.getCharacteristic(gatt, Bluetooth.RINGLY_RUBRIC_CHARACTERISTIC)
        );
        if (!ring.getRinglyRubricCharacteristic().isPresent()) {
            if (Utilities.refreshServiceCache(gatt)) {
                //noinspection HardCodedStringLiteral
                Log.w(TAG, "couldn't get Ringly rubric characteristic, trying again...");
                gatt.discoverServices();
            } else {
                //noinspection HardCodedStringLiteral
                Log.e(TAG, "couldn't get Ringly rubric characteristic, giving up.");
            }
            return;
        }

        // the device will initiate bonding **after** the connection request.
        // if this device is madison (or newer) version,
        // and if this phone is not paired,
        // we should wait to receive the BOND_BONDED state intent (*)
        // (via BondStateReceiver) before attempting to proceed with the connection flow.

        if (Utilities.getCharacteristic(gatt, Bluetooth.RINGLY_CLEAR_BOND_CHARACTERISTIC).isPresent()) {
            // characteristic is present and this device is likely madison. (ie. requires encryption)
            // we must ensure that we are bonded with the device, or wait for the user
            // to tap pair on the dialog after the device initiates the authentication flow.
            Log.d(TAG, "*** found Clear Bond Characteristic...this device likely requires encryption; checking bond status...");

            int bondState = gatt.getDevice().getBondState();
            if (bondState == BluetoothDevice.BOND_NONE) {
                Log.d(TAG, "*** ...device is not bonded...");
                // TODO: the ring is supposed to initiate authentication, but i've seen it stop trying...should we attempt to createBond() here?
            }
            else if (bondState == BluetoothDevice.BOND_BONDING) {
                // it may take some time for bonding to proceed through BOND_BONDING to BOND_BONDED
                // while we wait for the devices to exchange keys.
                Log.d(TAG, "*** ...device is bonding, must wait for device pairing...");
                return;
            }
            else {
                Log.d(TAG, "*** ...device is bonded, continuing with connection setup...");
                mBondedConnectionAttemptCount++;
            }

            // if the connection is not encrypted...the subsequent reads will not succeed.
            // the central device is **supposed to** re-encrypt the link (or pair) if the peripheral
            // returns an Insufficient Authentication error; however, it seems like something is
            // failing silently and re-encryption is not always happening.
            //
            // so, how to re-encrypt the link? do we need to manually clear the bonding and re-bond?
            boolean result = Utilities.deviceIsEncrypted(gatt.getDevice());
            Log.d(TAG, "*** ...device encryption status: " + result);
        }

        // we're all set...let's finish this.
        completeConnectionSetup(ring);
    }

    @Override
    public void onDisconnect(final BluetoothGatt gatt) {
        //noinspection HardCodedStringLiteral
        Log.d(TAG, "onDisconnect");

        closeGattAndStartReconnect(gatt);
    }

    @Override
    public void onRead(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
        if (!isSelectedDevice(gatt)) return;
        final UUID uuid = characteristic.getUuid();
        final Ring ring = mRing.get();

        if (characteristic.getValue() == null) { // TODO why does this happen?
            Log.e(TAG, "characteristic has null value: " + uuid);
            return;
        }

        if (Bluetooth.FIRMWARE_REVISION_STRING_CHARACTERISTIC.getUuid().equals(uuid)) {
            ring.setFirmwareRevision(Optional.of(characteristic.getStringValue(0)));
        }
        else if (Bluetooth.HARDWARE_REVISION_STRING_CHARACTERISTIC.getUuid().equals(uuid)) {
            ring.setHardwareRevision(Optional.of(characteristic.getStringValue(0)));
        }
        else if (Bluetooth.BOOTLOADER_REVISION_STRING_CHARACTERISTIC.getUuid().equals(uuid)) {
            ring.setBootloaderRevision(Optional.of(characteristic.getStringValue(0)));
        }
        else if (Bluetooth.BATTERY_LEVEL_CHARACTERISTIC.getUuid().equals(uuid)) {
            ring.setBatteryLevel(Optional.of(
                    characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0)));
        }
        else if (Bluetooth.CHARGE_STATE_CHARACTERISTIC.getUuid().equals(uuid)) {
            ring.setChargeState(Optional.of(
                    characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0)));
        }
        else if (Bluetooth.ACTIVITY_TRACKING_DATA_CHARACTERISTIC.getUuid().equals(uuid)) {
            byte[] bytes = characteristic.getValue();
            for(int i = 0; i < bytes.length / 5; ++i) {
                processActivityTrackingRecord(Arrays.copyOfRange(bytes, i * 5, (i + 1) * 5));
            }
        }

        // if we read the characteristic, then we've successfully re-authenticated (if necessary)
        mBondedConnectionAttemptCount = 0;
    }

    @Override
    public void onReadRssi(final BluetoothGatt gatt, final int rssi) {
    }

    @Override
    public void onFailure(final BluetoothGatt gatt) {
        Log.w(TAG, "onFailure");
        closeGattAndStartReconnect(gatt);
    }


    ////
    //// Ring.Listener callbacks, registered in connect()
    ////

    @Override
    public void onUpdate(final Ring ring) {
        //noinspection HardCodedStringLiteral
        Log.d(TAG, "onUpdate: n=" + mListeners.size() + " id=" + hashCode());

        if (mRing.isPresent() && mRing.get().equals(ring)) {
            // copy before iterating, in case a callback adds or removes listeners:
            for (final Ring.Listener listener : Sets.newHashSet(mListeners)) listener.onUpdate(ring);
        } else {
            //noinspection HardCodedStringLiteral
            Log.w(TAG, "received update on unselected Ring");
        }
    }
    ////
    //// private methods
    ////

    private boolean isSelectedDevice(final BluetoothGatt gatt) {
        if (!mRing.isPresent()) {
            //noinspection HardCodedStringLiteral
            Log.w(TAG, "passed device " + gatt.getDevice() + " even though no ring is selected!");
            return false;
        }

        if (!mRing.get().getGatt().getDevice().equals(gatt.getDevice())) {
            //noinspection HardCodedStringLiteral
            Log.w(TAG, "passed device " + gatt.getDevice() + " but the selected ring is " + mRing.get().getGatt().getDevice());
            return false;
        }

        return true;
    }

    private void closeGattAndStartReconnect(final BluetoothGatt gatt) {
        // if the current ring has disconnected then try to reconnect,
        // but if it's another (presumably old) ring, then close it
        if (isSelectedDevice(gatt)) {
            mRing.get().setConnected(false);
            gatt.close();
            startReconnect(gatt);
        } else gatt.close(); // TODO should this ever happen?
    }

    private void connect(final String address, final boolean reconnect) {
        if (mRing.isPresent()) {
            if (mRing.get().getGatt().getDevice().getAddress().equals(address) && !reconnect) {
                // already connected! TODO is this unusual?
                Log.w(TAG, "attempted to connect() to an already connected device.");
                return;
            }
            disconnect();
        }

        final BluetoothAdapter adapter = Utilities.getBluetoothAdapter(this);
        final boolean result = adapter.cancelDiscovery();
        if (!result) {
            Log.w(TAG, "*** error calling BluetoothAdapter.cancelDiscovery()");
        }

        Log.d(TAG, "*** about to connectGatt...");

        // using this connection class to work around race condition in connectGatt which seems
        // to ignore the value of the `autoConnect` parameter, performing a direct connection when
        // a background connection is requested. this seems to create unexpected problems.
        final BluetoothGatt gatt = new BleConnectionCompat(getApplicationContext())
                .connectGatt(adapter.getRemoteDevice(address), true, mGattCallback);
        if (gatt == null) {
            //noinspection HardCodedStringLiteral
            Log.w(TAG, "BluetoothDevice.connectGatt() returned null for address " + address);
            return;
        }

        // XXX: could be causing trouble...let's reserve refreshing the cache for when services don't appear as expected.
        //Utilities.refreshServiceCache(gatt);
        mRing = Optional.of(new Ring(gatt, this, mMixpanel));
    }

    private void disconnect() {
        if (mRing.isPresent()) {
            final BluetoothGatt gatt = mRing.get().getGatt();
            gatt.disconnect();
            gatt.close();
            mRing = Optional.absent();
            stopReconnect();
        }
    }

    private void startReconnect(final BluetoothGatt gatt) {
        if (!mReconnecting) {
            mReconnecting = true;
            mReconnectDelay = MIN_RECONNECT_DELAY;
            mHandler.postDelayed(mReconnect, mReconnectDelay);
            clearBondStateIfNeeded(gatt.getDevice());
        }
    }

    private void stopReconnect() {
        mHandler.removeCallbacks(mReconnect);
        mReconnecting = false;
    }

    private final Runnable mReconnect = new Runnable() {
        @Override
        public void run() {
            final Optional<String> address = mPreferences.getRingAddress();
            if (address.isPresent()) {
                //noinspection HardCodedStringLiteral
                Log.d(TAG, "reconnect timer: delay=" + mReconnectDelay);

                connect(address.get(), true);

                mHandler.postDelayed(this, mReconnectDelay);

                // exponential backoff:
                mReconnectDelay = Math.min(mReconnectDelay * 2, MAX_RECONNECT_DELAY);
            }
        }
    };

    private void completeConnectionSetup(final Ring ring) {
        if (ring.isConnected()) {
            Log.d(TAG, "*** ...ring is already marked as connected...");
            if (ring.getFirmwareRevision().isPresent()) {
                Log.d(TAG, "*** ...and it seems that this ring connection has already been set up.");
                return;
            } else {
                Log.d(TAG, "*** ...but it seems that this ring connection did not complete setup...");
                // XXX: hack, if there were previous items in the queue, and they time out...then the
                // entire queue (including the new commands below) would be cleared and the app gets stuck.
                mGattCallback.abortAll();
            }
        }

        Log.d(TAG, "*** ...completing connection setup.");

        if (ring.getGatt().getServices().isEmpty()) {
            Log.w(TAG, "*** gatt services empty...should not be here, must wait for service discovery, or must re-discover services.");
            // TODO: now what? calling discoverServices() here usually fails because service discovery is already pending.
            // disconnecting and restarting the connection process does not often help.
        }

        ring.setConnected(true);

        // need to identify ourselves as Android within 3 seconds of connect:
        sendRubric(Rubric.ANDROID_OS);

        //// initial read, don't notify ////
        setupCharacteristic(Bluetooth.FIRMWARE_REVISION_STRING_CHARACTERISTIC, true, false);
        setupCharacteristic(Bluetooth.HARDWARE_REVISION_STRING_CHARACTERISTIC, true, false);
        ring.setHasBootloaderRevision( // older rings do not have this characteristic
                setupCharacteristic(Bluetooth.BOOTLOADER_REVISION_STRING_CHARACTERISTIC, true, false)
                        .isPresent());

        //// initial read, and receive notifications ////
        setupCharacteristic(Bluetooth.BATTERY_LEVEL_CHARACTERISTIC, true, true);
        setupCharacteristic(Bluetooth.CHARGE_STATE_CHARACTERISTIC, true, true);

        //// set ring settings from preferences ////
        sendPrefs();
        // TODO set profile settings on every connect, to keep things consistent

        //// setup activity tracking notifications ////
        setupCharacteristic(Bluetooth.ACTIVITY_TRACKING_DATA_CHARACTERISTIC, false, true);

        // HACK - fix rings that were improperly named
        checkIfAdvertismentNameNeedsRewrite(ring);
        startActivityTracking();
    }

    private void clearBondStateIfNeeded(final BluetoothDevice device) {
        if (mBondedConnectionAttemptCount >= MAX_BONDED_CONNECTION_ATTEMPTS) {
            Utilities.removeBond(device);
            mBondedConnectionAttemptCount = 0;
        }
    }

    private void doCommand(final Intent intent) {
        sendRubric(Command.valueOf(intent.getStringExtra(Name.TYPE_EXTRA.toString())).rubric);
    }

    private void doNotify(final Intent intent) {
        final Optional<NotificationType> type = com.ringly.ringly.Utilities.valueOfNullable(
                NotificationType.class, intent.getStringExtra(Name.TYPE_EXTRA.toString())
        );

        // unsupported app
        if (!type.isPresent()) {
            final Optional<String> bundleId = Optional.fromNullable(intent.getStringExtra(Name.BUNDLE_EXTRA.toString()));
            if(bundleId.isPresent()) trackNotified(Mixpanel.getUnsupportedNotificationProperties(bundleId.get()));
            return;
        }

        final NotificationType notificationType = type.get();

        // "special" apps (phone and text) do not have bundle ids!
        final String bundle = notificationType.ids.isEmpty()
                ? notificationType.name()
                : notificationType.ids.iterator().next();

        // supported app, but disabled by user
        if(mPreferences.getNotificationMode(type.get()) == NotificationMode.DISABLED) {
            trackNotified(Mixpanel.getDisabledNotificationProperties(bundle));
            return;
        }

        final Optional<String> phoneNumber
                = Optional.fromNullable(intent.getStringExtra(Name.PHONE_NUMBER_EXTRA.toString()));
        final Mode mode = com.ringly.ringly.Utilities.valueOfNullable(
                Mode.class, intent.getStringExtra(Name.MODE_EXTRA.toString())
        ).or(Mode.NORMAL);

        // in test contact color mode, phone number is just the contact id,
        // and we use it as notification color so that it happens immediately:
        final Color color = mode == Mode.TEST_CONTACT_COLOR
                ? mPreferences.getContactColor(phoneNumber.get()).or(Color.NONE)
                : mPreferences.getNotificationColor(type.get());

        final Vibration vibration
                = mode == Mode.TEST_NOTIFICATION_COLOR || mode == Mode.TEST_CONTACT_COLOR
                ? Vibration.NONE : mPreferences.getNotificationVibration(type.get());

        Color contactColor = Color.NONE;
        if (mode != Mode.TEST_CONTACT_COLOR && phoneNumber.isPresent() &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED) {
            boolean enabledContact = false;
            for (final String contact : getContacts(phoneNumber.get())) {
                final Optional<Color> c = mPreferences.getContactColor(contact);
                if (c.isPresent()) {
                    enabledContact = true;
                    contactColor = c.get();
                    if (contactColor != Color.NONE) break; // go with first colored contact
                }
            }

            // if phone number doesn't correspond to an enabled contact
            // and Inner Ring is enabled then we record event and return:
            if (mPreferences.getInnerRing() && !enabledContact) {
                trackNotified(Mixpanel.getUnsentNotificationProperties(bundle));
                return;
            }
        }

        final Runnable mTrackSent = new Runnable() {
            @Override
            public void run() {
                trackNotified(Mixpanel.getSentNotificationProperties(bundle));
            }
        };

        sendRubric(com.ringly.ringly.config.Utilities.getNotificationRubric(
                        color, vibration, contactColor, mode != Mode.NORMAL),
                mode == Mode.NORMAL ? mTrackSent : Runnables.doNothing(),
                Runnables.doNothing());

    }

    // should only be called as part of a disconnection flow.
    private void doClearBond(final Intent intent) {
        Log.d(TAG, "*** about to clear bond information on ring...");
        final Ring ring = mRing.get();
        if (ring == null) {
            Log.d(TAG, "*** asked to clear bond but ring is not connected");
            return;
        }

        // tell the ring to clear the whitelist:
        final Optional<BluetoothGattCharacteristic> c = Utilities.getCharacteristic(ring.getGatt(), Bluetooth.RINGLY_CLEAR_BOND_CHARACTERISTIC);
        if (c.isPresent()) {
            byte[] data = {0x1};
            c.get().setValue(data);
            boolean result = ring.getGatt().writeCharacteristic(c.get());
            Log.d(TAG, String.format("*** ...wrote to clear bond characteristic (result: %b)", result));
        }

        // tell the phone to clear bonding information:
        BluetoothDevice device = ring.getGatt().getDevice();
        if (device.getBondState() != BluetoothDevice.BOND_NONE) {
            boolean result = Utilities.removeBond(device);
            Log.d(TAG, "*** removed bond for device: " + device.getName());
        }
    }

    private void trackNotified(final Map<Mixpanel.Property, ?> properties) {
        // we only track Notified if the ring is connected, to mimic iOS behavior
        if (mRing.isPresent() && mRing.get().isConnected()) {
            mMixpanel.track(Mixpanel.Event.NOTIFIED, properties);
        }
    }

    private void sendRubric(final Rubric rubric) {
        sendRubric(rubric, Runnables.doNothing(), Runnables.doNothing());
    }
    private void sendRubric(
            final Rubric rubric, final Runnable onSuccess, final Runnable onFailure
    ) {
        if (!(mRing.isPresent() && mRing.get().getRinglyRubricCharacteristic().isPresent())) {
            onFailure.run();
            return;
        }

        final Ring ring = mRing.get();
        final BluetoothGattCharacteristic characteristic = ring.getRinglyRubricCharacteristic().get();

        mGattCallback.performReadOrWrite(new Runnable() {
            @Override
            public void run() {
                final byte[] data = rubric.serialize();
                characteristic.setValue(data);
                boolean result = ring.getGatt().writeCharacteristic(characteristic);
                Log.d(TAG,"sent write: " + com.ringly.ringly.Utilities.toHexString(characteristic.getValue()) + " with result: " + result);
                if (result) {
                    if (ring.isConnected()) {
                        onSuccess.run(); // probable success
                        return;
                    }
                    else {
                        //noinspection HardCodedStringLiteral
                        Log.w(TAG, "writeCharacteristic() succeeded but isConnected() is false");
                    }
                } else {
                    //noinspection HardCodedStringLiteral
                    Log.w(TAG, "writeCharacteristic() failed");
                }
                onFailure.run(); // probable failure
            }
        });
    }

    private Optional<BluetoothGattCharacteristic> setupCharacteristic(
            final BluetoothGattCharacteristic characteristic, final boolean read, final boolean notify
    ) {
        final Ring ring = mRing.get();
        final Optional<BluetoothGattCharacteristic> c
                = Utilities.getCharacteristic(ring.getGatt(), characteristic);
        if (c.isPresent()) {
            if (read) {
                mGattCallback.performReadOrWrite(new Runnable() {
                    @Override
                    public void run() {
                        ring.getGatt().readCharacteristic(c.get());
                    }
                });
            }
            if (notify) {
                mGattCallback.performReadOrWrite(new Runnable() {
                    @Override
                    public void run() {
                        if (!Utilities.enableNotifications(ring.getGatt(), c.get())) {
                            //noinspection HardCodedStringLiteral
                            Log.w(TAG, "couldn't enable notifications for characteristic " + c.get().getUuid());
                        }
                    }
                });
            }
        }
        return c;
    }

    private void startActivityTracking() {
        if (!mRing.isPresent()) {
            Log.e(TAG, "startActivityTracking: No ring");
            return;
        }

        // grab the product type - default to bracelet
        final RingName.ProductType productType;
        Optional<String> nameOptional = mPreferences.getRingName();
        if (nameOptional.isPresent()) {
            Optional<RingName> parsedNameOptional = RingName.fromString(nameOptional.get());
            if (parsedNameOptional.isPresent()) {
                RingName parsedName = parsedNameOptional.get();
                productType = parsedName.productType;
            } else {
                Log.e(TAG, "startActivityTracking: Invalid name format: " +
                        nameOptional.get());
                productType = RingName.ProductType.BRACELET;
            }
        } else {
            Log.e(TAG, "startActivityTracking: Missing ring name from preferences!");
            productType = RingName.ProductType.BRACELET;
        }

        final Optional<BluetoothGattCharacteristic> c =
            Utilities.getCharacteristic(mRing.get().getGatt(),
                Bluetooth.ACTIVITY_TRACKING_CONTROL_CHARACTERISTIC);

        if (!c.isPresent()) {
            Log.e(TAG, "startActivityTracking: No activity tracking");
            Preferences.setRingSupportsActivity(this, false);
            return;
        }

        Preferences.setRingSupportsActivity(this, true);

        long now = System.currentTimeMillis();
        sendRubric(
            new Rubric.Builder().type(Rubric.Type.DATE_TIME)
                .currentTime(now)
                .build()
        );

        mGattCallback.performReadOrWrite(() -> {
            BluetoothGattCharacteristic cp = c.get();

            // Enable activity tracking control point
            // Command 1 - (re) Start/stop activity logging w/ parameters
            // TODO - these bluetooth service data payload should be abstracted

            // 6-7: Minimum peak intensity (uint16, little endian) - sets the minimum peak power of the FFT
            //      necessary to trigger step counting in the time window. Default is 850
            byte[] mpiBytes = intToByteArrayLittleEndian(productType.minimumPeakIntensity, 2);

            // 8-9: Minimum peak height (uint16, little endian) - sets the minimum height necessary for a
            //      peak to be counted as a step, if the time window has already been identified as containing steps. Default is 375
            byte[] mphBytes = intToByteArrayLittleEndian(productType.minimumPeakHeight, 2);

            byte[] data = {
                    1, // 0-1: command bytes
                    0,
                    7, // 2: length (3 if only accel, 7 if also setting step tracking parameters)
                    4, // 3: 0 = off, 4 = on
                    1, // 4: accelerometer sensitivity - default 1
                    1, // 5: mode (0 = low-power 8-bit, 1 = normal 10-bit, 2 = high-resolution 12-bit) - default 1
                    mpiBytes[0],
                    mpiBytes[1],
                    mphBytes[0],
                    mphBytes[1],
            };

            cp.setValue(data);

            boolean result = mRing.get().getGatt().writeCharacteristic(cp);
            Log.d(TAG, String.format("startActivityTracking: Wrote to activity tracking result %b",
                result));

            mHandler.postDelayed(this::updateActivity, ACTIVITY_TRACKING_CONNECT_DELAY);
        });
    }

    private void updateActivity() {
        if(!mRing.isPresent()) {
            Log.e(TAG, "updateActivity: No ring");
            return;
        }

        final Optional<BluetoothGattCharacteristic> c =
            Utilities.getCharacteristic(mRing.get().getGatt(),
                Bluetooth.ACTIVITY_TRACKING_CONTROL_CHARACTERISTIC);

        if(!c.isPresent()) {
            Log.e(TAG, "updateActivity: No activity tracking");
            return;
        }

        Db.getInstance(this)
            .getLatestDeviceEvent(mRing.get().getGatt().getDevice().getAddress())
            .map(ose -> ose.transform(se ->
                se.date.getTime() - TimeUnit.MINUTES.toMillis(1)).or(Utilities.START_DATE))
            .first()
            .subscribe(
                lastUpdate -> {
                    if (mRing.isPresent()) { //check if the ring has not disconnected
                        mGattCallback.performReadOrWrite(() -> {
                            BluetoothGattCharacteristic cp = c.get();

                            // Activity Tracking Control Point - send information from lastUpdate
                            byte[] data = Bytes.concat(new byte[]{0, 0, 3},
                                    Utilities.msToMinuteBytes(lastUpdate));

                            cp.setValue(data);

                            boolean result = mRing.get().getGatt().writeCharacteristic(cp);
                            Log.d(TAG, String.format("updateActivity: Wrote to activity tracking result %b",
                                    result));
                        });
                    }
                }
            );
    }

    private void processActivityTrackingRecord(byte[] bytes) {
        if(bytes.length != 5) {
            throw new IllegalArgumentException("An activity tracking record must have 5 bytes");
        }

        if(BuildConfig.LOG_DB) {
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format(" %02x", b));
            }
            Log.d(TAG, "processActivityTrackingRecord: " + sb.toString());
        }

        Db.getInstance(this).recordBytes(bytes, mRing.get().getGatt().getDevice().getAddress());
    }

    private static final String[] CONTACT_COLUMNS = {ContactsContract.Contacts.LOOKUP_KEY};
    @SuppressWarnings("TypeMayBeWeakened")
    private Set<String> getContacts(final String phoneNumber) {
        final Set<String> contacts = Sets.newHashSet();

        // It appears the value passed in can sometimes be an empty string or null and
        // this leads to an IllegalArgumentException from a readExceptionFromParcel in
        // the `getContentResolver().query(...)` call?
        //
        // https://stackoverflow.com/a/6350751/376489
        // https://github.com/grandcentrix/tray/pull/84/files
        //
        // TODO - resolve this in a more Android way...
        //
        if (phoneNumber != null && !"".equals(phoneNumber)) {
            final Uri findContact = ContactsContract.PhoneLookup.CONTENT_FILTER_URI.buildUpon()
                    .appendPath(phoneNumber).build();

            final Cursor cursor = getContentResolver().query(findContact, CONTACT_COLUMNS, null, null, null);
            if (cursor != null) {
                try {
                    while (cursor.moveToNext()) contacts.add(cursor.getString(0));
                } finally {
                    cursor.close();
                }
            }
        }
        return contacts;
    }

    private void sendPrefs() {
        doCommand(mPreferences.getConnectionLight()
            ? Command.ENABLE_CONNECTION_LIGHT : Command.DISABLE_CONNECTION_LIGHT, this);
        doCommand(mPreferences.getDisconnectionBuzz()
            ? Command.ENABLE_DISCONNECTION_BUZZ : Command.DISABLE_DISCONNECTION_BUZZ, this);
        doCommand(mPreferences.getSleepMode()
            ? Command.ENABLE_SLEEP_MODE : Command.DISABLE_SLEEP_MODE, this);
    }

    /**
     * Fix
     * DATE rings that were improperly named ROSE
     * GO01 rings that were improperly named LOVE
     * GO02 rings that were improperly named LOVE
     * @param ring
     */
    private void checkIfAdvertismentNameNeedsRewrite(Ring ring) {

        Optional<String> deviceName = mPreferences.getRingName();
        Optional<String> deviceAddress = mPreferences.getRingAddress();
        if (!deviceName.isPresent() || !deviceAddress.isPresent()) {
            //This should never happened, but in some rare condition, the device could disconnect and be null here
            //Check to avoid NPE
            Crashlytics.logException(new Error("RingName Is null"));
            return;
        }
        if (DevicesNameRewriteUtils.needsRoseToDateRewrite(deviceName.get(), deviceAddress.get())) {
            rewriteAdvertisementName(ring, "DATE");
        } else if (DevicesNameRewriteUtils.needsLoveToGO02Rewrite(deviceName.get(), deviceAddress.get())) {
            rewriteAdvertisementName(ring, "GO02");
        } else if (DevicesNameRewriteUtils.needsLoveToGO01Rewrite(deviceName.get(), deviceAddress.get())) {
            rewriteAdvertisementName(ring, "GO01");
        }
    }

    private void rewriteAdvertisementName(Ring ring, String newName) {
        Runnable updatePreferences = new Runnable() {
            @Override
            public void run() {
                String address = ring.getGatt().getDevice().getAddress();
                String name = ring.getGatt().getDevice().getName();
                if (address!= null && name != null) {
                    Optional<String> modifiedName = RingName.replaceName(name,newName);
                    if (modifiedName.isPresent()) {
                        mPreferences.setRing(address, modifiedName.get());
                    }
                }

            }
        };
        sendRubric(new Rubric.Builder().type(Rubric.Type.ADVERTISING_NAME)
                .advertisingName(newName)
                .diamondClub(false)
                .build(), updatePreferences, Runnables.doNothing());
    }
}
