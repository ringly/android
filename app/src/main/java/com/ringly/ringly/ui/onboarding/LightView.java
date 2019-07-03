package com.ringly.ringly.ui.onboarding;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * Created by lindaliu on 1/20/16.
 */
public class LightView extends ImageView {

    Paint mPaint;
    Path mPath;
    float density;
    float width;
    float height;
    float offsetY;

    public LightView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mPaint = new Paint();
        mPath = new Path();
        density = getContext().getResources().getDisplayMetrics().density;
        offsetY = 0.8f;
    }

    @Override
    public void onSizeChanged (int w, int h, int oldw, int oldh){
        super.onSizeChanged(w, h, oldw, oldh);
        width = w;
        height = h;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        mPath.moveTo(0, height * offsetY);
        mPath.lineTo(50 * density, height * offsetY + 25 * density);
        mPath.lineTo(50 * density, height * offsetY - 25 * density);
        mPath.lineTo(0, height * offsetY);
        canvas.drawPath(mPath, mPaint);
    }
}
