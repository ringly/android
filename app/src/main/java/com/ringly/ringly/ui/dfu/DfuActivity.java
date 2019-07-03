package com.ringly.ringly.ui.dfu;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.ringly.ringly.R;
import com.ringly.ringly.bluetooth.HardwareFamily;
import com.ringly.ringly.bluetooth.RinglyService;
import com.ringly.ringly.config.Mixpanel;
import com.ringly.ringly.ui.BaseActivity;

public final class DfuActivity extends BaseActivity {

    private static final String TAG = DfuActivity.class.getCanonicalName();
    private static final String PACKAGE = DfuActivity.class.getPackage().getName();

    private enum Name {
        RECOVERY_EXTRA,
        HARDWARE_FAMILY
        ;

        @Override
        public String toString() {
            //noinspection MagicCharacter
            return PACKAGE + '.' + name();
        }
    }

    public static void start(final Firmwares urls, final HardwareFamily hardwareFamily,
                             final int requestCode, final Activity activity) {
        activity.startActivityForResult(
                new Intent(activity, DfuActivity.class)
                        .putExtras(urls.toBundle())
                        .putExtra(Name.HARDWARE_FAMILY.toString(), hardwareFamily.name()),
                requestCode
        );
    }

    public static void startRecovery(final Context context, final HardwareFamily hardwareFamily) {
        context.startActivity(new Intent(context, DfuActivity.class)
                .putExtra(Name.RECOVERY_EXTRA.toString(), true)
                .putExtra(Name.HARDWARE_FAMILY.toString(), hardwareFamily.name()));
    }


    private Firmwares mUrls;
    private Firmwares mFiles;
    private boolean mRecovery;
    private HardwareFamily mHardwareFamily;


    ////
    //// Activity methods
    ////

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        //noinspection HardCodedStringLiteral
        Log.d(TAG, "onCreate: " + savedInstanceState);
        super.onCreate(savedInstanceState);

        mPreferences.restoreRing(); // if we temporarily disabled ring at some point, restore it.
        // TODO can this onCreate happen during DFU, when ring is intentionally disabled?

        // initialize super properties in case this is our first run:
        mMixpanel.updateNotificationSuperProperties();
        mMixpanel.updateContactSuperProperties();
        mMixpanel.updateSettingSuperProperties();

        setContentView(R.layout.activity_dfu);

        final Bundle extras = getIntent().getExtras();

        // URLs only get passed through when we're not in recovery mode
        mUrls = Firmwares.fromBundle(extras);

        // Recovery boolean is passed through to denote that we're in recovery mode
        mRecovery = extras.getBoolean(Name.RECOVERY_EXTRA.toString());

        // Hardware family is passed through, so we know what type of
        // hardware family to proceed with for both recovery mode and regular updates
        // TODO(mad-uuids) - is there any way that this could potentially fail to find the hardware family if we got this far?
        mHardwareFamily = HardwareFamily.lookupName(extras.getString(Name.HARDWARE_FAMILY.toString())).get();

        showFragment(new IntroFragment());
    }

    @Override
    public void onBackPressed() {
        // ignore back button
    }


    ////
    //// fragment callbacks
    ////

    public void onIntroDone() {
        showFragment(DownloadFragment.newInstance(mUrls, mRecovery, mHardwareFamily));

        mMixpanel.track(Mixpanel.Event.DFU_START);
    }

    public void onIntroCanceled() {
        finish();

        mMixpanel.track(Mixpanel.Event.DFU_CANCELLED);
    }

    public void onDownloadDone(final Firmwares files) {
        mFiles = files;
        showFragment(ChargerFragment.newInstance(mRecovery));

        mMixpanel.track(Mixpanel.Event.DFU_DOWNLOADED);
    }

    public void onDownloadFailed() {
        Toast.makeText(this, R.string.download_failed_description, Toast.LENGTH_LONG).show();
        finish();

        mMixpanel.track(Mixpanel.Event.DFU_DOWNLOAD_FAILED);
    }

    public void onChargerDone() {
        showFragment(UpdateFragment.newInstance(mFiles, mRecovery, mHardwareFamily));
    }

    public void onChargerCanceled() {
        finish();

        mMixpanel.track(Mixpanel.Event.DFU_CANCELLED);
    }

    public void onUpdateDone() {
        showFragment(new DoneFragment());

        mMixpanel.track(Mixpanel.Event.DFU_COMPLETED);

        RinglyService.doReconnect(this);
    }

    public void onUpdateFailed() {
        try {
            new FailureFragment().show(getFragmentManager(), null);


        } catch (IllegalStateException e) {
            //This happen when the activity is closed before to show the failure fragment, one solution should call showAllowingStateLoss but now is hide in the code
            //Nothing todo
        }
        mMixpanel.track(Mixpanel.Event.DFU_FAILED);
    }

    public void onDone() {
        setResult(RESULT_OK);
        finish();
    }

    public void onFailed() {
        finish();
    }


    ////
    //// private methods
    ////

    private void showFragment(final Fragment fragment) {
        getFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commitAllowingStateLoss(); // commit() crashes if the screen is off…
    }
}
