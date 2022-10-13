package com.asu.seawavesapp.service;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.telephony.SmsManager;

public class SmsService {
    private Activity context;

    public SmsService(Activity context) {
        this.context = context;
    }

    public void send(String phoneNumber, String message) {
        SmsManager mngr = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) ?
                context.getSystemService(SmsManager.class) :
                SmsManager.getDefault();
        mngr.sendTextMessage(phoneNumber, null, message, null, null);

    }
}
