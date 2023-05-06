package com.asu.seawavesapp.log;

import androidx.appcompat.app.AppCompatActivity;

import com.asu.seawavesapp.data.Reading;
import com.asu.seawavesapp.util.Utility;

import java.io.File;

/**
 * ErrorLog is used to write the errors encountered while running the application.
 * It is a subclass of the <code>Logger</code> class.
 */
public class ReadingLog extends Logger {

    /**
     * Creates an instance of the ReadingLog.
     *
     * @param logFile - log file to use
     * @param context - application context
     */
    public ReadingLog(File logFile, AppCompatActivity context) {
        super(logFile, context);
    }

    /**
     * Writes the reading to the log file. If server is true,
     * 'Posted to server' will also be appended.
     *
     * @param reading - the reading to write
     * @param server  - set to <code>true</code> if this reading has been posted to the server
     */
    public void write(Reading reading, boolean server) {
        // ignore invalid readings
        if (!reading.isValid() || reading.getSentTimestampDate() == null) return;

        // write the heading
        if (isFirstWrite()) {
            String heading = "Timestamp, Heading Angle, Pitch Angle, Roll Angle, Gyroscope-X, Gyroscope-Y, Gyroscope-Z, Accelerometer-X, Accelerometer-Y, Accelerometer-Z, Magnetometer-X, Magnetometer-Y, Magnetometer-Z, Latitude, Longitude, Altitude";
            write(heading);
        }

        // setup the content to be written
        String content;
        if (server)
            content = reading.getFormattedTimestamp() + "," + reading;  // see toString() of Reading
        else
            content = Utility.getCurrentTimestamp() + "," + reading;    // see toString() of Reading
        if (server) content += ", Posted to server";

        // write to file
        write(content);
    }
}
