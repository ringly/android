package com.ringly.ringly.ui.screens.preferences;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.preference.PreferenceDialogFragmentCompat;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioGroup;

import com.ringly.ringly.R;
import com.ringly.ringly.config.model.Weight;

import static com.ringly.ringly.config.model.Weight.WeightUnit.KG;
import static com.ringly.ringly.config.model.Weight.WeightUnit.LB;

public class WeightPreferenceDialogFragmentCompat extends PreferenceDialogFragmentCompat {
    private static final String TAG = WeightPreferenceDialogFragmentCompat.class.getSimpleName();

    private static final int DEFAULT_WEIGHT_KG = 75;
    private static final int DEFAULT_WEIGHT_LB = 170;

    private Weight mWeight;
    private boolean mCleared;
    private EditText mValue;
    private RadioGroup mRg;

    public static WeightPreferenceDialogFragmentCompat newInstance(String key) {
        final WeightPreferenceDialogFragmentCompat
            fragment = new WeightPreferenceDialogFragmentCompat();
        final Bundle b = new Bundle(1);
        b.putString(ARG_KEY, key);
        fragment.setArguments(b);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            mWeight = ((WeightPreference) getPreference()).getWeight();
        }

        if (mWeight == null) {
            mWeight = new Weight(DEFAULT_WEIGHT_LB, LB);
        } else if(mWeight.value == -1) {
            mWeight = new Weight(mWeight.unit == LB ? DEFAULT_WEIGHT_LB : DEFAULT_WEIGHT_KG,
                mWeight.unit);
        }

        mCleared = false;
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        mValue = (EditText) view.findViewById(R.id.weight_picker);
        mValue.setText(String.valueOf(mWeight.value));

        mRg = (RadioGroup) view.findViewById(R.id.weight_unit_picker);
        mRg.check(mWeight.unit == LB ? R.id.unit_lb : R.id.unit_kg);
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
        Weight.WeightUnit unit = mRg.getCheckedRadioButtonId() == R.id.unit_lb ? LB : KG;
        if(!(positiveResult || mCleared)) {
            return;
        } else if(mCleared) {
            mWeight = new Weight(-1, unit);
        } else {
            float weight;

            try {
                weight = Float.valueOf(mValue.getText().toString());
            } catch (NumberFormatException e) {
                Log.e(TAG, "onDialogClosed: NumberFormatException", e);
                weight = mWeight.unit == LB ? DEFAULT_WEIGHT_LB : DEFAULT_WEIGHT_KG;
            }

            mWeight = new Weight(weight, unit);
        }

        if (getWeightPreference().callChangeListener(mWeight)) {
            getWeightPreference().setWeight(mWeight);
        }
    }

    private WeightPreference getWeightPreference() {
        return (WeightPreference) getPreference();
    }
}
