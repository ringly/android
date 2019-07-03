package com.ringly.ringly.ui.screens.mindfulness;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;
import com.ringly.ringly.Preferences;
import com.ringly.ringly.R;
import com.ringly.ringly.ui.BaseActivity;
import com.ringly.ringly.ui.Utilities;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Monica on 6/12/2017.
 */

public class BreathingActivity extends BaseActivity implements View.OnClickListener {


    private View minutesButtonSelected;
    private int secondsSelected;
    private Timer updateTimer;
    private TimerTask updateTimerTask;
    private int second;
    private int times;
    private AnimatorSet animatorSet;
    private View timerLayout;
    private TextView timerTextView;
    private TextView statusTextView;
    private Handler handler;

    private PowerManager.WakeLock wakeLock;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_breathing);
        setDefaultTypeface(findViewById(R.id.layoutMain));
        PowerManager powerManager = (PowerManager)getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK, "My Lock");

        findViewById(R.id.layoutBeginning).setVisibility(View.VISIBLE);
        findViewById(R.id.layoutBreathing).setVisibility(View.GONE);


        Button button1 = (Button) findViewById(R.id.btn1minute);
        setMinutes(button1);

        findViewById(R.id.btnCheckDone).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.layoutBreathing) {
            //Hide or show the timer when the user click on the screen
            if (timerLayout.getVisibility() == View.VISIBLE) {
                timerLayout.setVisibility(View.INVISIBLE);
            } else {
                timerLayout.setVisibility(View.VISIBLE);
            }

        } else if (v.getId() == R.id.btnCheckDone) {
            v.setVisibility(View.INVISIBLE);
            findViewById(R.id.imgDone).setVisibility(View.INVISIBLE);
            goToBeginningScreen();

        }
    }

    private void goToBeginningScreen() {
        findViewById(R.id.layoutBeginning).setVisibility(View.VISIBLE);
        findViewById(R.id.layoutBreathing).setVisibility(View.GONE);
    }

    public void setMinutes(View view) {
        if (minutesButtonSelected != null) {
            minutesButtonSelected.setSelected(false);
        }
        int minutes= 1;
        try {
            minutes = Integer.valueOf((String) view.getTag());
        } catch (Exception e) {
            //this should never fail because the button tag is setted in the xml with the values 1 - 3 - 5
            Crashlytics.logException(e);
        }
        secondsSelected = minutes * 60;
        view.setSelected(true);
        minutesButtonSelected = view;
    }

    /**
     * This show the breathing screen and animate the circles
     */
    public void startBreathing(View view) {
        findViewById(R.id.layoutBeginning).setVisibility(View.GONE);

        LinearLayout breathingLayout = (LinearLayout) findViewById(R.id.layoutBreathing);
        breathingLayout.setVisibility(View.VISIBLE);
        breathingLayout.setOnClickListener(this);

        animateCircles();
    }

    /**
     * This animate the 2 circles. Make the circles bigger for 4 seconds, Hold 2 seconds,
     * and make the circles smaller 4 seconds.
     * It repeats the animaton 6 times x minutes, and run the minutes selected by the user in the previous screen.
     */
    private void animateCircles() {
        initializeFlagsAndControls();

        CircleView circle1 = (CircleView)findViewById(R.id.circle1);
        CircleView circle2 = (CircleView)findViewById(R.id.circle2);
        circle1.setVisibility(View.VISIBLE);
        circle2.setVisibility(View.VISIBLE);

        // if the user has selected 3 minutes, then seconds selected = 180, then the animations will run 18 times
        times = secondsSelected / 10;

        //The animators scale the circle to the double of the width and translate 300px
        ObjectAnimator scaleUpCircle1 = ObjectAnimator.ofPropertyValuesHolder(circle1,
                PropertyValuesHolder.ofFloat(View.SCALE_X.getName(), 2f),
                PropertyValuesHolder.ofFloat(View.SCALE_Y.getName(), 2f),
                PropertyValuesHolder.ofFloat(View.TRANSLATION_Y.getName(), 200f));
        scaleUpCircle1.setDuration(4000);
        scaleUpCircle1.setInterpolator(new DecelerateInterpolator());

        ObjectAnimator scaleDownCircle1 = ObjectAnimator.ofPropertyValuesHolder(circle1,
                PropertyValuesHolder.ofFloat(View.SCALE_X.getName(), 1f),
                PropertyValuesHolder.ofFloat(View.SCALE_Y.getName(), 1f),
                PropertyValuesHolder.ofFloat(View.TRANSLATION_Y.getName(), 0f));
        scaleDownCircle1.setDuration(3000);
        scaleDownCircle1.setInterpolator(new DecelerateInterpolator());

        ObjectAnimator scaleUpCircle2 = ObjectAnimator.ofPropertyValuesHolder(circle2,
                PropertyValuesHolder.ofFloat(View.SCALE_X.getName(), 2f),
                PropertyValuesHolder.ofFloat(View.SCALE_Y.getName(), 2f),
                PropertyValuesHolder.ofFloat(View.TRANSLATION_Y.getName(), -200f));
        scaleUpCircle2.setDuration(4000);
        scaleUpCircle2.setInterpolator(new DecelerateInterpolator());
        ObjectAnimator scaleDownCircle2 = ObjectAnimator.ofPropertyValuesHolder(circle2,
                PropertyValuesHolder.ofFloat(View.SCALE_X.getName(), 1f),
                PropertyValuesHolder.ofFloat(View.SCALE_Y.getName(), 1f),
                PropertyValuesHolder.ofFloat(View.TRANSLATION_Y.getName(), 0f));
        scaleDownCircle2.setDuration(3000);
        scaleDownCircle2.setInterpolator(new DecelerateInterpolator());
        //I set an updateListener for each animation, because I need to redraw the circle,
        //The app make the border wider when scale the circle, then I set the animatedfraction to calculate the new border width
        scaleUpCircle1.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                circle1.setAnimatedFraction(animation.getAnimatedFraction());
                circle1.invalidate();
            }
        });
        scaleUpCircle2.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                circle2.setAnimatedFraction(animation.getAnimatedFraction());
                circle2.invalidate();
            }
        });
        scaleDownCircle1.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                circle1.setAnimatedFraction(1-animation.getAnimatedFraction());
                circle1.invalidate();
            }
        });
        scaleDownCircle2.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                circle2.setAnimatedFraction(1-animation.getAnimatedFraction());
                circle2.invalidate();
            }
        });


        animatorSet = new AnimatorSet();
        animatorSet.setupStartValues();
        animatorSet.setupEndValues();
        //Set animations for circles
        AnimatorSet animatorSetUp = new AnimatorSet();
        animatorSetUp.setupStartValues();
        animatorSetUp.setupEndValues();
        animatorSetUp.play(scaleUpCircle1).with(scaleUpCircle2);

        AnimatorSet animatorSetDown = new AnimatorSet();
        animatorSetDown.setupStartValues();
        animatorSetDown.setupEndValues();
        animatorSetDown.setStartDelay(2000);
        animatorSetDown.play(scaleDownCircle1).with(scaleDownCircle2);

        animatorSet.playSequentially(animatorSetUp, animatorSetDown);
        animatorSet.setStartDelay(1000);
        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if (times > 1) {
                    times --;
                    animatorSet.start();
                }
            }
        });

        acquireWakeLock();
        scheduleTimerTask();
        handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                findViewById(R.id.txtIntro).animate().alpha(0.0f).setDuration(800);
                statusTextView.animate().alpha(1.0f).setDuration(800);
                timerLayout.setVisibility(View.VISIBLE);
                updateTimer = new Timer();
                updateTimer.schedule(updateTimerTask, 1000, 1000);
            }
        }, 4000);

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                animatorSet.start();
            }
        }, 3000);


    }

    /**
     * Initialize flags and controls before start the animation
     */
    private void initializeFlagsAndControls() {
        timerLayout = findViewById(R.id.layoutTimer);
        timerLayout.setVisibility(View.INVISIBLE);
        findViewById(R.id.layoutBreathing).setClickable(true);
        findViewById(R.id.txtStatus).setVisibility(View.VISIBLE);
        findViewById(R.id.txtIntro).setAlpha(1.0f);
        timerTextView = (TextView)findViewById(R.id.txtTimer);

        //After 5 seconds hide Timer Layout
        second = secondsSelected;
        timerTextView.setText(Utilities.formatSeconds(second));

        statusTextView = (TextView) findViewById(R.id.txtStatus);
        statusTextView.setAlpha(0.0f);
        statusTextView.setText(getString(R.string.inhale));

    }

    private void scheduleTimerTask() {
        updateTimerTask = new TimerTask() {
            @Override
            public void run() {
//                if (getActivity() != null) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            second--;
                            timerTextView.setText(Utilities.formatSeconds(second));

                            if (secondsSelected - second == 5) {
                                timerLayout.setVisibility(View.INVISIBLE);

                            } else if (second == 0) {
                                updateTimerTask.cancel();
                                updateTimer = null;
                                Preferences.addMindfulMinutesCount(BreathingActivity.this, secondsSelected);
                                findViewById(R.id.layoutBreathing).setClickable(false);
                                findViewById(R.id.circle1).setVisibility(View.INVISIBLE);
                                findViewById(R.id.circle2).setVisibility(View.INVISIBLE);
                                findViewById(R.id.btnCheckDone).setVisibility(View.VISIBLE);
                                findViewById(R.id.imgDone).setVisibility(View.VISIBLE);
                                statusTextView.setAlpha(0.0f);
                                animatorSet.cancel();
                                releaseWakeLock();

                            } else {
                                if ((secondsSelected - second)%10==4) {
                                    statusTextView.setText(getString(R.string.hold));
                                } else if ((secondsSelected - second)%10==6) {
                                    statusTextView.setText(getString(R.string.exhale));
                                } else if ((secondsSelected - second)%10==0) {
                                    statusTextView.setText(getString(R.string.inhale));
                                }
                            }

                        }
                    });
                }
