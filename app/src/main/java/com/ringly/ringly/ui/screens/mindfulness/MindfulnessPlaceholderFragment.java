package com.ringly.ringly.ui.screens.mindfulness;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ringly.ringly.R;
import com.ringly.ringly.config.model.GuidedMeditation;
import com.ringly.ringly.ui.MainActivity;

import java.util.List;

import rx.android.schedulers.AndroidSchedulers;

public class MindfulnessPlaceholderFragment extends Fragment {
    public static final String TAG = MindfulnessPlaceholderFragment.class.getCanonicalName();

    private MainActivity mActivity;

    @Override
    public void onAttach(final Activity activity) {
        //noinspection HardCodedStringLiteral
        Log.d(TAG, "onAttach");
        super.onAttach(activity);

        mActivity = (MainActivity) activity;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_mindfulness_placeholder, container, false);
        mActivity.onCreateView(view);


        return view;
    }


}
