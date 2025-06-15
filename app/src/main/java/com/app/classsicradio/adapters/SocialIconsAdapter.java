package com.app.classsicradio.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.app.classicradio.R;
import com.app.classsicradio.models.SocialIconsModel;
import java.util.List;
import app.utils.tools.Tools;

public class SocialIconsAdapter extends RecyclerView.Adapter<SocialIconsAdapter.ViewHolder> {

    private final List<SocialIconsModel> iconList;
    private final Context context;

    public SocialIconsAdapter(List<SocialIconsModel> iconList, Context context) {
        this.iconList = iconList;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_social, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SocialIconsModel item = iconList.get(position);
        holder.imageView.setImageResource(item.getIconResId());

        // Open URL when icon is clicked
        holder.imageView.setOnClickListener(v -> {
            if(item.getUrl() != null) {
                Tools.startWebActivity(context, item.getUrl());
            }
        });
    }

    @Override
    public int getItemCount() {
        return iconList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.social_icon);
        }
    }
}
