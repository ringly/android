package com.ringly.ringly.ui.onboarding;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.common.base.Optional;
import com.ringly.ringly.R;
import com.ringly.ringly.config.RingType;
import com.ringly.ringly.ui.Utilities;

/**
 * Created by lindaliu on 1/18/16.
 */
public class ConnectedFragment extends Fragment {

    private OnboardingActivity mActivity;

    @Override
    public void onAttach(final Activity activity) {
        super.onAttach(activity);

        mActivity = (OnboardingActivity) activity;
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState
    ) {

        final View view = inflater.inflate(R.layout.fragment_onbd_connected, container, false);
        mActivity.onCreateView(view);


        Optional<RingType> type = mActivity.getType();
        if (type.isPresent()) {

            final ImageView basePhoto = (ImageView) view.findViewById(R.id.base);
            basePhoto.setImageResource(type.get().baseId);

            final ImageView stonePhoto = (ImageView) view.findViewById(R.id.stone);
            stonePhoto.setImageResource(type.get().stoneId);

            final FrameLayout ring = (FrameLayout) view.findViewById(R.id.ring);

            final TextView text = (TextView) view.findViewById(R.id.text);

            Utilities.uppercaseAndKern(text, "“" + getString(type.get().nameId) + "”\n" + "connected");

            Animation fadeIn = new AlphaAnimation(0.0f, 1.0f);
            fadeIn.setDuration(1000);
            fadeIn.setStartOffset(0);

            Animation slideDown = new TranslateAnimation(0, 0.0f, 0, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f,
                    Animation.RELATIVE_TO_SELF, 0.1f);

            Animation slideUp = new TranslateAnimation(0, 0.0f, 0, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f,
                    Animation.RELATIVE_TO_SELF, -0.8f);

            Animation wait = new AlphaAnimation(1.0f, 1.0f);
            wait.setDuration(1500);
            wait.setStartOffset(1200);

            slideDown.setDuration(1000);
            slideDown.setStartOffset(700);
            slideUp.setDuration(1000);
            slideUp.setStartOffset(700);

            AnimationSet s = new AnimationSet(false);
            s.addAnimation(fadeIn);
            s.addAnimation(slideDown);
            s.addAnimation(wait);

            s.setFillAfter(true);

            ring.startAnimation(s);

            s = new AnimationSet(false);
            s.addAnimation(fadeIn);
            s.addAnimation(slideUp);
            s.addAnimation(wait);

            s.setFillAfter(true);

            text.startAnimation(s);

            s.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationRepeat(Animation animation) {}

                @Override
                public void onAnimationStart(Animation animation) {}

                @Override
                public void onAnimationEnd(Animation animation) {
                    mActivity.initOnboarding();
                }
            });
        }

        return view;
    }
}
