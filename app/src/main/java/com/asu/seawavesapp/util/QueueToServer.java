package com.asu.seawavesapp.util;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.asu.seawavesapp.api.ApiClient;
import com.asu.seawavesapp.api.RestApi;
import com.asu.seawavesapp.data.Reading;
import com.asu.seawavesapp.log.ErrorLog;
import com.asu.seawavesapp.log.ReadingLog;

import java.util.LinkedList;
import java.util.Queue;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class QueueToServer implements Runnable {
    private Queue<Reading> queue;
    private RestApi restApi;
    public boolean killed;

    Context context;
    ReadingLog logger;
    ErrorLog errorLog;

    /**
     * Creates a queue that stores readings in preparation for server posting.
     * @param context
     * @param logger
     * @param errorLog
     */
    public QueueToServer(Context context, ReadingLog logger, ErrorLog errorLog) {
        restApi = ApiClient.getApi();
        queue = new LinkedList<>();
        killed = false;
        this.context = context;
        this.logger = logger;
        this.errorLog = errorLog;
    }

    /**
     * Terminates this queue.
     */
    public void kill() {
        killed = true;
    }

    /**
     * Checks whether the queue is still alive or not.
     * @return
     */
    public boolean isKilled() {
        return killed;
    }

    /**
     * Adds a reading to queue to server posting.
     * @param reading
     */
    public void add(Reading reading) {
        queue.add(reading);
//        Log.v("queuetoserver", "Added " + reading);
//        Log.v("queuetoserver", "Queue Count " + queue.size());
    }

    /**
     * Attempts to post a reading to the server.
     */
    private void saveToServer(Reading reading) {
        Call<Reading> call = restApi.addReading(reading);

        if (reading == null || reading.getHeadingAngle() == null) {
            queue.remove(reading);
            return;
        }

        call.enqueue(new Callback<Reading>() {
            @Override
            public void onResponse(Call<Reading> call, Response<Reading> response) {
                Reading responseReading = response.body();
                if (responseReading ==  null || responseReading.getFormattedTimestamp() == null) {
                    errorLog.write("Posting of a reading to server returned null.");
                }
                else {
                    logger.write(responseReading, true);
                    queue.remove(reading);
//                    Log.v("queuetoserver", "Queue Count: " + queue.size() + "; Removed " + reading);
                }
            }

            @Override
            public void onFailure(Call<Reading> call, Throwable t) {
                errorLog.write(t.getLocalizedMessage());
//                Log.v("queuetoserver", "Error: " + t.getLocalizedMessage());
//                Toast.makeText(context, "Cannot save reading to the server.", Toast.LENGTH_SHORT).show();
                call.cancel();
            }
        });

    }

    @Override
    public void run() {
//        Log.v("queuetoserver", "running");
        while (!isKilled()) {
            while (!queue.isEmpty()) {
                Reading reading = queue.peek();
//                Log.v("queuetoserver", reading.toString());
                saveToServer(reading);
                try {
                    Thread.sleep(1000); // pause for 1 second before trying again
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
//        Log.v("queuetoserver", "stopped");
    }
}
