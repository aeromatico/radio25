package com.app.classsicradio.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.app.classicradio.R;
import com.app.classsicradio.models.Recording;
import java.util.List;

public class RecordingsAdapter extends RecyclerView.Adapter<RecordingsAdapter.ViewHolder> {

    private final List<Recording> recordings;
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Recording recording, View view);
    }

    public RecordingsAdapter(List<Recording> recordings, OnItemClickListener listener) {
        this.recordings = recordings;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recording, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Recording recording = recordings.get(position);
        holder.bind(recording, listener);
    }

    @Override
    public int getItemCount() {
        return recordings.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView fileNameTextView;
        private final ImageView menuImageView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            fileNameTextView = itemView.findViewById(R.id.file_name);
            menuImageView = itemView.findViewById(R.id.menu_icon);
        }

        public void bind(final Recording recording, final OnItemClickListener listener) {
            fileNameTextView.setText(recording.getName());
            menuImageView.setOnClickListener(v -> {
                listener.onItemClick(recording, menuImageView);
            });
        }
    }
}