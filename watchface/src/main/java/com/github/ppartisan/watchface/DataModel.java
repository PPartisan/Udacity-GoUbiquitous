package com.github.ppartisan.watchface;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
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

    final int max, min;
    final Bitmap image;

    private DataModel(int max, int min, Bitmap image) {
        this.max = max;
        this.min = min;
        this.image = image;
    }

    /**
     * Returns a model representation of data in {@link DataItem}.
     * <p>
     * This method may block the thread it is called on for a substantial amount of time, and as
     * such should only be called from a background thread. See
     * {@link #fromItemAsync(Callback, GoogleApiClient, DataItem)} to call this
     * asynchronously with a {@link Callback}
     *
     * @param client A {@link GoogleApiClient}
     * @param item {@link DataItem} containing {@link DataMap} with values corresponding to
     *                             constant keys in DataModel.class
     * @return New {@link DataModel}
     *
     * @see {@link #fromItemAsync(Callback, GoogleApiClient, DataItem)}
     */
    @SuppressWarnings("unused")
    @NonNull
    static DataModel fromItem(GoogleApiClient client, DataItem item) {

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

    static void fromItemAsync(Callback callback, GoogleApiClient client, DataItem item) {
        final DataMap map = DataMapItem.fromDataItem(item).getDataMap();
        new GetDataModelFromAssetAsync(callback, client).execute(map);
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

            mClient.disconnect();

            return BitmapFactory.decodeStream(inputStream);
        }

    }

    private static class GetDataModelFromAssetAsync extends AsyncTask<DataMap, Void, DataModel> {

        private Callback mCallback;
        private final GoogleApiClient mClient;

        private GetDataModelFromAssetAsync(Callback callback, GoogleApiClient client) {
            mCallback = callback;
            mClient = client;
        }

        @Override
        protected DataModel doInBackground(DataMap... maps) {

            final DataMap map = maps[0];

            final int max = map.getInt(MAX_KEY);
            final int min = map.getInt(MIN_KEY);
            final Asset weatherAsset = map.getAsset(WEATHER_ID_KEY);

            if (!mClient.isConnected()) {
                mClient.blockingConnect(5, TimeUnit.SECONDS);
            }

            InputStream inputStream =
                    Wearable.DataApi.getFdForAsset(mClient, weatherAsset).await().getInputStream();

            if (inputStream == null) return null;

            final Bitmap weatherImage = BitmapFactory.decodeStream(inputStream);

            return new DataModel(max, min, weatherImage);
        }

        @Override
        protected void onPostExecute(DataModel model) {
            mCallback.onDataModelReady(model);
            mCallback = null;
        }
    }

    interface Callback {
        void onDataModelReady(DataModel model);
    }

}
