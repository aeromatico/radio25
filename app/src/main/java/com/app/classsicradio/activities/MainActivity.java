package com.app.classsicradio.activities;

import static com.app.classsicradio.Config.ENABLE_AUTOPLAY;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.media3.common.util.UnstableApi;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.app.classsicradio.adapters.SocialIconsAdapter;
import com.app.classsicradio.albumart.AlbumArtShapeUtil;
import com.app.classsicradio.recorder.Recorder;
import com.app.classsicradio.utils.Preferences;
import com.app.classsicradio.utils.SleepTimeReceiver;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.app.classsicradio.Config;
import com.app.classicradio.R;
import com.app.classsicradio.utils.Utilities;
import com.app.classsicradio.services.PlaybackStatus;
import com.app.classsicradio.services.RadioManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.makeramen.roundedimageview.RoundedImageView;
import com.tomergoldst.tooltips.ToolTip;
import com.tomergoldst.tooltips.ToolTipsManager;

import java.util.Random;

import app.utils.BaseActivity;
import app.utils.tools.Tools;
import eu.gsottbauer.equalizerview.EqualizerView;

@UnstableApi
@SuppressLint("StaticFieldLeak")
public class MainActivity extends BaseActivity implements Utilities.EventListener, View.OnClickListener {


    private ToolTipsManager toolTipsManager;
    private AlertDialog dialog;
    private Preferences sharedPref;
    private Recorder recorder;
    private AppCompatImageView recordIcon;
    private FloatingActionButton playPauseButton;
    private RadioManager radioManager;
    private ProgressBar loadingProgressBar;
    private EqualizerView equalizerView;
    private boolean isRecording = false;

    private static ImageView backgroundImageView;
    private static RoundedImageView albumArtImageView;
    private static TextView artistName, songName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(getString(R.string.app_name));
        setSupportActionBar(toolbar);

        initializeUI();
        initializeSocialIcons();
        initializeRadioManager();
        toolTipsManager = new ToolTipsManager();

        if (isPlaying()) {
            onAudioSessionId(RadioManager.getService().getAudioSessionId());
        }

