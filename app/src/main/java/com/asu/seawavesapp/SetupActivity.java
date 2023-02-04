package com.asu.seawavesapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.asu.seawavesapp.databinding.ActivitySetupBinding;
import com.asu.seawavesapp.util.DecimalFormatter;

public class SetupActivity extends AppCompatActivity implements SensorEventListener {
    private final Handler handler = new Handler();
    private final DecimalFormatter df = new DecimalFormatter(2);
    private SensorManager manager;
    private TextView tvSetupPitch;
    private TextView tvSetupRoll;

    private final long readingDelay = 250; // milliseconds
    private float pitchAngle = 0f;
    private float rollAngle = 0f;

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

        // when the Start button is clicked, pass in the current
        // pitch and roll angles (will serve as the initial position) to the MainActivity
        // as this will be used for reading adjustment
        Button btStart = findViewById(R.id.btStart);
        btStart.setOnClickListener(view -> {
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            intent.putExtra("pitchAngle", pitchAngle);
            intent.putExtra("rollAngle", rollAngle);
            startActivity(intent);
            finish();
        });

        Button btClose = findViewById(R.id.btClose);
        btClose.setOnClickListener(view -> finishAndRemoveTask());

        checkPermissions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean sensorInit = initSensors();
        if (sensorInit) {
            startTimer();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacksAndMessages(null);
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

        return super.onOptionsItemSelected(item);
    }

}