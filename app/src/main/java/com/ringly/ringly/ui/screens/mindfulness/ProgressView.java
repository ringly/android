package com.ringly.ringly.ui.screens.mindfulness;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import com.ringly.ringly.R;
import com.ringly.ringly.ui.Utilities;

/**
 * Created by Monica on 6/20/2017.
 */

public class ProgressView extends View {
    //Paints
    protected Paint barPaint = new Paint();
    protected Paint circlePaint = new Paint();
    protected Paint rimPaint = new Paint();
    protected Paint textPaint = new Paint();

    //Rectangles
    protected RectF innerCircleBounds = new RectF();
    protected String text;
    protected int barWidth = 7;
    private int barWidthPixels;

    //Colors (with defaults)
    protected int barColor = getResources().getColor(R.color.blue);
    protected int circleColor = Color.WHITE;
    protected int rimColor = getResources().getColor(R.color.light_blue);
    protected float progress = 0;
    protected int padding;

    public ProgressView(Context context) {
        super(context);
    }

    public ProgressView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public ProgressView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * Use onSizeChanged instead of onAttachedToWindow to get the dimensions of the view,
     * because this method is called after measuring the dimensions of MATCH_PARENT & WRAP_CONTENT.
     * Use this dimensions to setup the bounds and paints.
     */
    @Override
    protected void onSizeChanged(int newWidth, int newHeight, int oldWidth, int oldHeight) {
        super.onSizeChanged(newWidth, newHeight, oldWidth, oldHeight);
        setupPaints();
        setupBounds();
        invalidate();
    }


    /**
     * Set the properties of the paints we're using to
     * draw the progress wheel
     */
    private void setupPaints() {
        barWidthPixels = (int) Utilities.convertDpToPixel(barWidth, getContext());
        barPaint.setColor(barColor);
        barPaint.setAntiAlias(true);
        barPaint.setStyle(Paint.Style.STROKE);
        barPaint.setStrokeWidth(barWidthPixels);

        rimPaint.setColor(rimColor);
        rimPaint.setAntiAlias(true);
        rimPaint.setStyle(Paint.Style.STROKE);
        rimPaint.setStrokeWidth(barWidthPixels);

        circlePaint.setColor(circleColor);
        circlePaint.setAntiAlias(true);
        circlePaint.setStyle(Paint.Style.FILL);

        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(Utilities.convertDpToPixel(30, getContext()));
    }

    /**
     * Set the bounds of the component
     */
    private void setupBounds() {
        padding = (int)Utilities.convertDpToPixel(7, getContext());
        int width = getWidth()-padding;

        innerCircleBounds = new RectF(barWidthPixels, barWidthPixels,
                width ,
                width);

    }


    //----------------------------------
    //Animation stuff
    //----------------------------------

    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //Draw the inner circle
        canvas.drawArc(innerCircleBounds, 360, 360, false, circlePaint);
        //Draw the rim
        canvas.drawArc(innerCircleBounds, 360, 360, false, rimPaint);
        //Draw the bar
        canvas.drawArc(innerCircleBounds, -90, progress, false, barPaint);

    }

    /**
     * Increment the progress by 1 (of 360)
     */
    public void incrementProgress() {
        incrementProgress(1);
    }

    public void incrementProgress(float amount) {
        progress += amount;
        if (progress > 360)
            progress = 360;
        postInvalidate();
    }


    /**
     * Set the progress to a specific value
     */
    public void setProgress(int i) {
        progress = i;
        postInvalidate();
    }

}
