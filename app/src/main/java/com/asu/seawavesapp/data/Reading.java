package com.asu.seawavesapp.data;

import androidx.annotation.NonNull;

import com.asu.seawavesapp.util.Utility;
import com.google.gson.annotations.SerializedName;

import java.util.Date;

/**
 * The Reading class represents a record from the Reading table in the database.
 * This class is also used to link to the server's API.
 */
public class Reading {
    @SerializedName("id")
    Long id;
    @SerializedName("boat")
    Long boatId;
    @SerializedName("voyage")
    Long voyageId;
    @SerializedName("heading_angle")
    Float headingAngle;
    @SerializedName("pitch_angle")
    Float pitchAngle;
    @SerializedName("roll_angle")
    Float rollAngle;
    @SerializedName("gyro_x")
    Float gyroX;
    @SerializedName("gyro_y")
    Float gyroY;
    @SerializedName("gyro_z")
    Float gyroZ;
    @SerializedName("accel_x")
    Float accelX;
    @SerializedName("accel_y")
    Float accelY;
    @SerializedName("accel_z")
    Float accelZ;
    @SerializedName("mag_x")
    Float magX;
    @SerializedName("mag_y")
    Float magY;
    @SerializedName("mag_z")
    Float magZ;
    @SerializedName("latitude")
    Float latitude;
    @SerializedName("longitude")
    Float longitude;
    @SerializedName("altitude")
    Float altitude;
    @SerializedName("signalStrength")
    Integer signalStrength;
    @SerializedName("speed")
    Float speed;
    @SerializedName("timestamp")
    Date timestamp;
    @SerializedName("sent_timestamp")
    String sent_timestamp;

    /**
     * Creates an instance of the Reading class with null values.
     */
    public Reading() {
        clear();
    }

    /**
     * Creates an instance of the Reading class with given values.
     *
     * @param boatId       - Boat's ID
     * @param headingAngle - Heading angle
     * @param pitchAngle   - Pitch angle
     * @param rollAngle    - Roll angle
     * @param gyroX        - Gyroscope reading in the X-axis
     * @param gyroY        - Gyroscope reading in the Y-axis
     * @param gyroZ        - Gyroscope reading in the Z-axis
     * @param accelX       - Accelerometer reading in the X-axis
     * @param accelY       - Accelerometer reading in the Y-axis
     * @param accelZ       - Accelerometer reading in the Z-axis
     * @param magX         - Magnetometer reading in the X-axis
     * @param magY         - Magnetometer reading in the Y-axis
     * @param magZ         - Magnetometer reading in the Z-axis
     * @param latitude     - Latitude, in degrees
     * @param longitude    - Longitude, in degrees
     * @param altitude     - Altitude, in meters
     * @param timestamp    - Timestamp of the reading
     * @param sent         - Date/time when the reading is sent to the server
     * @param voyageId     - Id of this voyage
     * @param signalStrength         - Signal strength level
     * @param speed         - Speed in m/s
     */
    public Reading(Long boatId, Float headingAngle, Float pitchAngle, Float rollAngle, Float gyroX,
                   Float gyroY, Float gyroZ, Float accelX, Float accelY, Float accelZ,
                   Float magX, Float magY, Float magZ, Float latitude, Float longitude, Float altitude,
                   Date timestamp, String sent, Long voyageId, Integer signalStrength, Float speed) {
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
        this.voyageId = voyageId;
        this.signalStrength = signalStrength;
        this.speed = speed;
    }

    /**
     * Returns a clone of this reading.
     *
     * @return clone
     */
    @NonNull
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
        rClone.voyageId = this.voyageId;
        rClone.signalStrength = this.signalStrength;
        rClone.speed = speed;
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

    public Date getSentTimestampDate() {
        return Utility.timestampToDate(sent_timestamp);
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

    /**
     * Returns the timestamp in yyyy-MM-dd HH:mm:ss format.
     *
     * @return formatted timestamp
     */
    public String getFormattedTimestamp() {
        return timestamp == null ? null : Utility.formatTimestamp(timestamp);
    }

    /**
     * Returns the timestamp in yyyy-MM-dd'T'HH:mm:ss'Z' (SQL format).
     *
     * @return formatted timestamp in SQL format
     */
    public String getFormattedTimestampForServer() {
        return timestamp == null ? null : Utility.formatTimestampForServer(timestamp);
    }

    public Long getVoyageId() {
        return voyageId;
    }

    public void setVoyageId(Long voyageId) {
        this.voyageId = voyageId;
    }

    public Integer getSignalStrength() {
        return signalStrength;
    }

    public void setSignalStrength(Integer signalStrength) {
        this.signalStrength = signalStrength;
    }

    public Float getSpeed() {
        return speed;
    }

    public void setSpeed(Float speed) {
        this.speed = speed;
    }

    /**
     * Clears the values of this reading.
     */
    public void clear() {
        this.boatId = null;
        this.voyageId = null;
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
        this.signalStrength = null;
        this.speed = null;
        this.timestamp = null;
        this.sent_timestamp = null;
    }

    /**
     * Will only consider valid if the heading angle is available.
     *
     * @return <code>true</code> if the reading is valid; <code>false</code> otherwise
     */
    public boolean isValid() {
        return (headingAngle != null);
    }

    @NonNull
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
                "," + altitude +
                "," + voyageId +
                "," + signalStrength +
                "," + speed;
    }
}
