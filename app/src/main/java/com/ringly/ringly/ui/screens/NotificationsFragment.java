package com.ringly.ringly.ui.screens;


import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.v4.app.ListFragment;
import android.support.v4.util.Pair;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.ringly.ringly.Preferences;
import com.ringly.ringly.R;
import com.ringly.ringly.bluetooth.RinglyService;
import com.ringly.ringly.config.Color;
import com.ringly.ringly.config.Mixpanel;
import com.ringly.ringly.config.NotificationMode;
import com.ringly.ringly.config.NotificationType;
import com.ringly.ringly.config.Vibration;
import com.ringly.ringly.ui.MainActivity;
import com.ringly.ringly.ui.Utilities;

import java.util.List;
import java.util.Set;


public final class NotificationsFragment extends ListFragment {

    private static final String TAG = NotificationsFragment.class.getCanonicalName();

    private static final Color[] COLORS = Color.values();
    private static final Vibration[] VIBRATIONS = Vibration.values();
    private static final int SET_VIBRATION_DELAY = 500; // milliseconds
    private static final int TRACK_DELAY = 5000; // milliseconds

    private final class NotificationsAdapter extends BaseAdapter {
        private List<NotificationType> mTypes;
        private final List<NotificationType> mTypesAll;
        private List<NotificationType> mTypesOn;
        private boolean mEditing = false;

        private NotificationsAdapter() {
            final ImmutableList.Builder<NotificationType> builder = ImmutableList.builder();
            for (final NotificationType type : NotificationType.values()) {
                if (type.ids.isEmpty()) builder.add(type); // special notifications
                else if (Utilities.isAppInstalled(type.ids, getActivity())) {
                        builder.add(type);
                }

            }
            mTypesAll = builder.build();
            setTypes();
        }

        @Override
        public int getCount() {
            return mTypes.size();
        }

        @Override
        public NotificationType getItem(final int position) {
            return mTypes.get(position);
        }

        @Override
        public long getItemId(final int position) {
            return 0;
        }

