package com.app.classsicradio.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.support.v4.media.session.MediaSessionCompat;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.media3.common.util.UnstableApi;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.FutureTarget;
import com.bumptech.glide.request.target.Target;
import com.app.classicradio.R;
import com.app.classsicradio.activities.MainActivity;

import java.util.concurrent.ExecutionException;

@UnstableApi
public class MediaNotificationManager {

    public static final int NOTIFICATION_ID = 555;
    public static final String NOTIFICATION_CHANNEL_ID = "radio_channel";

    private final RadioService service;
    private String nowPlaying;
    private String songName;
    private Bitmap notifyIcon;
    private String playbackStatus;

    private final NotificationManager notificationManager;
    private NotificationCompat.Builder builder;

    @OptIn(markerClass = UnstableApi.class)
    public MediaNotificationManager(RadioService service) {
        this.service = service;
        this.notificationManager = (NotificationManager) service.getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    service.getString(R.string.radio_channel_name),
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setSound(null, null);
            notificationManager.createNotificationChannel(channel);
        }
    }

    public void startNotification(String playbackStatus) {
        this.playbackStatus = playbackStatus;
        buildNotification();
        service.startForeground(NOTIFICATION_ID, builder.build());
    }

    public void updateNotificationIcon(Bitmap notifyIcon) {
        this.notifyIcon = notifyIcon;
        buildNotification();
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    public void updateNotificationMetadata(String songName, String nowPlaying) {
        this.songName = songName;
        this.nowPlaying = nowPlaying;
        buildNotification();
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    private void buildNotification() {
        if (playbackStatus == null) return;

        if (notifyIcon == null) {
            notifyIcon = BitmapFactory.decodeResource(service.getResources(), R.drawable.radio_image);
        }

        Intent playbackActionIntent = new Intent(service, RadioService.class);
        playbackActionIntent.setAction(playbackStatus.equals(PlaybackStatus.PAUSED) ? RadioService.ACTION_PLAY : RadioService.ACTION_PAUSE);

        PendingIntent playbackAction = PendingIntent.getService(
                service,
                playbackStatus.hashCode(),
                playbackActionIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        Intent stopIntent = new Intent(service, RadioService.class);
        stopIntent.setAction(RadioService.ACTION_STOP);
        PendingIntent stopAction = PendingIntent.getService(
                service,
                RadioService.ACTION_STOP.hashCode(),
                stopIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        Intent openAppIntent = new Intent(service, MainActivity.class);
        openAppIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(
                service,
                0,
                openAppIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        int playPauseIcon = playbackStatus.equals(PlaybackStatus.PAUSED) ? androidx.media3.session.R.drawable.media3_icon_play : androidx.media3.session.R.drawable.media3_icon_pause;
        String playPauseLabel = playbackStatus.equals(PlaybackStatus.PAUSED) ? service.getString(R.string.play) : service.getString(R.string.pause);

        MediaSessionCompat mediaSession = service.getMediaSession();

        builder = new NotificationCompat.Builder(service, NOTIFICATION_CHANNEL_ID)
                .setContentTitle(nowPlaying)
                .setContentText(songName)
                .setLargeIcon(notifyIcon)
                .setContentIntent(contentIntent)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setSmallIcon(androidx.media3.session.R.drawable.media_session_service_notification_ic_music_note)
                .addAction(playPauseIcon, playPauseLabel, playbackAction)
                .addAction(androidx.media3.session.R.drawable.media3_icon_stop, service.getString(R.string.stop), stopAction)
                .setWhen(System.currentTimeMillis())
                .setColor(service.getResources().getColor(R.color.color_light_primary))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mediaSession.getSessionToken())
                        .setShowActionsInCompactView(0, 1)
                        .setShowCancelButton(true)
                        .setCancelButtonIntent(stopAction));
    }

    public void cancelNotification() {
        service.stopForeground(true);
        NotificationManagerCompat.from(service).cancel(NOTIFICATION_ID);
    }

    public void loadBitmapFromURL(@NonNull String url) {
        new Thread(() -> {
            try {
                FutureTarget<Bitmap> futureTarget = Glide.with(service)
                        .asBitmap()
                        .load(url)
                        .submit(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL); // Request higher resolution
                notifyIcon = futureTarget.get();
                Glide.with(service).clear(futureTarget);
                updateNotificationIcon(notifyIcon);
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }
}