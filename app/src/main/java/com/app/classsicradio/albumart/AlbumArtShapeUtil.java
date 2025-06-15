package com.app.classsicradio.albumart;

import com.makeramen.roundedimageview.RoundedImageView;

public class AlbumArtShapeUtil {

    public static void setShape(RoundedImageView albumArtImage, RoundedImageView albumArtImageBg,
                                AlbumArtType shapeType, boolean isBorderEnabled,
                                float borderWidth, int borderColor) {
        float[] radii = getCornerRadii(shapeType);
        albumArtImageBg.setCornerRadius(radii[0], radii[1], radii[2], radii[3]);
        albumArtImage.setCornerRadius(radii[0], radii[1], radii[2], radii[3]);

        if (shapeType == AlbumArtType.CIRCLE) {
            albumArtImageBg.setOval(true);
            albumArtImage.setOval(true);
        }

        if (isBorderEnabled) {
            setBorder(albumArtImage, borderWidth, borderColor);
            setBorder(albumArtImageBg, borderWidth, borderColor);
        }
    }

    private static float[] getCornerRadii(AlbumArtType shapeType) {
        switch (shapeType) {
            case ROUNDED_CORNER:
                return new float[]{50, 50, 50, 50};
            case SQUARE:
                return new float[]{5, 5, 5, 5};
            case T_LEFT_B_RIGHT_CORNER_CURVED:
                return new float[]{100, 0, 0, 100};
            case T_RIGHT_B_LEFT_CORNER_CURVED:
                return new float[]{0, 100, 100, 0};
            default:
                return new float[]{10, 10, 10, 10};
        }
    }

    private static void setBorder(RoundedImageView imageView, float borderWidth, int borderColor) {
        imageView.setBorderWidth(borderWidth);
        imageView.setBorderColor(borderColor);
    }
}
