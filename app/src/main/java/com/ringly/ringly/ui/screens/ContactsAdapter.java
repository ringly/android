package com.ringly.ringly.ui.screens;

import android.animation.ObjectAnimator;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.util.Pair;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.ringly.ringly.R;
import com.ringly.ringly.bluetooth.RinglyService;
import com.ringly.ringly.config.Color;
import com.ringly.ringly.config.Mixpanel;
import com.ringly.ringly.config.NotificationType;
import com.ringly.ringly.ui.MainActivity;
import com.ringly.ringly.ui.Utilities;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public final class ContactsAdapter extends RecyclerView.Adapter<ContactsAdapter.ContactHolder> {

    @SuppressWarnings("StaticVariableNamingConvention")
    private static final Color[] COLORS = Color.values();
    private static final int TRACK_DELAY = 5000; // milliseconds

    private static final int CONTACT_TYPE = 1;
    private static final int HEADER_TYPE = 2;


    // this class is only public because generics force it to be:
    @SuppressWarnings({"InstanceVariableNamingConvention", "PublicField"})
    public static final class ContactHolder extends RecyclerView.ViewHolder {
        public final TextView text;
        public final ImageView delete;

        public ContactHolder(final View itemView) {
            super(itemView);

            text = (TextView) itemView.findViewById(R.id.name);
            delete = (ImageView) itemView.findViewById(R.id.delete);
        }
    }


    // note that binarySearch doesn't use equals() or hashCode(), so we don't implement them
    @SuppressWarnings({"InstanceVariableNamingConvention", "PublicField"})
    private static final class Contact implements Comparable<Contact> {
        public final String key;
        public final String name;

        public Contact(final String key, final String name) {
            this.key = key;
            this.name = name;
        }

        @SuppressWarnings({"ParameterNameDiffersFromOverriddenParameter", "UnnecessaryThis"})
        @Override
        public int compareTo(@NonNull final Contact that) {
            final int keyDiff = this.key.compareTo(that.key);
            if (keyDiff == 0) return 0; // equal keys may have different names, but are still equal

            // otherwise, sort first by name, then break ties by key:
            final int nameDiff = this.name.compareToIgnoreCase(that.name);
            if (nameDiff != 0) return nameDiff;
            return keyDiff;
        }

        @SuppressWarnings("UnnecessaryThis")
        @Override
        public boolean equals(final Object o) {
            if (!(o instanceof Contact)) return false;
            final Contact that = (Contact) o;

            return this.key.equals(that.key);
        }

        @Override
        public int hashCode() {
            return key.hashCode();
        }
    }


    private final MainActivity mActivity;

    private final List<Contact> mContacts = Lists.newArrayList();
    private final Set<Pair<String, Mixpanel.NotificationMethod>> mChangedContacts = Sets.newHashSet();
    private final Handler mHandler = new Handler();


    public ContactsAdapter(final MainActivity activity) {
        mActivity = activity;
    }

    public int add(final String key, final String name) {
        return add(new Contact(key, name));
    }

    private int add(final Contact contact) {
        int i = Collections.binarySearch(mContacts, contact);
        if (i < 0) {
            i = -i - 1;
            mContacts.add(i, contact);
            notifyItemInserted(i + 1); // + 1 to account for header at beginning
        }
        return i;
    }

    private void remove(final Contact contact) {
        final int i = Collections.binarySearch(mContacts, contact);
        if (i >= 0) {
            mContacts.remove(i);
            notifyItemRemoved(i + 1); // + 1 to account for header at beginning
        }
    }

    public void flush() {
        Utilities.flushDelayed(mSettingTracker, mHandler);
    }


    //
    // Adapter methods
    //

    @Override
    public int getItemCount() {
        return mContacts.size() + 1; // contacts plus header
    }

    @Override
    public int getItemViewType(final int position) {
        if (position > 0) return CONTACT_TYPE;
        else return HEADER_TYPE;
    }

    @Override
    public ContactHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
        // TODO onCreateView is always used after inflate(), so combine them
        final View view = LayoutInflater.from(parent.getContext())
                .inflate(viewType == CONTACT_TYPE
                        ? R.layout.listitem_contacts : R.layout.header_contacts, parent, false);
        mActivity.onCreateView(view);

        if (viewType == HEADER_TYPE) { // there is only one header, so we just set it up once

            final CheckBox box1 = (CheckBox) view.findViewById(R.id.checkbox1);
            final CheckBox box2 = (CheckBox) view.findViewById(R.id.checkbox2);
            final LinearLayout setting1 = (LinearLayout) view.findViewById(R.id.setting1);
            final LinearLayout setting2 = (LinearLayout) view.findViewById(R.id.setting2);

            boolean innerRingSet = mActivity.getPreferences().getInnerRing();

            box1.setChecked(!innerRingSet);
            box2.setChecked(innerRingSet);

            box1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                    box2.setChecked(!isChecked);
                    mActivity.getPreferences().setInnerRing(!isChecked);

                    mActivity.getMixpanel().trackSetting(R.string.inner_ring);
                }
            });

            box2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                    box1.setChecked(!isChecked);
                    mActivity.getPreferences().setInnerRing(isChecked);

                    mActivity.getMixpanel().trackSetting(R.string.inner_ring);
                }
            });

            setting1.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            box1.setChecked(!box1.isChecked());
                        }
                    }
            );

            setting2.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            box2.setChecked(!box2.isChecked());
                        }
                    }
            );
        }

        return new ContactHolder(view);
    }

    @Override
    public void onBindViewHolder(final ContactHolder holder, final int position) {
        if (getItemViewType(position) != CONTACT_TYPE) return;

        final Contact contact = mContacts.get(position - 1); // - 1 because of header
        final ImageView swipe = (ImageView) holder.itemView.findViewById(R.id.swipe);

        Utilities.uppercaseAndKern(holder.text, contact.name);

        update(holder.itemView, contact.key);

        holder.itemView.setOnTouchListener(new View.OnTouchListener() {
            View view = holder.itemView;
            final float den = view.getResources().getDisplayMetrics().density;
            final float SWIPE_START_THRESHOLD = 30;
            final float SWIPE_FINISH_THRESHOLD = 60;
            final float TAP_THRESHOLD = 4 * 2;

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
                        i = mActivity.getPreferences().getContactColor(contact.key).or(Color.NONE).ordinal();
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
                            view.setBackgroundColor(view.getContext().getResources().getColor(color.id));
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
                            swipe(view, contact.key, 1, color, true);
                        } else if (totalDeltaX < -1 * SWIPE_FINISH_THRESHOLD && lastDeltaX <= 0) { // R->L swipe
                            swipe(view, contact.key, -1, color, true);
                        } else if (Math.abs(totalDeltaX) < TAP_THRESHOLD) { // tap
                            swipe.setTranslationX(0);
                            color = COLORS[(i + 1) % COLORS.length];
                            view.setBackgroundColor(view.getContext().getResources().getColor(color.id));
                            swipe(view, contact.key, -1, color, false);
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

        holder.delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                remove(contact);
                mActivity.getPreferences().removeContact(contact.key);
            }
        });
    }


    ////
    //// private methods
    ////

    private void swipe(final View view, final String key, final int direction, final Color color,
                       final boolean isPhysicalSwipe) {
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
                update(view, key);
                swipe.setTranslationX(0);
            }
        });

        mActivity.getPreferences()
                .setContactColor(key, color);

        mChangedContacts.add(Pair.create(key, isPhysicalSwipe ?
            Mixpanel.NotificationMethod.PAN : Mixpanel.NotificationMethod.TAP));
        Utilities.delay(mSettingTracker, TRACK_DELAY, mHandler);

        RinglyService.doNotify(NotificationType.PHONE_CALL,
                Optional.of(key), RinglyService.Mode.TEST_CONTACT_COLOR,
                view.getContext());

    }

    private void update(final View view, final String contact) {
        //noinspection ResourceType
        view.findViewById(R.id.swipe).setBackgroundResource(
                mActivity.getPreferences().getContactColor(contact).or(Color.NONE).id);
    }

    private final Runnable mSettingTracker = new Runnable() {
        @Override
        public void run() {
            mActivity.getMixpanel().updateContactSuperProperties();

            for (final Pair<String, Mixpanel.NotificationMethod> contact : mChangedContacts) {
                final Optional<Color> color = mActivity.getPreferences().getContactColor(contact.first);

                if (color.isPresent()) {
                    mActivity.getMixpanel().track(Mixpanel.Event.CHANGED_CONTACT, ImmutableMap.of(
                            Mixpanel.Property.COLOR, Mixpanel.capitalized(color.get()),
                            Mixpanel.Property.METHOD, contact.second
                    ));
                } else {
                    mActivity.getMixpanel().track(Mixpanel.Event.DISABLED_CONTACT);
                }
            }
            mChangedContacts.clear();
        }
    };

}
