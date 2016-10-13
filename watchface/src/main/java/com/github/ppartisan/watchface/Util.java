package com.github.ppartisan.watchface;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.Wearable;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

final class Util {

    private Util() { throw new AssertionError(); }

    //ToDo must be called Async
    static Bitmap getBitmapFromAsset(GoogleApiClient client, @NonNull Asset asset) {

        client.blockingConnect(5, TimeUnit.SECONDS);

        //if(!client.isConnected()) return null;

        InputStream inputStream =
                Wearable.DataApi.getFdForAsset(client, asset).await().getInputStream();

        if (inputStream == null) return null;

        return BitmapFactory.decodeStream(inputStream);

    }

}
