package com.asu.seawavesapp.service;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.widget.TextView;

import androidx.appcompat.content.res.AppCompatResources;

import com.asu.seawavesapp.R;
import com.asu.seawavesapp.data.Reading;

/**
 * Alert class is used to check for alerts, displaying appropriate color,
 * and playing appropriate sound.
 */
public class Alert {
    // alert levels
    public static final int NORMAL = 0;
    public static final int YELLOW = 1;
    public static final int ORANGE = 2;
    public static final int RED = 3;

    private final Activity context;
    private final String boatName;
    private MediaPlayer mediaPlayer;
    private int status = Alert.NORMAL;
    private int lastPlayed = 0;
    private static float pitchAngleAlert = 0f;
    private static float rollAngleAlert = 0f;

    /**
     * Creates an instance of the Alert class.
     *
     * @param context         - application context
     * @param boatName        - boat's name
     * @param pitchAngleAlert - pitch angle's critical level
     * @param rollAngleAlert  - roll angle's critical level
     */
    public Alert(Activity context, String boatName, float pitchAngleAlert, float rollAngleAlert) {
        this.context = context;
        this.boatName = boatName;
        Alert.pitchAngleAlert = pitchAngleAlert;
        Alert.rollAngleAlert = rollAngleAlert;
    }

    /**
     * Checks the reading whether it will have an alert or not.
     *
     * @param reading - the reading to check
     * @return alert level
     */
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
        } else {
            rStatus = Alert.NORMAL;
        }

        return rStatus;
    }

    /**
     * Checks the reading whether it will have an alert or not.
     *
     * @param reading - the reading to check
     * @return alert level
     */
    public int check(Reading reading) {
        status = checkReadingForAlert(reading);
        return status;
    }

    /**
     * Renders the alert by setting the status' text and the background color,
     * and plays the sound alarm.
     */
    public void render(TextView tvStatus) {
        tvStatus.setText(getStatus());
        tvStatus.setBackground(getColor());
        // set an appropriate text color
        if (status == Alert.YELLOW) {
            tvStatus.setTextColor(Color.DKGRAY);
        } else {
            tvStatus.setTextColor(Color.WHITE);
        }

        // play/stop the sound alert
        int toPlay = getSoundId();
        // when the player is running, and either there should be no alert
        // or it gets a new type of alert, stop the player
        if (mediaPlayer != null && (toPlay == 0 || lastPlayed != toPlay)) {
            stopAlert();
        }
        // if there should be an alert, play the sound
        if (mediaPlayer == null && toPlay != 0) {
            playAlert(toPlay);
        }
        // keep track of the last status
        lastPlayed = toPlay;
    }

    /**
     * Returns the background for the status depending on the alert level.
     *
     * @return background color
     */
    private Drawable getColor() {
        switch (status) {
            case Alert.YELLOW:
                return AppCompatResources.getDrawable(context, R.drawable.rounded_rect_y);
            case Alert.ORANGE:
                return AppCompatResources.getDrawable(context, R.drawable.rounded_rect_o);
            case Alert.RED:
                return AppCompatResources.getDrawable(context, R.drawable.rounded_rect_r);
            default:
                return AppCompatResources.getDrawable(context, R.drawable.rounded_rect);
        }
    }

    /**
     * Returns the string representation of the status.
     *
     * @return - status
     */
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

    /**
     * Returns a message status of the boat.
     *
     * @param extraInfo - any other information to be added to the message
     * @return message status
     */
    public String getMessage(String extraInfo) {
        String message = "";
        if (status != Alert.NORMAL) {
            message = boatName + " is in " + getStatus().toUpperCase() + ". " + extraInfo;
        }
        return message;
    }

    /**
     * Returns the sound to be played based on the status.
     *
     * @return sound resource
     */
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

    /**
     * Plays the specified sound resource.
     *
     * @param sound - sound resource
     */
    public void playAlert(int sound) {
        if (sound == 0) return;

        mediaPlayer = MediaPlayer.create(context, sound);
        if (mediaPlayer != null) {
            mediaPlayer.setLooping(true);
            mediaPlayer.start();
        }
    }

    /**
     * Stops playing the sound.
     */
    public void stopAlert() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.reset();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}
