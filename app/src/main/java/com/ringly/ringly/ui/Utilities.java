package com.ringly.ringly.ui;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Handler;
import android.support.annotation.StringRes;
import android.text.Editable;
import android.text.InputFilter;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Patterns;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Set;

import rx.functions.Action0;
import rx.functions.Func1;

public final class Utilities {
    private Utilities() {}

    private static final String NON_BREAKING_SPACE = "\u00A0";


    public static boolean isAppInstalled(final Set<String> ids, final Context context) {
        for (final String id : ids) {
            if (Utilities.isAppInstalled(id, context)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isAppInstalled(final String id, final Context context) {
        try {
            return context.getPackageManager().getApplicationEnabledSetting(id)
                    != PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
        } catch (final IllegalArgumentException ignored) { // unknown id
            return false;
        }
    }


    public static void uppercaseAndKern(final TextView view) {
        uppercaseAndKern(view, view.getText());
    }

    public static void uppercaseAndKern(final TextView view, @StringRes final int stringId) {
        uppercaseAndKern(view, view.getContext().getResources().getString(stringId));
    }

    public static void uppercaseAndKern(final TextView view, CharSequence text) {
        view.setText(uppercaseAndKern(text));
    }

    /*
     * Uppercase text and interleave with spaces, preserving any spans.
     */
    public static Spanned uppercaseAndKern(final CharSequence text) {
        final SpannableStringBuilder s = new SpannableStringBuilder();
        // uppercase:
        s.setFilters(new InputFilter[]{new InputFilter.AllCaps()});
        s.append(text); // copies spans
        // kern:
        for (int i = 1; i < s.length(); i += 2) s.insert(i, NON_BREAKING_SPACE);
        return s;
    }

    public static void delay(final Runnable runnable, final int delay, final Handler handler) {
        handler.removeCallbacks(runnable);
        handler.postDelayed(runnable, delay);
    }

    public static void flushDelayed(final Runnable runnable, final Handler handler) {
        handler.removeCallbacks(runnable);
        runnable.run();
    }

    /**
     * Creates a TextWatcher that calls an action if a filter condition is met.
     */
    public static TextWatcher createErrorChecker(final Func1<CharSequence, Boolean> filter,
                                                 final Action0 action) {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if(filter.call(s)) {
                    action.call();
                }
            }
        };
    }

    /**
     * Requests focus for the EditText and opens the keyboard
     */
    public static void requestEditTextFocus(Context context, EditText e){
        e.post(() -> {
            e.requestFocus();
            ((InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE))
                .showSoftInput(e, 0);
        });
    }

    public static boolean isValidEmail(CharSequence target) {
        return !TextUtils.isEmpty(target) && Patterns.EMAIL_ADDRESS.matcher(target).matches();
    }

    public static void setVisibility(View v, boolean visible) {
        v.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    /**
     * Truncate a cal at the specified calendar date. This doesn't support weeks.
     * @param cal
     * @param field
     * @return
     */
    public static Calendar truncateCal(Calendar cal, int field) {
        Calendar calp = (Calendar) cal.clone();
        calp.set(Calendar.MILLISECOND, 0);
        calp.set(Calendar.SECOND, 0);

        if(field == Calendar.MINUTE) {
            return calp;
        }

        calp.set(Calendar.MINUTE, 0);

        if(field == Calendar.HOUR_OF_DAY || field == Calendar.HOUR) {
            return calp;
        }

        calp.set(Calendar.HOUR_OF_DAY, 0);

        if(field == Calendar.DAY_OF_MONTH) {
            return calp;
        }

        calp.set(Calendar.DAY_OF_MONTH, 0);

        return calp;
    }

    /**
     * Format seconds in 0:00
     * @param seconds
     * @return
     */
    public static String formatSeconds(int seconds) {
        SimpleDateFormat sf = new SimpleDateFormat("m:ss");
        Date date = new Date(seconds*1000);
        return sf.format(date);
    }

    /**
     * Convert dp To pixels
     * @param dp
     * @param context
     * @return
     */
    public static float convertDpToPixel(float dp, Context context){
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        float px = dp * ((float)metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
        return px;
    }
}
