package com.asu.seawavesapp.service;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.widget.TextView;

import com.asu.seawavesapp.R;
import com.asu.seawavesapp.data.Reading;

public class Alert {
    // alert levels
    public static final int NORMAL = 0;
    public static final int YELLOW = 1;
    public static final int ORANGE = 2;
    public static final int RED = 3;

    private Activity context;
    private MediaPlayer mediaPlayer;
    private String boatName;
    private static float pitchAngleAlert = 0f;
    private static float rollAngleAlert = 0f;
    private int status = Alert.NORMAL;
    private TextView tvStatus;
    private int lastPlayed = 0;

    public Alert(Activity context, String boatName, float pitchAngleAlert, float rollAngleAlert) {
        this.context = context;
        this.boatName = boatName;
        this.pitchAngleAlert = pitchAngleAlert;
        this.rollAngleAlert = rollAngleAlert;
        tvStatus = context.findViewById(R.id.tvStatus);
    }

    public static int checkReadingForAlert(Reading reading) {
        if (reading == null || reading.getPitchAngle() == null) return Alert.NORMAL;

        float pitch = Math.abs(reading.getPitchAngle());
        float roll = Math.abs(reading.getRollAngle());
        int rStatus;

        if (pitch >= pitchAngleAlert || roll >= rollAngleAlert) {
            rStatus = Alert.RED;
        }
        // 10% before critical
        else if (pitch >= (pitchAngleAlert * 0.9) || roll >= (rollAngleAlert * 0.9)) {
            rStatus = Alert.ORANGE;
        }
        // 20% before critical
        else if (pitch >= (pitchAngleAlert * .8) || roll >= (rollAngleAlert * 0.8)) {
            rStatus = Alert.YELLOW;
        }
        else {
            rStatus = Alert.NORMAL;
        }

        return rStatus;
    }

    public int check(Reading reading) {
        status = checkReadingForAlert(reading);
        return status;
    }

    public void render() {
        tvStatus.setText(getStatus());
        tvStatus.setBackground(getColor());
        if (status == Alert.YELLOW) {
            tvStatus.setTextColor(Color.DKGRAY);
        } else {
            tvStatus.setTextColor(Color.WHITE);
        }

        int toPlay = getSoundId();
        if (mediaPlayer != null && (toPlay == 0 || lastPlayed != toPlay)) {
            stopAlert();
        }
        if (mediaPlayer == null && toPlay != 0) {
            playAlert(toPlay);
        }
        lastPlayed = toPlay;
    }

    private Drawable getColor() {
        switch (status) {
            case Alert.YELLOW:
                return context.getDrawable(R.drawable.rounded_rect_y);
            case Alert.ORANGE:
                return context.getDrawable(R.drawable.rounded_rect_o);
            case Alert.RED:
                return context.getDrawable(R.drawable.rounded_rect_r);
            default:
                return context.getDrawable(R.drawable.rounded_rect);
        }
    }

    private String getStatus() {
        switch (status) {
            case Alert.YELLOW:
                return "Yellow Alert";
            case Alert.ORANGE:
                return "Orange Alert";
            case Alert.RED:
                return "Red Alert";
            default:
                return "Normal";
        }
    }

    public String getMessage() {
        return getMessage("");
    }

    public String getMessage(String extraInfo) {
        String message = "";
        if (status != Alert.NORMAL) {
            message = boatName + " is in " + getStatus().toUpperCase() + ". " + extraInfo;
        }
        return message;
    }

    private int getSoundId() {
        switch (status) {
            case Alert.YELLOW:
                return R.raw.beepsingle;
            case Alert.ORANGE:
                return R.raw.beepdouble;
            case Alert.RED:
                return R.raw.beeplong;
            default:
                return 0;
        }
    }

    public void playAlert(int sound) {
        if (sound == 0) return;

        mediaPlayer = MediaPlayer.create(context, sound);
        if (mediaPlayer != null) {
            mediaPlayer.setLooping(true);
            mediaPlayer.start();
        }
    }

    public void stopAlert() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.reset();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}
