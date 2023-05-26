package com.asu.seawavesapp.data;

import com.google.gson.annotations.SerializedName;

import java.util.Date;

public class Voyage {
    @SerializedName("id")
    Long id;
    @SerializedName("boat")
    Long boatId;
    @SerializedName("started_at")
    Date startedAt;
    @SerializedName("ended_at")
    Date endedAt;
    @SerializedName("max_roll")
    Float maxRoll;
    @SerializedName("max_pitch")
    Float maxPitch;
    @SerializedName("max_speed")
    Float maxSpeed;
    @SerializedName("avg_speed")
    Float avgSpeed;

    public Voyage(Long boatId, Date startedAt, Date endedAt,
                  Float maxRoll, Float maxPitch, Float maxSpeed, Float avgSpeed) {
        this.boatId = boatId;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.maxRoll = maxRoll;
        this.maxPitch = maxPitch;
        this.maxSpeed = maxSpeed;
        this.avgSpeed = avgSpeed;
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

    public Date getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Date startedAt) {
        this.startedAt = startedAt;
    }

    public Date getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(Date endedAt) {
        this.endedAt = endedAt;
    }

    public Float getMaxRoll() {
        return maxRoll;
    }

    public void setMaxRoll(Float maxRoll) {
        this.maxRoll = maxRoll;
    }

    public Float getMaxPitch() {
        return maxPitch;
    }

    public void setMaxPitch(Float maxPitch) {
        this.maxPitch = maxPitch;
    }

    public Float getMaxSpeed() {
        return maxSpeed;
    }

    public void setMaxSpeed(Float maxSpeed) {
        this.maxSpeed = maxSpeed;
    }

    public Float getAvgSpeed() {
        return avgSpeed;
    }

    public void setAvgSpeed(Float avgSpeed) {
        this.avgSpeed = avgSpeed;
    }
}
