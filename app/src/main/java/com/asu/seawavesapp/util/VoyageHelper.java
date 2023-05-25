package com.asu.seawavesapp.util;

import android.util.Log;

import com.asu.seawavesapp.api.ApiClient;
import com.asu.seawavesapp.api.RestApi;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VoyageHelper {
    private final RestApi restApi;
    private final Long boatId;
    private Long voyagePk = null;
    private boolean isOnline;

    public VoyageHelper(Long boatId) {
        this.boatId = boatId;
        restApi = ApiClient.getApi();
    }

    public void startVoyage() {
        Call<Long> call = restApi.startVoyage(boatId);

        try {
            call.enqueue(new Callback<Long>() {
                @Override
                public void onResponse(Call<Long> call, Response<Long> response) {
                    Log.v("voyage", "online");
                    setOnline(true);
                    voyagePk = response.body();
                }

                @Override
                public void onFailure(Call<Long> call, Throwable t) {
                    setOnline(false);
                    Log.v("voyage", "offline");
                    call.cancel();
                }
            });
        } catch (Exception e) {
            Log.v("voyage", "Error: " + e.getLocalizedMessage());
            setOnline(false);
            call.cancel();
        }
    }

    /**
     * Stops the voyage and returns {@code true} if successful.
     * @return
     */
    public void stopVoyage(Long voyageId) {
        Call<Long> call = restApi.stopVoyage(voyageId);

        try {
            call.enqueue(new Callback<Long>() {
                @Override
                public void onResponse(Call<Long> call, Response<Long> response) {
                    Log.v("voyage", "online");
                    setOnline(true);
                    voyagePk = response.body();
                }

                @Override
                public void onFailure(Call<Long> call, Throwable t) {
                    setOnline(false);
                    Log.v("voyage", "offline");
                    call.cancel();
                }
            });
        } catch (Exception e) {
            Log.v("voyage", "Error: " + e.getLocalizedMessage());
            setOnline(false);
            call.cancel();
        }
    }

    public Long getBoatId() {
        return boatId;
    }

    public boolean isOnline() {
        return isOnline;
    }

    public void setOnline(boolean online) {
        isOnline = online;
    }

    public Long getVoyagePk() {
        return voyagePk;
    }

    public void setVoyagePk(Long voyagePk) {
        this.voyagePk = voyagePk;
    }
}
