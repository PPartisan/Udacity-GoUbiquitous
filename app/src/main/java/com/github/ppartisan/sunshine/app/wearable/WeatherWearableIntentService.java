package com.github.ppartisan.sunshine.app.wearable;

import android.app.IntentService;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;

import com.github.ppartisan.sunshine.app.Utility;
import com.github.ppartisan.sunshine.app.data.WeatherContract;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;

import static com.github.ppartisan.sunshine.app.data.WeatherContract.WeatherEntry.COLUMN_MAX_TEMP;
import static com.github.ppartisan.sunshine.app.data.WeatherContract.WeatherEntry.COLUMN_MIN_TEMP;
import static com.github.ppartisan.sunshine.app.data.WeatherContract.WeatherEntry.COLUMN_WEATHER_ID;

public final class WeatherWearableIntentService extends IntentService {

    private static final String[] FORECAST_COLUMNS =
            { COLUMN_WEATHER_ID, COLUMN_MAX_TEMP, COLUMN_MIN_TEMP };

    private GoogleApiClient mGoogleApiClient;

    @SuppressWarnings("unused")
    public WeatherWearableIntentService() {
        this(WeatherWearableIntentService.class.getSimpleName());
    }

    public WeatherWearableIntentService(String name) {
        super(name);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();
    }


    @Override
    protected void onHandleIntent(Intent intent) {

        Cursor cursor = null;

        mGoogleApiClient.blockingConnect();

        try {
            final String location = Utility.getPreferredLocation(this);
            Uri weatherForLocationUri =
                    WeatherContract.WeatherEntry.buildWeatherLocationWithStartDate(
                            location, System.currentTimeMillis()
                    );
            cursor = getContentResolver().query(
                    weatherForLocationUri, FORECAST_COLUMNS, null, null, null
            );

            if (cursor != null && cursor.moveToFirst()) {
                final int weatherId = cursor.getInt(cursor.getColumnIndex(FORECAST_COLUMNS[0]));
                final float maxTemp = cursor.getFloat(cursor.getColumnIndex(FORECAST_COLUMNS[1]));
                final float minTemp = cursor.getFloat(cursor.getColumnIndex(FORECAST_COLUMNS[2]));

                final Asset weatherAsset =
                        WearableUtils.getAssetForWeatherId(getResources(), weatherId);

                PutDataMapRequest putDataMapRequest = PutDataMapRequest.create("/weather");
                putDataMapRequest.getDataMap().putAsset(FORECAST_COLUMNS[0], weatherAsset);
                putDataMapRequest.getDataMap().putInt(FORECAST_COLUMNS[1], Math.round(maxTemp));
                putDataMapRequest.getDataMap().putInt(FORECAST_COLUMNS[2], Math.round(minTemp));

                //Testing, to ensure each packet unique
                putDataMapRequest.getDataMap().putLong("time", System.currentTimeMillis());
                putDataMapRequest.setUrgent();

                Wearable.DataApi.putDataItem(mGoogleApiClient, putDataMapRequest.asPutDataRequest());

            }
        } finally {
            mGoogleApiClient.disconnect();
            if (cursor != null && !cursor.isClosed()) cursor.close();
        }

    }

}
