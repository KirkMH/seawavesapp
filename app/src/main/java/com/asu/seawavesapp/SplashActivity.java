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
import com.asu.seawavesapp.data.Boat;
import com.asu.seawavesapp.data.Setting;
import com.asu.seawavesapp.databinding.ActivitySplashBinding;
import com.asu.seawavesapp.util.OnActionComplete;
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
    private Integer boatId = null;
    private Float critical = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivitySplashBinding binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        getSupportActionBar().hide();

        retrieveSavedPreferences();
        restApi = ApiClient.getApi();

        getSetting();
    }

    @Override
    protected void onResume() {
        super.onResume();

//        new Handler().postDelayed(this::verifySavedPreference, 2000);
    }

    /**
     * Retrieves the boat ID and roll angle critical level from saved preferences.
     */
    private void retrieveSavedPreferences() {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        try {
            this.boatId = Integer.parseInt(pref.getString(getResources().getString(R.string.id_key), ""));
            this.critical = Float.parseFloat(pref.getString(getResources().getString(R.string.roll_key), "0"));
        } catch (Exception e) { }
    }

    /**
     * Checks whether the device has been registered to the server or not.
     * If not yet registered, the registration form will be opened.
     * If already registered, the setup activity will be opened.
     */
    private void verifySavedPreference() {
        if (critical == null)
            processErrorResponse(null, true, null);
        else if (this.boatId == null)
            registerUser();
        else {
            // launch SetupActivity
            startActivity(new Intent(getApplicationContext(), SetupActivity.class));
            finish();
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
        getGeneralSetting(new OnActionComplete() {
            @Override
            public void onComplete(boolean success) {
                if (success) {
                    getBoatSetting(new OnActionComplete() {
                        @Override
                        public void onComplete(boolean success) {
                            verifySavedPreference();
                        }

                        @Override
                        public void onError(Throwable t) {

                        }
                    });
                }
                else
                    processErrorResponse(null, false, null);
            }

            @Override
            public void onError(Throwable t) {
                processErrorResponse(null, false, t.getMessage());
            }
        });
    }

    private void getGeneralSetting(OnActionComplete onActionComplete) {
        // for the general settings
        new Thread(() -> {
            Call<Setting> call = restApi.getSettings();
            call.enqueue(new Callback<Setting>() {
                @Override
                public void onResponse(Call<Setting> call, Response<Setting> response) {
                    Setting rSetting = response.body();
                    if (rSetting != null) {
                        String roll = rSetting.criticalRollAngle.toString();
                        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                        pref.edit()
                                .putString(getResources().getString(R.string.pitch_key), rSetting.criticalPitchAngle.toString())
                                .putString(getResources().getString(R.string.roll_key), roll)
                                .putString(getResources().getString(R.string.reading_key), rSetting.readingRate.toString())
                                .putString(getResources().getString(R.string.saving_key), rSetting.savingRate.toString())
                                .putString(getResources().getString(R.string.sms_key), rSetting.smsRate.toString())
                                .putString(getResources().getString(R.string.post_key), rSetting.postRate.toString())
                                .putString(getResources().getString(R.string.cp_key), rSetting.mobileNumber)
                                .apply();
                        if (roll != null) {
                            critical = Float.parseFloat(roll);
                            onActionComplete.onComplete(true);
                            return;
                        }
                    } else {
                        processErrorResponse(call, false, null);
                    }

                    onActionComplete.onComplete(false);
                }

                @Override
                public void onFailure(Call<Setting> call, Throwable t) {
                    processErrorResponse(call, false, null);
                    onActionComplete.onError(t);
                }
            });
        }).start();
    }


    private void getBoatSetting(OnActionComplete onActionComplete) {
        new Thread(() -> {
            if (boatId == null) {
                onActionComplete.onComplete(true);
                return;
            }
            // for the boat details
            Call<Boat> boatCall = restApi.getBoatDetail(boatId);
            boatCall.enqueue(new Callback<Boat>() {
                @Override
                public void onResponse(Call<Boat> call, Response<Boat> response) {
                    Boat rBoat = response.body();
                    if (rBoat != null) {
                        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                        pref.edit()
                                .putString(getResources().getString(R.string.boat_key), rBoat.name)
                                .putString(getResources().getString(R.string.owner_key), rBoat.owner)
                                .putString(getResources().getString(R.string.contact_key), rBoat.ownerContact)
                                .putString(getResources().getString(R.string.length_key), rBoat.length.toString())
                                .putString(getResources().getString(R.string.width_key), rBoat.width.toString())
                                .putString(getResources().getString(R.string.height_key), rBoat.height.toString())
                                .apply();
                        onActionComplete.onComplete(true);
                    } else {
                        processErrorResponse(boatCall, false, null);
                        onActionComplete.onComplete(false);
                    }
                }

                @Override
                public void onFailure(Call<Boat> call, Throwable t) {
                    processErrorResponse(call, false, null);
                    onActionComplete.onError(t);
                }
            });
        }).start();
    }

    /**
     * Displays an error message either as a <code>SnackBar</code> (if the error comes from a required process,
     * so that it can be retried) or as a <code>Toast</code>.
     *
     * @param call     - the call that was tried to be executed
     * @param required - when <code>true</code>, displays a SnackBar; <code>false</code> displays a Toast
     */
    private void processErrorResponse(Call call, boolean required, String message) {
        if (required) {
            LinearLayout llSplash = findViewById(R.id.llSplash);
            Snackbar.make(llSplash, "Cannot retrieve settings from the server.", Snackbar.LENGTH_INDEFINITE)
                    .setAction("Retry", view -> getSetting())
                    .show();
        } else if (message != null) {
            Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
        }else {
            Toast.makeText(getApplicationContext(), "Please ensure good internet connection. ", Toast.LENGTH_SHORT).show();
        }
        if (call != null)
            call.cancel();
    }
}