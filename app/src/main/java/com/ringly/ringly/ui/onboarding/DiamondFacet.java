package com.ringly.ringly.ui.onboarding;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.ringly.ringly.R;

/**
 * Created by lindaliu on 1/22/16.
 */
public class DiamondFacet extends ImageView {
    Paint mPaint;
    Path mPath;
    float p1_x, p1_y,
            p2_x, p2_y,
            p3_x, p3_y;

    float density;

    public DiamondFacet(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.DiamondFacet,
                0, 0);

        try {
            p1_x = a.getFloat(R.styleable.DiamondFacet_p1_x, 0.0f);
            p1_y = a.getFloat(R.styleable.DiamondFacet_p1_y, 0.0f);
            p2_x = a.getFloat(R.styleable.DiamondFacet_p2_x, 0.0f);
            p2_y = a.getFloat(R.styleable.DiamondFacet_p2_y, 0.0f);
            p3_x = a.getFloat(R.styleable.DiamondFacet_p3_x, 0.0f);
            p3_y = a.getFloat(R.styleable.DiamondFacet_p3_y, 0.0f);
        } finally {
            a.recycle();
        }

        mPaint = new Paint();
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        mPaint.setColor(Color.WHITE);
        mPath = new Path();

        density = getContext().getResources().getDisplayMetrics().density;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        mPath.moveTo(p1_x * 0.8f * density, p1_y * 0.6f * density);
        mPath.lineTo(p2_x * 0.8f * density, p2_y * 0.6f * density);
        mPath.lineTo(p3_x * 0.8f * density, p3_y * 0.6f * density);
        mPath.lineTo(p1_x * 0.8f * density, p1_y * 0.6f * density);
        canvas.drawPath(mPath, mPaint);
    }
}
