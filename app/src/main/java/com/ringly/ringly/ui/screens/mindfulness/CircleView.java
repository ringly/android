package com.ringly.ringly.ui.screens.mindfulness;


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import com.ringly.ringly.ui.Utilities;

/**
 * Created by Monica on 5/23/2017.
 */

public class CircleView extends View {

    private final Paint paint;
    private float animatedFraction = 0;

    public CircleView(Context context) {
        super(context);
        paint = new Paint();
    }

    public CircleView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        paint = new Paint();
    }

    public CircleView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        paint = new Paint();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int x = getWidth();
        int y = getHeight();
        int radius = (int)(Utilities.convertDpToPixel(137, getContext())/2);
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.STROKE);
        paint.setAntiAlias(true);
        float widthDp = 8;
        if (animatedFraction != 0) {
            widthDp = widthDp / (animatedFraction + 1);
        }
        float strokeWidth = Utilities.convertDpToPixel(widthDp, getContext());
        paint.setStrokeWidth(strokeWidth);
        canvas.drawCircle((x/2), (y/2), radius-(strokeWidth/2), paint);
    }

    public void setAnimatedFraction(float value) {
        animatedFraction = value;
    }
}
