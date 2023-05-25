package com.asu.seawavesapp.data;

import com.google.gson.annotations.SerializedName;

public class LocalReadingAndError {
    @SerializedName("id")
    Long id;
    @SerializedName("boat")
    Long boatId;
    @SerializedName("readings")
    String readings;
    @SerializedName("errors")
    String errors;

    public LocalReadingAndError(Long boatId, String readings, String errors) {
        this.boatId = boatId;
        this.readings = readings;
        this.errors = errors;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getBoatId() {
        return boatId;
    }

    public void setBoatId(Long boatId) {
        this.boatId = boatId;
    }

    public String getReadings() {
        return readings;
    }

    public void setReadings(String readings) {
        this.readings = readings;
    }

    public String getErrors() {
        return errors;
    }

    public void setErrors(String errors) {
        this.errors = errors;
    }
}
