package com.ringly.ringly.ui.screens.mindfulness;



import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.ringly.ringly.Preferences;
import com.ringly.ringly.R;
import com.ringly.ringly.config.GuidedMeditationsCache;
import com.ringly.ringly.config.model.GuidedMeditation;
import com.ringly.ringly.ui.MainActivity;

import java.io.IOError;
import java.io.IOException;
import java.util.List;

import rx.android.schedulers.AndroidSchedulers;


/**
 * MindfulnessFragment
 */
public final class MindfulnessFragment extends Fragment {

    private static final String TAG = MindfulnessFragment.class.getCanonicalName();

    private MainActivity mActivity;
    private Snackbar snackbar;

    @Override
    public void onAttach(final Context activity) {
        Log.d(TAG, "onAttach");
        super.onAttach(activity);
        mActivity = (MainActivity) activity;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        Log.d(TAG, "onCreate: " + savedInstanceState);
        super.onCreate(savedInstanceState);
    }

    @SuppressWarnings("RefusedBequest")
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: " + savedInstanceState);

        final View view = inflater.inflate(R.layout.fragment_mindfulness, container, false);
        view.findViewById(R.id.layoutBreathing).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getActivity(), BreathingActivity.class);
                getActivity().startActivity(i);
            }
        });
        TextView feedbackLink = (TextView) view.findViewById(R.id.feedbackLink);
        if (feedbackLink != null) {
            feedbackLink.setMovementMethod(LinkMovementMethod.getInstance());
        }
        this.downloadGuidedMeditations();

        mActivity.onCreateView(view);

        return view;
    }

    private void setProgress() {
        if (getActivity()!= null) {
            TextView progressTextView = (TextView) getActivity().findViewById(R.id.txtProgress);
            int goal = Preferences.getMindfulMinutesGoal(getContext());
            int progress = Preferences.getMindfulMinutesCount(getContext());
            progressTextView.setText(getString(R.string.mindful_minutes_progress, progress, goal));
            ProgressView progressView = (ProgressView)getActivity().findViewById(R.id.progressMindful);
            progressView.setProgress(progress*360/goal);
        }
    }
    public void downloadGuidedMeditations() {
        GuidedMeditationsCache.getInstance().getGuidedMeditationsList()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        gm -> onResult(gm),
                        err -> {
                            String message = "";
                            GuidedMeditationsCache.getInstance().reset();
                            if (getActivity()!= null) { //I have to check here if the activity is still alive.
                                if (err instanceof IOException) {
                                    message = getString(R.string.error_no_connection);
                                } else {
                                    message = getString(R.string.error_general);
                                }
                                snackbar = Snackbar
                                        .make(getView(), message, Snackbar.LENGTH_INDEFINITE)
                                        .setAction(getString(R.string.retry), new View.OnClickListener() {
                                            @Override
                                            public void onClick(View view) {
                                                downloadGuidedMeditations();
                                            }
                                        });

                                snackbar.show();
                            }
                            Log.e(TAG, "Download Guided Meditation error: " + err);
                        }
                );
    }

    public void onResult(List<GuidedMeditation> gmList) {
        if (gmList != null && getActivity() != null) {
            LinearLayout guidedMeditationsLayout = (LinearLayout) getActivity().findViewById(R.id.layoutGuidedMeditations);
            if (guidedMeditationsLayout != null) { //I have to add this check because the user can close the screen before the guided meditations finish downloading
                guidedMeditationsLayout.removeAllViews();
                for (GuidedMeditation gm : gmList) {
                    MindfulnessRow row = new MindfulnessRow(getContext());
                    row.setClickable(true);
                    row.setGuidedMeditations(gm);
                    ImageView rowIV = row.getRowIV();
                    Glide.with(this)
                            .load(gm.iconImage3x)
                            .into(rowIV);

                    mActivity.setDefaultTypeface(row);
                    guidedMeditationsLayout.addView(row);
                    row.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            MindfulnessRow row = (MindfulnessRow) v;
                            Intent i = new Intent(getActivity(), GuidedMeditationActivity.class);
                            i.putExtra(GuidedMeditation.GUIDED_MEDITATION_ID, row.getGuidedMeditation());
                            getActivity().startActivity(i);
                        }
                    });
                }
            }
        }
        Log.d(TAG, "completed");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (snackbar != null) {
            snackbar.dismiss();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        setProgress();
    }
}
