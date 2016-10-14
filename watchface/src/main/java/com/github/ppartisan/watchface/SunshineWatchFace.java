/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.ppartisan.watchface;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

import com.github.ppartisan.watchface.watchhand.WatchHand;
import com.github.ppartisan.watchface.watchticks.WatchTicks;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataItemBuffer;
import com.google.android.gms.wearable.Wearable;

import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class SunshineWatchFace extends CanvasWatchFaceService  {

    //Only updates once per minute as there is no second counter
    static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.MINUTES.toMillis(1);
    static final int MSG_UPDATE_TIME = 0;

    @Override
    public Engine onCreateEngine() {
        return new Engine(this);
    }

    class Engine extends CanvasWatchFaceService.Engine implements DataApi.DataListener,
            GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
            DataModel.Callback, ResultCallback<DataItemBuffer> {

        private SunshineWatchFace sunshineWatchFace;
        private final Rect mPeekCardBounds = new Rect();
        private final Handler mUpdateTimeHandler = new EngineHandler(this);
        private Calendar mCalendar;

        private final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            }
        };

        private final WatchHand.Factory mWatchHandFactory;
        private final WatchTicks mWatchTicks;

        private boolean mRegisteredTimeZoneReceiver = false;
        private boolean mIsRound = false;
        private float mCenterX;
        private float mCenterY;

        private Paint mBackgroundPaint, mTextPaint;

        private GoogleApiClient mGoogleApiClient;
        private DataModel mDataModel;

        Engine(SunshineWatchFace sunshineWatchFace) {
            this.sunshineWatchFace = sunshineWatchFace;

            final int primaryColor = ContextCompat.getColor(getApplicationContext(), R.color.primary);
            mWatchHandFactory = new WatchHand.Factory(primaryColor);
            mWatchTicks = new WatchTicks(SunshineWatchFace.this, null);
        }

        /*
        * Several method in WatchFaceStyle.Builder class show as deprecated, but no alternatives
        * offered in documentation.
        */
        @SuppressWarnings("deprecation")
        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(sunshineWatchFace)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setViewProtectionMode(
                            WatchFaceStyle.PROTECT_STATUS_BAR |
                                    WatchFaceStyle.PROTECT_HOTWORD_INDICATOR
                    )
                    .setShowSystemUiTime(false)
                    .build());

            mCalendar = Calendar.getInstance();

            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(Color.BLACK);

            final int primary = ContextCompat.getColor(SunshineWatchFace.this, R.color.primary);

            mTextPaint = new Paint();
            mTextPaint.setColor(primary);
            mTextPaint.setTextSize(36);
            mTextPaint.setAntiAlias(true);

            mGoogleApiClient = new GoogleApiClient.Builder(SunshineWatchFace.this)
                    .addApi(Wearable.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            Wearable.DataApi.removeListener(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
            super.onDestroy();
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);
            mIsRound = insets.isRound();
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            updateTimer();
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);
            mCenterX = width / 2f;
            mCenterY = height / 2f;
            mWatchTicks.setCenterPoint(mCenterX, mCenterY);
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {

            long now = System.currentTimeMillis();
            mCalendar.setTimeInMillis(now);

            final int canvasColor = (isInAmbientMode()) ? Color.BLACK : Color.WHITE;
            canvas.drawColor(canvasColor);

            if(!mIsRound || isInAmbientMode()) {
                final @WatchTicks.Mode int mode = (isInAmbientMode())
                        ? WatchTicks.AMBIENT : WatchTicks.REGULAR;
                mWatchTicks.drawTicks(canvas, mode);
            }

            final float minutesRotation = mCalendar.get(Calendar.MINUTE) * 6f;

            final float hourHandOffset = mCalendar.get(Calendar.MINUTE) / 2f;
            final float hoursRotation = (mCalendar.get(Calendar.HOUR) * 30) + hourHandOffset;

            canvas.save();

            canvas.rotate(hoursRotation, mCenterX, mCenterY);

            final WatchHand hourHand = (isInAmbientMode())
                    ? mWatchHandFactory.getWatchHand(WatchHand.AMBIENT_HOUR)
                    : mWatchHandFactory.getWatchHand(WatchHand.HOUR);

            hourHand.drawWatchHand(canvas, bounds.centerX(), hourHand.getWatchHandRadius()*2);

            canvas.rotate(minutesRotation - hoursRotation, mCenterX, mCenterY);

            final WatchHand minuteHand = (isInAmbientMode())
                    ? mWatchHandFactory.getWatchHand(WatchHand.AMBIENT_MINUTE)
                    : mWatchHandFactory.getWatchHand(WatchHand.MINUTE);

            minuteHand.drawWatchHand(canvas, bounds.centerX(), minuteHand.getWatchHandRadius() * 2);

            canvas.restore();

            if (!isInAmbientMode()) {
                canvas.drawRect(mPeekCardBounds, mBackgroundPaint);
                if (mDataModel != null) {
                    drawTemperatureText(canvas);
                    if (mDataModel.image != null) {
                        drawWeatherBitmap(canvas);
                    }
                }
            }

        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                Wearable.DataApi.addListener(mGoogleApiClient, this);
                mGoogleApiClient.connect();
                registerReceiver();
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            } else {
                Wearable.DataApi.removeListener(mGoogleApiClient, this);
                mGoogleApiClient.disconnect();
                unregisterReceiver();
            }
            updateTimer();

        }

        @SuppressWarnings("deprecation")
        @Override
        public void onPeekCardPositionUpdate(Rect rect) {
            super.onPeekCardPositionUpdate(rect);
            mPeekCardBounds.set(rect);
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            sunshineWatchFace.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            sunshineWatchFace.unregisterReceiver(mTimeZoneReceiver);
        }

        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        private void drawWeatherBitmap(Canvas canvas) {
            final int x = (int)(mCenterX - (mDataModel.image.getWidth()/2));
            final int y = (int)(mCenterY - (mDataModel.image.getHeight()/1.667f));
            canvas.drawBitmap(mDataModel.image, x, y, mBackgroundPaint);
        }

        private void drawTemperatureText(Canvas canvas) {
            final String text =
                    getString(R.string.max_min_temp_template, mDataModel.max, mDataModel.min);
            final int x =
                    (int)((canvas.getWidth()/2) - mTextPaint.measureText(text)/2);
            final int y =
                    (int) ((canvas.getHeight() / 1.33f) -
                            ((mTextPaint.descent() + mTextPaint.ascent()))/2);
            canvas.drawText(text, x, y, mTextPaint);
        }

        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }

        void handleUpdateTimeMessage() {
            invalidate();
            if (shouldTimerBeRunning()) {
                long timeMs = System.currentTimeMillis();
                long delayMs = INTERACTIVE_UPDATE_RATE_MS
                        - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
            }
        }

        @Override
        public void onDataChanged(DataEventBuffer dataEventBuffer) {
            for (DataEvent dataEvent : dataEventBuffer) {
                if (dataEvent.getType() == DataEvent.TYPE_CHANGED) {
                    DataModel.fromItemAsync(
                            Engine.this, mGoogleApiClient, dataEvent.getDataItem()
                    );
                }
            }
        }

        @Override
        public void onConnected(@Nullable Bundle bundle) {
            Wearable.DataApi.addListener(mGoogleApiClient, this);
            Wearable.DataApi.getDataItems(mGoogleApiClient).setResultCallback(this);
        }

        @Override
        public void onConnectionSuspended(int i) {}

        @Override
        public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {}

        @Override
        public void onDataModelReady(DataModel model) {
            mDataModel = model;
            Log.d(getClass().getSimpleName(), mDataModel.toString());
            invalidate();
        }

        @Override
        public void onResult(@NonNull DataItemBuffer dataItems) {
            for (DataItem item : dataItems) {
                DataModel.fromItemAsync(
                        Engine.this, mGoogleApiClient, item
                );
            }
        }

    }

}
