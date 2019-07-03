package com.ringly.ringly.ui.dfu;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.View;

import com.ringly.ringly.R;

public class FailureFragment extends DialogFragment {
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final DfuActivity activity = (DfuActivity) getActivity();
        final View view = activity.getLayoutInflater().inflate(R.layout.fragment_dfu_failed, null);
        activity.onCreateView(view);

        view.findViewById(R.id.proceed).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.onFailed();
            }
        });

        return new AlertDialog.Builder(getActivity()).setView(view).create();
    }
}
