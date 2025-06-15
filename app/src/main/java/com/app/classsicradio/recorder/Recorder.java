package com.app.classsicradio.recorder;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import androidx.annotation.RequiresApi;

import com.app.classsicradio.Config;
import com.app.classsicradio.constants.Constants;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Recorder {

    private FileOutputStream fileOutputStream;
    private InputStream inputStream;
    private final Context context;
    private static final String LOG_TAG = "Audio Recorder";
    private Thread recordingThread;
    private volatile boolean isRecordingActive = false;

    public Recorder(Context context) {
        this.context = context;
    }

    public void startRecording() {
        if (isRecordingActive) {
            Log.w(LOG_TAG, "Recording is already in progress.");
            return;
        }

        try {
            recordingThread = new Thread(new Record());
            recordingThread.start();
            isRecordingActive = true;
            Constants.isRecording = true;
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error starting recording", e);
        }
    }

    private class Record implements Runnable {

        @RequiresApi(api = Build.VERSION_CODES.S)
        @Override
        public void run() {
            String outputFilePath = null;
            try {
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
                String fileName = "Recording-" + timeStamp + ".mp3";
                File storageDir = context.getFilesDir();
                if (!storageDir.exists() && !storageDir.mkdirs()) {
                    throw new Exception("Failed to create directory");
                }
                outputFilePath = new File(storageDir, fileName).getAbsolutePath();

                URLConnection connection = new URL(Config.RADIO_STREAM_URL).openConnection();
                connection.setRequestProperty("User-Agent", "Mozilla/5.0");
                connection.setRequestProperty("Accept", "*/*");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                inputStream = new BufferedInputStream(connection.getInputStream());
                fileOutputStream = new FileOutputStream(outputFilePath);

                byte[] buffer = new byte[8192];
                int bytesRead;
                while (isRecordingActive && (bytesRead = inputStream.read(buffer)) != -1) {
                    fileOutputStream.write(buffer, 0, bytesRead);
                }
            } catch (Exception e) {
                Log.e(LOG_TAG, "Error during recording", e);
            } finally {
                closeStreams();
            }

            if (outputFilePath != null) {
                context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(new File(outputFilePath))));
                Log.d(LOG_TAG, "Recording saved to " + outputFilePath);
            } else {
                Log.e(LOG_TAG, "Recording failed");
            }
        }
    }

    public void stopRecording() {
        try {
            if (!isRecordingActive) {
                Log.w(LOG_TAG, "Recording is not currently active.");
                return;
            }

            isRecordingActive = false;
            Constants.isRecording = false; // Ensures loop exits naturally

            if (recordingThread != null && recordingThread.isAlive()) {
                recordingThread.join(1000); // Wait for thread to finish
            }

            closeStreams();
            Log.d(LOG_TAG, "Recording stopped.");
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error stopping recording", e);
        }
    }

    private void closeStreams() {
        try {
            if (fileOutputStream != null) {
                fileOutputStream.close();
                fileOutputStream = null;
            }
            if (inputStream != null) {
                inputStream.close();
                inputStream = null;
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error closing streams", e);
        }
    }

    public Boolean isRecording() {
        return isRecordingActive;
    }
}