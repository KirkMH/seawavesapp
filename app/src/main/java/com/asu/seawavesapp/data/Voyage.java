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

    public Voyage(Long boatId, Date startedAt, Date endedAt) {
        this.boatId = boatId;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
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
}
