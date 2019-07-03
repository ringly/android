package com.ringly.ringly.ui.login;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ringly.ringly.R;

public class SplashFragment extends Fragment {

    public static final String TAG = SplashFragment.class.getSimpleName();

    private LoginActivity mActivity;

    @Override
    public void onAttach(Context context) {
        Log.d(TAG, "onAttach: ");

        mActivity = (LoginActivity) context;

        super.onAttach(context);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_splash, container, false);

        view.findViewById(R.id.button_create_user)
            .setOnClickListener(__ -> mActivity.switchToCreate());
        view.findViewById(R.id.button_login)
            .setOnClickListener(__ -> mActivity.switchToLogin());

        mActivity.setDefaultTypeface(view.findViewById(R.id.ringly_main));

        return view;
    }
}