//            }
        };


    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        cancelAnimations();
        releaseWakeLock();
    }

    private void cancelAnimations() {
        cancelAnimator(animatorSet);
        if (handler != null) {
            try {
                handler.removeCallbacksAndMessages(null);
            }catch (Exception e) {
                //Nothing to do
            }
        }
        if (updateTimer != null) {
            try {
                updateTimer.cancel();
                updateTimer = null;
                Preferences.addMindfulMinutesCount(BreathingActivity.this, secondsSelected-second);
            } catch (Exception e) {
                //this should never fail
                Crashlytics.logException(e);
            }
        }

    }

    private void cancelAnimator(AnimatorSet animator) {
        if (animator!= null && animator.isRunning()) {
            try {
                animator.end();
                animator = null;
            } catch (Exception e) {
                //this should never fail
                Crashlytics.logException(e);
            }
        }
    }

    public void resetAnimaion() {
        this.cancelAnimations();
        this.goToBeginningScreen();
    }

    private void acquireWakeLock() {
        if (wakeLock != null) {
            try{
                wakeLock.acquire();
            } catch (Exception e) {
                Crashlytics.logException(e);
            }
        }
    }

    private void releaseWakeLock() {
        if (wakeLock != null) {
            try{
                wakeLock.release();
            } catch (Exception e) {
                //Nothing todo
            }
        }
    }

    public void closeScreen(View view) {
        this.finish();
    }
}
