package com.asu.seawavesapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.asu.seawavesapp.util.DecimalFormatter;

public class SetupActivity extends AppCompatActivity implements SensorEventListener {
    private Handler handler = new Handler();
    private Runnable readingRunnable;

    private SensorManager manager;
    private Sensor rotationVectorSensor;

    private final long readingDelay = 250; // milliseconds
    private float pitchAngle = 0f;
    private float rollAngle = 0f;

    private TextView tvSetupPitch;
    private TextView tvSetupRoll;
    private Button btStart;

    private DecimalFormatter df = new DecimalFormatter(2);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

        getSupportActionBar().hide();

        tvSetupPitch = findViewById(R.id.tvSetupPitch);
        tvSetupRoll = findViewById(R.id.tvSetupRoll);
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

        boolean sensorInit = initSensors();
        if (sensorInit) {
            startTimer();
        }
    }

    private boolean sensorAvailable() {
        boolean response = true;
        if (manager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) == null) {
            Toast.makeText(getApplicationContext(),
                    "The app was closed because your device has no rotation vector sensor, which is required by this application.", Toast.LENGTH_LONG).show();
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
            manager.registerListener(this, rotationVectorSensor, SensorManager.SENSOR_DELAY_GAME);
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
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}