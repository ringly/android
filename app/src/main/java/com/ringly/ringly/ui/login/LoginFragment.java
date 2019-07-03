package com.ringly.ringly.ui.login;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.google.common.collect.ImmutableMap;
import com.ringly.ringly.R;
import com.ringly.ringly.config.Mixpanel;
import com.ringly.ringly.config.model.AuthToken;
import com.ringly.ringly.ui.Utilities;

import java.net.UnknownHostException;

import retrofit2.adapter.rxjava.HttpException;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

import static com.ringly.ringly.ui.Utilities.createErrorChecker;
import static com.ringly.ringly.ui.Utilities.isValidEmail;
import static com.ringly.ringly.ui.Utilities.requestEditTextFocus;
import static com.ringly.ringly.ui.login.LoginActivity.PASSWORD_MIN_LENGTH;

public class LoginFragment extends Fragment {

    private static final String TAG = LoginFragment.class.getSimpleName();

    public static final String EXTRA_LOGIN = "login";

    private LoginActivity mActivity;

    private TextInputLayout mEmailLayout;
    private TextInputLayout mPasswordLayout;

    private Button mLoginButton;
    private Button mForgotPasswordButton;
    private TextView mSwitchText;

    private boolean mIsLogin;

    @Override
    public void onAttach(Context context) {
        Log.d(TAG, "onAttach: ");

        mActivity = (LoginActivity) context;

        super.onAttach(context);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login, container, false);

        mIsLogin = getArguments().getBoolean(EXTRA_LOGIN);

        mEmailLayout = (TextInputLayout) view.findViewById(R.id.text_layout_email);
        mPasswordLayout = (TextInputLayout) view.findViewById(R.id.text_layout_password);

        mEmailLayout.getEditText().addTextChangedListener(createErrorChecker(
            Utilities::isValidEmail,
            () -> mEmailLayout.setError(null)
        ));

        mPasswordLayout.getEditText().addTextChangedListener(createErrorChecker(
            s -> s.length() >= PASSWORD_MIN_LENGTH,
            () -> mPasswordLayout.setError(null)
        ));

        mLoginButton = (Button) view.findViewById(R.id.button_login);
        mLoginButton.setText(mIsLogin ? R.string.login : R.string.create_user);
        mLoginButton.setOnClickListener(__ -> this.login());

        mForgotPasswordButton = (Button) view.findViewById(R.id.button_forgot_password);
        mForgotPasswordButton.setOnClickListener(__ -> this.forgotPassword());

        mSwitchText = (TextView) view.findViewById(R.id.button_switch);
        mSwitchText.setText(mIsLogin ? R.string.switch_to_signup : R.string.switch_to_login);
        mSwitchText.setOnClickListener(__ -> setLogin(!mIsLogin));

        requestEditTextFocus(mActivity, mEmailLayout.getEditText());

        return view;
    }

    public void setLogin(boolean login) {
        mIsLogin = login;
        mSwitchText.setText(login ? R.string.switch_to_signup : R.string.switch_to_login);
        mLoginButton.setText(login ? R.string.login : R.string.create_user);
        mForgotPasswordButton.setVisibility(login ? View.VISIBLE : View.GONE);
    }

    private void login() {
        String email = mEmailLayout.getEditText().getText().toString();
        String password = mPasswordLayout.getEditText().getText().toString();

        if (!isValidEmail(email)) {
            mEmailLayout.setError(getString(R.string.invalid_email));
            return;
        }

        if (password.length() < PASSWORD_MIN_LENGTH) {
            mPasswordLayout.setError(getString(R.string.password_length));
            return;
        }

        Observable<AuthToken> authTokenObservable = mIsLogin ?
            mActivity.getLoginService().authorize(email, password)
                .doOnCompleted(() ->
                    mActivity.getMixpanel()
                        .track(Mixpanel.Event.AUTHENTICATION_COMPLETED,
                            ImmutableMap.of(Mixpanel.Property.METHOD, R.string.login))
                )
            :
            mActivity.getLoginService()
                .createUser(email, password, true)
                .doOnCompleted(() ->
                    mActivity.getMixpanel()
                        .track(Mixpanel.Event.AUTHENTICATION_COMPLETED,
                            ImmutableMap.of(Mixpanel.Property.METHOD, R.string.register))
                )
                .flatMap(user -> mActivity.getLoginService().authorize(email, password));

        Action1<Throwable> errorHandler = err -> {
            Log.e(TAG, "login error: " + err, err);
            setEnabled(true);
            int errRes;
            if(err instanceof UnknownHostException) {
                errRes = R.string.no_connection;
            }
            else if (mIsLogin) {
                errRes = R.string.login_error;
            }
            else {
                errRes = R.string.email_in_use;
            }
            mActivity.snack(mEmailLayout, errRes);
            requestEditTextFocus(mActivity, mEmailLayout.getEditText());

            if (err instanceof HttpException) {
                mActivity.getMixpanel().track(Mixpanel.Event.AUTHENTICATION_FAILED,
                    ImmutableMap.of(Mixpanel.Property.CODE, ((HttpException) err).code(),
                        Mixpanel.Property.DOMAIN, ((HttpException) err).response().raw().request().url().host()));
            }
        };

        setEnabled(false);

        authTokenObservable.observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                at -> mActivity.onAuthToken(email, at),
                errorHandler
            );
    }

    private void forgotPassword() {
        Intent fp =
            new Intent(Intent.ACTION_VIEW, Uri.parse("https://ringly.com/users/forgot-password"));
        ActivityCompat.startActivity(mActivity, fp, null);
    }

    private void setEnabled(boolean enabled) {
        mEmailLayout.setEnabled(enabled);
        mPasswordLayout.setEnabled(enabled);
        mLoginButton.setEnabled(enabled);
    }
}

