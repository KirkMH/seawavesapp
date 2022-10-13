package com.asu.seawavesapp.api;

import com.asu.seawavesapp.data.Boat;
import com.asu.seawavesapp.data.Reading;
import com.asu.seawavesapp.data.Setting;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface RestApi {

    @Headers("Content-Type: application/json")
    @POST("boat/add")
    Call<Boat> addBoat(@Body Boat boat);

    @GET("settings")
    Call<Setting> getSettings();

    @Headers("Content-Type: application/json")
    @POST("record/add")
    Call<Reading> addReading(@Body Reading reading);

}
