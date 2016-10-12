package com.github.ppartisan.watchface;

import android.graphics.Bitmap;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;

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

    static DataModel buildDataModelFromItem(GoogleApiClient client, DataItem item) {

        DataMap map = DataMapItem.fromDataItem(item).getDataMap();

        final Asset weatherAsset = map.getAsset(WEATHER_ID_KEY);
        //ToDo: Must be called Async
        //final Bitmap weatherImage = Util.getBitmapFromAsset(client, weatherAsset);
        final int max = map.getInt(MAX_KEY);
        final int min = map.getInt(MIN_KEY);

        return new DataModel(max, min, null);

    }

    @Override
    public String toString() {
        return "DataModel{" +
                "max=" + max +
                ", min=" + min +
                ", image=" + image +
                '}';
    }
}
