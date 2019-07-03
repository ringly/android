package com.ringly.ringly.ui.login;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.ringly.ringly.R;
import com.ringly.ringly.ui.Utilities;

import rx.android.schedulers.AndroidSchedulers;

import static com.ringly.ringly.ui.Utilities.createErrorChecker;
import static com.ringly.ringly.ui.Utilities.isValidEmail;
import static com.ringly.ringly.ui.Utilities.requestEditTextFocus;
import static com.ringly.ringly.ui.login.LoginActivity.PASSWORD_MIN_LENGTH;

public class CreateUserFragment extends Fragment {

    private static final String TAG = CreateUserFragment.class.getSimpleName();

    private LoginActivity mActivity;

    private TextInputLayout mEmailLayout;
    private TextInputLayout mPasswordLayout;

    private Button mCreateUserButton;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onAttach(Context context) {
        Log.d(TAG, "onAttach: ");

        mActivity = (LoginActivity) context;

        super.onAttach(context);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_create_user, container, false);

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

        mCreateUserButton = (Button) view.findViewById(R.id.button_create_user);
        mCreateUserButton.setOnClickListener(__ -> this.createUser());

        view.findViewById(R.id.button_cancel).setOnClickListener(__ -> mActivity.showSplash());

        requestEditTextFocus(mActivity, mEmailLayout.getEditText());

        return view;
    }

    private void createUser() {
        String email = mEmailLayout.getEditText().getText().toString();
        String password = mPasswordLayout.getEditText().getText().toString();

        if(!isValidEmail(email)) {
            mEmailLayout.setError(getString(R.string.invalid_email));
            return;
        }

        if(password.length() < PASSWORD_MIN_LENGTH) {
            mPasswordLayout.setError(getString(R.string.password_length));
            return;
        }

        setEnabled(false);
        mActivity.getLoginService()
            .createUser(email, password, true)
            .flatMap(user -> mActivity.getLoginService().authorize(email, password))
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                at -> mActivity.onAuthToken(email, at),
                err -> {
                    Log.e(TAG, "createUser error: " + err, err);
                    setEnabled(true);
                    mActivity.snack(mEmailLayout, R.string.email_in_use);
                    requestEditTextFocus(mActivity, mEmailLayout.getEditText());
                }
            );
    }

    private void setEnabled(boolean enabled) {
        mEmailLayout.setEnabled(enabled);
        mPasswordLayout.setEnabled(enabled);
        mCreateUserButton.setEnabled(enabled);
    }
}
