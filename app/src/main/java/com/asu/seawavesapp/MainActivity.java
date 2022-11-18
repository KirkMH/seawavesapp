package com.asu.seawavesapp;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.asu.seawavesapp.data.Reading;
import com.asu.seawavesapp.databinding.ActivityMainBinding;
import com.asu.seawavesapp.log.ErrorLog;
import com.asu.seawavesapp.log.HtmlFile;
import com.asu.seawavesapp.log.ReadingLog;
import com.asu.seawavesapp.service.Alert;
import com.asu.seawavesapp.service.SmsService;
import com.asu.seawavesapp.util.CompassHelper;
import com.asu.seawavesapp.util.DecimalFormatter;
import com.asu.seawavesapp.util.QueueToServer;
import com.asu.seawavesapp.util.Sampler;
import com.asu.seawavesapp.util.Utility;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private ActivityMainBinding binding;
    private TextView tvDateTime;
    private TextView tvStatus;
    private TextView tvHeading;
    private TextView tvPitch;
    private TextView tvRoll;
    private TextView tvGyroX;
    private TextView tvGyroY;
    private TextView tvGyroZ;
    private TextView tvAccelX;
    private TextView tvAccelY;
    private TextView tvAccelZ;
    private TextView tvMagnetX;
    private TextView tvMagnetY;
    private TextView tvMagnetZ;
    private TextView tvLatitude;
    private TextView tvLongitude;
    private TextView tvAltitude;
    private Button btStop;

    private final int REQUEST_LOCATION_PERMISSION = 100;
    private final int REQUEST_WRITE_EXTERNAL = 101;
    private final int REQUEST_READ_EXTERNAL = 102;
    private final int REQUEST_SEND_SMS = 103;

    private Long boatId;
    private String boatName;
    private Float pitchAngleAlert = 0f;
    private Float rollAngleAlert = 0f;
    private Float initPitch = 0f;
    private Float initRoll = 0f;

    private Sampler sampler;
    private float headingAngle = 0f;
    private float pitchAngle = 0f;
    private float rollAngle = 0f;
    private float gyroX = 0f, gyroY = 0f, gyroZ = 0f;
    private float accelX = 0f, accelY = 0f, accelZ = 0f;
    private float magnetX = 0f, magnetY = 0f, magnetZ = 0f;

    private ReadingLog logger;  // CSV file to store reading logs
    private ErrorLog errLogs; // text file that will store errors encountered
    private HtmlFile htmlContent; // will be used to generate contents for the Info and About pages

    private SensorManager manager;
    private Sensor mGyroscope;
    private Sensor mAccelerometer;
    private Sensor mMagnetometer;
    private Sensor mRotationVector;
    private boolean sensorsInitialized = false;

    FusedLocationProviderClient mFusedLocationClient;
    Location mLastLocation;
    LocationCallback mLocationCallback;

    private Handler handler = new Handler();
    private Runnable readingRunnable;
    private Runnable smsRunnable;
    private Runnable savingRunnable;
    private Runnable postingRunnable;
    private Runnable dtRunnable;
    private boolean isRunning = true;
    private final Long locationDelay = 30000L; // 30 seconds
    private Long readingDelay = 1000L; // milliseconds
    private Long smsDelay = 1000L; // milliseconds
    private Long savingDelay = 1000L; // milliseconds
    private Long postingDelay = 1000L; // milliseconds
    private String message = "";
    private String phoneNumber = "";

    private HashMap<String, String> reading;
    private QueueToServer savingQueue;
    private Thread thread;
    private Alert alert;
    private int lastPlayed = 0;
    private float magneticDeclination;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        // get initial angles
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            initPitch = extras.getFloat("pitchAngle", 0f);
            initRoll = extras.getFloat("rollAngle", 0f);
        }


        initLogger();
        initPreferences();
        initUi();
        initLocationServices();
        boolean sensorInit = initSensors();

        sampler = new Sampler();
        alert = new Alert(this, boatName, pitchAngleAlert, rollAngleAlert);
        savingQueue = new QueueToServer(getApplicationContext(), logger, errLogs);
        thread = new Thread(savingQueue);
        thread.start();

        if (sensorInit)
            startTimer();
        if (sensorsInitialized)
            registerSensors();

        btStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // launch SetupActivity
                startActivity(new Intent(getApplicationContext(), SetupActivity.class));
                finish();
            }
        });
    }

    /**
     * Initializes the log files both for readings and errors.
     */
    private void initLogger() {
        boolean isSdPresent = false;
        String state = Environment.getExternalStorageState();
        if (state.equals(Environment.MEDIA_MOUNTED)) {
            if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]
                                {Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_WRITE_EXTERNAL);
            } else if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]
                                {Manifest.permission.READ_EXTERNAL_STORAGE},
                        REQUEST_READ_EXTERNAL);
            }
            isSdPresent = true;
        }

        // file name for the log file
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        String filename = sdf.format(new Date()) + ".csv";

        // prepare the log file
        File folder;
        if (isSdPresent) {
            // SD Card present
            try {
                folder = getApplicationContext().getExternalFilesDirs(null)[1];
            } catch (Exception e) {
                folder = getApplicationContext().getExternalFilesDirs(null)[0];
            }
        }
        else {
            // no SD card; store to internal storage
            folder = new File(getApplicationContext().getFilesDir(), "logs");
            if (!folder.exists()) folder.mkdir();
            Toast.makeText(getApplicationContext(), "No SD card found. Using the internal memory.",
                    Toast.LENGTH_SHORT).show();
        }

        File logFile = new File(folder, filename);
        logger = new ReadingLog(logFile, this);
        File errorFile = new File(folder, "errors.txt");
        errLogs = new ErrorLog(errorFile, this);
        File htmlFile = new File(folder, "info.html");
        htmlContent = new HtmlFile(htmlFile, this);
    }

    /**
     * Retrieves settings stores in the SharedPreferences.
     */
    private void initPreferences() {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        this.boatId = Long.parseLong(pref.getString(getResources().getString(R.string.id_key), "0"));
        this.boatName = pref.getString(getResources().getString(R.string.boat_key), "Boat");
        this.pitchAngleAlert = Float.parseFloat(pref.getString(getResources().getString(R.string.pitch_key), "0"));
        this.rollAngleAlert = Float.parseFloat(pref.getString(getResources().getString(R.string.roll_key), "0"));
        this.readingDelay = Long.parseLong(pref.getString(getResources().getString(R.string.reading_key), "0"));
        this.smsDelay = Long.parseLong(pref.getString(getResources().getString(R.string.sms_key), "0"));
        this.savingDelay = Long.parseLong(pref.getString(getResources().getString(R.string.saving_key), "0"));
        this.postingDelay = Long.parseLong(pref.getString(getResources().getString(R.string.post_key), "0"));
        this.phoneNumber = pref.getString(getResources().getString(R.string.contact_key), "096900338822");
    }

    /**
     * Finds the UI component and binds them to a local object.
     */
    private void initUi() {
        tvDateTime = findViewById(R.id.tvDateTime);
        tvStatus = findViewById(R.id.tvStatus);
        tvHeading = findViewById(R.id.tvHeading);
        tvPitch = findViewById(R.id.tvPitch);
        tvPitch = findViewById(R.id.tvPitch);
        tvRoll = findViewById(R.id.tvRoll);
        tvGyroX = findViewById(R.id.tvGyroX);
        tvGyroY = findViewById(R.id.tvGyroY);
        tvGyroZ = findViewById(R.id.tvGyroZ);
        tvAccelX = findViewById(R.id.tvAccelX);
        tvAccelY = findViewById(R.id.tvAccelY);
        tvAccelZ = findViewById(R.id.tvAccelZ);
        tvMagnetX = findViewById(R.id.tvMagnetX);
        tvMagnetY = findViewById(R.id.tvMagnetY);
        tvMagnetZ = findViewById(R.id.tvMagnetZ);
        tvLatitude = findViewById(R.id.tvLat);
        tvLongitude = findViewById(R.id.tvLong);
        tvAltitude = findViewById(R.id.tvAlt);
        btStop = findViewById(R.id.btStop);
    }

    /**
     * Initializes the location services using FusedLocationClient
     */
    private void initLocationServices() {
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);
                mLastLocation = locationResult.getLastLocation();
                isLocationRetrieved = false;
                if (mLastLocation != null) {
                    DecimalFormatter df = new DecimalFormatter(6);
                    tvLatitude.setText(df.format((float) mLastLocation.getLatitude()));
                    tvLongitude.setText(df.format((float) mLastLocation.getLongitude()));
                    if (mLastLocation.hasAltitude()) {
                        tvAltitude.setText(df.format((float) mLastLocation.getAltitude()));
                        magneticDeclination = CompassHelper.calculateMagneticDeclination(
                                mLastLocation.getLatitude(), mLastLocation.getLongitude(), mLastLocation.getAltitude());
                        isLocationRetrieved = true;
                    }
                } else {
                    tvLatitude.setText("0.00");
                    tvLongitude.setText("0.00");
                }
            }
        };
    }

    /**
     * Method called by requestLocationUpdates() method in startTrackingLocation()
     * that uses high accuracy level and refreshes every 30 seconds.
     * @return
     */
    private LocationRequest getLocationRequest() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(locationDelay);
        locationRequest.setFastestInterval(30000); // 30 seconds
        locationRequest.setPriority(Priority.PRIORITY_HIGH_ACCURACY);
        return locationRequest;
    }

    /**
     * Checks the permission for location access and starts reading device location.
     */
    private void startTrackingLocation() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]
                            {Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION_PERMISSION);
        } else {
            // already permitted; proceed to operation
            mFusedLocationClient.requestLocationUpdates
                    (getLocationRequest(), mLocationCallback,
                            null /* Looper */);
        }
    }

    /**
     * Checks whether sensors used by this app are available: Gyroscope, Accelerometer,
     * Magnetometer, and Rotation Vector.
     * @return boolean. true if all sensors are available, false otherwise.
     */
    private boolean areSensorsAvailable() {
        boolean passed = false;
        ArrayList<String> noSensors = new ArrayList<>();

        if (manager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) == null)
            noSensors.add("Gyroscope");
        else if (manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) == null)
            noSensors.add("Accelerometer");
        else if (manager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) == null)
            noSensors.add("Magnetometer");
        else if (manager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) == null)
            noSensors.add("Rotation Vector");
        else
            passed = true;

        if (!passed) {
            String s = noSensors.get(0);
            for (int i = 1; i < noSensors.size(); i++) {
                s += ", " + noSensors.get(i);
            }
            Toast.makeText(getApplicationContext(), "Error: Required sensors undetected.",
                    Toast.LENGTH_LONG).show();
            errLogs.write("Error: Undetected sensors - " + s);
            finishAndRemoveTask();
        }
        return passed;
    }

    /**
     * Initializes the sensors.
     * @return
     */
    private boolean initSensors() {
        boolean response = false;

        manager = (SensorManager) getSystemService(SENSOR_SERVICE);
        // checking if the device have the required sensors
        if (areSensorsAvailable()) {
            mGyroscope = manager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
            mAccelerometer = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            mMagnetometer = manager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
            mRotationVector = manager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
            sensorsInitialized = true;
            response = true;
        }

        return response;
    }

    /**
     * Method called by dtRunnable to update the current date/time every second
     */
    private void displayDateTime() {
        // current timestamp
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM dd, yyyy  hh:mm:ss a");
        tvDateTime.setText(sdf.format(new Date()));
    }

    /**
     * Checks whether the specified permission was granted or not.
     * @param grantResults
     * @param permissionDescription
     * @return
     */
    private boolean isPermissionGranted(int[] grantResults, String permissionDescription) {
        boolean result = true;

        if (!(grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
            Toast.makeText(getApplicationContext(), "The permission for " + permissionDescription + " was not granted yet.",
                    Toast.LENGTH_LONG).show();
            result = false;
        }

        return result;
    }

    private void startTimer() {
        final Long oneSecond = 1000L;

        dtRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    if (isRunning)
                        handler.postDelayed(this, oneSecond);
                    displayDateTime();
                } catch (Exception e) {
                    criticalErrorHandler(e);
                }
            }
        };
        readingRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    if (isRunning)
                        handler.postDelayed(this, readingDelay);
                    Reading reading = displayReadings();
                    checkAlert(reading);

                } catch (Exception e) {
                    criticalErrorHandler(e);
                }
            }
        };
        savingRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    if (isRunning)
                        handler.postDelayed(this, savingDelay);
                    saveReading(null);
                } catch (Exception e) {
                    criticalErrorHandler(e);
                }
            }
        };
        postingRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    if (isRunning)
                        handler.postDelayed(this, postingDelay);
                    postReading();
                } catch (Exception e) {
                    criticalErrorHandler(e);
                }
            }
        };
        smsRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    if (isRunning)
                        handler.postDelayed(this, smsDelay);

                    if (ActivityCompat.checkSelfPermission(getApplicationContext(),
                            Manifest.permission.SEND_SMS)
                            != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]
                                        {Manifest.permission.SEND_SMS},
                                REQUEST_SEND_SMS);
                    } else {
                        smsAlert();
                    }
                } catch (Exception e) {
                    criticalErrorHandler(e);
                }
            }
        };

        // wait for a second before starting the timers
        handler.postDelayed(dtRunnable, oneSecond);
        handler.postDelayed(readingRunnable, oneSecond);
        handler.postDelayed(savingRunnable, oneSecond);
        handler.postDelayed(postingRunnable, oneSecond);
        handler.postDelayed(smsRunnable, oneSecond);
    }

    private void criticalErrorHandler(Exception exception) {
        errLogs.write(exception);
        Toast.makeText(getApplicationContext(), "Error: " + exception.getLocalizedMessage(),
                Toast.LENGTH_LONG).show();
        finishAndRemoveTask();
    }

    private void registerSensors() {
        manager.registerListener(this, mGyroscope, SensorManager.SENSOR_DELAY_GAME);
        manager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME);
        manager.registerListener(this, mMagnetometer, SensorManager.SENSOR_DELAY_GAME);
        manager.registerListener(this, mRotationVector, SensorManager.SENSOR_DELAY_GAME);
    }

    private Reading getCurrentReading() {
        float lat = 0f;
        float lng = 0f;
        float alt = 0f;
        if (mLastLocation != null) {
            lat = (float) mLastLocation.getLatitude();
            lng = (float) mLastLocation.getLongitude();
            if (mLastLocation.hasAltitude())
                alt = (float) mLastLocation.getAltitude();
        }

        return new Reading(
                boatId,
                headingAngle,
                (pitchAngle - initPitch),    // adjustments based on device
                (rollAngle - initRoll),       // position set at SetupActivity
                gyroX,
                gyroY,
                gyroZ,
                accelX,
                accelY,
                accelZ,
                magnetX,
                magnetY,
                magnetZ,
                lat,
                lng,
                alt,
                null,
                Utility.formatTimestamp(new Date())
        );
    }

    private Reading displayReadings() {
        Reading reading = getCurrentReading();
        DecimalFormatter df = new DecimalFormatter();
        tvHeading.setText(df.format(reading.getHeadingAngle()));
        tvRoll.setText(df.format(reading.getRollAngle()));
        tvPitch.setText(df.format(reading.getPitchAngle()));
        tvGyroX.setText(df.format(reading.getGyroX()));
        tvGyroY.setText(df.format(reading.getGyroY()));
        tvGyroZ.setText(df.format(reading.getGyroZ()));
        tvAccelX.setText(df.format(reading.getAccelX()));
        tvAccelY.setText(df.format(reading.getAccelY()));
        tvAccelZ.setText(df.format(reading.getAccelZ()));
        tvMagnetX.setText(df.format(reading.getMagX()));
        tvMagnetY.setText(df.format(reading.getMagY()));
        tvMagnetZ.setText(df.format(reading.getMagZ()));
        sampler.add(reading);
        return reading;
    }

    private void checkAlert(Reading reading) {
        int status = alert.check(reading);
        if (status > Alert.NORMAL)
            sampler.add(reading); // save an extra sampling for the alert status
        alert.render();
    }

    private void saveReading(Reading reading) {
        // if reading is not null, this method was called from postReading and was saved to the server
        // otherwise, it's from the periodic save method call
        boolean savedToServer = (reading != null);
        if (!savedToServer)
            reading = sampler.getReadingForSave();
        logger.write(reading, savedToServer);
    }

    private void postReading() {
        Reading reading = sampler.getReadingForPost();
        if (reading == null || reading.getHeadingAngle() == null) return;
        savingQueue.add(reading);
    }


    private void smsAlert() {
        Reading reading = sampler.getReadingForSMS();
        // check if there is an alert for this batch. If there is, send a message.
        Alert smsAlert = new Alert(this, boatName, pitchAngleAlert, rollAngleAlert);
        int status = smsAlert.check(reading);
        if (status != Alert.NORMAL) {
            SmsService sms = new SmsService(this);
            if (sms != null)
                sms.send(phoneNumber, smsAlert.getMessage());
            else
                Toast.makeText(getApplicationContext(), "Error: Cannot access SMS service.",
                        Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        isRunning = false;
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        manager.unregisterListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        savingQueue.kill();
    }

    @Override
    protected void onResume() {
        super.onResume();
//        if (!sensorsInitialized) {
//            initSensors();
//            registerSensors();
//        }

        registerSensors();
        startTrackingLocation();
        startTimer();
        isRunning = true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case REQUEST_READ_EXTERNAL: case REQUEST_WRITE_EXTERNAL:
                if (isPermissionGranted(grantResults, "Read/Write External Storage")) {
                    initLogger();
                }
                break;
            case REQUEST_LOCATION_PERMISSION:
                if (isPermissionGranted(grantResults, "Location")) {
                    startTrackingLocation();
                }
                break;
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

        return super.onOptionsItemSelected(item);
    }

    // https://talesofcode.com/developing-compass-android-application/ (talesofcode)
    private final float[] accelerometerReading = new float[3];
    private final float[] magnetometerReading = new float[3];
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        final int sensorType = sensorEvent.sensor.getType();

        if (sensorType == Sensor.TYPE_GYROSCOPE) {
            int n = sensorEvent.values.length;
            float[] vals = sensorEvent.values.clone();

            // convert from rad/s to deg/s
            gyroX = (float) (vals[0] * (180 / Math.PI));
            gyroY = (float) (vals[1] * (180 / Math.PI));
            gyroZ = (float) (vals[2] * (180 / Math.PI));
        }
        else if (sensorType == Sensor.TYPE_ACCELEROMETER) {
            int n = sensorEvent.values.length;
            float[] vals = sensorEvent.values.clone();

            accelX = vals[0];
            accelY = vals[1];
            accelZ = vals[2];

            // talesofcode
            //make sensor readings smoother using a low pass filter
            CompassHelper.lowPassFilter(sensorEvent.values.clone(), accelerometerReading);
        }
        else if (sensorType == Sensor.TYPE_MAGNETIC_FIELD) {
            int n = sensorEvent.values.length;
            float[] vals = sensorEvent.values.clone();

            magnetX = vals[0];
            magnetY = vals[1];
            magnetZ = vals[2];

            // talesofcode
            //make sensor readings smoother using a low pass filter
            CompassHelper.lowPassFilter(sensorEvent.values.clone(), magnetometerReading);
        }
        else if (sensorType == Sensor.TYPE_ROTATION_VECTOR) {
            int n = sensorEvent.values.length;
            float[] g = sensorEvent.values.clone();

            // https://stackoverflow.com/questions/48975355/rotation-vector-sensor-values-to-azimuth-roll-and-pitch
            //Normalise
            double norm = Math.sqrt(g[0] * g[0] + g[1] * g[1] + g[2] * g[2] + g[3] * g[3]);
            g[0] /= norm;
            g[1] /= norm;
            g[2] /= norm;
            g[3] /= norm;

            //Set values to commonly known quaternion letter representatives
            double x = g[0];
            double y = g[1];
            double z = g[2];
            double w = g[3];

            //Calculate Pitch in degrees (-180 to 180)
            double sinP = 2.0 * (w * x + y * z);
            double cosP = 1.0 - 2.0 * (x * x + y * y);
            double pitch = Math.atan2(sinP, cosP) * (180 / Math.PI);

            //Calculate Tilt in degrees (-90 to 90)
            double tilt;
            double sinT = 2.0 * (w * y - z * x);
            if (Math.abs(sinT) >= 1)
                tilt = Math.copySign(Math.PI / 2, sinT) * (180 / Math.PI);
            else
                tilt = Math.asin(sinT) * (180 / Math.PI);

            //Calculate Azimuth in degrees (0 to 360; 0 = North, 90 = East, 180 = South, 270 = West)
            double sinA = 2.0 * (w * z + x * y);
            double cosA = 1.0 - 2.0 * (y * y + z * z);
            double azimuth = Math.atan2(sinA, cosA) * (180 / Math.PI);

//            headingAngle = (float) ((azimuth < 0) ? Math.abs(azimuth) : 180.0 + (180.0 - azimuth));
//            headingAngle = (float) azimuth;
            pitchAngle = (float) pitch;
            rollAngle = (float) tilt;
        }

        // talesofcode
        updateHeading();
    }

    // https://talesofcode.com/developing-compass-android-application/
    private boolean isLocationRetrieved = false;
    private void updateHeading() {
        float heading = CompassHelper.calculateHeading(accelerometerReading, magnetometerReading);
        heading = CompassHelper.convertRadtoDeg(heading);
        heading = CompassHelper.map180to360(heading);
//        Log.v("headingangle", "Heading: " + heading);
//        Log.v("headingangle", "Magnetic Declination: " + magneticDeclination);
//        Log.v("headingangle", "isLocationRetrieved: " + isLocationRetrieved);

        if(isLocationRetrieved) {
            // using TRUE heading
            headingAngle = heading + magneticDeclination;
            if(headingAngle > 360) { //if trueHeading was 362 degrees for example, it should be adjusted to be 2 degrees instead
                headingAngle = headingAngle - 360;
            }
        }
        else {
            // using MAGNETIC heading
            headingAngle = heading;
        }
//        Log.v("headingangle", "Heading Angle: " + headingAngle);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // ignore this time
    }
}