package com.asu.seawavesapp.log;

import androidx.appcompat.app.AppCompatActivity;

import com.asu.seawavesapp.util.Utility;

import java.io.File;

public class ErrorLog extends Logger {

    public ErrorLog(File logFile, AppCompatActivity context) {
        super(logFile, context);
    }

    /**
     * Appends the message from the exception to the error log file,
     * and adds the stack trace to it.
     * @param ex
     */
    public void write(Exception ex) {
        String message = Utility.getCurrentTimestamp() + "\t" + ex.getLocalizedMessage() + "\n";
        super.write(message);
    }

    /**
     * Takes a string parameter to be appended to the error log file.
     * @param str
     */
    public void write(String str) {
        String message = Utility.getCurrentTimestamp() + "\t" + str + "\n";
        super.write(message);
    }
}
