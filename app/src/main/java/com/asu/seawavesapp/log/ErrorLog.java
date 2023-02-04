package com.asu.seawavesapp.log;

import androidx.appcompat.app.AppCompatActivity;

import com.asu.seawavesapp.util.Utility;

import java.io.File;

/**
 * ErrorLog is used to write the errors encountered while running the application.
 * It is a subclass of the <code>Logger</code> class.
 */
public class ErrorLog extends Logger {

    /**
     * Creates an instance of the ErrorLog class.
     *
     * @param logFile - log file to use
     * @param context - application context
     */
    public ErrorLog(File logFile, AppCompatActivity context) {
        super(logFile, context);
    }

    /**
     * Appends the message from the exception to the error log file,
     * and adds the stack trace to it.
     *
     * @param ex - Exception encountered
     */
    public void write(Exception ex) {
        String message = Utility.getCurrentTimestamp() + "\t" + ex.getLocalizedMessage() + "\n";
        super.write(message);
    }

    /**
     * Takes a string parameter to be appended to the error log file.
     *
     * @param str - message to write to the log file
     */
    public void write(String str) {
        String message = Utility.getCurrentTimestamp() + "\t" + str + "\n";
        super.write(message);
    }
}
