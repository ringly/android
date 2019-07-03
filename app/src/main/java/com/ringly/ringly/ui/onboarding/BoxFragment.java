package com.ringly.ringly.ui.onboarding;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.google.common.base.Optional;
import com.ringly.ringly.R;
import com.ringly.ringly.config.RingType;

/**
 * Created by lindaliu on 1/8/16.
 */
public class BoxFragment extends Fragment {

    private OnboardingActivity mActivity;
    boolean didAnimate = false;

    @Override
    public void onAttach(final Activity activity) {
        //noinspection HardCodedStringLiteral
        //Log.d(TAG, "onAttach");
        super.onAttach(activity);

        mActivity = (OnboardingActivity) activity;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if(isVisibleToUser) {
            doAnimation();
        }
    }

    public void doAnimation() {
        if(didAnimate) {
            return;
        }
        didAnimate = true;

        final View view = getView();
        final FrameLayout ring = (FrameLayout) view.findViewById(R.id.ring);
        final LightView light = (LightView) view.findViewById(R.id.light);

        ImageView image = (ImageView) view.findViewById(R.id.box_top);
        LinearLayout layout = (LinearLayout) view.findViewById(R.id.content);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(image.getWidth(),
                image.getHeight());
        params.gravity = Gravity.CENTER;
        layout.setLayoutParams(params);

        Animation fade = new AlphaAnimation(0.0f, 1.0f);
        fade.setDuration(400);
        fade.setStartOffset(1200);
        fade.setRepeatMode(Animation.REVERSE);
        fade.setRepeatCount(Animation.INFINITE);
        light.startAnimation(fade);

        Animation slide = new TranslateAnimation(0, 0.0f, 0, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, 0.75f);

        slide.setDuration(1000);
        slide.setFillAfter(true);
        ring.startAnimation(slide);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState
    ) {

        final View view = inflater.inflate(R.layout.fragment_onbd_box, container, false);
        mActivity.onCreateView(view);

        Optional<RingType> type = mActivity.getType();
        if (type.isPresent()) {
            final ImageView stonePhoto = (ImageView) view.findViewById(R.id.stone);
            stonePhoto.setImageResource(type.get().stoneId);
            stonePhoto.setVisibility(View.VISIBLE);

            final ImageView basePhoto = (ImageView) view.findViewById(R.id.base);
            basePhoto.setImageResource(type.get().baseId);
            basePhoto.setVisibility(View.VISIBLE);
        }

        return view;
    }
}
