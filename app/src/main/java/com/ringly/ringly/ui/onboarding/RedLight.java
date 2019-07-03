package com.ringly.ringly.ui.onboarding;

import android.content.Context;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.util.AttributeSet;

/**
 * Created by lindaliu on 1/27/16.
 */
public class RedLight extends LightView {

    public RedLight(Context context, AttributeSet attrs) {
        super(context, attrs);
        mPaint.setShader(new LinearGradient(0, 0.0f, 30 * density, 0.0f,
                0xff9d181f,
                0x009d181f,
                Shader.TileMode.CLAMP));

        offsetY = 0.62f;
    }
}
