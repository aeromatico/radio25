package com.app.classsicradio.services;

import static android.content.ContentValues.TAG;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Metadata;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.datasource.HttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.analytics.AnalyticsListener;
import androidx.media3.exoplayer.hls.DefaultHlsExtractorFactory;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.source.ProgressiveMediaSource;
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter;
import androidx.media3.extractor.DefaultExtractorsFactory;
import androidx.media3.extractor.ts.DefaultTsPayloadReaderFactory;

import com.app.classsicradio.Config;
import com.app.classicradio.R;
import com.app.classsicradio.activities.MainActivity;
import com.app.classsicradio.models.AlbumArt;
import com.app.classsicradio.albumart.CallbackAlbumArt;
import com.app.classsicradio.albumart.RestAdapter;
import com.app.classsicradio.utils.Utilities;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@UnstableApi
public class RadioService extends Service implements Player.Listener, AudioManager.OnAudioFocusChangeListener {

    public static final String ACTION_PLAY = ".ACTION_PLAY";
    public static final String ACTION_PAUSE = ".ACTION_PAUSE";
    public static final String ACTION_STOP = ".ACTION_STOP";
    private final IBinder iBinder = new LocalBinder();

    private String currentlyPlaying = "";
    private String songMetadata = "";

    Call<CallbackAlbumArt> callback = null;
    ExoPlayer exoPlayer;
    DataSource.Factory dataSourceFactory;
    DefaultBandwidthMeter bandwidthMeter;
    MediaSource newMediaSource;

    private MediaSessionCompat mediaSession;
    private MediaControllerCompat.TransportControls transportControls;
    private WifiManager.WifiLock wifiLock;
    private AudioManager audioManager;
    private MediaNotificationManager notificationManager;

    private boolean serviceInUse = false;
    private String status;
    private String streamUrl;

    public boolean isServiceInUse() {
        return serviceInUse;
    }

    public void setServiceInUse(boolean serviceInUse) {
        this.serviceInUse = serviceInUse;
    }

    public class LocalBinder extends Binder {
        public RadioService getService() {
            return RadioService.this;
        }
    }

