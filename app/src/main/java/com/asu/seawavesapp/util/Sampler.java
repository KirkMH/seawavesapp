package com.asu.seawavesapp.util;

import android.util.Log;

import com.asu.seawavesapp.data.Reading;

public class Sampler {
    private ReadingSampler postSampler;
    private ReadingSampler saveSampler;
    private ReadingSampler smsSampler;

    public Sampler() {
        postSampler = new ReadingSampler();
        saveSampler = new ReadingSampler();
        smsSampler = new ReadingSampler();
    }

    public void add(Reading sample) {
        if (sample.isValid()) {
            postSampler.add(sample);
            saveSampler.add(sample);
            smsSampler.add(sample);
        }
    }

    public Reading getReadingForPost() {
        return postSampler.getReading();
    }

    public Reading getReadingForSave() {
        return saveSampler.getReading();
    }

    public Reading getReadingForSMS() {
        return smsSampler.getReading();
    }
}


class ReadingSampler {
    private Reading reading;

    public ReadingSampler() {
        reading = new Reading();
    }

    public void add(Reading sample) {
        Reading data = sample.clone();
        // reading is not set yet; copy entire contents
        if (reading == null || reading.getHeadingAngle() == null)
            reading = data;
        else {
            // will maintain the latest reading for the heading, latitude, longitude, altitude, and timestamp
            reading.setHeadingAngle(data.getHeadingAngle());
            reading.setLatitude(data.getLatitude());
            reading.setLongitude(data.getLongitude());
            reading.setAltitude(data.getAltitude());
            reading.setTimestamp(data.getTimestamp());
            reading.setSent_timestamp(data.getSent_timestamp());
            // store the maximum value for the rest of the readings
            reading.setRollAngle(max(reading.getRollAngle(), data.getRollAngle()));
            reading.setPitchAngle(max(reading.getPitchAngle(), data.getPitchAngle()));
            reading.setAccelX(max(reading.getAccelX(), data.getAccelX()));
            reading.setAccelY(max(reading.getAccelY(), data.getAccelY()));
            reading.setAccelZ(max(reading.getAccelZ(), data.getAccelZ()));
            reading.setGyroX(max(reading.getGyroX(), data.getGyroX()));
            reading.setGyroY(max(reading.getGyroY(), data.getGyroY()));
            reading.setGyroZ(max(reading.getGyroZ(), data.getGyroZ()));
            reading.setMagX(max(reading.getMagX(), data.getMagX()));
            reading.setMagY(max(reading.getMagX(), data.getMagY()));
            reading.setMagZ(max(reading.getMagX(), data.getMagZ()));
        }
//        Log.v("sw_reading", reading.toString());
    }

    /**
     * Determines the relative maximum value between two measures,
     * disregarding the sign. However, the actual value 
     * (including the sign) will be returned.
     * @param x
     * @param y
     * @return The maximum between two values, disregarding the sign during comparison, but returns the actual value.
     */
    private float max(float x, float y) {
        float x_abs = Math.abs(x);
        float y_abs = Math.abs(y);
        float max = Float.max(x_abs, y_abs);
        return (max == x_abs) ? x : y;
    }

    public Reading getReading() {
        Reading cReading = reading.clone();
        reading.clear();
        return cReading;
    }
}