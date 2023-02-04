package com.asu.seawavesapp;

import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class CalibrationActivity extends AppCompatActivity implements SensorEventListener {
    private TextView tvLevel;
    private int level = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calibration);

        tvLevel = findViewById(R.id.calib_level);
        Button btDone = findViewById(R.id.calib_done);
        btDone.setOnClickListener(view -> {
            if (level >= SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM)
                finish();
        });

        SensorManager manager = (SensorManager) getSystemService(SENSOR_SERVICE);
        Sensor magneticFieldSensor = manager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        manager.registerListener(this, magneticFieldSensor, SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        // ignore it here
    }

    /**
     * Vibrate for 500ms
     */
    private void vibratePhone() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ((Vibrator) getSystemService(VIBRATOR_SERVICE)).vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            ((Vibrator) getSystemService(VIBRATOR_SERVICE)).vibrate(500);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        level = accuracy;
        switch (accuracy) {
            case SensorManager.SENSOR_STATUS_ACCURACY_LOW:
                tvLevel.setText(R.string.low);
                tvLevel.setTextColor(Color.RED);
                break;

            case SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM:
                tvLevel.setText(R.string.medium);
                tvLevel.setTextColor(Color.BLUE);
                break;

            case SensorManager.SENSOR_STATUS_ACCURACY_HIGH:
                // when the accuracy is high, vibrate phone and close this activity
                tvLevel.setText(R.string.high);
                tvLevel.setTextColor(Color.GREEN);
                vibratePhone();
                finish();
                Toast.makeText(getApplicationContext(), "Compass accuracy is high.", Toast.LENGTH_SHORT).show();
                break;
        }
    }
}