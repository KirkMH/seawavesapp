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
    private Reading lastAdded;
    private Reading forSending = null;

    private Context context;
    private ReadingLog logger;
    private ErrorLog errorLog;
    private Long postingDelay = 0L; // expected time difference between posts (in milliseconds)

    /**
     * Creates a queue that stores readings in preparation for server posting.
     * @param context
     * @param logger
     * @param errorLog
     */
    public QueueToServer(Context context, ReadingLog logger, ErrorLog errorLog, Long postingDelay) {
        restApi = ApiClient.getApi();
        queue = new LinkedList<>();
        killed = false;
        this.context = context;
        this.logger = logger;
        this.errorLog = errorLog;
        this.postingDelay = postingDelay;
    }

    /**
     * Sets the posting delay for the queue
     * @param postingDelay
     */
    public void setPostingDelay(Long postingDelay) {
        this.postingDelay = postingDelay;
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
    public synchronized void add(Reading reading) {
        // make sure that it is a valid reading
        if (reading != null && reading.isValid()) {
            Log.v("qts.add", "Received " + reading);
            // check if the time difference between the last received reading and this reading is as expected
            boolean isTimeDifValid = true;
            if (lastAdded != null) {
                long diff = reading.getSentTimestampDate().getTime() - lastAdded.getSentTimestampDate().getTime();
                isTimeDifValid = diff >= postingDelay;
                Log.v("qts.add", "Time Diff: " + diff);
            }
            if (isTimeDifValid) {
                Log.v("qts.add", "Added: " + reading);
                queue.add(reading);
                lastAdded = reading;
            }
        }
    }

    /**
     * Attempts to post a reading to the server.
     */
    private synchronized void saveToServer(Reading reading) {
        Call<Reading> call = restApi.addReading(reading);
        Log.v("qts.save", reading.toString());

        call.enqueue(new Callback<Reading>() {
            @Override
            public void onResponse(Call<Reading> call, Response<Reading> response) {
                Reading responseReading = response.body();
                if (responseReading ==  null || responseReading.getFormattedTimestamp() == null) {
                    errorLog.write("Posting of a reading to server returned null.");
                }
                else {
                    logger.write(responseReading, true);
                    forSending = null;
                    Log.v("qts.save", "Sent " + reading);
                }
            }

            @Override
            public void onFailure(Call<Reading> call, Throwable t) {
                errorLog.write(t.getLocalizedMessage());
                call.cancel();
            }
        });

    }

    @Override
    public void run() {
        while (!isKilled()) {
            while (!queue.isEmpty()) {
                if (forSending == null) {
                    forSending = queue.remove();
                    saveToServer(forSending);
                }
                try {
                    Thread.sleep(1000); // pause for 1 second before trying again
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
