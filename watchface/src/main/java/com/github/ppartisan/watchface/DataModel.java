package com.github.ppartisan.watchface;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Looper;
import android.support.annotation.NonNull;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;

import java.io.InputStream;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

final class DataModel {

    private static final String MAX_KEY = "max";
    private static final String MIN_KEY = "min";
    private static final String WEATHER_ID_KEY = "weather_id";
    //ToDo; Debug only
    private static final String TIME_KEY = "time";

    final int max, min;
    final Bitmap image;

    private DataModel(int max, int min, Bitmap image) {
        this.max = max;
        this.min = min;
        this.image = image;
    }

    /*
     * Blocking - only use off UI thread.
     */
    @NonNull
    static DataModel buildDataModelFromItem(GoogleApiClient client, DataItem item) {

        if(Looper.myLooper() == Looper.getMainLooper()) {
            throw new RuntimeException("Method must be called off main thread");
        }

        DataMap map = DataMapItem.fromDataItem(item).getDataMap();

        final int max = map.getInt(MAX_KEY);
        final int min = map.getInt(MIN_KEY);
        final Asset weatherAsset = map.getAsset(WEATHER_ID_KEY);

        Bitmap weatherImage = null;

        try {
            weatherImage = new GetBitmapFromAssetAsync(client).execute(weatherAsset).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        return new DataModel(max, min, weatherImage);

    }

    @Override
    public String toString() {
        return "DataModel{" +
                "max=" + max +
                ", min=" + min +
                ", image=" + image +
                '}';
    }

    private static class GetBitmapFromAssetAsync extends AsyncTask<Asset, Void, Bitmap> {

        private final GoogleApiClient mClient;

        private GetBitmapFromAssetAsync(GoogleApiClient client) {
            mClient = client;
        }

        @Override
        protected Bitmap doInBackground(Asset... assets) {

            if (!mClient.isConnected()) {
                mClient.blockingConnect(5, TimeUnit.SECONDS);
            }

            InputStream inputStream =
                    Wearable.DataApi.getFdForAsset(mClient, assets[0]).await().getInputStream();

            if (inputStream == null) return null;

            return BitmapFactory.decodeStream(inputStream);
        }

    }

}
