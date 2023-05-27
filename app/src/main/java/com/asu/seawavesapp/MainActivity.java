package com.asu.seawavesapp;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.asu.seawavesapp.data.Reading;
import com.asu.seawavesapp.databinding.ActivityMainBinding;
import com.asu.seawavesapp.log.ErrorLog;
import com.asu.seawavesapp.log.ReadingLog;
import com.asu.seawavesapp.service.Alert;
import com.asu.seawavesapp.service.SmsService;
import com.asu.seawavesapp.util.CompassHelper;
import com.asu.seawavesapp.util.DecimalFormatter;
import com.asu.seawavesapp.util.QueueToServer;
import com.asu.seawavesapp.util.Sampler;
import com.asu.seawavesapp.util.Utility;
import com.asu.seawavesapp.util.VoyageHelper;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.io.File;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private TextView tvDateTime;
    private TextView tvStatus;
    private TextView tvHeading;
    private TextView tvPitch;
    private TextView tvRoll;
    private TextView tvGyro;
    private TextView tvAccel;
    private TextView tvMagnet;
    private TextView tvSignal;
    private TextView tvSpeed;
    private TextView tvLatitude;
    private TextView tvLongitude;
    private TextView tvAltitude;
    private TextView tvResponse;
    private Button btStop;

    private final int REQUEST_LOCATION_PERMISSION = 100;
    private final int REQUEST_WRITE_EXTERNAL = 101;
    private final int REQUEST_READ_EXTERNAL = 102;
    private final int REQUEST_SEND_SMS = 103;
    private final int REQUEST_PHONE_STATE = 104;

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

    private SensorManager manager;
    private Sensor mGyroscope;
    private Sensor mAccelerometer;
    private Sensor mMagnetometer;
    private Sensor mRotationVector;
    private boolean sensorsInitialized = false;

    FusedLocationProviderClient mFusedLocationClient;
    Location mLastLocation;
    Location previousLocation;
    LocationCallback mLocationCallback;
    private float longitude;
    private float latitude;

    private final Handler handler = new Handler();
    private Runnable postingRunnable;
    private boolean isRunning = true;
    private final long ONE_SECOND = 1000; // milliseconds
    private final long ONE_MINUTE = ONE_SECOND * 60;
    private Long smsDelay = ONE_MINUTE;
    private Long savingDelay = ONE_MINUTE;
    private Long postingDelay = ONE_MINUTE;
    private Long readingDelay = ONE_SECOND;
    private String phoneNumber = "";
    private String controlPhoneNumber = "";

    private QueueToServer savingQueue;
    private Alert alert;
    private Reading lastSaved;
    private float magneticDeclination;
    // for a more frequent posting when an alert is encounter
    private boolean wasAlertTriggered = false;
    private int noAlertCounter = 0;
    private boolean isLocationRetrieved = false;
    private boolean isResponseStatusVisible = false;
    private Long voyageId;
    private VoyageHelper voyageHelper;
    private boolean initialSent = false;
    private int initSentTries = 0;
    private float speed = 0f;

    @RequiresApi(api = Build.VERSION_CODES.P)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // get initial angles
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            initPitch = extras.getFloat("pitchAngle", 0f);
            initRoll = extras.getFloat("rollAngle", 0f);
            voyageId = extras.getLong("voyageId", 0);
        }

        initLogger();
        initPreferences();
        initUi();
        initLocationServices();
        tvResponse.setVisibility(View.INVISIBLE);
        boolean sensorInit = initSensors();

        if (voyageId > 0)
            voyageHelper = new VoyageHelper(boatId);
        else
            openSetupActivity();

        sampler = new Sampler();
        alert = new Alert(this, boatName, pitchAngleAlert, rollAngleAlert);
        savingQueue = new QueueToServer(logger, errLogs, postingDelay);
        savingQueue.schedule();

        if (sensorInit)
            startTimer();
        if (sensorsInitialized)
            registerSensors();

        btStop.setOnClickListener(view -> {
            forceSendReading();
            voyageHelper.stopVoyage(voyageId);
            openSetupActivity();
        });
    }

    private void openSetupActivity() {
        Intent intent = new Intent(getApplicationContext(), SetupActivity.class);
        if (voyageId != null) {
            intent.putExtra("voyageId", voyageId);
            intent.putExtra("maxPitch", sampler.getMaxPitch());
            intent.putExtra("maxRoll", sampler.getMaxRoll());
            intent.putExtra("minSignal", sampler.getMinSignal());
            intent.putExtra("maxSpeed", sampler.getMaxSpeed());
        }
        try {
            Thread.sleep(1000); // try to delay 1 second to allow other transactions to complete
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        handler.removeCallbacksAndMessages(null);
        startActivity(intent);
        finish();
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
        File folder = Utility.getLogFile(getApplicationContext(), isSdPresent);

        // save the folder to shared preferences
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        pref.edit()
                .putString("logFolder", folder.getAbsolutePath())
                .commit();

        File logFile = new File(folder, filename);
        logger = new ReadingLog(logFile, this);
        Log.v("Logger", "Reading: " + logger.getFileUrl());
        File errorFile = new File(folder, "errors.txt");
        errLogs = new ErrorLog(errorFile, this);
        Log.v("Logger", "Error: " + errLogs.getFileUrl());
    }

    /**
     * Retrieves settings stores in the SharedPreferences.
     */
    private void initPreferences() {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        this.boatId = Long.parseLong(pref.getString(getResources().getString(R.string.id_key), "0"));
        this.boatName = pref.getString(getResources().getString(R.string.boat_key), "Boat");
        this.pitchAngleAlert = Float.parseFloat(pref.getString(getResources().getString(R.string.pitch_key), "15"));
        this.rollAngleAlert = Float.parseFloat(pref.getString(getResources().getString(R.string.roll_key), "20"));
        this.phoneNumber = pref.getString(getResources().getString(R.string.contact_key), "09815639036");
        this.controlPhoneNumber = pref.getString(getResources().getString(R.string.cp_key), "09815639036");
        initTimerPreferences();
    }

    /**
     * Initialize timers based on <code>SharedPreferences</code>.
     */
    private void initTimerPreferences() {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        this.readingDelay = Long.parseLong(pref.getString(getResources().getString(R.string.reading_key), ONE_MINUTE + ""));
        this.smsDelay = Long.parseLong(pref.getString(getResources().getString(R.string.sms_key), ONE_MINUTE + ""));
        this.savingDelay = Long.parseLong(pref.getString(getResources().getString(R.string.saving_key), ONE_MINUTE + ""));
        this.postingDelay = Long.parseLong(pref.getString(getResources().getString(R.string.post_key), ONE_MINUTE + ""));
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
        tvGyro = findViewById(R.id.tvGyro);
        tvAccel = findViewById(R.id.tvAccel);
        tvMagnet = findViewById(R.id.tvMagnet);
        tvSpeed = findViewById(R.id.tvSpeed);
        tvSignal = findViewById(R.id.tvSignal);
        tvLatitude = findViewById(R.id.tvLat);
        tvLongitude = findViewById(R.id.tvLong);
        tvAltitude = findViewById(R.id.tvAlt);
        tvResponse = findViewById(R.id.tvResponse);
        btStop = findViewById(R.id.btStop);
    }

    /**
     * Initializes the location services using FusedLocationClient
     */
    private void initLocationServices() {
        // make sure that the location service is enabled
        Utility.checkLocationService(getApplicationContext());

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);
                mLastLocation = locationResult.getLastLocation();
                isLocationRetrieved = false;
                if (mLastLocation != null) {
                    DecimalFormatter df = new DecimalFormatter(6);
                    // display location details
                    tvLatitude.setText(df.format((float) mLastLocation.getLatitude()));
                    tvLongitude.setText(df.format((float) mLastLocation.getLongitude()));
                    if (mLastLocation.hasAltitude()) {
                        tvAltitude.setText(df.format((float) mLastLocation.getAltitude()));
                        magneticDeclination = CompassHelper.calculateMagneticDeclination(
                                mLastLocation.getLatitude(), mLastLocation.getLongitude(), mLastLocation.getAltitude());
                    }

                    // compute for the speed
                    if (mLastLocation.hasSpeed() && mLastLocation.getSpeed() > 0) {
                        Log.v("speed", "Using getSpeed");
                        speed = mLastLocation.getSpeed();
                    }
                    else if (previousLocation != null) {
                        Log.v("speed", "calculating");
                        // Convert milliseconds to seconds
                        float elapsedTimeInSeconds = (mLastLocation.getTime() - previousLocation.getTime()) / 1000f;
                        float distanceInMeters = previousLocation.distanceTo(mLastLocation);
                        speed = distanceInMeters / elapsedTimeInSeconds;
                        Log.v("speed", "d = " + distanceInMeters);
                        Log.v("speed", "t = " + elapsedTimeInSeconds);
                    }
                    previousLocation = mLastLocation;
                    Log.v("speed", "Speed: " + speed);

                    isLocationRetrieved = true;
                } else {
                    // if no location is retrieved, set to zero
                    tvLatitude.setText(R.string.zero);
                    tvLongitude.setText(R.string.zero);
                }
            }
        };
    }

    /**
     * Method called by requestLocationUpdates() method in startTrackingLocation()
     * that uses high accuracy level and refreshes every 30 seconds.
     *
     * @return instance of <code>LocationRequest</code>
     */
    private LocationRequest getLocationRequest() {
        final long locationDelay = 30 * ONE_SECOND; // 30 seconds
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(locationDelay);
        locationRequest.setFastestInterval(locationDelay);
        locationRequest.setFastestInterval(locationDelay);
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
            StringBuilder s = new StringBuilder(noSensors.get(0));
            for (int i = 1; i < noSensors.size(); i++) {
                s.append(", ").append(noSensors.get(i));
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
     *
     * @return <code>true</code> if initialization succeeds; <code>false</code> otherwise
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
     *
     * @param grantResults - permission to check
     * @return <code>true</code> if granted; <code>false</code> otherwise
     */
    private boolean isPermissionGranted(int[] grantResults) {
        return grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED;
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    private void forceSendReading() {
        Reading iReading = getCurrentReading();
        iReading.setTimestamp(new Date());
        savingQueue.forceSend(iReading);
        saveReading(iReading);
    }

    private void startTimer() {
        Runnable dtRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    if (isRunning)
                        handler.postDelayed(this, ONE_SECOND);
                    displayDateTime();
                    displayResponseStatus();
                } catch (Exception e) {
                    criticalErrorHandler(e);
                }
            }
        };
        Runnable readingRunnable = new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.P)
            @Override
            public void run() {
                try {
                    if (isRunning)
                        handler.postDelayed(this, readingDelay);
                    Reading reading = displayReadings();
                    checkAlert(reading);
                    if (!initialSent && reading.getLongitude() != 0) {
                        // not send and location is available
                        // send data to server for the first location
                        forceSendReading();
                        initialSent = true;
                    }
                    else if (!initialSent && initSentTries > 20) { // 20 tries (5 seconds) is threshold limit
                        // pass 5 seconds and location still not available; send initial anyway
                        forceSendReading();
                        initialSent = true;
                    }
                    else if (!initialSent)
                        initSentTries++;
                } catch (Exception e) {
                    criticalErrorHandler(e);
                }
            }
        };
        Runnable savingRunnable = new Runnable() {
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
        setPostingRunnable();
        Runnable smsRunnable = new Runnable() {
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
                    } else if (ActivityCompat.checkSelfPermission(getApplicationContext(),
                            Manifest.permission.READ_PHONE_STATE)
                            != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]
                                        {Manifest.permission.READ_PHONE_STATE},
                                REQUEST_PHONE_STATE);
                    } else {
                        smsAlert();
                    }
                } catch (Exception e) {
                    criticalErrorHandler(e);
                }
            }
        };

        // wait for a second before starting the timers
        handler.postDelayed(dtRunnable, ONE_SECOND);
        handler.postDelayed(readingRunnable, ONE_SECOND);
        handler.postDelayed(savingRunnable, ONE_SECOND);
        handler.postDelayed(smsRunnable, ONE_SECOND);
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

    @RequiresApi(api = Build.VERSION_CODES.P)
    private Reading getCurrentReading() {
        float lat = 0f;
        float lng = 0f;
        float alt = 0f;

        if (mLastLocation != null) {
            lat = (float) mLastLocation.getLatitude();
            lng = (float) mLastLocation.getLongitude();
            alt = (float) mLastLocation.getAltitude();
        }

        int signalStrength = Utility.getSignalStrength(getApplicationContext());

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
                Utility.formatTimestamp(new Date()),
                voyageId,
                signalStrength,
                speed
        );
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    private Reading displayReadings() {
        Reading reading = getCurrentReading();
        DecimalFormatter df = new DecimalFormatter();
        tvHeading.setText(df.format(reading.getHeadingAngle()));
        tvRoll.setText(df.format(reading.getRollAngle()));
        tvPitch.setText(df.format(reading.getPitchAngle()));
        tvGyro.setText(df.format(reading.getGyroX()) + " · " +
                df.format(reading.getGyroY()) + " · " +
                df.format(reading.getGyroZ()));
        tvAccel.setText(df.format(reading.getAccelX()) + " · " +
                df.format(reading.getAccelY()) + " · " +
                df.format(reading.getAccelZ()));
        tvMagnet.setText(df.format(reading.getMagX()) + " · " +
                df.format(reading.getMagY()) + " · " +
                df.format(reading.getMagZ()));
        tvSignal.setText(reading.getSignalStrength().toString());
        tvSpeed.setText(df.format(reading.getSpeed()));

        sampler.add(reading);
        return reading;
    }

    private void checkAlert(Reading reading) {
        int status = alert.check(reading);
        if (status > Alert.NORMAL) {
            sampler.add(reading); // save an extra sampling for the alert status
            // if first time that the alert was triggered, immediately post to server and send a message
            if (!this.wasAlertTriggered) {
                postReading(reading);
                sendSMS(alert.getMessage("Location is at " +
                        reading.getLongitude() + " deg. longitude, " +
                        reading.getLatitude() + " deg. latitude."));
            }
            // make reading more frequent until "Normal"
            this.wasAlertTriggered = true;
            this.noAlertCounter = 0;
            resetPostingDelay(ONE_MINUTE);
            Log.v("alertF", "Triggered");
        }
        alert.render(tvStatus);
    }

    private void saveReading(Reading reading) {
        // if reading is not null, this method was called from postReading and was saved to the server
        // otherwise, it's from the periodic save method call
        boolean savedToServer = (reading != null);
        if (!savedToServer) {
            if (lastSaved != null) {
                long diff = new Date().getTime() - lastSaved.getSentTimestampDate().getTime();
                Log.v("qts.save", "Time Diff for saving: " + diff);
                // ignore operation when time difference is below the set delay
                if (diff < savingDelay) return;
            }

            reading = sampler.getReadingForSave();
            lastSaved = reading;
        }
        logger.write(reading, savedToServer);
    }

    private void setPostingRunnable() {
        handler.removeCallbacks(postingRunnable);
        Log.v("qts.set", "Handler for posting cancelled.");

        postingRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    Log.v("qts.set", "Run Posting at " + postingDelay);
                    postReading(null);
                    if (isRunning)
                        handler.postDelayed(this, postingDelay);
                } catch (Exception e) {
                    criticalErrorHandler(e);
                }
            }
        };
        handler.postDelayed(postingRunnable, 0);
    }

    private void resetPostingDelay(long delay) {
        if (delay == this.postingDelay) return; // no update, so ignore

        this.postingDelay = delay;
        savingQueue.setPostingDelay(delay);
        postReading(null);
        Log.v("qts.set", "Posting delay: " + delay);
        setPostingRunnable();
    }

    /**
     * Posts a reading to the server. If the reading is <code>null</code>, it indicates that it is
     * an emergency post, and that reading will be sent immediately. Otherwise, it will get a reading
     * from the sampler.
     *
     * @param reading - reading to post
     */
    private void postReading(Reading reading) {
        if (reading == null)
            reading = sampler.getReadingForPost();

        if (!reading.isValid()) {
            Log.v("qts.set", "Invalid: " + reading);
            return;
        }
        savingQueue.add(reading);
//        saveReading(reading);         // this could be the cause of that null in timestamp
        // checking for frequency
        if (wasAlertTriggered) {
            if (Alert.checkReadingForAlert(reading) != Alert.NORMAL) {
                this.noAlertCounter = 0;
                Log.v("alertF", ":" + Alert.checkReadingForAlert(reading));
            } else {
                this.noAlertCounter++;
                if (this.noAlertCounter >= 5) {
                    this.wasAlertTriggered = false;
                    initTimerPreferences();
                    resetPostingDelay(this.postingDelay);
                    sendSMS(boatName + "'s status has normalized.");
                    Log.v("alertF", "Normalized");
                }
            }
        }
    }

    private void sendSMS(String message) {
        SmsService sms = new SmsService(this);
        sms.send(phoneNumber, message);
        sms.send(controlPhoneNumber, message);
    }


    private void smsAlert() {
        Reading reading = sampler.getReadingForSMS();
        // check if there is an alert for this batch. If there is, send a message.
        Alert smsAlert = new Alert(this, boatName, pitchAngleAlert, rollAngleAlert);
        int status = smsAlert.check(reading);
        if (status != Alert.NORMAL) {
            sendSMS(smsAlert.getMessage("Location is at " +
                    mLastLocation.getLongitude() + " deg. longitude, " +
                    mLastLocation.getLatitude() + " deg. latitude."));
            // save to server also
            postReading(reading);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        alert.stopAlert();
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
                if (isPermissionGranted(grantResults)) {
                    initLogger();
                }
                break;
            case REQUEST_LOCATION_PERMISSION:
                if (isPermissionGranted(grantResults)) {
                    startTrackingLocation();
                }
                break;
            case REQUEST_SEND_SMS: case REQUEST_PHONE_STATE:
                if (isPermissionGranted(grantResults)) {
                    smsAlert();
                }
                break;
        }
    }

    // https://talesofcode.com/developing-compass-android-application/ (talesofcode)
    private final float[] accelerometerReading = new float[3];
    private final float[] magnetometerReading = new float[3];
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        final int sensorType = sensorEvent.sensor.getType();

        if (sensorType == Sensor.TYPE_GYROSCOPE) {
            float[] vals = sensorEvent.values.clone();

            // convert from rad/s to deg/s
            gyroX = (float) (vals[0] * (180 / Math.PI));
            gyroY = (float) (vals[1] * (180 / Math.PI));
            gyroZ = (float) (vals[2] * (180 / Math.PI));
        }
        else if (sensorType == Sensor.TYPE_ACCELEROMETER) {
            float[] vals = sensorEvent.values.clone();

            accelX = vals[0];
            accelY = vals[1];
            accelZ = vals[2];

            // talesofcode: make sensor readings smoother using a low pass filter
            CompassHelper.lowPassFilter(sensorEvent.values.clone(), accelerometerReading);
        }
        else if (sensorType == Sensor.TYPE_MAGNETIC_FIELD) {
            float[] vals = sensorEvent.values.clone();

            magnetX = vals[0];
            magnetY = vals[1];
            magnetZ = vals[2];

            // talesofcode
            //make sensor readings smoother using a low pass filter
            CompassHelper.lowPassFilter(sensorEvent.values.clone(), magnetometerReading);
        }
        else if (sensorType == Sensor.TYPE_ROTATION_VECTOR) {
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

            pitchAngle = (float) pitch;
            rollAngle = (float) tilt;
        }

        // talesofcode
        updateHeading();
    }

    // https://talesofcode.com/developing-compass-android-application/
    private void updateHeading() {
        float heading = CompassHelper.calculateHeading(accelerometerReading, magnetometerReading);
        heading = CompassHelper.convertRadtoDeg(heading);
        heading = CompassHelper.map180to360(heading);

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
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // ignore this time
    }

    private void displayResponseStatus() {
        String msg = "";

        boolean isLocationActive = Utility.checkLocationService(getApplicationContext());
        boolean isOnline = savingQueue.isOnline();

        Log.v("response", "New online status: " + isOnline);
        if (!isOnline && !isLocationActive)
            msg = "Offline and location is off.";
        else if (!isOnline)
            msg = "Offline";
        else if (!isLocationActive)
            msg = "Location is off.";

        if (msg.isEmpty() && isResponseStatusVisible) {
            tvResponse.setVisibility(View.INVISIBLE);
            isResponseStatusVisible = false;
        }
        else if (!msg.isEmpty() && !isResponseStatusVisible) {
            tvResponse.setVisibility(View.VISIBLE);
            tvResponse.setText(msg);
            isResponseStatusVisible = true;
        }
        else if (!msg.isEmpty() && isResponseStatusVisible) {
            tvResponse.setText(msg);
            isResponseStatusVisible = true;
        }

        Log.v("response", isResponseStatusVisible + " " + msg);
    }
}