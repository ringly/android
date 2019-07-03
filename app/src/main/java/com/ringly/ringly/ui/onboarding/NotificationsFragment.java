package com.ringly.ringly.ui.onboarding;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.AnimRes;
import android.support.annotation.IdRes;
import android.support.v4.app.Fragment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.ringly.ringly.R;

import java.util.Random;

/**
 * Created by lindaliu on 1/8/16.
 */
public class NotificationsFragment extends Fragment {

    private OnboardingActivity mActivity;
    boolean didAnimate = false;

    @Override
    public void onAttach(final Activity activity) {
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
        final @IdRes int[] iconIds = {
                R.id.onbd_phone, R.id.onbd_wa, R.id.onbd_cal,
                R.id.onbd_twit, R.id.onbd_mail, R.id.onbd_fb,
                R.id.onbd_msgs, R.id.onbd_insta
        };
        final @IdRes int[] rowIds = {
                R.id.blue_row, R.id.green_row, R.id.yellow_row, R.id.purple_row
        };
        final @IdRes int[] vibIds = {
                R.id.vib4, R.id.vib3, R.id.vib2, R.id.vib1
        };

        ImageView image = (ImageView) view.findViewById(R.id.phone_screen);
        LinearLayout layout = (LinearLayout) view.findViewById(R.id.content);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(image.getWidth(),
                image.getHeight());
        params.gravity = Gravity.CENTER;
        layout.setLayoutParams(params);

        Random r = new Random();
        int i = 0;
        for(int id : iconIds) {
            AnimationSet s = new AnimationSet(false);

            Animation fadeIn = new AlphaAnimation(0.0f, 0.5f);
            fadeIn.setStartOffset(r.nextInt(100) + 200);
            fadeIn.setDuration(r.nextInt(1000) + 100);

            @AnimRes int anim = R.anim.fadeout_shrink;
            if(i < 4) {
                anim = R.anim.fadein_grow;
                float shift = (i == 2 || i == 3) ? -2.76f : -0.44f;
                Animation slide = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f,
                        Animation.RELATIVE_TO_SELF, shift, 0, 0.0f, 0, 0.0f);

                slide.setDuration(500);
                slide.setStartOffset(2100);
                s.addAnimation(slide);

                Animation slide2 = new TranslateAnimation(Animation.RELATIVE_TO_SELF, -1.0f,
                        Animation.RELATIVE_TO_SELF, 0.0f, 0, 0.0f, 0, 0.0f);

                slide2.setDuration(600);
                slide2.setStartOffset(2500 + i * 200);
                slide2.setFillAfter(true);
                view.findViewById(rowIds[i]).startAnimation(slide2);

                Animation fade = new AlphaAnimation(0.0f, 1.0f);
                fade.setStartOffset(3100 + i * 200);
                fade.setDuration(200);
                fade.setFillAfter(true);
                view.findViewById(vibIds[i]).startAnimation(fade);
            }

            Animation fadeInMore = AnimationUtils.loadAnimation(getActivity(), anim);
            s.setFillAfter(true);
            s.addAnimation(fadeIn);
            s.addAnimation(fadeInMore);
            view.findViewById(id).startAnimation(s);

            i++;
        }
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState
    ) {
        final View view = inflater.inflate(R.layout.fragment_onbd_notif, container, false);
        mActivity.onCreateView(view);

        view.findViewById(R.id.onbd_done).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View _) {
                mActivity.onDone();
            }
        });

        return view;
    }
}
