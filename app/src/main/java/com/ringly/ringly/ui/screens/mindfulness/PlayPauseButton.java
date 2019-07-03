package com.ringly.ringly.ui.screens.mindfulness;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import com.ringly.ringly.R;
import com.ringly.ringly.ui.Utilities;


/**
 * Created by Monica on 5/23/2017.
 */

public class PlayPauseButton extends ProgressView {

    private Bitmap playBitmap;
    private Bitmap pauseBitmap;
    private boolean playing = false;
    private int percentageProgress;

    public PlayPauseButton(Context context) {
        super(context);
        init();
    }

    public PlayPauseButton(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PlayPauseButton(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        rimColor = getResources().getColor(R.color.light_light_gray);
        barColor = getResources().getColor(R.color.dark_blue);
        barWidth = 10;
        playBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.audio_play);
        pauseBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.audio_pause);
    }



    //----------------------------------
    //Animation stuff
    //----------------------------------

    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //Draw the inner circle
        if (isEnabled()) {
            if (!playing) {
                canvas.drawBitmap(playBitmap, getWidth() / 2 - playBitmap.getWidth() / 2, getHeight() / 2 - playBitmap.getHeight() / 2 + padding / 2, barPaint);
            } else {
                canvas.drawBitmap(pauseBitmap, getWidth() / 2 - pauseBitmap.getWidth() / 2, getHeight() / 2 - pauseBitmap.getHeight() / 2 + padding / 2, barPaint);
            }
        } else {
            float progressX = getWidth()/2-textPaint.measureText(percentageProgress+"%")/2+padding/2;
            Rect bounds = new Rect();
            textPaint.getTextBounds("9", 0, 1, bounds);
            float progressY = getHeight()/2+bounds.height()/2+padding/2;
            canvas.drawText(percentageProgress+"%", progressX, progressY, textPaint);
        }
    }


    public void pause() {
        playing = false;
        postInvalidate();
    }

    public void play() {
        playing = true;
        postInvalidate();
    }

    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        postInvalidate();
    }

    public void setPercentageProgress(int percentage) {
        this.percentageProgress = percentage;
        this.progress = percentage *360/100;
        postInvalidate();
    }
}
