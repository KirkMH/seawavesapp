package com.asu.seawavesapp;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class CalibrationActivity extends AppCompatActivity implements SensorEventListener {

    private TextView tvLevel;
    private Button btDone;
    private SensorManager manager;
    private Sensor magneticFieldSensor;
    private int level = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calibration);

        tvLevel = findViewById(R.id.calib_level);
        btDone = findViewById(R.id.calib_done);
        btDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (level >= SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM)
                    finish();
            }
        });

        manager = (SensorManager) getSystemService(SENSOR_SERVICE);
        magneticFieldSensor = manager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        manager.registerListener(this, magneticFieldSensor, SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        // ignore it here
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        level = accuracy;
        switch (accuracy) {
            case SensorManager.SENSOR_STATUS_ACCURACY_LOW:
                tvLevel.setText("Low");
                tvLevel.setTextColor(Color.RED);
                break;

            case SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM:
                tvLevel.setText("Medium");
                tvLevel.setTextColor(Color.BLUE);
                break;

            case SensorManager.SENSOR_STATUS_ACCURACY_HIGH:
                tvLevel.setText("High");
                tvLevel.setTextColor(Color.GREEN);
                finish();
                break;
        }
    }
}