    private final MediaSessionCompat.Callback mediasSessionCallback = new MediaSessionCompat.Callback() {
        @Override
        public void onPause() {
            super.onPause();
            pause();
        }

        @Override
        public void onStop() {
            super.onStop();
            stop();
            notificationManager.cancelNotification();
        }

        @Override
        public void onPlay() {
            super.onPlay();
            resume();
        }
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        serviceInUse = true;
        return iBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        notificationManager = new MediaNotificationManager(this);
        wifiLock = ((WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE)).createWifiLock(WifiManager.WIFI_MODE_FULL, "mcScPAmpLock");
        mediaSession = new MediaSessionCompat(this, getClass().getSimpleName());
        transportControls = mediaSession.getController().getTransportControls();
        mediaSession.setActive(true);
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mediaSession.setMetadata(new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, songMetadata)
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, currentlyPlaying)
                .build());
        mediaSession.setCallback(mediasSessionCallback);
        registerReceiver(onCallIncome, new IntentFilter("android.intent.action.PHONE_STATE"));
        registerReceiver(onHeadPhoneDetect, new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));
        bandwidthMeter = new DefaultBandwidthMeter.Builder(this).build();
        dataSourceFactory = buildDataSourceFactory();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || intent.getAction() == null)
            return START_NOT_STICKY;

        String action = intent.getAction();
        Log.d(TAG, "onStartCommand: Received action: " + action);

        int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            stop();
            return START_NOT_STICKY;
        }

        // Direct ExoPlayer control instead of using transportControls
        if (action.equalsIgnoreCase(ACTION_PLAY)) {
            if (exoPlayer != null) {
                resume();
            } else if (streamUrl != null) {
                newPlay(streamUrl);
            }
        } else if (action.equalsIgnoreCase(ACTION_PAUSE)) {
            pause();
        } else if (action.equalsIgnoreCase(ACTION_STOP)) {
            stop();
        }

        return START_NOT_STICKY;
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                exoPlayer.setVolume(0.8f);
                resume();
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                stop();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                if (isPlaying()) pause();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                if (isPlaying())
                    exoPlayer.setVolume(0.1f);
                break;
        }
    }

    @Override
    public boolean onUnbind(Intent intent) {
        serviceInUse = false;
        if (status != null) {
            if (status.equals(PlaybackStatus.IDLE))
                stopSelf();
        }
        return super.onUnbind(intent);
    }

    @Override
    public void onRebind(final Intent intent) {
        serviceInUse = true;
    }

    BroadcastReceiver onCallIncome = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String a = intent.getStringExtra(TelephonyManager.EXTRA_STATE);

            try {
                if (exoPlayer.getPlayWhenReady()) {
                    assert a != null;
                    if (a.equals(TelephonyManager.EXTRA_STATE_OFFHOOK) || a.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
                        exoPlayer.setPlayWhenReady(false);
                    }
                }
            } catch (Exception e) {
                Log.e("RadioService", "onReceive: ", e);

            }
        }
    };

    BroadcastReceiver onHeadPhoneDetect = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                if (exoPlayer.getPlayWhenReady()) {
                    if (streamUrl != null) {
                        playOrPause(streamUrl);
                    }
                }
            } catch (Exception e) {
                Log.e("RadioService", "onReceive: ", e);
            }
        }
    };

    @Override
    public void onPlaybackStateChanged(int playbackState) {
        switch (playbackState) {
            case Player.STATE_BUFFERING:
                status = PlaybackStatus.LOADING;
                break;
            case Player.STATE_ENDED:
                status = PlaybackStatus.STOPPED;
                break;
            case Player.STATE_READY:
                if (exoPlayer.getPlayWhenReady()) {
                    status = PlaybackStatus.PLAYING;
                } else {
                    status = PlaybackStatus.PAUSED;
                }
                break;
            default:
                status = PlaybackStatus.IDLE;
                break;
        }

        if (!status.equals(PlaybackStatus.IDLE))
            notificationManager.startNotification(status);

        Utilities.onEvent(status);
    }

    @Override
    public void onPlayerError(@NonNull PlaybackException error) {
        Utilities.onEvent(PlaybackStatus.ERROR);
    }

    public void newPlay(String streamUrl) {
        this.streamUrl = streamUrl;
        if (wifiLock != null && !wifiLock.isHeld()) {
            wifiLock.acquire();
        }
        exoPlayer = new ExoPlayer.Builder(this).build();
        exoPlayer.addListener(this);
        exoPlayer.addAnalyticsListener(new AnalyticsListener() {
            @Override
            public void onAudioSessionIdChanged(@NonNull EventTime eventTime, int audioSessionId) {
                Utilities.onAudioSessionId(getAudioSessionId());
            }
        });

        MediaItem mediaItem = MediaItem.fromUri(Uri.parse(streamUrl));

        if (streamUrl.contains(".m3u8") || streamUrl.contains(".M3U8")) {
            newMediaSource = new androidx.media3.exoplayer.hls.HlsMediaSource.Factory(dataSourceFactory)
                    .setAllowChunklessPreparation(false)
                    .setExtractorFactory(new DefaultHlsExtractorFactory(DefaultTsPayloadReaderFactory.FLAG_IGNORE_H264_STREAM, false))
                    .createMediaSource(mediaItem);
        } else {
            newMediaSource = new ProgressiveMediaSource.Factory(dataSourceFactory, new DefaultExtractorsFactory())
                    .createMediaSource(mediaItem);
        }
        exoPlayer.setMediaSource(newMediaSource);
        exoPlayer.prepare();
        exoPlayer.setPlayWhenReady(true);
    }

    private DataSource.Factory buildDataSourceFactory() {
        return buildNewDataSourceFactory(bandwidthMeter);
    }

    public DataSource.Factory buildNewDataSourceFactory(DefaultBandwidthMeter bandwidthMeter){
        return new DefaultDataSource.Factory(this, buildHttpDataSourceFactory(bandwidthMeter));
    }

    public HttpDataSource.Factory buildHttpDataSourceFactory(DefaultBandwidthMeter bandwidthMeter){
        return new DefaultHttpDataSource.Factory().setUserAgent(Utilities.getUserAgent(this)).setTransferListener(bandwidthMeter);
    }

    public int getAudioSessionId() {
        return exoPlayer.getAudioSessionId();
    }

    public void resume() {
        if (exoPlayer != null) {
            exoPlayer.setPlayWhenReady(true);
            status = PlaybackStatus.PLAYING;
            notificationManager.startNotification(status);
            Utilities.onEvent(status);
        }
    }

    public void pause() {
        if (exoPlayer != null) {
            exoPlayer.setPlayWhenReady(false);
            status = PlaybackStatus.PAUSED;
            notificationManager.startNotification(status);
            Utilities.onEvent(status);
        }
    }

    public void stop() {
        if (exoPlayer != null) {
            exoPlayer.stop();
        }
        exoPlayer = null;
        notificationManager.cancelNotification();
        audioManager.abandonAudioFocus(this);
        try {
            unregisterReceiver(onCallIncome);
            unregisterReceiver(onHeadPhoneDetect);
        } catch (Exception e) {
            Log.e("RadioService", "stop: ", e);
        }
        wifiLockRelease();
    }

    public void playOrPause(String url) {
        if (url != null) {
            if (exoPlayer != null) {
                if (isPlaying()) {
                    pause();
                } else {
                    resume();
                }
            } else {
                newPlay(url);
            }

        }
    }

    public String getStatus() {
        return status;
    }

    public MediaSessionCompat getMediaSession() {
        return mediaSession;
    }

    public boolean isPlaying() {
        if (exoPlayer != null) {
            return this.status.equals(PlaybackStatus.PLAYING);
        } else {
            return false;
        }
    }

    private void wifiLockRelease() {
        if (wifiLock != null && wifiLock.isHeld()) {
            wifiLock.release();
        }
    }

    @Override
    public void onDestroy() {
        pause();
        try {
            notificationManager.cancelNotification();
            if (exoPlayer != null) {
                exoPlayer.release();
                exoPlayer.removeListener(this);
            }
            mediaSession.release();
            unregisterReceiver(onCallIncome);
            unregisterReceiver(onHeadPhoneDetect);
        } catch (Exception e) {
            Log.e("RadioService", "onDestroy: ", e);
        }
        super.onDestroy();
    }

    @Override
    public void onMetadata(@NonNull Metadata metadata) {
        if(Config.ENABLE_METADATA){
            Handler handler = new Handler();
            handler.post(() -> getMetadata(metadata));
        }
    }

    private void getMetadata(Metadata metadata) {
        if (!metadata.get(0).toString().isEmpty()) {
            String data = metadata.get(0).toString().replace("ICY: ", "");
            ArrayList<String> arrayList = new ArrayList<>(Arrays.asList(data.split(",")));
            String[] mediaMetadata = arrayList.get(0).split("=");

            String currentSong = mediaMetadata[1].replace("\"", "");

            // Handle cases where the song name is null or empty
            if (currentSong.contains("null") || currentSong.isEmpty()) {
                currentSong = getString(R.string.unknown_song);
            }

            // Check if the song is playing
            if (isPlaying() && !currentSong.equals(getString(R.string.unknown_song))) {
                currentlyPlaying = getString(R.string.now_playing);
                songMetadata = currentSong;

                // Separate artist and title if possible
                String artist = songMetadata;
                String title = "";

                // Common format is "Artist - Title"
                if (songMetadata.contains(" - ")) {
                    String[] parts = songMetadata.split(" - ", 2);
                    artist = parts[0].trim();
                    title = parts[1].trim();
                }

                // Ensure we have valid values
                if (artist.isEmpty()) artist = getString(R.string.unknown_artist);
                if (title.isEmpty()) title = getString(R.string.unknown_title);

                // Update media session metadata with separated artist and title
                mediaSession.setMetadata(new MediaMetadataCompat.Builder()
                        .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
                        .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
                        .build());

                // Update notification and UI
                notificationManager.updateNotificationMetadata(artist, title);
                notificationManager.startNotification(status);
                MainActivity.updateSongName(artist, title);

                // Update album art using just the artist name for better search results
                if(Config.ENABLE_ALBUM_ART) {
                    searchAlbumArt(artist, title);
                }
            }
        }
    }

    private void searchAlbumArt(String artist, String title) {
        String searchQuery = artist;
        if (!title.isEmpty()) {
            // Optionally include title for more specific search
            searchQuery += " " + title;
        }

        callback = RestAdapter.createAlbumArtAPI().getAlbumArt(searchQuery, "music", 1);
        callback.enqueue(new Callback<>() {
            public void onResponse(@NonNull Call<CallbackAlbumArt> call, @NonNull Response<CallbackAlbumArt> response) {
                CallbackAlbumArt resp = response.body();
                if (resp != null && resp.resultCount != 0) {
                    ArrayList<AlbumArt> albumArts = resp.results;
                    String artWorkUrl = albumArts.get(0).artworkUrl100.replace("100x100bb", "300x300bb");
                    MainActivity.updateAlbumArt(artWorkUrl);
                    notificationManager.loadBitmapFromURL(artWorkUrl);

                    loadBitmapFromURL(artWorkUrl, bitmap -> {
                        notifyMediaBrowserService(artist, title, bitmap);
                    });
                } else {
                    MainActivity.updateAlbumArt("");
                    notificationManager.loadBitmapFromURL("");
                }
            }

            public void onFailure(@NonNull Call<CallbackAlbumArt> call, @NonNull Throwable th) {
                Log.d(TAG, "onFailure");
            }
        });
    }

    // Update this method to handle separate artist and title
    private void notifyMediaBrowserService(String artist, String title, Bitmap albumArt) {
        RadioMediaBrowserService instance = RadioMediaBrowserService.getInstance();
        if (instance != null) {
            instance.updateMetadata(title, artist, albumArt);
            instance.setPlayingState(true);
        }
    }

    private void loadBitmapFromURL(String url, BitmapCallback callback) {
        if (TextUtils.isEmpty(url)) {
            callback.onBitmapLoaded(null);
            return;
        }

        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(url).openConnection();
                connection.setDoInput(true);
                connection.connect();
                InputStream input = connection.getInputStream();
                Bitmap bitmap = BitmapFactory.decodeStream(input);
                new Handler(Looper.getMainLooper()).post(() -> callback.onBitmapLoaded(bitmap));
            } catch (IOException e) {
                Log.e(TAG, "Error loading bitmap from URL", e);
                new Handler(Looper.getMainLooper()).post(() -> callback.onBitmapLoaded(null));
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }).start();
    }

    interface BitmapCallback {
        void onBitmapLoaded(@Nullable Bitmap bitmap);
    }
}