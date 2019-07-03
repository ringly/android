package com.ringly.ringly;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import com.google.common.base.Optional;
import com.ringly.ringly.bluetooth.RinglyService;
import com.ringly.ringly.config.NotificationType;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Set;

public final class TelephonyReceiver extends BroadcastReceiver {
    private static final String TAG = TelephonyReceiver.class.getCanonicalName();

    private static final String SMS_RECEIVED_ACTION = "android.provider.Telephony.SMS_RECEIVED";
    private static final String MMS_RECEIVED_ACTION = "android.provider.Telephony.WAP_PUSH_RECEIVED";
    private static final String MMS_DATA_TYPE = "application/vnd.wap.mms-message";
    private static final String PDUS_EXTRA = "pdus";

    private static final String NON_INCOMING_MMS = "non incoming mms";
    @Override
    public void onReceive(final Context context, final Intent intent) {
        //noinspection HardCodedStringLiteral
        Log.d(TAG, "onReceive: " + intent);
        String action = intent.getAction();
        String type = intent.getType();

        if (TelephonyManager.ACTION_PHONE_STATE_CHANGED.equals(action)) {
            onReceivePhoneCall(intent, context);
        } else if (SMS_RECEIVED_ACTION.equals(action)) {
            onReceiveTextMessage(intent, context);
        } else if (MMS_RECEIVED_ACTION.equals(action) && type.equals(MMS_DATA_TYPE)) {
            onReceiveMultimediaMessage(intent, context);
        } else {
            //noinspection HardCodedStringLiteral
            Log.w(TAG, "unknown action: " + action);
        }
    }


    private static void onReceivePhoneCall(final Intent intent, final Context context) {
        if (!TelephonyManager.EXTRA_STATE_RINGING
                .equals(intent.getStringExtra(TelephonyManager.EXTRA_STATE))) {
            return;
        }

        final Optional<String> phoneNumber
                = Optional.fromNullable(intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER));
        if (!phoneNumber.isPresent()) {
            //noinspection HardCodedStringLiteral
            Log.w(TAG, "no number for phone call " + intent);
        }

        RinglyService.doNotify(NotificationType.PHONE_CALL, phoneNumber, context);
    }

    private static void onReceiveTextMessage(final Intent intent, final Context context) {
        Optional<String> phoneNumber = Optional.absent();
        // 'pdus' is an Object[] with elements of type byte[], but not necessarily a byte[][]...
        final Object pdus = intent.getExtras().get(PDUS_EXTRA);
        if (pdus instanceof Object[]) {
            for (final Object pdu : (Object[]) pdus) {
                if (pdu instanceof byte[]) {
                    final SmsMessage sms = SmsMessage.createFromPdu((byte[]) pdu);
                    if (sms != null) {
                        phoneNumber = Optional.fromNullable(sms.getOriginatingAddress());
                        if (phoneNumber.isPresent()) break; // first number is good enough

                        //noinspection HardCodedStringLiteral
                        Log.w(TAG, "null sms address");
                    } else {
                        //noinspection HardCodedStringLiteral
                        Log.w(TAG, "null sms");
                    }
                } else {
                    //noinspection HardCodedStringLiteral
                    Log.w(TAG, "pdu is not an array: " + pdu);
                }
            }
        } else {
            //noinspection HardCodedStringLiteral
            Log.w(TAG, "'pdus' extra is not an array: " + pdus);
        }

        RinglyService.doNotify(NotificationType.TEXT_MESSAGE, phoneNumber, context);
    }

    private void onReceiveMultimediaMessage(final Intent intent, final Context context) {
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                String mmsId = getLastId(context);
                String incomingNumber = getAddressNumber(mmsId, context);
                if (incomingNumber != null && !incomingNumber.equals(NON_INCOMING_MMS)) {
                    RinglyService.doNotify(NotificationType.TEXT_MESSAGE, Optional.fromNullable(incomingNumber), context);
                }
            }
        }, 10000);

    }

    /**
     *This method query in the database the phone number for the mms id
     *@return  the addressnumber if found the id and the type == 137 incomming mms
     *         empty if not found the id
     *         NON_INCOMING_MMS if found the id but the type is not 137
     */
    private String getAddressNumber(String id, Context context) {
        String uriStr = MessageFormat.format("content://mms/{0}/addr", id);
        Uri uriAddress = Uri.parse(uriStr);
        String[] columns = { "address, type" };
        Cursor cursor = context.getContentResolver().query(uriAddress, columns,
                null, null, null);
        String address = "";
        String val, type;
        if (cursor != null && cursor.moveToFirst()) {
            do {
                val = cursor.getString(cursor.getColumnIndex("address"));
                type = cursor.getString(cursor.getColumnIndex("type"));
                if (val != null) {
                    if (type != null && type.equals("137")) { //type=137 is the type for incoming mms
                        address = val;
                        break;
                    } else {
                        address = NON_INCOMING_MMS;
                    }
                }
            } while (cursor.moveToNext());
        }
        if (cursor != null) {
            cursor.close();
        }
        return address;
    }

    private  String getLastId(Context context) {
        Cursor query = context.getContentResolver ().query(Uri.parse("content://mms/inbox"), null, null, null, null);
        String lastId = "";
        if (query!= null && query.moveToFirst() && query.getColumnIndex("_id")!= -1) {
            lastId = query.getString(query.getColumnIndex("_id"));
        }
        return lastId;
    }
}
