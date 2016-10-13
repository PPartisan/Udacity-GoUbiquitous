package com.github.ppartisan.sunshine.app.wearable;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.github.ppartisan.sunshine.app.Utility;
import com.google.android.gms.wearable.Asset;

import java.io.ByteArrayOutputStream;

final class WearableUtils {

    private WearableUtils() { throw new AssertionError(); }

    static Asset getAssetForWeatherId(Resources res, int weatherId) {
        final int weatherResId = Utility.getArtResourceForWeatherCondition(weatherId);
        final Bitmap weatherBitmap = BitmapFactory.decodeResource(res, weatherResId);
        final Bitmap fiveSixthsBitmap = Bitmap.createScaledBitmap(
                weatherBitmap,
                toFiveSixths(weatherBitmap.getWidth()),
                toFiveSixths(weatherBitmap.getHeight()),
                true
        );
        return getAssetFromWeatherBitmap(fiveSixthsBitmap);
    }

    private static Asset getAssetFromWeatherBitmap(Bitmap bitmap) {
        final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteStream);
        return Asset.createFromBytes(byteStream.toByteArray());
    }

    private static int toFiveSixths(int input) {
        return (input/6)*5;
    }

}