        checkAutoplay();
        recorder = new Recorder(this);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                showExitDialog();
            }
        });
    }

    private void initializeUI() {
        sharedPref = new Preferences(this);
        sharedPref.checkSleepTime();
        playPauseButton = findViewById(R.id.fab_play);
        playPauseButton.setOnClickListener(this);
        loadingProgressBar = findViewById(R.id.progress_bar);
        equalizerView = findViewById(R.id.equalizer_view);
        equalizerView.setAnimationDuration(6000);
        LinearLayout socialIconsLayout = findViewById(R.id.social_icons_layout);
        if (Config.ENABLE_SOCIAL_ICONS) {
            socialIconsLayout.setVisibility(View.VISIBLE);
        } else {
            socialIconsLayout.setVisibility(View.GONE);
        }

        RoundedImageView albumArtBackground = findViewById(R.id.album_art_bg);
        albumArtImageView = findViewById(R.id.album_art);

        AlbumArtShapeUtil.setShape(
                albumArtImageView,
                albumArtBackground,
                Config.ALBUM_ART_TYPE,
                Config.ALBUM_ART_BORDER_ENABLED,
                Config.ALBUM_ART_BORDER_WIDTH,
                ContextCompat.getColor(this, Config.ALBUM_ART_BORDER_COLOR)
        );

        backgroundImageView = findViewById(R.id.background_image);
        Glide.with(this).load(R.drawable.bg_image).into(backgroundImageView);

        artistName = findViewById(R.id.artistName);
        songName = findViewById(R.id.songTitle);
        songName.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        songName.setMarqueeRepeatLimit(-1); // Infinite repeat
        songName.setSingleLine(true);
        songName.setSelected(true);
        songName.setHorizontallyScrolling(true);
        songName.setFocusable(true);
        songName.setFocusableInTouchMode(true);
        songName.postDelayed(() -> songName.setSelected(true), 500);
        songName.setFreezesText(false);

        AppCompatImageView timerIcon = findViewById(R.id.timer_icon);
        timerIcon.setOnClickListener(v -> {
            if (sharedPref.getIsSleepTimeOn()) {
                showStopTimerDialog(sharedPref.getSleepTime() - System.currentTimeMillis());
            } else {
                showTimeSelectionDialog();
            }
        });

        recordIcon = findViewById(R.id.record_icon);
        recordIcon.setOnClickListener(v -> handleRecording());

        AppCompatImageView recordListIcon = findViewById(R.id.record_list_icon);
        recordListIcon.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RecordingsActivity.class);
            startActivity(intent);
        });

        AppCompatImageView volumeIcon = findViewById(R.id.volume_icon);
        volumeIcon.setOnClickListener(v -> showVolumeDialog());

        if (Config.ENABLE_RECORDING) {
            recordIcon.setVisibility(View.VISIBLE);
            recordListIcon.setVisibility(View.VISIBLE);
        } else {
            recordIcon.setVisibility(View.GONE);
            recordListIcon.setVisibility(View.GONE);
        }
    }

    private void initializeRadioManager() {
        radioManager = RadioManager.with();
    }

    @Override
    public void onEvent(String status) {
        if (status != null) {
            handlePlaybackStatus(status);
        }
    }

    public static void updateSongName(String artist, String songTitle) {
        artistName.setText(artist);
        songName.setText(songTitle);
    }

    public static void updateAlbumArt(String artworkUrl) {
        if (artworkUrl != null && !artworkUrl.isEmpty()) {
            Glide.with(albumArtImageView.getContext())
                    .load(artworkUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, @NonNull Target<Drawable> target, boolean isFirstResource) {
                            albumArtImageView.setImageResource(R.drawable.radio_image);
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(@NonNull Drawable resource, @NonNull Object model, @NonNull Target<Drawable> target, @NonNull DataSource dataSource, boolean isFirstResource) {
                            albumArtImageView.setImageDrawable(resource);
                            return false;
                        }
                    }).into(albumArtImageView);
        } else {
            albumArtImageView.setImageResource(R.drawable.radio_image);
        }
    }

    public static void updateBackgroundImage(String artworkUrl) {
        if (Config.ENABLE_DYNAMIC_BG) {
            Glide.with(backgroundImageView.getContext())
                    .load(artworkUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, @NonNull Target<Drawable> target, boolean isFirstResource) {
                            backgroundImageView.setImageResource(R.drawable.bg_image);
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(@NonNull Drawable resource, @NonNull Object model, @NonNull Target<Drawable> target, @NonNull DataSource dataSource, boolean isFirstResource) {
                            backgroundImageView.setImageDrawable(resource);
                            return false;
                        }
                    }).into(backgroundImageView);
        }
    }

    private void handlePlaybackStatus(String status) {
        switch (status) {
            case PlaybackStatus.LOADING:
                setBuffering(true);
                break;
            case PlaybackStatus.ERROR:
                Toast.makeText(this, R.string.error_retry, Toast.LENGTH_SHORT).show();
                break;
        }

        if (!PlaybackStatus.LOADING.equals(status)) {
            setBuffering(false);
            updatePlayPauseButton(this);
        }
    }

    @Override
    public void onAudioSessionId(Integer audioSessionId) {
        // Handle audio session ID if needed
    }

    private void checkAutoplay() {
        if (ENABLE_AUTOPLAY) {
            new Handler().postDelayed(() -> {
                if (RadioManager.getService() == null) {
                    Toast.makeText(MainActivity.this, getString(R.string.please_wait), Toast.LENGTH_SHORT).show();
                    checkAutoplay();
                } else {
                    playPauseButton.callOnClick();
                }
            }, 1000);
        }
    }

    private void updatePlayPauseButton(Activity activity) {
        if (isPlaying()) {
            playPauseButton.setImageDrawable(ContextCompat.getDrawable(activity, androidx.media3.session.R.drawable.media3_icon_pause));
        } else {
            playPauseButton.setImageDrawable(ContextCompat.getDrawable(activity, androidx.media3.session.R.drawable.media3_icon_play));
        }

        if (isPlaying()) {
            equalizerView.animateBars();
            loadingProgressBar.setVisibility(View.GONE);
        } else {
            equalizerView.stopBars();
        }
    }

    public void setBuffering(boolean isBuffering) {
        if (isBuffering) {
            equalizerView.setVisibility(View.GONE);
            loadingProgressBar.setVisibility(View.VISIBLE);
        } else {
            equalizerView.setVisibility(View.VISIBLE);
            loadingProgressBar.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onClick(View v) {
        togglePlayPause();
    }

    private void togglePlayPause() {
        radioManager.playOrPause(Config.RADIO_STREAM_URL);
        updatePlayPauseButton(this);
    }

    private boolean isPlaying() {
        return radioManager != null && RadioManager.getService() != null && RadioManager.getService().isPlaying();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Utilities.registerAsListener(this);
    }

    @Override
    protected void onStop() {
        Utilities.unregisterAsListener(this);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (!radioManager.isPlaying()) {
            radioManager.unbind(this);
        }
        stopAndDestroyService();
        if (recorder != null && recorder.isRecording()) {
            recorder.stopRecording();
        }
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updatePlayPauseButton(this);
        radioManager.bind(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    private void stopAndDestroyService() {
        if (radioManager != null) {
            radioManager.unbind(this);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_share) {
            Tools.shareApp(this, "Share the app");
            return true;
        } else if (item.getItemId() == R.id.action_about) {
            showAboutDialog();
            return true;
        } else if (item.getItemId() == R.id.action_rate) {
            Tools.rateApp(this);
            return true;
        } else if (item.getItemId() == R.id.action_privacy) {
            Tools.startWebActivity(this, Config.PRIVACY_POLICY_URL);
            return true;
        } else if (item.getItemId() == R.id.action_exit) {
            showExitDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showAboutDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle(getString(R.string.about_title))
                .setMessage(getString(R.string.about_message))
                .setPositiveButton(getString(R.string.about_ok), (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void showExitDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("Exit");
        builder.setMessage("Do you really want to exit the app?");
        builder.setPositiveButton("Minimize", (dialog, which) -> minimizeApp());
        builder.setNegativeButton("Exit", (dialog, which) -> System.exit(0));
        builder.show();
    }

    private void minimizeApp() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private void showVolumeDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("Volume");
        builder.setMessage("Adjust the volume");

        // Initialize AudioManager
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

        // Create SeekBar
        SeekBar seekBar = new SeekBar(this);
        seekBar.setMax(maxVolume);  // Set max to actual system volume range
        seekBar.setProgress(currentVolume); // Set current volume

        // Handle SeekBar changes
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {  // Only change volume if user interacts
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Set View and Show Dialog
        builder.setView(seekBar);
        builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    @SuppressLint("ScheduleExactAlarm")
    private void showTimeSelectionDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("Timer");
        builder.setMessage("Set the sleep timer");

        // Create layout programmatically
        LinearLayout layout = new LinearLayout(this);
        layout.setPadding(40, 10, 40, 10);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);

        // Create SeekBar
        SeekBar seekBar = new SeekBar(this);
        seekBar.setMax(60);
        seekBar.setProgress(0);

        // Create TextView
        TextView textView = new TextView(this);
        textView.setText("0 min");
        textView.setPadding(40, 0, 0, 0);

        // Add SeekBar and TextView to layout
        layout.addView(seekBar);
        layout.addView(textView);
        builder.setView(layout);

        // Set SeekBar change listener
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress < 1) {
                    seekBar.setProgress(1);  // Set minimum value to 1
                    progress = 1;
                }
                textView.setText(progress + " min");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // No action needed
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // No action needed
            }
        });

        // Set positive button
        builder.setPositiveButton("Set", (dialog, which) -> {
            String hours = String.valueOf(seekBar.getProgress() / 60);
            String minute = String.valueOf(seekBar.getProgress() % 60);

            if (hours.length() == 1) {
                hours = "0" + hours;
            }

            if (minute.length() == 1) {
                minute = "0" + minute;
            }

            String totalTime = hours + ":" + minute;
            long total_timer = Utilities.convertToMilliSeconds(totalTime) + System.currentTimeMillis();

            Random random = new Random();
            int id = random.nextInt(100);

            sharedPref.setSleepTime(true, total_timer, id);

            int FLAG;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                FLAG = PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_ONE_SHOT;
            } else {
                FLAG = PendingIntent.FLAG_ONE_SHOT;
            }

            Intent intent = new Intent(MainActivity.this, SleepTimeReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), id, intent, FLAG);
            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
            assert alarmManager != null;
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, total_timer, pendingIntent);
        });

        // Set negative button
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        // Show dialog
        builder.show();
    }

    private void showStopTimerDialog(long millisInFuture) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("Stop Timer");
        builder.setMessage("Radio will stop after 00:00 minutes"); // Default message

        CountDownTimer countDownTimer = new CountDownTimer(millisInFuture, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long minutes = (millisUntilFinished / 1000) / 60;
                long seconds = (millisUntilFinished / 1000) % 60;
                @SuppressLint("DefaultLocale") String timeRemaining = String.format("Radio will stop after %d:%02d minutes", minutes, seconds);

                // Update the message on each tick
                if (dialog != null && dialog.isShowing()) {
                    dialog.setMessage(timeRemaining);
                }
            }

            @Override
            public void onFinish() {
                if (dialog != null && dialog.isShowing()) {
                    dialog.setMessage("Timer finished");
                }
                stopRadio();
            }
        };

        builder.setPositiveButton("Cancel Timer", (dialog, which) -> {
            countDownTimer.cancel();
            cancelTimer();
            this.dialog.dismiss();  // Use the correct dialog reference
        });

        builder.setNegativeButton("Close", (dialog, which) -> this.dialog.dismiss());  // Use the correct dialog reference

        this.dialog = builder.create();  // Initialize the dialog here
        this.dialog.setOnShowListener(d -> countDownTimer.start());
        this.dialog.show();
    }

    private void cancelTimer() {
        sharedPref.setSleepTime(false, 0, 0);
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        Intent intent = new Intent(MainActivity.this, SleepTimeReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, intent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);
        alarmManager.cancel(pendingIntent);
    }

    private void stopRadio() {
        if (radioManager != null && RadioManager.getService() != null) {
            RadioManager.getService().stop();
            updatePlayPauseButton(this);
        }
    }

    @Override
    protected void onApiKeyNotValid() {
        Tools.showDialog(this);
    }

    private void initializeSocialIcons() {
        RecyclerView recyclerView = findViewById(R.id.social_icons_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        SocialIconsAdapter adapter = new SocialIconsAdapter(Config.SOCIAL_ICONS, this);
        recyclerView.setAdapter(adapter);
    }

    private void handleRecording() {
        ToolTip.Builder builder = new ToolTip.Builder(MainActivity.this, recordIcon, findViewById(R.id.main), "Recording", ToolTip.POSITION_ABOVE);
        if (isRecording) {
            if (recorder != null && recorder.isRecording()) {
                recorder.stopRecording();
                isRecording = false;
                Toast.makeText(MainActivity.this, "Recording ended", Toast.LENGTH_SHORT).show();
                toolTipsManager.dismissAll();
                recordIcon.setImageResource(R.drawable.mic_simple);
            }
        } else {
            if (!isPlaying()) {
                Toast.makeText(MainActivity.this, "Please start the radio first", Toast.LENGTH_SHORT).show();
                return;
            }
            if (recorder != null) {
                recorder.startRecording();
                isRecording = true;
                recordIcon.setImageResource(R.drawable.mic_filled);
                toolTipsManager.show(builder.build());
            }
        }
    }
}