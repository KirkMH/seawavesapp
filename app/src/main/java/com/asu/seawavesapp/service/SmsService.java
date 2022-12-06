package com.asu.seawavesapp.service;

import android.Manifest;
import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.telephony.SmsManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.preference.PreferenceManager;

import com.asu.seawavesapp.R;

import java.util.List;

public class SmsService {
    private final int REQUEST_CODE_PHONE_STATE = 301;
    private final Activity context;
    private boolean errorShown = false;
    private SmsManager manager;
    private SmsManager sim1;
    private SmsManager sim2;
    private String phoneNumber;
    private String message;

    public SmsService(Activity context) {
        this.context = context;
    }

    private void sendSMS() {
        if (this.phoneNumber == null || this.message == null) return;

        if (this.manager != null)
            this.manager.sendTextMessage(phoneNumber, null, message, null, null);
        else if (this.sim1 != null)
            this.sim1.sendTextMessage(phoneNumber, null, message, null, null);
        else if (this.sim2 != null)
            this.sim2.sendTextMessage(phoneNumber, null, message, null, null);
        else if (!errorShown) {
            Toast.makeText(context, "Cannot send SMS at this time.", Toast.LENGTH_SHORT).show();
            errorShown = true;
        }
    }

    public void send(String phoneNumber, String message) {
        this.phoneNumber = phoneNumber;
        this.message = message;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            sendUsingSim1OrSim2();
        } else {
            sendUsingDefaultSim();
        }
    }

    private void sendUsingDefaultSim() {
        this.manager = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) ?
                context.getSystemService(SmsManager.class) :
                SmsManager.getDefault();
        sendSMS();
    }

    private void sendUsingSim1OrSim2() {
        SubscriptionManager localSubscriptionManager = SubscriptionManager.from(context);
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                context.requestPermissions(new String[] {Manifest.permission.READ_PHONE_STATE}, REQUEST_CODE_PHONE_STATE);
            }
            return;
        }
        Log.i("SimSelection", "SIM COUNT: " + localSubscriptionManager.getActiveSubscriptionInfoCount());
        if (localSubscriptionManager.getActiveSubscriptionInfoCount() > 1) {
            List localList = localSubscriptionManager.getActiveSubscriptionInfoList();

            Log.i("SimSelection", localList.toString());

            SubscriptionInfo simInfo1 = (SubscriptionInfo) localList.get(0);
            this.sim1 = SmsManager.getSmsManagerForSubscriptionId(simInfo1.getSubscriptionId());
            SubscriptionInfo simInfo2;
            if (localList.size() > 1) {
                simInfo2 = (SubscriptionInfo) localList.get(1);
                this.sim2 = SmsManager.getSmsManagerForSubscriptionId(simInfo2.getSubscriptionId());
            }

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            String sim = prefs.getString(context.getString(R.string.sim_key), "Default");

            Log.i("SimSelection", sim);

            if (sim.equals("SIM1")) {
                //SendSMS From SIM One
                this.manager = this.sim1;
                sendSMS();
            }
            else if (sim.equals("SIM2")) {
                //SendSMS From SIM Two
                this.manager = this.sim2;
            }
            else {
                sendUsingDefaultSim();
            }
        }
        else
            sendUsingDefaultSim();
    }
}
