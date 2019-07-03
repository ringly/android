package com.ringly.ringly.ui.screens.mindfulness;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.media.Image;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.ringly.ringly.R;
import com.ringly.ringly.config.model.GuidedMeditation;

/**
 * Created by Monica on 6/12/2017.
 */

public class MindfulnessRow extends LinearLayout {
    private TextView titleTV;
    private TextView typeTV;
    private TextView minutesTV;
    private ImageView rowIV;

    private GuidedMeditation guidedMeditation;

    public MindfulnessRow(Context context) {
        super(context, null, R.style.AppTheme);
        init();
    }

    public MindfulnessRow(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
        initAttr(attrs);
    }

    public MindfulnessRow(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
        initAttr(attrs);
    }


    private void init() {
        inflate(getContext(), R.layout.row_mindfulness, this);
        titleTV = (TextView) findViewById(R.id.txtTitle);
        typeTV = (TextView) findViewById(R.id.txtType);
        minutesTV = (TextView) findViewById(R.id.txtMinutes);
        rowIV = (ImageView) findViewById(R.id.imgRow);
    }

    private void initAttr(AttributeSet attrs) {
        TypedArray ta = getContext().obtainStyledAttributes(attrs, R.styleable.MindfulnessRow, 0, 0);
        try {
            String title = ta.getString(R.styleable.MindfulnessRow_title);
            titleTV.setText(title);
            String type = ta.getString(R.styleable.MindfulnessRow_type);
            typeTV.setText(type);
            String minutes = ta.getString(R.styleable.MindfulnessRow_minutes);
            minutesTV.setText(minutes);

        } finally {
            ta.recycle();
        }
    }

    public void setTitle(String title) {
        titleTV.setText(title);
    }

    public void setType(String type) {
        typeTV.setText(type);
    }

    public void setMinutes(String minutes) {
        minutesTV.setText(minutes);
    }

    public void setImage(Bitmap bitmap) {
        rowIV.setImageBitmap(bitmap);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
    }

    public void setGuidedMeditations(GuidedMeditation guidedMeditation) {
        this.guidedMeditation = guidedMeditation;
        setTitle(guidedMeditation.title);
        setMinutes(guidedMeditation.lengthSeconds/60 + " " + getResources().getString(R.string.minutes));
        setType(guidedMeditation.subtitle);
    }

    public GuidedMeditation getGuidedMeditation() {
        return this.guidedMeditation;
    }

    public ImageView getRowIV() {
        return this.rowIV;
    }
}
