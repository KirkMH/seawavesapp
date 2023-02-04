package com.asu.seawavesapp;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

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
@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {
    private RestApi restApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivitySplashBinding binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        getSupportActionBar().hide();

        restApi = ApiClient.getApi();
        getSetting();
    }

    @Override
    protected void onResume() {
        super.onResume();

        new Handler().postDelayed(this::verifySavedPreference, 2000);
    }

    /**
     * Checks whether the device has been registered to the server or not.
     * If not yet registered, the registration form will be opened.
     * If already registered, the setup activity will be opened.
     */
    private void verifySavedPreference() {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        try {
            String id = pref.getString(getResources().getString(R.string.id_key), "");
            float critical = Float.parseFloat(pref.getString(getResources().getString(R.string.roll_key), "0"));

            finish();

            if (critical == 0f)
                processErrorResponse(null, true);
            else if (id.isEmpty())
                registerUser();
            else {
                // launch SetupActivity
                startActivity(new Intent(getApplicationContext(), SetupActivity.class));
                finish();
            }
        } catch (Exception e) {
            registerUser();
        }
    }

    /**
     * Launches the register activity and closes this activity.
     */
    private void registerUser() {
        startActivity(new Intent(getApplicationContext(), RegisterActivity.class));
        finish();
    }

    /**
     * Retrieves settings from the server and stores it using shared preferences.
     */
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
                            .putString(getResources().getString(R.string.cp_key), rSetting.mobileNumber)
                            .apply();
                } else
                    processErrorResponse(call, false);
            }

            @Override
            public void onFailure(Call<Setting> call, Throwable t) {
                processErrorResponse(call, false);
            }
        });
    }

    /**
     * Displays an error message either as a <code>SnackBar</code> (if the error comes from a required process,
     * so that it can be retried) or as a <code>Toast</code>.
     *
     * @param call     - the call that was tried to be executed
     * @param required - when <code>true</code>, displays a SnackBar; <code>false</code> displays a Toast
     */
    private void processErrorResponse(Call<Setting> call, boolean required) {
        if (required) {
            LinearLayout llSplash = findViewById(R.id.llSplash);
            Snackbar.make(llSplash, "Cannot retrieve settings from the server.", Snackbar.LENGTH_INDEFINITE)
                    .setAction("Retry", view -> getSetting())
                    .show();
        } else {
            Toast.makeText(getApplicationContext(), "Please ensure good internet connection. ", Toast.LENGTH_SHORT).show();
        }
        if (call != null)
            call.cancel();
    }
}