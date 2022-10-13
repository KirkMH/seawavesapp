package com.asu.seawavesapp.data;

import com.asu.seawavesapp.util.Utility;
import com.google.gson.annotations.SerializedName;

import java.util.Date;

public class Reading {
    @SerializedName("id") Long id;
    @SerializedName("boat") Long boatId;
    @SerializedName("heading_angle") Float headingAngle;
    @SerializedName("pitch_angle") Float pitchAngle;
    @SerializedName("roll_angle") Float rollAngle;
    @SerializedName("gyro_x") Float gyroX;
    @SerializedName("gyro_y") Float gyroY;
    @SerializedName("gyro_z") Float gyroZ;
    @SerializedName("accel_x") Float accelX;
    @SerializedName("accel_y") Float accelY;
    @SerializedName("accel_z") Float accelZ;
    @SerializedName("mag_x") Float magX;
    @SerializedName("mag_y") Float magY;
    @SerializedName("mag_z") Float magZ;
    @SerializedName("latitude") Float latitude;
    @SerializedName("longitude") Float longitude;
    @SerializedName("altitude") Float altitude;
    @SerializedName("timestamp") Date timestamp;
    @SerializedName("sent_timestamp") String sent_timestamp;

    public Reading() {
        clear();
    }

    public Reading(Long boatId, Float headingAngle, Float pitchAngle, Float rollAngle, Float gyroX, Float gyroY, Float gyroZ, Float accelX, Float accelY, Float accelZ, Float magX, Float magY, Float magZ, Float latitude, Float longitude, Float altitude, Date timestamp, String sent) {
        this.boatId = boatId;
        this.headingAngle = headingAngle;
        this.pitchAngle = pitchAngle;
        this.rollAngle = rollAngle;
        this.gyroX = gyroX;
        this.gyroY = gyroY;
        this.gyroZ = gyroZ;
        this.accelX = accelX;
        this.accelY = accelY;
        this.accelZ = accelZ;
        this.magX = magX;
        this.magY = magY;
        this.magZ = magZ;
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitude = altitude;
        this.timestamp = timestamp;
        this.sent_timestamp = sent;
    }
    
    public Reading clone() {
        Reading rClone = new Reading();
        rClone.boatId = this.boatId;
        rClone.headingAngle = this.headingAngle;
        rClone.pitchAngle = this.pitchAngle;
        rClone.rollAngle = this.rollAngle;
        rClone.gyroX = this.gyroX;
        rClone.gyroY = this.gyroY;
        rClone.gyroZ = this.gyroZ;
        rClone.accelX = this.accelX;
        rClone.accelY = this.accelY;
        rClone.accelZ = this.accelZ;
        rClone.magX = this.magX;
        rClone.magY = this.magY;
        rClone.magZ = this.magZ;
        rClone.latitude = this.latitude;
        rClone.longitude = this.longitude;
        rClone.altitude = this.altitude;
        rClone.timestamp = this.timestamp;
        rClone.sent_timestamp = this.sent_timestamp;
        return rClone;
    }

    public Long getBoatId() {
        return boatId;
    }

    public Float getHeadingAngle() {
        return headingAngle;
    }

    public Float getPitchAngle() {
        return pitchAngle;
    }

    public Float getRollAngle() {
        return rollAngle;
    }

    public Float getGyroX() {
        return gyroX;
    }

    public Float getGyroY() {
        return gyroY;
    }

    public Float getGyroZ() {
        return gyroZ;
    }

    public Float getAccelX() {
        return accelX;
    }

    public Float getAccelY() {
        return accelY;
    }

    public Float getAccelZ() {
        return accelZ;
    }

    public Float getMagX() {
        return magX;
    }

    public Float getMagY() {
        return magY;
    }

    public Float getMagZ() {
        return magZ;
    }

    public Float getLatitude() {
        return latitude;
    }

    public Float getLongitude() {
        return longitude;
    }

    public Float getAltitude() {
        return altitude;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public String getSent_timestamp() {
        return sent_timestamp;
    }

    public void setBoatId(Long boatId) {
        this.boatId = boatId;
    }

    public void setHeadingAngle(Float headingAngle) {
        this.headingAngle = headingAngle;
    }

    public void setPitchAngle(Float pitchAngle) {
        this.pitchAngle = pitchAngle;
    }

    public void setRollAngle(Float rollAngle) {
        this.rollAngle = rollAngle;
    }

    public void setGyroX(Float gyroX) {
        this.gyroX = gyroX;
    }

    public void setGyroY(Float gyroY) {
        this.gyroY = gyroY;
    }

    public void setGyroZ(Float gyroZ) {
        this.gyroZ = gyroZ;
    }

    public void setAccelX(Float accelX) {
        this.accelX = accelX;
    }

    public void setAccelY(Float accelY) {
        this.accelY = accelY;
    }

    public void setAccelZ(Float accelZ) {
        this.accelZ = accelZ;
    }

    public void setMagX(Float magX) {
        this.magX = magX;
    }

    public void setMagY(Float magY) {
        this.magY = magY;
    }

    public void setMagZ(Float magZ) {
        this.magZ = magZ;
    }

    public void setLatitude(Float latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(Float longitude) {
        this.longitude = longitude;
    }

    public void setAltitude(Float altitude) {
        this.altitude = altitude;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public void setSent_timestamp(String sent_timestamp) {
        this.sent_timestamp = sent_timestamp;
    }

    public String getFormattedTimestamp() {
        return timestamp == null ? null : Utility.formatTimestamp(timestamp);
    }

    public String getFormattedTimestampForServer() {
        return timestamp == null ? null : Utility.formatTimestampForServer(timestamp);
    }
    
    public void clear() {
        this.boatId = null;
        this.headingAngle = null;
        this.pitchAngle = null;
        this.rollAngle = null;
        this.gyroX = null;
        this.gyroY = null;
        this.gyroZ = null;
        this.accelX = null;
        this.accelY = null;
        this.accelZ = null;
        this.magX = null;
        this.magY = null;
        this.magZ = null;
        this.latitude = null;
        this.longitude = null;
        this.altitude = null;
        this.timestamp = null;
        this.sent_timestamp = null;
    }

    /**
     * Will only consider valid if the heading angle and the latitude are available
     * (assumes the longitude is also if latitude is available)
     * @return
     */
    public boolean isValid() {
        return ((headingAngle != null && headingAngle > 0) &&
                (latitude != null && latitude > 0));
    }

    @Override
    public String toString() {
        return headingAngle +
                "," + pitchAngle +
                "," + rollAngle +
                "," + gyroX +
                "," + gyroY +
                "," + gyroZ +
                "," + accelX +
                "," + accelY +
                "," + accelZ +
                "," + magX +
                "," + magY +
                "," + magZ +
                "," + latitude +
                "," + longitude +
                "," + altitude;
    }
}
