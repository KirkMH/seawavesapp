package com.asu.seawavesapp.data;

import com.google.gson.annotations.SerializedName;

public class Setting {
    @SerializedName("critical_pitch_angle") public Double criticalPitchAngle;
    @SerializedName("critical_roll_angle") public Double criticalRollAngle;
    @SerializedName("reading_rate") public Long readingRate;
    @SerializedName("saving_rate") public Long savingRate;
    @SerializedName("sms_rate") public Long smsRate;
    @SerializedName("post_rate") public Long postRate;
    @SerializedName("mobile_number") public String  mobileNumber;
}
