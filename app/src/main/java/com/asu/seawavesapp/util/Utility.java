package com.asu.seawavesapp.util;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Utility {
    public static String getCurrentTimestamp() {
        // current timestamp
        return formatTimestamp(new Date());
    }

    public static String formatTimestamp(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String timestamp = sdf.format(date);
        return timestamp;
    }

    public static String formatTimestampForServer(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        String timestamp = sdf.format(date);
        return timestamp;
    }
}
