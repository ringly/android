package com.ringly.ringly.ui.screens.preferences;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.preference.PreferenceDialogFragmentCompat;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioGroup;

import com.ringly.ringly.R;
import com.ringly.ringly.config.model.Height;
import com.ringly.ringly.ui.Utilities;

import static com.ringly.ringly.config.model.Height.HeightUnit.CM;
import static com.ringly.ringly.config.model.Height.HeightUnit.IN;

public class HeightPreferenceDialogFragmentCompat extends PreferenceDialogFragmentCompat {
    private static final String TAG = HeightPreferenceDialogFragmentCompat.class.getSimpleName();

    private static final int DEFAULT_HEIGHT_CM = 165;
    private static final int DEFAULT_HEIGHT_IN = 65;

    private Height mHeight;
    private boolean mCleared;
    private EditText mCm;
    private EditText mIn;
    private EditText mFt;

    public static HeightPreferenceDialogFragmentCompat newInstance(String key) {
        final HeightPreferenceDialogFragmentCompat
            fragment = new HeightPreferenceDialogFragmentCompat();
        final Bundle b = new Bundle(1);
        b.putString(ARG_KEY, key);
        fragment.setArguments(b);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            mHeight = ((HeightPreference) getPreference()).getHeight();
        }

        if (mHeight == null) {
            mHeight = new Height(DEFAULT_HEIGHT_IN, IN);
        } else if (mHeight.value == -1) {
            mHeight = new Height(mHeight.unit == IN ? DEFAULT_HEIGHT_IN : DEFAULT_HEIGHT_CM,
                mHeight.unit);
        }

        mCleared = false;
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        final View imperial = view.findViewById(R.id.imperial);
        final View metric = view.findViewById(R.id.metric);

        mCm = (EditText) view.findViewById(R.id.edit_text_cm);
        mIn = (EditText) view.findViewById(R.id.edit_text_in);
        mFt = (EditText) view.findViewById(R.id.edit_text_ft);

        setTexts();
        toggleInput(imperial, metric);

        RadioGroup rg = (RadioGroup) view.findViewById(R.id.height_unit_picker);
        rg.check(mHeight.unit == IN ? R.id.unit_in : R.id.unit_cm);
        rg.setOnCheckedChangeListener((g, id) -> {
            switch (id) {
                case R.id.unit_in:
                    mHeight = new Height(DEFAULT_HEIGHT_IN, IN);
                    break;
                case R.id.unit_cm:
                    mHeight = new Height(DEFAULT_HEIGHT_CM, CM);
                    break;
            }

            setTexts();
            toggleInput(imperial, metric);
        });
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        super.onClick(dialog, which);

        if (which == DialogInterface.BUTTON_NEGATIVE) {
            mCleared = true;
        }
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        if (!(positiveResult || mCleared)) {
            return;
        } else if(mCleared) {
            mHeight = new Height(-1, mHeight.unit);
        } else {
            int height;
            if (mHeight.unit == IN) {
                try {
                    height = Integer.valueOf(mIn.getText().toString());
                    height += 12 * Integer.valueOf(mFt.getText().toString());
                } catch (NumberFormatException e) {
                    Log.e(TAG, "onDialogClosed: NumberFormatException", e);
                    height = DEFAULT_HEIGHT_IN;
                }
            } else {
                try {
                    height = Integer.valueOf(mCm.getText().toString());
                } catch (Exception e) {
                    Log.e(TAG, "onDialogClosed: NumberFormatException", e);
                    height = DEFAULT_HEIGHT_CM;
                }
            }

            mHeight = new Height(height, mHeight.unit);
        }

        if (getHeightPreference().callChangeListener(mHeight)) {
            getHeightPreference().setHeight(mHeight);
        }
    }

    private HeightPreference getHeightPreference() {
        return (HeightPreference) getPreference();
    }

    private void toggleInput(View imperial, View metric) {
        boolean isMetric = CM.equals(mHeight.unit);
        Utilities.setVisibility(metric, isMetric);
        Utilities.setVisibility(imperial, !isMetric);
    }

    private void setTexts() {
        mCm.setText(String.valueOf(mHeight.value));
        mIn.setText(String.valueOf(mHeight.value % 12));
        mFt.setText(String.valueOf(mHeight.value / 12));
    }
}
