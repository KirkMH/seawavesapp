package com.asu.seawavesapp.log;

import android.content.Context;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class Logger {
    private Context context;
    private File logFile;
    private BufferedWriter file;
    private boolean firstWrite;
    private boolean isError = false;

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
            Toast.makeText(context, "Error initializing logger files.", Toast.LENGTH_LONG).show();
            isError = true;
        }
    }

    public Context getContext() {
        return context;
    }

    protected String getFileUrl() {
        return logFile.getAbsolutePath();
    }

    /**
     * Determines whether an error occurred during this class's operation or not
     * @return
     */
    public boolean isError() {
        return isError;
    }

    /**
     * Determines whether or not it is the first time to write in this log file
     * @return
     */
    public boolean isFirstWrite() {
        return firstWrite;
    }

    /**
     * Takes a string parameter to be appended to the log file.
     * @param str
     */
    public void write(String str) {
        // write to file
        try {
            open();
            file.append(str);
            file.newLine();
            file.close();
            firstWrite = false;
        } catch (Exception e) {
            displayError(e.getLocalizedMessage());
        }
    }

    /**
     * Displays the error message in a toast.
     * @param message
     */
    private void displayError(String message) {
        Toast.makeText(context, "Log Error: " + message, Toast.LENGTH_LONG).show();
        isError = true;
    }
}
