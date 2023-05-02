package com.asu.seawavesapp.util;

import android.content.Context;
import android.location.LocationManager;
import android.widget.Toast;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * The Utility class contains different utility methods used by the app.
 */
public class Utility {
    /**
     * Returns a formatted current timestamp.
     *
     * @return formatted current timestamp
     */
    public static String getCurrentTimestamp() {
        // current timestamp
        return formatTimestamp(new Date());
    }

    /**
     * Formats the timestamp using the pattern yyyy-MM-dd HH:mm:ss
     *
     * @param date - timestamp to format
     * @return formatted timestamp
     */
    public static String formatTimestamp(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String timestamp = sdf.format(date);
        return timestamp;
    }

    /**
     * Formats the timestamp using the pattern yyyy-MM-dd'T'HH:mm:ss'Z' for SQL.
     *
     * @param date - timestamp to format
     * @return formatted timestamp
     */
    public static String formatTimestampForServer(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        String timestamp = sdf.format(date);
        return timestamp;
    }

    /**
     * Parses the timestamp and returns the date.
     * The timestamp's pattern should be yyyy-MM-dd HH:mm:ss.
     *
     * @param timestamp - timestamp to convert
     * @return date from the timestamp
     */
    public static Date timestampToDate(String timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            Date date = sdf.parse(timestamp);
            return date;
        } catch (ParseException e) {
            return null;
        }
    }


    /**
     * Checks whether the phone's location service is enabled or not.
     * When not enabled, a toast will be displayed to inform the user.
     * @param context   The context.
     * @return          True if enabled; false otherwise.
     */
    public static boolean checkLocationService(Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        Boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        Boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        if (!isGPSEnabled && !isNetworkEnabled) {
            Toast.makeText(context, "Please turn on the Location Service of your phone.",
                    Toast.LENGTH_LONG).show();
        }
        return (isGPSEnabled && isNetworkEnabled);
    }
}
