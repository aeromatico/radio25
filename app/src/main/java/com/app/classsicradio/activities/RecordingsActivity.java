package com.app.classsicradio.activities;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.media3.common.util.UnstableApi;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.app.classicradio.R;
import com.app.classsicradio.adapters.RecordingsAdapter;
import com.app.classsicradio.models.Recording;
import com.app.classsicradio.services.RadioManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import app.utils.BaseActivity;
import app.utils.tools.Tools;

@UnstableApi
public class RecordingsActivity extends BaseActivity {

    private RadioManager radioManager;
    private MediaPlayer mediaPlayer;
    private RecordingsAdapter adapter;
    private List<Recording> recordingsList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_recordings);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mediaPlayer = new MediaPlayer();
        radioManager = RadioManager.with();

        setupToolbar(this, findViewById(R.id.toolbar), "Recordings", true);

        RecyclerView recordingsRecyclerView = findViewById(R.id.recordings_list);
        recordingsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        recordingsList = new ArrayList<>();

        File recordingsDir = getFilesDir();
        File[] files = recordingsDir.listFiles();

        if (files == null || files.length == 0) {
            showNoFilesFoundDialog();
        } else {
            for (File file : files) {
                if (file.isFile() && file.getName().endsWith(".mp3")) {
                    recordingsList.add(new Recording(file.getName(), file.getAbsolutePath()));
                }
            }

            // Ensure we show dialog if there are no MP3 files found
            if (recordingsList.isEmpty()) {
                showNoFilesFoundDialog();
            }
        }

        adapter = new RecordingsAdapter(recordingsList, (recording, view) -> {
            showPopupMenu(view, recording);
        });

        recordingsRecyclerView.setAdapter(adapter);
    }

    private void showPopupMenu(View view, Recording recording) {
        PopupMenu popupMenu = new PopupMenu(this, view);
        MenuInflater inflater = popupMenu.getMenuInflater();
        inflater.inflate(R.menu.popup_menu, popupMenu.getMenu());

        popupMenu.setOnMenuItemClickListener(menuItem -> {
            int id = menuItem.getItemId();

            if (id == R.id.action_play) {
                if (radioManager != null && radioManager.isPlaying()) {
                    radioManager.stopServices();
                }
                playRecording(recording);
                return true;
            } else if (id == R.id.action_share) {
                shareRecording(recording);
                return true;
            } else if (id == R.id.action_delete) {
                deleteDialog(recording);
                return true;
            } else if (id == R.id.action_rename) {
                renameRecording(recording);
                return true;
            }
            return true;
        });

        popupMenu.show();
    }

    private void playRecording(Recording recording) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.lyt_play_recording_dialog, null);
        builder.setView(dialogView);

        TextView recordingNameTextView = dialogView.findViewById(R.id.recording_name);
        SeekBar seekBar = dialogView.findViewById(R.id.seekBar);
        MaterialButton playPauseButton = dialogView.findViewById(R.id.play_pause_button);
        TextView startTimeTextView = dialogView.findViewById(R.id.start_time);
        TextView endTimeTextView = dialogView.findViewById(R.id.end_time);

        recordingNameTextView.setText(recording.getName());

        Handler handler = new Handler();

        // Ensure MediaPlayer is initialized
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        mediaPlayer = new MediaPlayer();

        try {
            mediaPlayer.setDataSource(recording.getPath());
            mediaPlayer.prepareAsync();
        } catch (Exception e) {
            Toast.makeText(this, "Failed to play recording", Toast.LENGTH_SHORT).show();
            return;
        }

        // Format time in MM:SS format
        Runnable updateTimes = new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    int currentPosition = mediaPlayer.getCurrentPosition();
                    int duration = mediaPlayer.getDuration();

                    // Update start time
                    startTimeTextView.setText(formatTime(currentPosition));

                    // Update end time
                    endTimeTextView.setText(formatTime(duration));

                    // Update SeekBar
                    seekBar.setProgress(currentPosition);
                    handler.postDelayed(this, 500); // Update every 500ms
                }
            }
        };

        mediaPlayer.setOnPreparedListener(mp -> {
            seekBar.setMax(mediaPlayer.getDuration());
            mediaPlayer.start();
            playPauseButton.setIconResource(androidx.media3.session.R.drawable.media3_icon_pause);
            playPauseButton.setText(R.string.pause);

            // Start updating the times and SeekBar
            handler.post(updateTimes);
        });

        playPauseButton.setOnClickListener(v -> {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                playPauseButton.setIconResource(androidx.media3.session.R.drawable.media3_icon_play);
                playPauseButton.setText(R.string.play);
            } else {
                if (mediaPlayer.getCurrentPosition() >= mediaPlayer.getDuration()) {
                    mediaPlayer.seekTo(0);
                }
                mediaPlayer.start();
                playPauseButton.setIconResource(androidx.media3.session.R.drawable.media3_icon_pause);
                playPauseButton.setText(R.string.pause);

                // Resume updating times and SeekBar
                handler.post(updateTimes);
            }
        });

        mediaPlayer.setOnCompletionListener(mp -> {
            playPauseButton.setIconResource(androidx.media3.session.R.drawable.media3_icon_play);
            playPauseButton.setText(R.string.play);
            seekBar.setProgress(0);
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    mediaPlayer.seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        builder.setOnDismissListener(dialog -> {
            if (mediaPlayer != null) {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.release();
                mediaPlayer = null;
            }
            handler.removeCallbacks(updateTimes);
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @SuppressLint("DefaultLocale")
    private String formatTime(int milliseconds) {
        int seconds = (milliseconds / 1000) % 60;
        int minutes = (milliseconds / (1000 * 60)) % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    private void deleteDialog(Recording recording) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("Delete Recording");
        builder.setMessage("Are you sure you want to delete this recording?");
        builder.setPositiveButton("Yes", (dialog, which) ->
                deleteRecording(recording)
        );
        builder.setNegativeButton("No", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    @SuppressLint("NotifyDataSetChanged")
    private void deleteRecording(Recording recording) {
        File file = new File(recording.getPath());
        if (file.delete()) {
            recordingsList.remove(recording);
            adapter.notifyDataSetChanged();
            Toast.makeText(this, "Recording deleted", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Failed to delete recording", Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private void renameRecording(Recording recording) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle(R.string.rename_recording_title); // String resource

        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.lyt_rename_recording_dialog, null);
        builder.setView(dialogView);

        EditText input = dialogView.findViewById(R.id.rename_input);
        input.setText(recording.getName().replace(".mp3", ""));
        input.selectAll();

        builder.setPositiveButton(R.string.ok, (dialog, which) -> {
            String newName = input.getText().toString().trim();

            if (newName.isEmpty()) {
                Toast.makeText(this, R.string.name_cannot_be_empty, Toast.LENGTH_SHORT).show(); // String resource
                return;
            }

            newName += ".mp3";

            if (newName.equals(recording.getName())) {
                dialog.dismiss();
                return;
            }

            File file = new File(recording.getPath());
            File newFile = new File(file.getParent(), newName);

            if (newFile.exists()) {
                Toast.makeText(this, R.string.recording_already_exists, Toast.LENGTH_SHORT).show(); // String resource
                return;
            }

            if (file.renameTo(newFile)) {
                recording.setName(newName);
                recording.setPath(newFile.getAbsolutePath());
                int position = recordingsList.indexOf(recording);
                recordingsList.set(position, recording);
                adapter.notifyItemChanged(position);
                Toast.makeText(this, R.string.recording_renamed, Toast.LENGTH_SHORT).show();
                Intent intent = getIntent();
                finish();
                startActivity(intent);
            } else {
                Toast.makeText(this, R.string.failed_to_rename_recording, Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.cancel());

        AlertDialog dialog = builder.create();
        dialog.show();

        input.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
    }

    @Override
    protected void onApiKeyNotValid() {
        Tools.showDialog(this);
    }

    private void shareRecording(Recording recording) {
        Uri fileUri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".fileprovider", new File(recording.getPath()));
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("audio/mp3");
        shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(shareIntent, "Share Audio"));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    public static void setupToolbar(AppCompatActivity activity, Toolbar toolbar, String title, boolean backButton) {
        activity.setSupportActionBar(toolbar);
        final ActionBar actionBar = activity.getSupportActionBar();
        if (actionBar != null) {
            activity.getSupportActionBar().setDisplayHomeAsUpEnabled(backButton);
            activity.getSupportActionBar().setHomeButtonEnabled(backButton);
            activity.getSupportActionBar().setTitle(title);
        }
    }

    private void showNoFilesFoundDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("No Recordings Found");
        builder.setMessage("You have not recorded any audio yet.");
        builder.setPositiveButton("Back", (dialog, which) -> finish());
        builder.setCancelable(false);
        builder.show();
    }
}