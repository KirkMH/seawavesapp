package com.asu.seawavesapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.asu.seawavesapp.api.ApiClient;
import com.asu.seawavesapp.api.RestApi;
import com.asu.seawavesapp.data.Setting;
import com.asu.seawavesapp.databinding.ActivitySplashBinding;
import com.google.android.material.snackbar.Snackbar;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class SplashActivity extends AppCompatActivity {
    private ActivitySplashBinding binding;
    private RestApi restApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        getSupportActionBar().hide();

        restApi = ApiClient.getApi();
        getSetting();
    }

    @Override
    protected void onResume() {
        super.onResume();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                verifySavedPreference();
            }
        }, 2000);
    }

    private void verifySavedPreference() {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        try {
            String id = pref.getString(getResources().getString(R.string.id_key), "");
            Float critical = Float.parseFloat(pref.getString(getResources().getString(R.string.roll_key), "0"));

            finish();

            if (critical == 0f)
                processErrorResponse(null, true);
            else if (id == ""){
                registerUser();
            }
            else {
                // launch SetupActivity
                startActivity(new Intent(getApplicationContext(), SetupActivity.class));
                finish();
            }
        }
        catch (Exception e) {
            registerUser();
        }
    }

    private void registerUser() {
        startActivity(new Intent(getApplicationContext(), RegisterActivity.class));
        finish();
    }

    private void getSetting() {
        Call<Setting> call = restApi.getSettings();
        call.enqueue(new Callback<Setting>() {
            @Override
            public void onResponse(Call<Setting> call, Response<Setting> response) {
                Setting rSetting = response.body();
                if (rSetting != null) {
                    SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                    pref.edit()
                            .putString(getResources().getString(R.string.pitch_key), rSetting.criticalPitchAngle.toString())
                            .putString(getResources().getString(R.string.roll_key), rSetting.criticalRollAngle.toString())
                            .putString(getResources().getString(R.string.reading_key), rSetting.readingRate.toString())
                            .putString(getResources().getString(R.string.saving_key), rSetting.savingRate.toString())
                            .putString(getResources().getString(R.string.sms_key), rSetting.smsRate.toString())
                            .putString(getResources().getString(R.string.post_key), rSetting.postRate.toString())
                            .putString(getResources().getString(R.string.cp_key), rSetting.mobileNumber.toString())
                            .commit();
                }
                else
                    processErrorResponse(call, false);
            }

            @Override
            public void onFailure(Call<Setting> call, Throwable t) {
                processErrorResponse(call, false);
            }
        });
    }

    private void processErrorResponse(Call<Setting> call, boolean required) {
        if (required) {
            LinearLayout llSplash = findViewById(R.id.llSplash);
            Snackbar.make(llSplash, "Cannot retrieve settings from the server.", Snackbar.LENGTH_INDEFINITE)
                    .setAction("Retry", view -> getSetting());
        }
        else {
            Toast.makeText(getApplicationContext(), "Please ensure good internet connection. ", Toast.LENGTH_SHORT).show();
        }
        if (call != null)
            call.cancel();
    }
}