package com.asu.seawavesapp.util;

import android.util.Log;

import com.asu.seawavesapp.api.ApiClient;
import com.asu.seawavesapp.api.RestApi;
import com.asu.seawavesapp.data.Reading;
import com.asu.seawavesapp.log.ErrorLog;
import com.asu.seawavesapp.log.ReadingLog;

import java.util.Deque;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * QueueToServer class takes care of the records that will be sent to the server.
 * It stores the records in a queue and makes use of a thread to send one record per second.
 * It also uses <code>Retrofit</code> to connect to the server's API. However, if the app
 * cannot connect to the server, it simply adds the record to the queue and
 * tries again after one second.
 */
public class QueueToServer {
    private final Deque<Reading> queue;
    private final RestApi restApi;

    private final ReadingLog logger;
    private final ErrorLog errorLog;
    private Long postingDelay; // expected time difference between posts (in milliseconds)

    public boolean killed;
    private boolean isOnline;
    private boolean isFinished;
    private Reading lastAdded;
    private Reading forSending = null;
    private ScheduledFuture<?> future;

    /**
     * Creates an instance of QueueToServer class which contains a queue
     * that stores readings in preparation for server posting.
     *
     * @param logger   - logger file for readings
     * @param errorLog - logger file for errors
     */
    public QueueToServer(ReadingLog logger, ErrorLog errorLog, Long postingDelay) {
        restApi = ApiClient.getApi();
        queue = new LinkedList<>();
        killed = false;
        isOnline = true;
        this.logger = logger;
        this.errorLog = errorLog;
        this.postingDelay = postingDelay;
    }

    /**
     * Sets the posting delay for the queue.
     *
     * @param postingDelay - wait time in between posts; in milliseconds
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
     * @return <true>true</true> if killed; <code>false</code> otherwise
     */
    public boolean isKilled() {
        return killed;
    }

    /**
     * Adds a reading to queue to server posting.
     * @param reading - reading to be posted to the server
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
     * Directly sends the reading to the server, bypassing the queue.
     * Use carefully and sparingly.
     * @param reading
     */
    public void forceSend(Reading reading) {
        saveToServer(reading);
    }

    /**
     * Attempts to post a reading to the server.
     */
    private synchronized void saveToServer(Reading reading) {
        Call<Reading> call = restApi.addReading(reading);
        Log.v("qts.saveToServer", "Sending " + reading);
        forSending = null;
        isFinished = false;

        try {
            call.enqueue(new Callback<Reading>() {
                @Override
                public void onResponse(Call<Reading> call, Response<Reading> response) {
                    Log.v("response", "online");
                    isFinished = true;
                    setOnline(true);
                    Reading responseReading = response.body();
                    if (responseReading == null || responseReading.getFormattedTimestamp() == null) {
                        errorLog.write("Posting of a reading to server returned null.");
                    } else {
                        Log.v("qts.saveToServer", "Received " + responseReading);
                        logger.write(responseReading, true);
                        Log.v("qts.save", "Sent " + reading);
                    }
                }

                @Override
                public void onFailure(Call<Reading> call, Throwable t) {
                    setOnline(false);
                    Log.v("response", "offline");
                    errorLog.write(t.getLocalizedMessage());
                    call.cancel();
                    queue.addFirst(reading);
                }
            });
        } catch (Exception e) {
            Log.v("qts.save", "Error: " + e.getLocalizedMessage());
            setOnline(false);
            call.cancel();
            queue.addFirst(reading);
        }
    }

    /**
     * Creates a schedule for posting to the server every one second.
     */
    public void schedule() {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        future = executor.scheduleAtFixedRate(() -> {
            Log.v("qts", "scheduled");
            if (isKilled())
                future.cancel(false);
            else if (!queue.isEmpty() && forSending == null) {
                forSending = queue.remove();
                Log.v("qts", "Trying: " + forSending);
                saveToServer(forSending);
            }
        }, 0, 1000, TimeUnit.MILLISECONDS);
    }

    private void setOnline(boolean online) {
        isOnline = online;
    }

    public boolean isOnline() {
        return isOnline;
    }

    public boolean isFinished() {
        return isFinished;
    }
}
