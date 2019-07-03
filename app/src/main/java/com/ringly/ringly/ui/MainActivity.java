package com.ringly.ringly.ui;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;
import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.ringly.ringly.NotificationListener;
import com.ringly.ringly.Preferences;
import com.ringly.ringly.R;
import com.ringly.ringly.RinglyApp;
import com.ringly.ringly.bluetooth.HardwareFamily;
import com.ringly.ringly.bluetooth.Ring;
import com.ringly.ringly.bluetooth.RinglyService;
import com.ringly.ringly.config.Color;
import com.ringly.ringly.config.Command;
import com.ringly.ringly.config.GuidedMeditationsCache;
import com.ringly.ringly.config.Mixpanel;
import com.ringly.ringly.config.NotificationMode;
import com.ringly.ringly.config.NotificationType;
import com.ringly.ringly.config.Screen;
import com.ringly.ringly.config.Vibration;
import com.ringly.ringly.db.Db;
import com.ringly.ringly.ui.dfu.DfuActivity;
import com.ringly.ringly.ui.dfu.Firmwares;
import com.ringly.ringly.ui.login.LoginActivity;
import com.ringly.ringly.ui.onboarding.OnboardingActivity;
import com.ringly.ringly.ui.screens.connection.RingsFragment;
import com.ringly.ringly.ui.screens.mindfulness.MindfulnessFragment;
import com.ringly.ringly.ui.screens.preferences.PreferencesActivity;
import com.ringly.ringly.ui.screens.preferences.PreferencesFragment;

import org.json.JSONException;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

import io.fabric.sdk.android.Fabric;

import static android.R.color.transparent;

