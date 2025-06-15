package com.app.classsicradio.models;

public class SocialIconsModel {

    private final int iconResId;
    private final String url;

    public SocialIconsModel(int iconResId, String url) {
        this.iconResId = iconResId;
        this.url = url;
    }

    public int getIconResId() {
        return iconResId;
    }

    public String getUrl() {
        return url;
    }
}


