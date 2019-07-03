package com.ringly.ringly.ui.onboarding;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.google.common.base.Optional;
import com.ringly.ringly.R;
import com.ringly.ringly.config.RingType;

/**
 * Created by lindaliu on 1/8/16.
 */
public class GearsFragment extends Fragment {

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
        final LightView light = (LightView) view.findViewById(R.id.light);
        final ImageView[] dots = {(ImageView) view.findViewById(R.id.dot5),
                (ImageView) view.findViewById(R.id.dot6),
                (ImageView) view.findViewById(R.id.dot7),
                (ImageView) view.findViewById(R.id.dot8)};

        ImageView image = (ImageView) view.findViewById(R.id.phone_screen);
        LinearLayout layout = (LinearLayout) view.findViewById(R.id.content);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(image.getWidth(),
                image.getHeight());
        params.gravity = Gravity.CENTER;
        layout.setLayoutParams(params);

        for(int i = 0; i < 4; i++) {
            AnimationSet s = new AnimationSet(false);
            Animation fadeIn = new AlphaAnimation(0.0f, 1.0f);
            Animation fadeOut = new AlphaAnimation(1.0f, 0.0f);
            fadeIn.setDuration(200);
            fadeOut.setDuration(200);
            fadeIn.setStartOffset(75 * i);
            fadeOut.setStartOffset(75 * i + 160);
            s.addAnimation(fadeIn);
            s.addAnimation(fadeOut);
            dots[i].startAnimation(s);
        }

        Animation fade = new AlphaAnimation(0.0f, 1.0f);
        fade.setDuration(500);
        fade.setStartOffset(800);
        fade.setRepeatMode(Animation.REVERSE);
        fade.setRepeatCount(1);
        fade.setFillAfter(true);
        light.startAnimation(fade);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState
    ) {

        final View view = inflater.inflate(R.layout.fragment_onbd_gears, container, false);
        mActivity.onCreateView(view);

        Optional<RingType> type = mActivity.getType();
        if (type.isPresent()) {
            final ImageView stonePhoto = (ImageView) view.findViewById(R.id.stone);
            final Bitmap org_stone = BitmapFactory.decodeResource(getResources(), type.get().stoneId);
            final Bitmap cropped_stone = Bitmap.createBitmap(org_stone, 0, 0, org_stone.getWidth(),
                    (int)(org_stone.getHeight() * .8));
            stonePhoto.setImageBitmap(cropped_stone);
            stonePhoto.setVisibility(View.VISIBLE);

            final ImageView basePhoto = (ImageView) view.findViewById(R.id.base);
            final Bitmap org_base = BitmapFactory.decodeResource(getResources(), type.get().baseId);
            final Bitmap cropped_base = Bitmap.createBitmap(org_base, 0, 0, org_base
                    .getWidth(), (int)(org_base.getHeight() * .8));
            basePhoto.setImageBitmap(cropped_base);
            basePhoto.setVisibility(View.VISIBLE);
        }

        return view;
    }
}