public final class MainActivity extends BaseActivity
    implements Ring.Listener, NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = MainActivity.class.getCanonicalName();

    public static final String MIXPANEL_SOURCE = "source";

    private static final int ENABLE_BLUETOOTH_REQUEST_CODE = 1;
    public static final int DFU_REQUEST_CODE = 2;
    private static final int ONBOARDING_REQUEST_CODE = 3;
    public static final int SHOW_PREFERENCES_REQUEST_CODE = 4;

    private static final String RINGS_BACKSTACK = "ringsBackstack";
    public static final String FW_VERSION_BUNDLE = "fwVersion";

    private static final long FIND_UPDATES_PERIOD
            = 6 * com.ringly.ringly.Utilities.NANOSECONDS_PER_HOUR;


    private Optional<Long> mLastFindUpdatesTime = Optional.absent();
    private Firmwares mFirmwares;
    private Optional<HardwareFamily> mHardwareFamily;
    private Optional<String> mFWApplicationVersion;
    private Optional<String> mFWBootloaderVersion;

    protected Db mDb;

    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private NavigationView mNavView;
    private Screen mCurrentScreen;
    private Toolbar mToolbar;

    // Hold reference to listener because SharedPreferences doesn't
    private SharedPreferences.OnSharedPreferenceChangeListener mPreferenceListener;

    private boolean mActivityVisible = false;
    private boolean mShowSelectRings = false;
    private boolean mShowConnected = false;
    private Set<RingsFragment.Ring> mRings = null;

    ////
    //// Activity methods
    ////

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        //noinspection HardCodedStringLiteral
        Log.d(TAG, "onCreate: " + savedInstanceState);
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());
        mDb = Db.getInstance(this);
        // initialize super properties in case this is our first run:
        mMixpanel.updateNotificationSuperProperties();
        mMixpanel.updateContactSuperProperties();
        mMixpanel.updateSettingSuperProperties();

        mPreferences.restoreRing(); // if we temporarily disabled ring at some point, restore it.
        // TODO can this onCreate happen during DFU, when ring is intentionally disabled?

        // Go through setup flow if necessary
        if(!SetupHelper.isLoggedIn(this)) {
            startLogin();
            return;
        }

        // Everything's set up, show the app

        setContentView(R.layout.activity_main);

        //Download and cache GuidedMeditationsList
        GuidedMeditationsCache.getInstance();

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerToggle =
            new ActionBarDrawerToggle(this, mDrawerLayout, mToolbar, R.string.connection,
                R.string.preferences);

        mDrawerToggle.syncState();

        mNavView = (NavigationView) findViewById(R.id.navigation_view);
        mNavView.setCheckedItem(R.id.nav_item_connection);
        mNavView.setNavigationItemSelectedListener(this);

        ((TextView) mNavView.getHeaderView(0).findViewById(R.id.text_user_email))
            .setText(Preferences.getUserEmail(this).or(""));

        // we don't use onCreateView since this is the one piece of large text
        // in the entire app that isn't kerned
        final TextView updateHeader = (TextView) findViewById(R.id.update_header);
        updateHeader.setTypeface(mGothamBook);

        mFirmwares = new Firmwares();
        mHardwareFamily = Optional.absent();
        mFWApplicationVersion = Optional.absent();
        mFWBootloaderVersion = Optional.absent();

        //Fix if the user has setted on Email Notifications, split in AndriodEmail, Gmail and Ingbox
        splitEmailNotification();

        changeScreen(Screen.CONNECT, getIntent().getIntExtra(MIXPANEL_SOURCE, R.string.launch));


        mPreferenceListener = (__, pref) ->  {
                if(Preferences.isRingSetting(this, pref)) {
                    sendPrefs();
                }
            };

        Preferences.getPreferences(this)
            .registerOnSharedPreferenceChangeListener(mPreferenceListener);

        redirectUniversalLink(getIntent());
    }

    /**
     * When the app has been open from a universal link in intent.getData is the url
     * This method map the URL with the right Screen
     * This are the valid urls:
     * https://explore.ringly.com/app/connection
     * https://explore.ringly.com/app/notifications
     * https://explore.ringly.com/app/contacts
     * https://explore.ringly.com/app/activity
     * https://explore.ringly.com/app/preferences
     * https://explore.ringly.com/app/mindfulness
     * 
     * @param intent
     */
    private void redirectUniversalLink(Intent intent) {
        if (intent != null && intent.getData() != null) {
            Uri data = intent.getData();
            if (data.getPath().contains("/connection")) {
                changeScreen(Screen.CONNECT, R.string.tabs);
            } else if (data.getPath().contains("/notifications")) {
                changeScreen(Screen.NOTIFICATIONS, R.string.tabs);
            } else if (data.getPath().contains("/contacts")) {
                changeScreen(Screen.CONTACTS, R.string.tabs);
            } else if (data.getPath().contains("/activity")) {
                changeScreen(Screen.ACTIVITY_PLACEHOLDER, R.string.tabs);
            } else if (data.getPath().contains("/preferences")) {
                changeScreen(Screen.PREFERENCES, R.string.tabs);
            } else if (data.getPath().contains("/mindfulness")) {
                changeScreen(Screen.MINDFULNESS, R.string.tabs);
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        redirectUniversalLink(intent);
    }

    @Override
    protected void onStart() {
        //noinspection HardCodedStringLiteral
        Log.d(TAG, "onStart");
        super.onStart();

        addListener(this);

        mMixpanel.track(Mixpanel.Event.APPLICATION_LAUNCH);
    }

    @Override
    protected void onResume() {
        //noinspection HardCodedStringLiteral
        Log.d(TAG, "onResume");
        super.onResume();
        mActivityVisible = true;
        if (!com.ringly.ringly.bluetooth.Utilities.getBluetoothAdapter(this).isEnabled()) {
            startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE),
                    ENABLE_BLUETOOTH_REQUEST_CODE);
        }

        if (mPreferences.getRingAddress().isPresent()) {
            RinglyService.start(this);
        }

        if (mCurrentScreen != null) {
            // make sure toolbar checks the proper tab when we back button
            mNavView.setCheckedItem(mCurrentScreen.navId);
        }

        if (mShowSelectRings && mRings!= null) {
            mShowSelectRings = false;
            showSelectRings(mRings);
        }

        if (mShowConnected) {
            mShowConnected = false;
            showConnected();
        }
        mMixpanel.track(Mixpanel.Event.APPLICATION_FOREGROUND);
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        //noinspection HardCodedStringLiteral
        Log.d(TAG, "onActivityResult: " + requestCode);

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == ENABLE_BLUETOOTH_REQUEST_CODE) {
            if (resultCode != RESULT_OK) finish();
        } else if (requestCode == DFU_REQUEST_CODE) {
            if (resultCode != RESULT_OK) mLastFindUpdatesTime = Optional.absent(); // check again asap
        } else if (requestCode == ONBOARDING_REQUEST_CODE) {
            if (resultCode == RESULT_OK) changeScreen(Screen.NOTIFICATIONS, R.string.onboarding);
        } else if (requestCode == SHOW_PREFERENCES_REQUEST_CODE) {
            if (data != null) {
                if (data.hasExtra("refreshActivity") && (mCurrentScreen == Screen.ACTIVITY || mCurrentScreen == Screen.ACTIVITY_PLACEHOLDER)) {
                    changeScreen(mCurrentScreen, R.string.tabs);
                } else if (data.hasExtra(PreferencesFragment.PARAMETER_DISCONNECT) || data.hasExtra(PreferencesFragment.PARAMETER_SIGN_OUT)) {
                    changeScreen(Screen.CONNECT, R.string.tabs);
                }
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mActivityVisible = false;
        mMixpanel.track(Mixpanel.Event.APPLICATION_BACKGROUND);
    }

    @Override
    protected void onStop() {
        //noinspection HardCodedStringLiteral
        Log.d(TAG, "onStop");
        super.onStop();

        removeListener(this);
    }


    ////
    //// Ring.Listener callback
    ////

    @Override
    public void onUpdate(final Ring ring) {
        //noinspection HardCodedStringLiteral
        Log.d(TAG, "onUpdate");

        final Optional<String> hardwareRevision = ring.getHardwareRevision();

        mHardwareFamily = hardwareRevision.isPresent() ?
                HardwareFamily.lookupHardware(hardwareRevision.get()) :
                Optional.<HardwareFamily>absent();

        if (!mHardwareFamily.isPresent()) {
            Log.d(TAG, "Unable to find hardware family for hardware version: '" + hardwareRevision.orNull() + "'");
        }

        mFWApplicationVersion = ring.getFirmwareRevision();
        mFWBootloaderVersion = ring.getBootloaderRevision();

        if (hardwareRevision.isPresent() && ring.getFirmwareRevision().isPresent() &&
                (!ring.hasBootloaderRevision() || ring.getBootloaderRevision().isPresent()) &&
                mHardwareFamily.isPresent() &&
                (!mLastFindUpdatesTime.isPresent() ||
                        System.nanoTime() - mLastFindUpdatesTime.get() > FIND_UPDATES_PERIOD)) {
            // TODO check for internet connection as well
            mLastFindUpdatesTime = Optional.of(System.nanoTime());
            findUpdates(
                    ring.getHardwareRevision().get(),
                    ring.getBootloaderRevision(),
                    ring.getFirmwareRevision().get(),
                    mHardwareFamily.get()
            );

            // Also update the registry when we ping for firmware
            registerDevice(ring);
        }

        // HACK(peter) - trying to make activity placeholder update when ring state changes
        if (mCurrentScreen == Screen.ACTIVITY_PLACEHOLDER) {
            changeScreen(Screen.ACTIVITY_PLACEHOLDER, R.string.tabs);
        }
    }

    ////
    //// NavigationView callback
    ////

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        mDrawerLayout.closeDrawers();

        switch (item.getItemId()) {
            case R.id.nav_item_connection:
                changeScreen(Screen.CONNECT, R.string.tabs);
                return true;
            case R.id.nav_item_notifications:
                changeScreen(Screen.NOTIFICATIONS, R.string.tabs);
                return true;
            case R.id.nav_item_contacts:
                changeScreen(Screen.CONTACTS, R.string.tabs);
                return true;
            case R.id.nav_item_activity:
                changeScreen(Screen.ACTIVITY_PLACEHOLDER, R.string.tabs);
                return true;
            case R.id.nav_item_mindfulness:
                changeScreen(Screen.MINDFULNESS, R.string.tabs);
                return true;
            case R.id.nav_item_preferences:
                changeScreen(Screen.PREFERENCES, R.string.tabs);
                return true;
        }

        return false;
    }

    public void onSelectRing(final RingsFragment.Ring ring) {
        // TODO(mad-uuids) - verify RingsFragment.Ring is passing through hw family
        BluetoothDevice btDevice = ring.bluetoothDevice;
        // TODO(mad-uuids) - verify it's OK to use new if-statement to identify recovery mode?
        // if (btDevice.getName().startsWith(Bluetooth.DFU_PREFIX)) {
        if (ring.recoveryHardwareFamily.isPresent()) {
            // recovery mode, probably
            finish();
            DfuActivity.startRecovery(this, ring.recoveryHardwareFamily.get());
        } else {
            mPreferences.setRing(btDevice.getAddress(), btDevice.getName());
            if (mPreferences.getShowOnboarding() && NotificationListener.isEnabled(this)) {
                startOnboarding();
            }

            startService(new Intent(this, RinglyService.class));
            if (mActivityVisible) {
                showConnected();
            } else {
                mShowConnected =  true;
            }
        }
    }

    private void showConnected() {
        getSupportFragmentManager().popBackStackImmediate(RINGS_BACKSTACK,
                FragmentManager.POP_BACK_STACK_INCLUSIVE);
        changeScreen(Screen.CONNECT, R.string.post_connect);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        //No call for super(). Bug on API Level > 11.
    }

    private void fillActionbar(@StringRes int titleRes) {
        ActionBar toolbar = getSupportActionBar();
        toolbar.setTitle(titleRes);
        if("".equals(getString(titleRes))) {
            toolbar.setBackgroundDrawable(
                new ColorDrawable(ContextCompat.getColor(this, transparent))
            );
        }
        else {
            TypedValue val = new TypedValue();
            getTheme().resolveAttribute(R.attr.colorPrimary, val, true);

            toolbar.setBackgroundDrawable(
                new ColorDrawable(val.data)
            );
        }
    }

    /**
     * Used to determine whether to show onboarding
     * @return true if the user has modified the notification settings,
     * false otherwise
     */
    public boolean prefUnchanged() {
        for (final NotificationType type : NotificationType.values()) {
            if (type.ids.isEmpty() || type == NotificationType.EMAIL || type == NotificationType.CALENDAR) {
                if (getPreferences().getNotificationVibration(type) != type.defaultVibration ||
                        getPreferences().getNotificationColor(type) != type.defaultColor) {
                    return false;
                }
            } else for (final String id : type.ids) {
                if (Utilities.isAppInstalled(id, this)) {
                    if (getPreferences().getNotificationVibration(type) != Vibration.NONE ||
                            getPreferences().getNotificationColor(type) != Color.NONE) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    ////
    //// New user flow
    ////

    public void startLogin() {
        ActivityCompat.startActivity(this, new Intent(this, LoginActivity.class), null);
        finish();
    }

    public void startOnboarding() {
        mPreferences.setShowInstructions(true);
        Intent intent = new Intent(this, OnboardingActivity.class);
        intent.putExtra("ringName", mPreferences.getRingName()
            .or(getString(R.string.no_ring_connected)));
        ActivityCompat.startActivityForResult(this, intent, ONBOARDING_REQUEST_CODE, null);
    }

    ////
    //// Setup ring
    ////

    public void onRingsFound(final Set<RingsFragment.Ring> rings) {
        // TODO empty state
        if (rings.size() == 1) {
            onSelectRing(rings.iterator().next());
        }
        else {
            Log.d(TAG, "onRingsFound: " + getSupportFragmentManager().getBackStackEntryCount());
            if (mActivityVisible) {
                showSelectRings(rings);
            } else {
                this.mRings = rings;
                mShowSelectRings = true;
            }
        }
    }

    private void showSelectRings(final Set<RingsFragment.Ring> rings) {
        getSupportFragmentManager().popBackStackImmediate(RINGS_BACKSTACK,
                FragmentManager.POP_BACK_STACK_INCLUSIVE);
        showFragment(RingsFragment.newInstance(rings), true, RINGS_BACKSTACK);
    }

    public void changeScreen(final Screen item, @StringRes int source) {
        changeScreen(item, source, false);
    }

    public void changeScreen(final Screen item, @StringRes int source, boolean addToBackstack){
        if (item == Screen.CONNECT && SetupHelper.ringlyConnected(this)) {
            changeScreen(Screen.SETUP, source);
        } else if (item == Screen.SETUP && SetupHelper.canNotify(this)) {
            changeScreen(Screen.CONNECTION, source);
        } else if (item == Screen.ACTIVITY_PLACEHOLDER &&
            getDb().getCount().toBlocking().first() > 0 &&
            (!mFWApplicationVersion.isPresent() || !SetupHelper.needsActivityUpdate(mFWApplicationVersion.get()))) {
            changeScreen(Screen.ACTIVITY, source);
        } else if (item == Screen.PREFERENCES) {
            ActivityCompat.startActivityForResult(this, new Intent(this, PreferencesActivity.class), SHOW_PREFERENCES_REQUEST_CODE, null);
                //Modified to start Preference Activity with startActivityForResult,
               //depends of the preferences changes are the actions the app have to do when Preferences Activity finish
        } else {
            if (item == Screen.CONNECT) {
                mLastFindUpdatesTime = Optional.absent();
                clearUpdateBanner();
            } else {
                checkForUpdateBanner(item);
            }

            if (item == Screen.CONNECTION && !SetupHelper.isOnboarded(this)) {
                startOnboarding();
            }

            mNavView.setCheckedItem(item.navId);
            fillActionbar(item.actionbarTitle);

            Bundle bundle = new Bundle();

            if (mFirmwares.hasFirmwares()) {
                bundle.putBundle(Firmwares.BUNDLE_NAME, mFirmwares.toBundle());
            }

            if (mHardwareFamily.isPresent()) {
                bundle.putString(HardwareFamily.BUNDLE_NAME, mHardwareFamily.get().name());
            }

            if (mFWApplicationVersion.isPresent()) {
                bundle.putString(FW_VERSION_BUNDLE, mFWApplicationVersion.get());
            }

            Fragment frag = Fragment.instantiate(this, item.cls.getCanonicalName());
            frag.setArguments(bundle);
            showFragment(frag, addToBackstack);

            ImmutableMap<Mixpanel.Property, String> props = ImmutableMap.of(
                    Mixpanel.Property.TO, getResources().getString(item.nameId),
                    Mixpanel.Property.VIA, getResources().getString(source)
            );

            if (mCurrentScreen != null) {
                props = new ImmutableMap.Builder<Mixpanel.Property, String>()
                    .putAll(props)
                    .put(Mixpanel.Property.FROM, getResources().getString(mCurrentScreen.nameId))
                    .build();
            }

            mMixpanel.track(Mixpanel.Event.SWITCHED_MAIN_VIEW, props);

            mCurrentScreen = item;
        }
    }

    public void registerDevice(Ring ring) {
        // Hack for creating a UUID from a device-specific id (ANDROID_ID)
        String androidId =
            Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        RinglyApp.getInstance().getApi().registerDevice(ring, new UUID(androidId.hashCode(), androidId.hashCode()).toString())
            .subscribe(res -> Log.d(TAG, "registerDevice: " + res.deviceId),
                err -> Log.e(TAG, "registerDevice: " + err, err));
    }

    public Db getDb() {
        return mDb;
    }

    public void onUpdate() {
        findViewById(R.id.update_header).setVisibility(View.GONE);
    }

    ////
    //// private methods
    ////

    private void showFragment(final Fragment fragment, final boolean addToBackStack) {
        showFragment(fragment, addToBackStack, null);
    }

    private void showFragment(final Fragment fragment, final boolean addToBackStack,
                              String backstackName){
        final FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        if (addToBackStack) transaction.addToBackStack(backstackName);
        transaction.commitAllowingStateLoss();
    }

    private void sendPrefs() {
        RinglyService.doCommand(getPreferences().getConnectionLight()
            ? Command.ENABLE_CONNECTION_LIGHT : Command.DISABLE_CONNECTION_LIGHT, this);
        RinglyService.doCommand(getPreferences().getDisconnectionBuzz()
            ? Command.ENABLE_DISCONNECTION_BUZZ : Command.DISABLE_DISCONNECTION_BUZZ, this);
        RinglyService.doCommand(getPreferences().getSleepMode()
            ? Command.ENABLE_SLEEP_MODE : Command.DISABLE_SLEEP_MODE, this);
    }

    private void findUpdates(
            final String hardwareVersion,
            final Optional<String> bootloaderVersion,
            final String applicationVersion,
            final HardwareFamily hardwareFamily
    ) {
        Log.d(TAG, "findUpdates");

        new AsyncTask<Void, Void, Firmwares>() {
            @Override
            protected Firmwares doInBackground(final Void... params) {
                try {
                    return Firmwares.fromJson(RinglyApp.getInstance().getApi().getFirmwares(
                            Optional.of(hardwareVersion),
                            bootloaderVersion,
                            // only use firmware version before possible '+' followed by commit hash:
                            Optional.of(Splitter.on('+').split(applicationVersion).iterator().next()),
                            false, // all
                            false // force
                    ));
                } catch (IOException | JSONException e) {
                    Log.e(TAG, "failed to fetch firmware updates", e);
                    return new Firmwares();
                }
            }

            @Override
            protected void onPostExecute(final Firmwares urls) {
                final View header = findViewById(R.id.update_header);
                mFirmwares = urls;
                if (urls.hasFirmwares()) {
                    header.setVisibility(mCurrentScreen == Screen.CONNECTION ? View.VISIBLE : View.GONE);
                    mToolbar.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(final View v) {
                            if(mCurrentScreen != Screen.CONNECTION) {
                                return;
                            }

                            mToolbar.setOnClickListener(null);
                            header.setVisibility(View.GONE);

                            DfuActivity.start(urls, hardwareFamily, DFU_REQUEST_CODE, MainActivity.this);

                            mMixpanel.track(Mixpanel.Event.DFU_TAPPED);
                        }
                    });

                    mMixpanel.track(Mixpanel.Event.DFU_BANNER_SHOWN);
                } else {
                    clearUpdateBanner();
                }
            }
        }.execute();
    }

    /**
     * Hide the update header and also clear the toolbar open DFU onclick listener
     */
    private void clearUpdateBanner() {
        findViewById(R.id.update_header).setVisibility(View.GONE);
        mToolbar.setOnClickListener(null);
    }

    /**
     * Determine whether or not to show the update banner, assuming you're already on the
     * Connection screen.
     */
    private void checkForUpdateBanner(final Screen item) {
        View dfuBannerView = findViewById(R.id.update_header);
        if (mFirmwares.hasFirmwares() && (
                compareFirmwareToVersion(mFirmwares.application, mFWApplicationVersion) ||
                    compareFirmwareToVersion(mFirmwares.bootloader, mFWBootloaderVersion))) {
            if (item == Screen.CONNECTION) {
                dfuBannerView.setVisibility(View.VISIBLE);
            } else {
                dfuBannerView.setVisibility(View.GONE);
            }
        } else {
            clearUpdateBanner();
        }
    }

    /**
     * Helper for `checkForUpdateBanner` - returns true if both are present and the fwUpdate is newer.
     */
    private boolean compareFirmwareToVersion(Optional<Firmwares.Firmware> fwUpdate, Optional<String> currentFw) {
        if (fwUpdate.isPresent() && currentFw.isPresent()) {
            int i = com.ringly.ringly.Utilities.compareVersions(fwUpdate.get().version, currentFw.get());
            if (i > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Fix if the user has selected Email Notifications, split and select  AndriodEmail, Gmail and Inbox
     */
     private void splitEmailNotification() {
        NotificationMode notificationMode = getPreferences().getNotificationMode(NotificationType.EMAIL);
        if (notificationMode.equals(NotificationMode.ENABLED)) {
            Color color = getPreferences().getNotificationColor(NotificationType.EMAIL);
            Vibration vibration = getPreferences().getNotificationVibration(NotificationType.EMAIL);
            //if Gmail is installed add it
            checkIsInstalledAndSetup(NotificationType.GMAIL, notificationMode, color, vibration);
            //if Android Email is installed add it
            checkIsInstalledAndSetup(NotificationType.ANDROID_EMAIL, notificationMode, color, vibration);
            //if Inbox is installed add it
            checkIsInstalledAndSetup(NotificationType.INBOX, notificationMode, color, vibration);

            getPreferences().setNotificationMode(NotificationType.EMAIL, NotificationMode.REMOVED);
        }
    }


    /**
     * Check if the app is installed, if its installed set mode, color and vibration
     * @param type
     * @param mode
     * @param color
     * @param vibration
     */
    private  void checkIsInstalledAndSetup(NotificationType type, NotificationMode mode, Color color, Vibration vibration) {
        if (Utilities.isAppInstalled(type.ids, this)) {
            getPreferences().setNotificationMode(type, mode);
            getPreferences().setNotificationColor(type, color);
            getPreferences().setNotificationVibration(type, vibration);
        }
    }

}
