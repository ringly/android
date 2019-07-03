package com.ringly.ringly.ui.screens.activity;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.ringly.ringly.R;

/**
 * StepsProgress shows a bar that takes up a percent of its parent. It was created to show steps
 * out of a number of steps.
 */
public class StepsProgress extends View {
    private static final String TAG = StepsProgress.class.getSimpleName();

    private float mProgress;
    private Paint mBarPaint;
    private Rect mFullBounds;
    private RectF mBounds;

    public StepsProgress(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.getTheme().obtainStyledAttributes(
            attrs,
            R.styleable.StepsProgress,
            0, 0
        );

        int barColor = ContextCompat.getColor(context, R.color.purple);
        try {
            mProgress = a.getFloat(R.styleable.StepsProgress_progress, 0);
            barColor = a.getColor(R.styleable.StepsProgress_barColor, barColor);
        } finally {
            a.recycle();
        }

        mBarPaint = new Paint(0);
        mBarPaint.setColor(barColor);

        mFullBounds = new Rect(0, 0, 0, 0);
        mBounds = new RectF(mFullBounds);

        setWillNotDraw(false);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int desiredWidth = 100;
        int desiredHeight = 100;

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int width;
        int height;

        //Measure Width
        if (widthMode == MeasureSpec.EXACTLY) {
            //Must be this size
            width = widthSize;
        } else if (widthMode == MeasureSpec.AT_MOST) {
            //Can't be bigger than...
            width = Math.min(desiredWidth, widthSize);
        } else {
            //Be whatever you want
            width = desiredWidth;
        }

        //Measure Height
        if (heightMode == MeasureSpec.EXACTLY) {
            //Must be this size
            height = heightSize;
        } else if (heightMode == MeasureSpec.AT_MOST) {
            //Can't be bigger than...
            height = Math.min(desiredHeight, heightSize);
        } else {
            //Be whatever you want
            height = desiredHeight;
        }

        mFullBounds.right = width;
        mFullBounds.bottom = height;
        mBounds.right = width;
        mBounds.bottom = height;

        //MUST CALL THIS
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Log.d(TAG, "onDraw: Draw progress" + mProgress);

        mBounds.right = mFullBounds.right * mProgress;

        float roundness = mBounds.bottom * 0.5f;
        canvas.drawRoundRect(mBounds, roundness, roundness, mBarPaint);
    }

    public void setProgress(float progress) {
        Log.d(TAG, "setProgress: " + progress);
        mProgress = progress;
        invalidate();
        requestLayout();
    }
}

