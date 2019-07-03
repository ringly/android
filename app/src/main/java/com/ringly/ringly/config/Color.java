package com.ringly.ringly.config;

import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;

import com.ringly.ringly.R;

@SuppressWarnings({"InstanceVariableNamingConvention", "MagicNumber"})
public enum Color {
    // DO NOT CHANGE these names, they are persisted in the user preferences
    BLUE(R.color.blue, android.graphics.Color.BLUE, new int[]{
            android.graphics.Color.rgb(107, 187, 243),
            android.graphics.Color.rgb(126, 196, 244),
            android.graphics.Color.rgb(144, 204, 246),
            android.graphics.Color.rgb(163, 213, 247),
            android.graphics.Color.rgb(181, 221, 249)
    }),
    GREEN(R.color.green, android.graphics.Color.GREEN, new int[]{
            android.graphics.Color.rgb(107, 235, 195),
            android.graphics.Color.rgb(126, 237, 203),
            android.graphics.Color.rgb(144, 240, 210),
            android.graphics.Color.rgb(163, 243, 218),
            android.graphics.Color.rgb(181, 245, 225)
    }),
    YELLOW(R.color.yellow, android.graphics.Color.rgb(35, 155, 0), new int[]{
            android.graphics.Color.rgb(251, 223, 163),
            android.graphics.Color.rgb(251, 227, 175),
            android.graphics.Color.rgb(252, 231, 186),
            android.graphics.Color.rgb(252, 235, 198),
            android.graphics.Color.rgb(253, 239, 209)
    }),
    RED(R.color.red, android.graphics.Color.RED, new int[]{
            android.graphics.Color.rgb(255, 155, 155),
            android.graphics.Color.rgb(255, 168, 168),
            android.graphics.Color.rgb(255, 180, 180),
            android.graphics.Color.rgb(255, 193, 193),
            android.graphics.Color.rgb(255, 205, 205)
    }),
    PURPLE(R.color.purple, android.graphics.Color.rgb(191, 0, 255), new int[]{
            android.graphics.Color.rgb(217, 162, 219),
            android.graphics.Color.rgb(221, 174, 223),
            android.graphics.Color.rgb(226, 185, 228),
            android.graphics.Color.rgb(231, 197, 232),
            android.graphics.Color.rgb(236, 209, 237)
    }),
    NONE(android.R.color.black, android.graphics.Color.BLACK, new int[]{
            android.graphics.Color.rgb(123, 115, 110),
            android.graphics.Color.rgb(140, 133, 128),
            android.graphics.Color.rgb(156, 150, 146),
            android.graphics.Color.rgb(173, 168, 165),
            android.graphics.Color.rgb(189, 185, 182)
    }),
    ;

    @ColorRes public final int id;
    @ColorInt public final int ledColor;
    public final int[] gradientColors;
    Color(@ColorRes final int id, @ColorInt final int ledColor, final int[] gradientColors) {
        this.id = id;
        this.ledColor = ledColor;
        this.gradientColors = gradientColors;
    }
}
