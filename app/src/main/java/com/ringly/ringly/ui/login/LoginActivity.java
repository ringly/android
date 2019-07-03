package com.ringly.ringly.ui.login;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.View;

import com.google.common.base.Optional;
import com.ringly.ringly.Preferences;
import com.ringly.ringly.R;
import com.ringly.ringly.RinglyApp;
import com.ringly.ringly.config.LoginService;
import com.ringly.ringly.config.model.AuthToken;
import com.ringly.ringly.ui.BaseActivity;
import com.ringly.ringly.ui.MainActivity;

import static com.ringly.ringly.ui.MainActivity.MIXPANEL_SOURCE;

public class LoginActivity extends BaseActivity
    implements FragmentManager.OnBackStackChangedListener {

    private static final String TAG = LoginActivity.class.getSimpleName();

    public static final int PASSWORD_MIN_LENGTH = 8;

    private Optional<Snackbar> mSnackbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate: ");
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);

        mSnackbar = Optional.absent();

        getSupportFragmentManager().addOnBackStackChangedListener(this);

        showSplash();
    }

    @Override
    public void onBackStackChanged() {
        clearSnack();
    }

    public LoginService getLoginService() {
        return RinglyApp.getInstance().getApi().getLoginService();
    }

    public void onAuthToken(String email, AuthToken at) {
        Preferences.setAuthToken(this, at.token);
        Preferences.setUserEmail(this, email);

        Intent i = new Intent(this , MainActivity.class);
        i.putExtra(MIXPANEL_SOURCE, R.string.login);
        ActivityCompat.startActivity(this, i, null);
        finish();
    }

    public void snack(View v, int res) {
        clearSnack();

        mSnackbar = Optional.of(Snackbar.make(v, res, Snackbar.LENGTH_INDEFINITE));
        mSnackbar.get().show();
    }

    public void switchToLogin() {
        Bundle bundle = new Bundle();
        bundle.putBoolean(LoginFragment.EXTRA_LOGIN, true);
        Fragment loginFragment = new LoginFragment();
        loginFragment.setArguments(bundle);
        showFragment(loginFragment, true, null);
    }

    public void switchToCreate() {
        Bundle bundle = new Bundle();
        bundle.putBoolean(LoginFragment.EXTRA_LOGIN, false);
        Fragment loginFragment = new LoginFragment();
        loginFragment.setArguments(bundle);
        showFragment(loginFragment, true, null);
    }

    public void showSplash() {
        // Pop SplashFragment if it's there, otherwise create it.
        boolean fragmentPopped =
            getSupportFragmentManager().popBackStackImmediate(SplashFragment.TAG, 0);
        if (!fragmentPopped) {
            showFragment(new SplashFragment(), false, SplashFragment.TAG);
        }
    }

    private void clearSnack() {
        if (mSnackbar.isPresent()) {
            mSnackbar.get().dismiss();
            mSnackbar = Optional.absent();
        }
    }

    private void showFragment(Fragment frag, boolean addToBackstack, @Nullable String tag) {
        clearSnack();

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        if (addToBackstack) {
            ft.addToBackStack(tag);
        }
        ft.replace(R.id.login_content, frag);
        ft.commit();
    }
}
