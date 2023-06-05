package com.asu.seawavesapp;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.asu.seawavesapp.api.ApiClient;
import com.asu.seawavesapp.api.RestApi;
import com.asu.seawavesapp.data.LocalReadingAndError;
import com.asu.seawavesapp.databinding.ActivitySetupBinding;
import com.asu.seawavesapp.log.ErrorLog;
import com.asu.seawavesapp.log.Logger;
import com.asu.seawavesapp.log.ReadingLog;
import com.asu.seawavesapp.util.DecimalFormatter;
import com.asu.seawavesapp.util.OnActionComplete;
import com.asu.seawavesapp.util.Utility;
import com.asu.seawavesapp.util.VoyageHelper;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SetupActivity extends AppCompatActivity implements SensorEventListener {
    private final Handler handler = new Handler();
    private final DecimalFormatter df = new DecimalFormatter(2);
    private VoyageHelper voyageHelper;
    private SensorManager manager;
    private TextView tvSetupPitch;
    private TextView tvSetupRoll;

    private final long readingDelay = 250; // milliseconds
    private float pitchAngle = 0f;
    private float rollAngle = 0f;
    private boolean hasStarted = false;
    private Long voyageId;

    // for the permissions
    private final int REQUEST_CODE = 100;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivitySetupBinding binding = ActivitySetupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        tvSetupPitch = findViewById(R.id.tvSetupPitch);
        tvSetupRoll = findViewById(R.id.tvSetupRoll);

        // check for voyageId
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            Long vId = extras.getLong("voyageId", 0);
            float maxPitch = extras.getFloat("maxPitch", 0f);
            float maxRoll = extras.getFloat("maxRoll", 0f);
            int minSignal = extras.getInt("minSignal", 0);
            float maxSpeed = extras.getFloat("maxSpeed", 0f);
            if (vId > 0)
                displaySummary(maxPitch, maxRoll, minSignal, maxSpeed);
        }

        // when the Start button is clicked, pass in the current
        // pitch and roll angles (will serve as the initial position) to the MainActivity
        // as this will be used for reading adjustment
        Button btStart = findViewById(R.id.btStart);
        btStart.setOnClickListener(view -> {
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            intent.putExtra("pitchAngle", pitchAngle);
            intent.putExtra("rollAngle", rollAngle);
            startVoyage(intent);
        });

        Button btClose = findViewById(R.id.btClose);
        btClose.setOnClickListener(view -> {
            finishAndRemoveTask();
            System.exit(0);
        });

        checkPermissions();

        // make sure that the Location Service is on
        boolean isActiveLocationService = Utility.checkLocationService(getApplicationContext());
        if (!isActiveLocationService)
            Toast.makeText(getApplicationContext(), "Please turn on the Location Service.",
                    Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean sensorInit = initSensors();
        if (sensorInit) {
            startTimer();
        }
        hasStarted = false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacksAndMessages(null);
    }

    private void displaySummary(float maxPitch, float maxRoll, int minSignal, float maxSpeed) {
        final Dialog dialog = new Dialog(SetupActivity.this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);
        dialog.setContentView(R.layout.dialog_voyage_summary);

        DecimalFormatter df = new DecimalFormatter(2);
        TextView pitch = (TextView) dialog.findViewById(R.id.tvSummaryPitch);
        TextView roll = (TextView) dialog.findViewById(R.id.tvSummaryRoll);
        TextView signal = (TextView) dialog.findViewById(R.id.tvSummarySignal);
        TextView speed = (TextView) dialog.findViewById(R.id.tvSummarySpeed);
        pitch.setText(df.format(maxPitch));
        roll.setText(df.format(maxRoll));
        speed.setText(df.format(maxSpeed));
//        String signalMeaning = " - " + (minSignal == 4 ? "Very Good" : minSignal == 3 ? "Good" :
//                minSignal == 2 ? "Average" : minSignal == 1 ? "Poor" : "Very Poor");
        signal.setText(minSignal + "");

        Button ok = dialog.findViewById(R.id.btOK);
        ok.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    /**
     * Checks whether this app has the specified permissions.
     *
     * @param permissions - list of permissions to check
     * @return <code>true</code> if all permissions exist; <code>false</code> othewise
     */
    private boolean hasPermissions(String[] permissions) {
        boolean result = true;
        for (String perm : permissions) {
            result = result && ActivityCompat.checkSelfPermission(this, perm)
                    == PackageManager.PERMISSION_GRANTED;
        }
        Log.v("perm", "hasPermissions: " + result);
        return result;
    }

    /**
     * Tries to get user permissions right on this activity.
     */
    private void checkPermissions() {
        String[] permissions = {
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.SEND_SMS,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
        };

        if (!hasPermissions(permissions)) {
            ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE);
        }
    }

    /**
     * Checks whether this device has rotation and magnetic field sensors available or not.
     *
     * @return <code>true</code> if required sensors are available; <code>false</code> otherwise
     */
    private boolean sensorAvailable() {
        boolean response = true;
        if (manager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) == null) {
            Toast.makeText(getApplicationContext(),
                    "Rotation vector sensor unavailable.", Toast.LENGTH_LONG).show();
            finish();
            response = false;
        } else if (manager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) == null) {
            Toast.makeText(getApplicationContext(),
                    "Magnetic field sensor unavailable.", Toast.LENGTH_LONG).show();
            finish();
            response = false;
        }
        return response;
    }

    /**
     * Initializes the sensors when they are available.
     *
     * @return <code>true</code> if the initialization succeed; <code>false</code> otherwise
     */
    private boolean initSensors() {
        boolean response = false;
        manager = (SensorManager) getSystemService(SENSOR_SERVICE);
        // checking if the device has the required sensor
        if (sensorAvailable()) {
            Sensor rotationVectorSensor = manager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
            Sensor magneticFieldSensor = manager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
            manager.registerListener(this, rotationVectorSensor, SensorManager.SENSOR_DELAY_GAME);
            manager.registerListener(this, magneticFieldSensor, SensorManager.SENSOR_DELAY_GAME);
            response = true;
        }
        return response;
    }

    /**
     * Displays the readings to the text views.
     */
    private void displayReadings() {
        tvSetupPitch.setText(df.format(pitchAngle));
        tvSetupRoll.setText(df.format(rollAngle));
    }

    /**
     * Starts the reading timer.
     */
    private void startTimer() {
        Runnable readingRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    handler.postDelayed(this, readingDelay);
                    displayReadings();
                } catch (Exception e) {
                    Toast.makeText(SetupActivity.this, "Error: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                    finish();
                }
            }
        };
        handler.postDelayed(readingRunnable, 0);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorEvent.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            int n = sensorEvent.values.length;
            float[] vals = new float[n];
            System.arraycopy(sensorEvent.values, 0, vals, 0, n);

            // https://stackoverflow.com/questions/48975355/rotation-vector-sensor-values-to-azimuth-roll-and-pitch
            double x = vals[0];
            double y = vals[1];
            double z = vals[2];
            double w = vals[3];

            // normalize
            double norm = Math.sqrt(x * x + y * y + z * z + w * w);
            x /= norm;
            y /= norm;
            z /= norm;
            w /= norm;

            // calculate Pitch in degrees (-180 to 180)
            double sinP = 2.0 * (w * x + y * z);
            double cosP = 1.0 - 2.0 * (x * x + y * y);
            double pitch = Math.atan2(sinP, cosP) * (180 / Math.PI);

            // calculate Tilt in degrees
            double tilt;
            double sinT = 2.0 * (w * y - z * x);
            if (Math.abs(sinT) >= 1)
                tilt = Math.copySign(Math.PI / 2, sinT) * (180 / Math.PI);
            else
                tilt = Math.asin(sinT) * (180 / Math.PI);

            pitchAngle = (float) pitch;
            rollAngle = (float) tilt;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        if (hasStarted) return;

        if (sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            if (accuracy < SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM) {
                // when accuracy is low, calibration is required
                startActivity(new Intent(getApplicationContext(), CalibrationActivity.class));
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_info) {
            startActivity(new Intent(getApplicationContext(), InfoActivity.class));
            return true;
        }
        else if (id == R.id.action_about) {
            startActivity(new Intent(getApplicationContext(), AboutActivity.class));
            return true;
        }
        else if (id == R.id.action_calibrate) {
            startActivity(new Intent(getApplicationContext(), CalibrationActivity.class));
            return true;
        }
        else if (id == R.id.action_setting) {
            startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
            return true;
        }
        else if (id == R.id.action_upload) {
            uploadLocalData();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    private void startVoyage(Intent intent) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        Long boatId = Long.parseLong(pref.getString(getResources().getString(R.string.id_key), "0"));
        voyageHelper = new VoyageHelper(boatId);

        ProgressDialog pg = new ProgressDialog(SetupActivity.this);
        pg.setMessage("Connecting to server.\nPlease wait...");
        pg.setTitle("Start Voyage");
        pg.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        pg.show();
        pg.setCancelable(false);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    voyageHelper.startVoyage();
                    while (voyageHelper.getVoyagePk() == null) {
                        Thread.sleep(500);
                    }

                    voyageId = voyageHelper.getVoyagePk();
                    Log.v("voyage", "voyageId: " + voyageId);
                    pg.dismiss();

                    intent.putExtra("voyageId", voyageId);
                    startActivity(intent);
                    handler.removeCallbacksAndMessages(null);
                    hasStarted = true;
                    finish();
                } catch (Exception e) {
                    pg.dismiss();
                    e.printStackTrace();
                }
            }
        }).start();

    }


    private boolean success;
    private void uploadLocalData() {
        // retrieve folder location
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String folder = pref.getString("logFolder", "");
        Long boatId = Long.parseLong(pref.getString(getResources().getString(R.string.id_key), "0"));
        if (folder.isEmpty()) {
            Toast.makeText(getApplicationContext(), "No log file from this day.", Toast.LENGTH_SHORT).show();
            return;
        }

        // file name for the log file
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        String filename = sdf.format(new Date()) + ".csv";

        File logFile = new File(folder, filename);
        Logger logger = new ReadingLog(logFile, this);
        Log.v("Logger", "Reading: " + logger.getFileUrl());
        String data = logger.readAll();

        File errorFile = new File(folder, "errors.txt");
        Logger errLogs = new ErrorLog(errorFile, this);
        Log.v("Logger", "Error: " + errLogs.getFileUrl());
        String errors = errLogs.readAll();

        ProgressDialog pg = new ProgressDialog(SetupActivity.this);
        pg.setMessage("Uploading to server.\nPlease wait...");
        pg.setTitle("Upload Local Data");
        pg.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        pg.show();
        pg.setCancelable(false);

        LocalReadingAndError lre = new LocalReadingAndError(boatId, data, errors);
        RestApi restApi = ApiClient.getApi();
        Call<LocalReadingAndError> call = restApi.uploadLocalData(lre);

        upload(pg, call, new OnActionComplete() {
            @Override
            public void onComplete(boolean success) {
                Toast.makeText(getApplicationContext(), "Local data uploaded.", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(Throwable t) {
                Toast.makeText(getApplicationContext(), "Unable to upload local.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void upload(ProgressDialog pg, Call<LocalReadingAndError> call, OnActionComplete onActionComplete) {

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    call.enqueue(new Callback<LocalReadingAndError>() {
                        @Override
                        public void onResponse(Call<LocalReadingAndError> call, Response<LocalReadingAndError> response) {
                            Log.v("voyage", "online");
                            pg.dismiss();
                            onActionComplete.onComplete(true);
                        }

                        @Override
                        public void onFailure(Call<LocalReadingAndError> call, Throwable t) {
                            Log.v("voyage", "offline");
                            onActionComplete.onError(t);
                            call.cancel();
                            pg.dismiss();
                        }
                    });
                } catch (Exception e) {
                    success = false;
                    pg.dismiss();
                    e.printStackTrace();
                }
            }
        }).start();
    }

}