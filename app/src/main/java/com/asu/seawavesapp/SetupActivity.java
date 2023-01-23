package com.asu.seawavesapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.preference.PreferenceManager;

import android.Manifest;
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
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.asu.seawavesapp.databinding.ActivitySetupBinding;
import com.asu.seawavesapp.util.DecimalFormatter;

public class SetupActivity extends AppCompatActivity implements SensorEventListener {
    private ActivitySetupBinding binding;
    private Handler handler = new Handler();
    private Runnable readingRunnable;

    private SensorManager manager;
    private Sensor rotationVectorSensor;
    private Sensor magneticFieldSensor;

    private final long readingDelay = 250; // milliseconds
    private float pitchAngle = 0f;
    private float rollAngle = 0f;

    private TextView tvSetupPitch;
    private TextView tvSetupRoll;
//    private TextView tvLevel;
//    private TextView tvSim;
    private Button btStart;
    private Button btClose;

    // for the permissions
    private final int REQUEST_CODE = 100;


    private DecimalFormatter df = new DecimalFormatter(2);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivitySetupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        tvSetupPitch = findViewById(R.id.tvSetupPitch);
        tvSetupRoll = findViewById(R.id.tvSetupRoll);
//        tvLevel = findViewById(R.id.setup_level);
//        tvSim = findViewById(R.id.setup_sim);
        btStart = findViewById(R.id.btStart);
        btStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                intent.putExtra("pitchAngle", pitchAngle);
                intent.putExtra("rollAngle", rollAngle);
                startActivity(intent);
                finish();
            }
        });

        btClose = findViewById(R.id.btClose);
        btClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finishAndRemoveTask();
            }
        });

        checkPermissions();
//        boolean askAgain = true;
//        while (askAgain) {
//            askAgain = checkPermissions();
//        }
    }

    @Override
    protected void onResume() {
        super.onResume();

//        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
//        String sim = prefs.getString(getApplicationContext().getString(R.string.sim_key), "SIM1");

//        tvSim.setText(sim);
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
//        boolean askAgain = false;
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
//
//        String state = Environment.getExternalStorageState();
//        if (state.equals(Environment.MEDIA_MOUNTED)) {
//            if (ActivityCompat.checkSelfPermission(this,
//                    Manifest.permission.WRITE_EXTERNAL_STORAGE)
//                    != PackageManager.PERMISSION_GRANTED) {
//                ActivityCompat.requestPermissions(this, new String[]
//                                {Manifest.permission.WRITE_EXTERNAL_STORAGE},
//                        REQUEST_WRITE_EXTERNAL);
//                askAgain = true;
//            }
//            if (ActivityCompat.checkSelfPermission(this,
//                    Manifest.permission.READ_EXTERNAL_STORAGE)
//                    != PackageManager.PERMISSION_GRANTED) {
//                ActivityCompat.requestPermissions(this, new String[]
//                                {Manifest.permission.READ_EXTERNAL_STORAGE},
//                        REQUEST_READ_EXTERNAL);
//                askAgain = true;
//            }
//        }
//        if (ActivityCompat.checkSelfPermission(getApplicationContext(),
//                Manifest.permission.SEND_SMS)
//                != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(this, new String[]
//                            {Manifest.permission.SEND_SMS},
//                    REQUEST_SEND_SMS);
//            askAgain = true;
//        }
//        if (ActivityCompat.checkSelfPermission(getApplicationContext(),
//                Manifest.permission.READ_PHONE_STATE)
//                != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(this, new String[]
//                            {Manifest.permission.READ_PHONE_STATE},
//                    REQUEST_PHONE_STATE);
//            askAgain = true;
//        }
//        if (ActivityCompat.checkSelfPermission(this,
//                Manifest.permission.ACCESS_FINE_LOCATION)
//                != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(this, new String[]
//                            {Manifest.permission.ACCESS_FINE_LOCATION},
//                    REQUEST_LOCATION_PERMISSION);
//            askAgain = true;
//        }
//
//        return askAgain;
    }

    private boolean sensorAvailable() {
        boolean response = true;
        if (manager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) == null) {
            Toast.makeText(getApplicationContext(),
                    "Rotation vector sensor unavailable.", Toast.LENGTH_LONG).show();
            finish();
            response = false;
        }
        else if (manager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) == null) {
            Toast.makeText(getApplicationContext(),
                    "Magnetic field sensor unavailable.", Toast.LENGTH_LONG).show();
            finish();
            response = false;
        }
        return response;
    }

    private boolean initSensors() {
        boolean response = false;
        manager = (SensorManager) getSystemService(SENSOR_SERVICE);
        // checking if the device has the required sensor
        if (sensorAvailable()) {
            rotationVectorSensor = manager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
            magneticFieldSensor = manager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
            manager.registerListener(this, rotationVectorSensor, SensorManager.SENSOR_DELAY_GAME);
            manager.registerListener(this, magneticFieldSensor, SensorManager.SENSOR_DELAY_GAME);
            response = true;
        }
        return response;
    }

    private void displayReadings() {
        tvSetupPitch.setText(df.format(pitchAngle));
        tvSetupRoll.setText(df.format(rollAngle));
    }

    private void startTimer() {
        readingRunnable = new Runnable() {
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
            double x = (double) vals[0];
            double y = (double) vals[1];
            double z = (double) vals[2];
            double w = (double) vals[3];

            // normalize
            double norm = Math.sqrt(x*x + y*y + z*z + w*w);
            x /= norm;
            y /= norm;
            z /= norm;
            w /= norm;

            // calculate Pitch in degrees (-180 to 180)
            double sinP = 2.0 * (w * x + y * z);
            double cosP = 1.0 - 2.0 * (x * x + y * y);
            double pitch = Math.atan2(sinP, cosP) * (180 / Math.PI);

            // calculate Tilt in degrees
            double tilt = 0;
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
//            tvLevel.setText(accuracy == SensorManager.SENSOR_STATUS_ACCURACY_LOW ? "LOW" :
//                    accuracy == SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM ? "MEDIUM" :
//                    "HIGH");
            if (accuracy < SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM) {
                // when accuracy is low, calibration is required
                startActivity(new Intent(getApplicationContext(), CalibrationActivity.class));
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
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