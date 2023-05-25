package com.asu.seawavesapp.api;

import com.asu.seawavesapp.data.Boat;
import com.asu.seawavesapp.data.Reading;
import com.asu.seawavesapp.data.Setting;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface RestApi {

    @Headers("Content-Type: application/json")
    @POST("boat/add")
    Call<Boat> addBoat(@Body Boat boat);

    @GET("settings")
    Call<Setting> getSettings();

    @GET("boat/detail")
    Call<Boat> getBoatDetail(@Query(value = "boatId", encoded = true) int boatId);

    @Headers("Content-Type: application/json")
    @POST("record/add")
    Call<Reading> addReading(@Body Reading reading);

    @GET("voyage/start")
    Call<Long> startVoyage(@Query(value = "boatId", encoded = true) Long boatId);

    @GET("voyage/stop")
    Call<Long> stopVoyage(@Query(value = "voyagePk", encoded = true) Long voyagePk);
}
