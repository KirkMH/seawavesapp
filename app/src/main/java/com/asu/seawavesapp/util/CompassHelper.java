package com.asu.seawavesapp.util;

import android.hardware.GeomagneticField;

/**
 * CompassHelper calculates the heading angle more accurately,
 * taking the magnetic declination into account.
 * <p>
 * Source: https://talesofcode.com/developing-compass-android-application/
 */
public class CompassHelper {
    /**
     * Possible values: 0 ≤ ALPHA ≤ 1
     * Default: 0.15f
     * Note: Smaller ALPHA results in smoother sensor data but slower updates
     */
    public static final float ALPHA = 0.15f;

    /**
     * Calculates the heading angle based on the accelerometer and magnetometer readings.
     *
     * @param accelerometerReading - accelerometer reading
     * @param magnetometerReading  - magnetometer reading
     * @return heading angle in radians
     */
    public static float calculateHeading(float[] accelerometerReading, float[] magnetometerReading) {
        float Ax = accelerometerReading[0];
        float Ay = accelerometerReading[1];
        float Az = accelerometerReading[2];

        float Ex = magnetometerReading[0];
        float Ey = magnetometerReading[1];
        float Ez = magnetometerReading[2];

        //cross product of the magnetic field vector and the gravity vector
        float Hx = Ey * Az - Ez * Ay;
        float Hy = Ez * Ax - Ex * Az;
        float Hz = Ex * Ay - Ey * Ax;

        //normalize the values of resulting vector
        final float invH = 1.0f / (float) Math.sqrt(Hx * Hx + Hy * Hy + Hz * Hz);
        Hx *= invH;
        Hy *= invH;
        Hz *= invH;

        //normalize the values of gravity vector
        final float invA = 1.0f / (float) Math.sqrt(Ax * Ax + Ay * Ay + Az * Az);
        Ax *= invA;
        Ay *= invA;
        Az *= invA;

        //cross product of the gravity vector and the new vector H
        final float Mx = Ay * Hz - Az * Hy;
        final float My = Az * Hx - Ax * Hz;
        final float Mz = Ax * Hy - Ay * Hx;

        //arctangent to obtain heading in radians
        return (float) Math.atan2(Hy, My);
    }

    /**
     * Converts the radians to degrees.
     *
     * @param rad - measure in radians
     * @return - measure in degrees
     */
    public static float convertRadtoDeg(float rad) {
        return (float) (rad / Math.PI) * 180;
    }

    /**
     * Maps the angle from [-180,180] format to [0,360] format.
     *
     * @param angle - original angle
     * @return mapped angle
     */
    public static float map180to360(float angle) {
        return (angle + 360) % 360;
    }

    /**
     * Makes sensor readings smoother using a low pass filter.
     *
     * @param input  - sensor reading
     * @param output - previous reading/filtered result
     * @return filtered result
     */
    public static float[] lowPassFilter(float[] input, float[] output) {
        if (output == null) return input;

        for (int i = 0; i < input.length; i++) {
            output[i] = output[i] + ALPHA * (input[i] - output[i]);
        }
        return output;
    }

    /**
     * Calculates the magnetic declination using <code>GeomagneticField</code>.
     *
     * @param latitude  - degrees latitude
     * @param longitude - degrees longitude
     * @param altitude  - altitude in meters
     * @return magnetic declination
     */
    public static float calculateMagneticDeclination(double latitude, double longitude, double altitude) {
        // source: https://developer.android.com/reference/android/hardware/GeomagneticField.html
        GeomagneticField geoMag = new GeomagneticField(
                (float) latitude,
                (float) longitude,
                (float) altitude,
                System.currentTimeMillis());
        return geoMag.getDeclination();
    }
}