        @Override
        public View getView(final int position, final View convertView, final ViewGroup parent) {
            final View view;
            final @IdRes int[] vibIds = {
                    R.id.vib_edit_0, R.id.vib_edit_1, R.id.vib_edit_2,
                    R.id.vib_edit_3, R.id.vib_edit_4
            };

            @LayoutRes int layout = mEditing ? R.layout.listitem_notifications_edit :
                    R.layout.listitem_notifications;

            // use the tag to prevent unnecessary view inflating...
            if (convertView == null || (convertView.getTag().equals("Done") && mEditing)
                    || (convertView.getTag().equals("Edit") && !mEditing)) {
                view = getActivity().getLayoutInflater().inflate(layout, parent, false);
                mActivity.onCreateView(view);
            } else view = convertView;

            final NotificationType type = getItem(position);
            final TextView text = (TextView) view.findViewById(R.id.name);
            final ImageView swipe = (ImageView) view.findViewById(R.id.swipe);
            final ImageView vibration = (ImageView) view.findViewById(R.id.vibration);
            text.setCompoundDrawablesRelativeWithIntrinsicBounds(type.iconId, 0, 0, 0);
            Utilities.uppercaseAndKern(text, type.nameId);

            if(mEditing) {
                final CheckBox box = (CheckBox) view.findViewById(R.id.checkbox);
                box.setChecked(mTypesOn.contains(type) ||
                    mActivity.getPreferences().getNotificationMode(type)
                        .equals(NotificationMode.ENABLED));
                box.setOnClickListener(new View.OnClickListener() {
                   @Override
                   public void onClick(View v) {
                       boolean isChecked = box.isChecked();
                       if(isChecked) {
                           mActivity.getPreferences().setNotificationMode(type, NotificationMode.ENABLED);
                           if(!hasColorOrVibration(type)) {
                               mActivity.getPreferences().setNotificationVibration(type, Vibration.ONE);
                           }
                       }
                       else {
                           mActivity.getPreferences().setNotificationMode(type, NotificationMode.DISABLED);
                       }
                   }
                });

                text.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(final View v) {
                        box.performClick();
                    }
                });
            } else {
                update(view, type);

                text.setOnTouchListener(new View.OnTouchListener() {
                    final float den = getActivity().getResources().getDisplayMetrics().density;
                    final float SWIPE_START_THRESHOLD = 30;
                    final float SWIPE_FINISH_THRESHOLD = 60;
                    final float TAP_THRESHOLD = 4 * den;

                    // track lastX and lastlastX to change behavior if direction of swipe changes
                    private float x1, x2, lastX, lastlastX;
                    private int lastDirection;
                    private int i;
                    private Color color;

                    @Override
                    public boolean onTouch(View v, MotionEvent e) {
                        switch (e.getAction()) {
                            case MotionEvent.ACTION_DOWN: {
                                x1 = e.getX();
                                i = mActivity.getPreferences().getNotificationColor(type)
                                        .ordinal();
                                lastDirection = i;
                                return true;
                            }
                            case MotionEvent.ACTION_MOVE: {
                                lastlastX = lastX;
                                lastX = e.getX();
                                float deltaX = e.getX() - x1;
                                if (Math.abs(deltaX) > SWIPE_START_THRESHOLD) {
                                    view.getParent().requestDisallowInterceptTouchEvent(true);
                                }

                                int index = deltaX < 0 ? i + 1 : i - 1;
                                if (index != lastDirection) {
                                    color = COLORS[(index + COLORS.length) % COLORS.length];
                                    view.setBackgroundColor(getResources().getColor(color.id));
                                }

                                swipe.setTranslationX(deltaX);
                                return true;
                            }
                            case MotionEvent.ACTION_CANCEL: {
                                swipe.setTranslationX(0); // reset in case
                                return true;
                            }
                            case MotionEvent.ACTION_UP: {
                                x2 = e.getX();

                                float totalDeltaX = x2 - x1;
                                float lastDeltaX = x2 - lastlastX;

                                if (totalDeltaX > SWIPE_FINISH_THRESHOLD && lastDeltaX >= 0) { // L->R swipe
                                    swipe(view, type, 1, color, true);
                                } else if (totalDeltaX < -1 * SWIPE_FINISH_THRESHOLD && lastDeltaX <= 0) { // R->L swipe
                                    swipe(view, type, -1, color, true);
                                } else if (Math.abs(totalDeltaX) < TAP_THRESHOLD) { // tap
                                    swipe.setTranslationX(0);
                                    color = COLORS[(i + 1) % COLORS.length];
                                    view.setBackgroundColor(getResources().getColor(color.id));
                                    swipe(view, type, -1, color, false);
                                } else { // reset
                                    ObjectAnimator anim = ObjectAnimator.ofFloat(swipe, "translationX", 0);
                                    anim.start();
                                }
                                return true;
                            }
                        }
                        return false;
                    }
                });

                vibration.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent e) {
                        switch (e.getAction()) {
                            case MotionEvent.ACTION_DOWN: {
                                Drawable glow = getResources().getDrawable(R.drawable.gradient_glow);
                                int w = vibration.getWidth() / 2;
                                int h = vibration.getHeight() / 2;
                                glow.setBounds(w - 75, h - 75, w + 75, h + 75);
                                vibration.getOverlay().add(glow);
                                return true;
                            }
                            case MotionEvent.ACTION_CANCEL: {
                                vibration.getOverlay().clear();
                                return true;
                            }
                            case MotionEvent.ACTION_UP: {
                                vibration.getOverlay().clear();

                                view.findViewById(R.id.vib_edit_4).setVisibility(View.VISIBLE);
                                LinearLayout vib_image = (LinearLayout) view.findViewById(R.id.vib_anims);

                                Animation slide = new ScaleAnimation(0f, 1f, 1f, 1f,
                                        Animation.RELATIVE_TO_SELF, 1f, Animation.RELATIVE_TO_SELF, 1f);
                                slide.setDuration(200);

                                vib_image.setVisibility(View.VISIBLE);
                                vib_image.startAnimation(slide);

                                final int[] colors = mActivity.getPreferences().getNotificationColor(type).gradientColors;
                                for (int i = 0; i < vibIds.length; i++) {
                                    view.findViewById(vibIds[i]).setBackgroundColor(colors[i]);
                                }
                                return true;
                            }
                        }
                        return false;
                    }
                });

                for (int i = 0; i < vibIds.length; i++) {
                    final int vib_number = i;
                    ImageView vib_image = (ImageView) view.findViewById(vibIds[i]);
                    vib_image.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(final View v) {
                            view.findViewById(R.id.vib_edit_4).setVisibility(View.GONE);
                            LinearLayout vib_image = (LinearLayout) view.findViewById(R.id.vib_anims);

                            Animation slide = new ScaleAnimation(1f, 0f, 1f, 1f,
                                    Animation.RELATIVE_TO_SELF, 1f, Animation.RELATIVE_TO_SELF, 1f);
                            slide.setDuration(200);

                            vib_image.setVisibility(View.GONE);
                            vib_image.startAnimation(slide);

                            mActivity.getPreferences().setNotificationVibration(type,
                                    VIBRATIONS[vib_number]);

                            mTestVibrationType = type;
                            Utilities.delay(mTestVibration, SET_VIBRATION_DELAY, mHandler);

                            update(view, type);
                            trackSetting(type, Mixpanel.NotificationMethod.VIBRATION);
                        }
                    });
                }
            }

            return view;
        }

        public boolean isEditing() {
            return mEditing;
        }

        public void setEditing(final boolean editing) {
            mEditing = editing;
            setTypes();
            notifyDataSetChanged();
        }

        public boolean hasColorOrVibration(NotificationType type) {
            return mActivity.getPreferences().getNotificationVibration(type) != Vibration.NONE
                    || mActivity.getPreferences().getNotificationColor(type) != Color.NONE;
        }

        public void setTypes() {
            // users settings will be migrated to use NotificationMode after this is run once
            if(mEditing) {
                mTypes = mTypesAll;
            }
            else { // only want to display the apps that are enabled
                final ImmutableList.Builder<NotificationType> builder = ImmutableList.builder();
                for (final NotificationType type : NotificationType.values()) {
                    switch(mActivity.getPreferences().getNotificationMode(type)) {
                        case ENABLED:
                            builder.add(type);
                            break;
                        case DISABLED:
                            break;
                        // user is opening this page for the first time since NotificationMode was added
                        case UNSET:
                            if (hasColorOrVibration(type)) {
                                mActivity.getPreferences().setNotificationMode(type, NotificationMode.ENABLED);
                                builder.add(type);
                            }
                            else {
                                mActivity.getPreferences().setNotificationMode(type, NotificationMode.DISABLED);
                            }
                            break;
                    }
                }
                mTypesOn = builder.build();
                mTypes = mTypesOn;
            }
        }
    }

    private final Set<Pair<NotificationType, Mixpanel.NotificationMethod>> mChangedTypes = Sets.newHashSet();
    private NotificationType mTestVibrationType;

    private MainActivity mActivity;
    private Handler mHandler;

    private MenuItem mEditAction;
    private MenuItem mDoneAction;

    private TextView mInstructions;

    ////
    //// Fragment methods
    ////

    @Override
    public void onAttach(final Context activity) {
        //noinspection HardCodedStringLiteral
        Log.d(TAG, "onAttach");
        super.onAttach(activity);

        mActivity = (MainActivity) activity;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        //noinspection HardCodedStringLiteral
        Log.d(TAG, "onCreate: " + savedInstanceState);
        super.onCreate(savedInstanceState);

        mHandler = new Handler();

        setListAdapter(new NotificationsAdapter());
    }

    @SuppressWarnings("RefusedBequest")
    @Override
    public View onCreateView(
            @NonNull final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState
    ) {
        //noinspection HardCodedStringLiteral
        Log.d(TAG, "onCreateView: " + savedInstanceState);

        final View view = inflater.inflate(R.layout.fragment_notifications, container, false);

        setHasOptionsMenu(true);

        mInstructions = (TextView) view.findViewById(R.id.instructions);
        mInstructions.setText(Html.fromHtml("Add and remove<br>apps by tapping<br><b>the edit icon</b> above"));

        mActivity.onCreateView(view);

        if(mActivity.getPreferences().getShowInstructions()) {
            mInstructions.setVisibility(View.VISIBLE);
            if(!Preferences.isNotificationOnboarded(getContext())) {
                Animation wait = new AlphaAnimation(1.0f, 1.0f); // use this to create delay for animation
                wait.setDuration(1000);
                wait.setStartOffset(0);
                wait.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationRepeat(Animation animation) {
                    }

                    @Override
                    public void onAnimationStart(Animation animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        doAnimation();
                    }
                });

                mInstructions.startAnimation(wait);
            }
        }

        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_notifications, menu);
        mEditAction = menu.findItem(R.id.action_edit);
        mDoneAction = menu.findItem(R.id.action_done);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.action_edit:
                changeEditingState(true);
                return true;
            case R.id.action_done:
                changeEditingState(false);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void changeEditingState(boolean editing) {
        ((NotificationsAdapter) getListAdapter()).setEditing(editing);

        // If they change the editing state ever, we don't need to show the text anymore.
        mActivity.getPreferences().setShowInstructions(false);
        mInstructions.setVisibility(View.GONE);

        mEditAction.setVisible(!editing);
        mDoneAction.setVisible(editing);
    }

    public void doAnimation() {
        final View view = getView();
        final ImageView edit2 = (ImageView) view.findViewById(R.id.edit2);
        final ImageView background = (ImageView) view.findViewById(R.id.bg);

        background.setVisibility(View.VISIBLE);
        // using z-indices wasn't working well with this layout, so use a
        // second edit button on top of the overlay instead
        edit2.setVisibility(View.VISIBLE);

        Animation bounce = new TranslateAnimation(0, 0.0f, 0, 0.0f, Animation.RELATIVE_TO_SELF, -0.25f,
                Animation.RELATIVE_TO_SELF, 0f);

        bounce.setDuration(500);
        bounce.setStartOffset(0);
        bounce.setRepeatMode(Animation.REVERSE);
        bounce.setRepeatCount(4);
        bounce.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                edit2.setVisibility(View.INVISIBLE);
                background.setVisibility(View.GONE);

                Preferences.completeNotificationOnboarding(getContext());
            }
        });

        edit2.startAnimation(bounce);
    }

    @Override
    public void onDestroy() {
        //noinspection HardCodedStringLiteral
        Log.d(TAG, "onDestroy");

        mHandler.removeCallbacks(mTestVibration);
        Utilities.flushDelayed(mTrackSetting, mHandler);

        super.onDestroy();
    }

    ////
    //// private methods
    ////
    private void swipe(final View view, final NotificationType type, final int direction, final Color color,
                       boolean isPhysicalSwipe) {
        final ImageView swipe = (ImageView) view.findViewById(R.id.swipe);
        Animation slide = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, direction, 0, 0.0f, 0, 0.0f);

        slide.setDuration(400);
        swipe.startAnimation(slide);

        slide.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                update(view, type);
                trackSetting(type, isPhysicalSwipe ?
                    Mixpanel.NotificationMethod.PAN : Mixpanel.NotificationMethod.TAP);
                swipe.setTranslationX(0);
            }
        });

        mActivity.getPreferences()
                .setNotificationColor(type, color);

        mHandler.removeCallbacks(mTestVibration);

        RinglyService.doNotify(type, RinglyService.Mode.TEST_NOTIFICATION_COLOR,
                getActivity());

    }

    private void update(final View view, final NotificationType type) {
        //noinspection ResourceType
        view.findViewById(R.id.swipe).setBackgroundResource(
                mActivity.getPreferences().getNotificationColor(type).id);

        final ImageView dots = (ImageView) view.findViewById(R.id.vibration);
        dots.setImageResource(mActivity.getPreferences().getNotificationVibration(type).iconId);
    }

    private void trackSetting(final NotificationType type, Mixpanel.NotificationMethod method) {
        mChangedTypes.add(Pair.create(type, method));
        Utilities.delay(mTrackSetting, TRACK_DELAY, mHandler);
    }

    private final Runnable mTrackSetting = new Runnable() {
        @Override
        public void run() {
            for (final Pair<NotificationType, Mixpanel.NotificationMethod> type : mChangedTypes) {
                final Color color = mActivity.getPreferences().getNotificationColor(type.first);
                final Vibration vibration
                        = mActivity.getPreferences().getNotificationVibration(type.first);

                if (mActivity.getPreferences().getNotificationMode(type.first) != NotificationMode.DISABLED) {
                    mActivity.getMixpanel().track(Mixpanel.Event.CHANGED_NOTIFICATION,
                            Mixpanel.getNotificationProperties(type.first, color, vibration, type.second,
                                getActivity()));
                } else {
                    mActivity.getMixpanel().track(Mixpanel.Event.DISABLED_NOTIFICATION,
                            ImmutableMap.of(Mixpanel.Property.NAME, Mixpanel.capitalized(type.first)));
                }
            }
            mChangedTypes.clear();
        }
    };

    private final Runnable mTestVibration = new Runnable() {
        @Override
        public void run() {
            RinglyService.doNotify(mTestVibrationType, RinglyService.Mode.TEST_VIBRATION, getActivity());
        }
    };
}
