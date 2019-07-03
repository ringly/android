package com.ringly.ringly.ui;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.ringly.ringly.NotificationListener;
import com.ringly.ringly.Preferences;

public final class SetupHelper {
    public static boolean isLoggedIn(Context context) {
        return Preferences.getAuthToken(context).isPresent();
    }

    public static boolean ringlyConnected(Context context) {
        return Preferences.getRingAddress(context).isPresent();
    }

    public static boolean canReadPhoneState(Context context){
        return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) ==
                PackageManager.PERMISSION_GRANTED;
    }

    public static boolean canReceiveSms(Context context){
        return ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) ==
                PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) ==
                PackageManager.PERMISSION_GRANTED;
    }

    public static boolean canReceiveMms(Context context){
        return ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_MMS) ==
                PackageManager.PERMISSION_GRANTED;
    }

    public static boolean notificationsEnabled(Context context) {
        return NotificationListener.isEnabled(context);
    }

    public static boolean canNotify(Context context) {
        return notificationsEnabled(context) && canReadPhoneState(context) && canReceiveSms(context) && canReceiveMms(context);
    }

    public static boolean isOnboarded(Context context) {
        return Preferences.isOnboarded(context);
    }

    public static boolean hasActivityUpdate(String firmware) {
        String[] fw = firmware.split("\\.");
        try {
            float[] fwf = new float[fw.length];
            for (int i = 0; i < fw.length; i++) {
                fwf[i] = Float.parseFloat(fw[i]);
            }

            return fwf[0] >= 2 && fwf[1] >= 2;
        } catch (NumberFormatException e) {
            Log.e(SetupHelper.class.getCanonicalName(), "updateView: Couldn't parse firmware version", e);
            return false;
        }
    }

    public static boolean needsActivityUpdate(String firmware) {
        String[] fw = firmware.split("\\.");
        try {
            float[] fwf = new float[fw.length];
            for (int i = 0; i < fw.length; i++) {
                fwf[i] = Float.parseFloat(fw[i]);
            }

            return fwf[0] == 2 && fwf[1] < 2;
        } catch (NumberFormatException e) {
            Log.e(SetupHelper.class.getCanonicalName(), "updateView: Couldn't parse firmware version", e);
            return false;
        }
    }
}
