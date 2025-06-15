package com.app.classsicradio;

import com.app.classicradio.R;
import com.app.classsicradio.albumart.AlbumArtType;
import com.app.classsicradio.models.SocialIconsModel;
import java.util.Arrays;
import java.util.List;

public class Config {

    /** App Access Key */
    public static final String APP_ACCESS_KEY = "295084be8490664f0995306386c5a5592420e8f634982d486a46d263d7e90a1f";

    /*** Set the radio stream url */
    public static final String RADIO_STREAM_URL = "https://media-ice.musicradio.com/CapitalMP3";

    /*** Enable/disable autoplay */
    public static final boolean ENABLE_AUTOPLAY = true;

    /*** Enable/disable albumArt */
    public static final boolean ENABLE_ALBUM_ART = true;

    /*** Enable/disable dynamic background */
    public static final boolean ENABLE_DYNAMIC_BG = true;

    /*** Enable/disable metadata, (Note: Disabling metadata will also disable albumArt & dynamic bg) */
    public static final boolean ENABLE_METADATA = true;

    /**
     * Set the album art type
     * ROUNDED_CORNER
     * SQUARE
     * CIRCLE
     * T_LEFT_B_RIGHT_CORNER_CURVED
     * T_RIGHT_B_LEFT_CORNER_CURVED
     */
    public static final AlbumArtType ALBUM_ART_TYPE = AlbumArtType.CIRCLE;
    public static final boolean ALBUM_ART_BORDER_ENABLED = true;
    public static final float ALBUM_ART_BORDER_WIDTH = 2.0f;
    public static final int ALBUM_ART_BORDER_COLOR = R.color.album_art_image_border_color;

    /*** Enable/disable Recording */
    public static final boolean ENABLE_RECORDING = true;

    /*** Enable/disable Social */
    public static final boolean ENABLE_SOCIAL_ICONS = true;

    /*** Social Media Icons & Links */
    public static final List<SocialIconsModel> SOCIAL_ICONS = Arrays.asList(

            // Replace the icons with your own icons & links
            new SocialIconsModel(R.drawable.facebook, "https://www.facebook.com"),
            new SocialIconsModel(R.drawable.instagram, "https://www.instagram.com"),
            new SocialIconsModel(R.drawable.twitter, "https://twitter.com"),
            new SocialIconsModel(R.drawable.tiktok, "https://tiktok.com"),
            new SocialIconsModel(R.drawable.whatsapp, "https://wa.me/"),
            new SocialIconsModel(R.drawable.website, "https://example.com")

    );

    /*** Privacy policy url */
    public static final String PRIVACY_POLICY_URL = "https://example.com";

}
