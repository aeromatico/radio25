package com.app.classsicradio.services;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.core.app.NotificationCompat;
import androidx.media.MediaBrowserServiceCompat;
import androidx.media3.common.util.UnstableApi;

import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import java.util.ArrayList;
import java.util.List;

public class RadioMediaBrowserService extends MediaBrowserServiceCompat {

    private static final String TAG = "RadioMediaBrowserService";
    private static final String NOTIFICATION_CHANNEL_ID = "radio_channel";
    private static final int NOTIFICATION_ID = 1;
    private MediaSessionCompat mediaSession;

    // Singleton instance
    private static RadioMediaBrowserService instance;

    /**
     * Get the singleton instance of the service
     * @return The service instance or null if not yet created
     */
    public static RadioMediaBrowserService getInstance() {
        return instance;
    }

    @SuppressLint("LongLogTag")
    @Override
    public void onCreate() {
        super.onCreate();
        // Set the singleton instance
        instance = this;

        Log.d(TAG, "onCreate: MediaBrowserService created");
        createNotificationChannel();

        // Initialize MediaSession
        mediaSession = new MediaSessionCompat(this, "RadioMediaBrowserService");
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        // Set initial PlaybackState - CRITICAL for Android Auto
        PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder()
                .setActions(
                        PlaybackStateCompat.ACTION_PLAY |
                                PlaybackStateCompat.ACTION_PAUSE |
                                PlaybackStateCompat.ACTION_PLAY_PAUSE |
                                PlaybackStateCompat.ACTION_STOP |
                                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
                                PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                );
        mediaSession.setPlaybackState(stateBuilder.build());

        // In RadioMediaBrowserService.java, modify the MediaSessionCompat.Callback:

        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            @OptIn(markerClass = UnstableApi.class)
            @Override
            public void onPlay() {
                // Connect to the actual RadioService to start playback
                Intent intent = new Intent(getApplicationContext(), RadioService.class);
                intent.setAction(RadioService.ACTION_PLAY); // Make sure this action is defined in RadioService
                startService(intent);

                startForegroundService();
                mediaSession.setActive(true);
                PlaybackStateCompat playbackState = new PlaybackStateCompat.Builder()
                        .setState(PlaybackStateCompat.STATE_PLAYING, 0, 1.0f)
                        .setActions(PlaybackStateCompat.ACTION_PAUSE | PlaybackStateCompat.ACTION_STOP)
                        .build();
                mediaSession.setPlaybackState(playbackState);
            }

            @OptIn(markerClass = UnstableApi.class)
            @Override
            public void onPause() {
                // Connect to the actual RadioService to pause playback
                Intent intent = new Intent(getApplicationContext(), RadioService.class);
                intent.setAction(RadioService.ACTION_PAUSE);
                startService(intent);

                PlaybackStateCompat playbackState = new PlaybackStateCompat.Builder()
                        .setState(PlaybackStateCompat.STATE_PAUSED, 0, 0)
                        .setActions(PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_STOP)
                        .build();
                mediaSession.setPlaybackState(playbackState);
            }

            @OptIn(markerClass = UnstableApi.class)
            @Override
            public void onStop() {
                // Connect to the actual RadioService to stop playback
                Intent intent = new Intent(getApplicationContext(), RadioService.class);
                intent.setAction(RadioService.ACTION_STOP);
                startService(intent);

                PlaybackStateCompat playbackState = new PlaybackStateCompat.Builder()
                        .setState(PlaybackStateCompat.STATE_STOPPED, 0, 0)
                        .setActions(PlaybackStateCompat.ACTION_PLAY)
                        .build();
                mediaSession.setPlaybackState(playbackState);
                mediaSession.setActive(false);
                stopForeground(true);
                stopSelf();
            }
        });

        // Set session token
        setSessionToken(mediaSession.getSessionToken());
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    "Radio Playback",
                    NotificationManager.IMPORTANCE_LOW);

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void startForegroundService() {
        // Create a basic notification for the foreground service
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setContentTitle("Radio is playing")
                .setContentText("Tap to control playback")
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(true);

        // Link the notification to the media session
        builder.setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                .setMediaSession(mediaSession.getSessionToken())
                .setShowActionsInCompactView(0, 1));

        startForeground(NOTIFICATION_ID, builder.build());
    }

    @SuppressLint("LongLogTag")
    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, Bundle rootHints) {
        // Check if client is Android Auto
        boolean isAutomotive = rootHints != null &&
                rootHints.getBoolean("android.media.browse.CONTENT_STYLE_SUPPORTED", false);

        if (isAutomotive) {
            Log.d(TAG, "onGetRoot: Connection from Android Auto");
        }

        return new BrowserRoot("root", null);
    }

    @SuppressLint("LongLogTag")
    @Override
    public void onLoadChildren(@NonNull String parentId, @NonNull Result<List<MediaBrowserCompat.MediaItem>> result) {
        List<MediaBrowserCompat.MediaItem> mediaItems = new ArrayList<>();
        // Add at least one media item for browsing
        MediaDescriptionCompat description = new MediaDescriptionCompat.Builder()
                .setMediaId("radio_station")
                .setTitle("Radio Station")
                .build();
        mediaItems.add(new MediaBrowserCompat.MediaItem(description,
                MediaBrowserCompat.MediaItem.FLAG_PLAYABLE));
        result.sendResult(mediaItems);

        Log.d(TAG, "onLoadChildren: Provided " + mediaItems.size() + " items");
    }

    /**
     * Updates the metadata displayed in Android Auto and other media controllers
     */
    @SuppressLint("LongLogTag")
    public void updateMetadata(String songTitle, String albumName, Bitmap albumArt) {
        if (mediaSession == null) {
            Log.e(TAG, "updateMetadata: mediaSession is null");
            return;
        }

        // Much more detailed metadata to ensure Android Auto displays it
        MediaMetadataCompat.Builder builder = new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, songTitle)
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, songTitle)
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, albumName)
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, albumName)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, "Radio Station")
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION, "Currently playing")
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, -1);

        if (albumArt != null) {
            builder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, albumArt)
                    .putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, albumArt);
        }

        Log.d(TAG, "Setting metadata: " + songTitle + " - " + albumName);
        mediaSession.setMetadata(builder.build());

        // Ensure we have an active playback state
        setPlayingState(true);
    }

    public void setPlayingState(boolean isPlaying) {
        if (mediaSession == null) return;

        int state = isPlaying ? PlaybackStateCompat.STATE_PLAYING : PlaybackStateCompat.STATE_PAUSED;
        long actions = isPlaying ?
                (PlaybackStateCompat.ACTION_PAUSE | PlaybackStateCompat.ACTION_STOP) :
                (PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_STOP);

        PlaybackStateCompat playbackState = new PlaybackStateCompat.Builder()
                .setState(state, 0, isPlaying ? 1.0f : 0.0f)
                .setActions(actions)
                .build();
        mediaSession.setPlaybackState(playbackState);

        if (isPlaying) {
            mediaSession.setActive(true);
        }
    }

    @SuppressLint("LongLogTag")
    @Override
    public void onDestroy() {
        if (mediaSession != null) {
            mediaSession.release();
            mediaSession = null;
        }
        instance = null;
        Log.d(TAG, "onDestroy: MediaBrowserService destroyed");
        super.onDestroy();
    }
}