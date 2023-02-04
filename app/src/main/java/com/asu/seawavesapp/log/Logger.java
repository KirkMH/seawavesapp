package com.asu.seawavesapp.log;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * The Logger class is the base class of logger files used in this project.
 */
public class Logger {
    private final Context context;
    private final File logFile;
    private BufferedWriter file;
    private boolean firstWrite;
    private boolean isError = false;

    /**
     * Creates an instance of the Logger class.
     *
     * @param logFile - file to which logs will be written
     * @param context - application context
     */
    public Logger(File logFile, AppCompatActivity context) {
        this.logFile = logFile;
        this.context = context;
        firstWrite = !logFile.exists();
    }

    /**
     * Prepares the log file for writing.
     */
    private void open() {
        try {
            this.file = new BufferedWriter(new FileWriter(logFile, true));
        } catch (IOException e) {
            displayError(e.getLocalizedMessage());
        }
    }

    public Context getContext() {
        return context;
    }

    /**
     * Returns the absolute path of the log file.
     *
     * @return - absolute path
     */
    public String getFileUrl() {
        return logFile.getAbsolutePath();
    }

    /**
     * Determines whether an error occurred during this class's operation or not
     *
     * @return <code>true</code> if an error occurred during processing; <code>false</code> otherwise
     */
    public boolean isError() {
        return isError;
    }

    /**
     * Determines whether or not it is the first time to write in this log file
     *
     * @return <code>true</code> if it is the first time to write; <code>false</code> otherwise
     */
    public boolean isFirstWrite() {
        return firstWrite;
    }

    /**
     * Takes a string parameter to be appended to the log file.
     * @param str - the string to write
     */
    public void write(String str) {
        // write to file
        try {
            open();
            file.append(str);
            file.newLine();
            file.close();
            firstWrite = false;
            Log.v("Logger", "wrote: " + str);
        } catch (Exception e) {
            e.printStackTrace();
            displayError(e.getLocalizedMessage());
        }
    }

    /**
     * Displays the error message in a toast.
     * @param message - the error message to display
     */
    private void displayError(String message) {
//        if (!isError())
        Toast.makeText(context, "Log Error: " + message, Toast.LENGTH_LONG).show();
        isError = true;
    }
}
