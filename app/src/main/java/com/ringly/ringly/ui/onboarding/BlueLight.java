package com.ringly.ringly.ui.onboarding;

import android.content.Context;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.util.AttributeSet;

/**
 * Created by lindaliu on 1/27/16.
 */
public class BlueLight extends LightView {

    public BlueLight(Context context, AttributeSet attrs) {
        super(context, attrs);
        mPaint.setShader(new LinearGradient(0, 0.0f, 30 * density, 0.0f,
                0xff617DD2,
                0x00617DD2,
                Shader.TileMode.CLAMP));

        offsetY = 0.81f;
    }
}
