package com.ringly.ringly.ui.onboarding;

import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;

import com.ringly.ringly.R;

/**
 * Created by lindaliu on 1/22/16.
 */
public class IndicatorFragment extends Fragment{

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState
    ) {

        final View view = inflater.inflate(R.layout.spinner, container, false);
        final @IdRes int[] facetIds = {
                R.id.facet1, R.id.facet2, R.id.facet3,
                R.id.facet4, R.id.facet5, R.id.facet6,
                R.id.facet7, R.id.facet8, R.id.facet9,
                R.id.facet10, R.id.facet11
        };

        for(int i = 0; i < facetIds.length; i++) {
            Animation fadeIn = new AlphaAnimation(0.6f, 0.9f);
            fadeIn.setStartOffset((i + 2) % 6 * 50);
            fadeIn.setDuration(200);
            fadeIn.setRepeatMode(Animation.REVERSE);
            fadeIn.setRepeatCount(Animation.INFINITE);
            view.findViewById(facetIds[i]).startAnimation(fadeIn);
        }

        return view;
    }
}
