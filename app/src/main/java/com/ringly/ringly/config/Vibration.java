package com.ringly.ringly.config;

import android.support.annotation.DrawableRes;

import com.ringly.ringly.R;

@SuppressWarnings("InstanceVariableNamingConvention")
public enum Vibration {
    // DO NOT CHANGE these names, they are persisted in the user preferences
    NONE(0, R.drawable.vibration_0),
    ONE(1, R.drawable.vibration_1),
    TWO(2, R.drawable.vibration_2),
    THREE(3, R.drawable.vibration_3),
    FOUR(4, R.drawable.vibration_4),
    ;

    public final int count;
    @DrawableRes public final int iconId;
    Vibration(final int count, @DrawableRes final int iconId) {
        this.count = count;
        this.iconId = iconId;
    }
}
