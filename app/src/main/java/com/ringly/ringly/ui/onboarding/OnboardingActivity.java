package com.ringly.ringly.ui.onboarding;

import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.common.base.Optional;
import com.ringly.ringly.R;
import com.ringly.ringly.bluetooth.Ring;
import com.ringly.ringly.bluetooth.RingName;
import com.ringly.ringly.config.RingType;
import com.ringly.ringly.ui.BaseActivity;
import com.ringly.ringly.ui.Utilities;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by lindaliu on 1/8/16.
 */
public class OnboardingActivity extends BaseActivity implements Ring.Listener {
    boolean shown = true;

    ViewPager mPager;
    MyAdapter mAdapter;

    Optional<RingType> type;

    List<ImageView> dots = new ArrayList<>();

    @Override
    protected void onStart() {
        super.onStart();
        addListener(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        removeListener(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.spinner);

        Optional<RingName> name = RingName.fromString(getIntent().getExtras().getString("ringName"));

        if (name.isPresent()) {
            final String shortName = name.get().type;
            try {
                type = Optional.of(RingType.valueOf(
                        Character.isDigit(shortName.charAt(0)) ? "_" + shortName : shortName));
            } catch (final IllegalArgumentException ignored) {
                type = Optional.of(RingType.DAYD); //if its a new model that doesn't exist in the app set DAYD model
            }
        } else {
            type = Optional.of(RingType.DAYD);
        }

        Fragment mFragment = new IndicatorFragment();
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.onbd_frame, mFragment);
        ft.commit();

        getPreferences().setShowOnboarding(false);
    }

    @Override
    public void onCreateView(final View view) {
        if (view instanceof ViewGroup) {
            final ViewGroup parent = (ViewGroup) view;
            for (int i = 0; i < parent.getChildCount(); i++) onCreateView(parent.getChildAt(i));
        } else if (view instanceof TextView) {
            final TextView text = (TextView) view;

            final Typeface typeface = text.getTypeface();
            text.setTypeface(mGothamBook, typeface != null ? typeface.getStyle() : Typeface.NORMAL);

            if(view.getId() != R.id.bottom_text) Utilities.uppercaseAndKern(text);
        }
    }

    @Override
    public void onUpdate(final Ring ring) {
        if (ring.isConnected() && shown) {
            shown = false;
            Fragment mFragment = new ConnectedFragment();
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left);
            ft.replace(R.id.onbd_frame, mFragment);
            ft.commit();
        }
    }

    public void initOnboarding() {
        setContentView(R.layout.fragment_onboarding);

        mAdapter = new MyAdapter(getSupportFragmentManager());
        mPager = (ViewPager) findViewById(R.id.pager);
        mPager.setAdapter(mAdapter);
        mPager.setOffscreenPageLimit(mAdapter.getCount() - 1);

        dots.add((ImageView) findViewById(R.id.dot1));
        dots.add((ImageView) findViewById(R.id.dot2));
        dots.add((ImageView) findViewById(R.id.dot3));

        mPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }

            @Override
            public void onPageSelected(int position) {
                LinearLayout layout = (LinearLayout) findViewById(R.id.dots);
                if (position < 3) {
                    for (int i = 0; i < 3; i++) {
                        dots.get(i).setAlpha(i == position ? 1.0f : 0.5f);
                    }
                    layout.setVisibility(View.VISIBLE);
                } else {
                    layout.setVisibility(View.INVISIBLE);
                }
            }
        });
    }

    public void onDone() {
        setResult(RESULT_OK);
        finish();
    }

    public Optional<RingType> getType() {
        return type;
    }

    public static class MyAdapter extends FragmentPagerAdapter {
        public MyAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public int getCount() {
            return 4;
        }

        @Override
        public Fragment getItem(int position) {
            Fragment fragment = new WelcomeFragment();
            switch (position) {
                case 0:
                    fragment = new WelcomeFragment();
                    break;
                case 1:
                    fragment = new GearsFragment();
                    break;
                case 2:
                    fragment = new BoxFragment();
                    break;
                case 3:
                    fragment = new NotificationsFragment();
                    break;
                default:
                    break;
            }

            return fragment;
        }
    }
}

