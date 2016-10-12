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

import com.github.ppartisan.watchface.watchhand.WatchHand;
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

/**
 * Analog watch face with a ticking second hand. In ambient mode, the second hand isn't
 * shown. On devices with low-bit ambient mode, the hands are drawn without anti-aliasing in ambient
 * mode. The watch face is drawn with less contrast in mute mode.
 */
public class SunshineWatchFace extends CanvasWatchFaceService  {

    /*
     * Update rate in milliseconds for interactive mode. We update once a minute to advance the
     * minute hand.
     */
    static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.MINUTES.toMillis(1);

    /**
     * Handler message id for updating the time periodically in interactive mode.
     */
    static final int MSG_UPDATE_TIME = 0;

    @Override
    public Engine onCreateEngine() {
        return new Engine(this);
    }

    class Engine extends CanvasWatchFaceService.Engine implements DataApi.DataListener,
            GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

        private SunshineWatchFace sunshineWatchFace;
        private final Rect mPeekCardBounds = new Rect();
        /* Handler to update the time once a second in interactive mode. */
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

        private boolean mRegisteredTimeZoneReceiver = false;
        private boolean mMuteMode;
        private float mCenterX;
        private float mCenterY;
//        private Bitmap mBackgroundBitmap;
//        private Bitmap mGrayBackgroundBitmap;
        private boolean mAmbient;
        private boolean mLowBitAmbient;
        private boolean mBurnInProtection;

        private Paint mBackgroundPaint;

        private GoogleApiClient mGoogleApiClient;

        Engine(SunshineWatchFace sunshineWatchFace) {
            this.sunshineWatchFace = sunshineWatchFace;

            final int primaryColor = ContextCompat.getColor(getApplicationContext(), R.color.primary);
            mWatchHandFactory = new WatchHand.Factory(primaryColor);

        }

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(sunshineWatchFace)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .build());
            mCalendar = Calendar.getInstance();
            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(Color.BLACK);

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
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
            mBurnInProtection = properties.getBoolean(PROPERTY_BURN_IN_PROTECTION, false);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            mAmbient = inAmbientMode;
            updateTimer();
        }


        @Override
        public void onInterruptionFilterChanged(int interruptionFilter) {
            super.onInterruptionFilterChanged(interruptionFilter);
            boolean inMuteMode = (interruptionFilter == INTERRUPTION_FILTER_NONE);

            /* Dim display in mute mode. */
            if (mMuteMode != inMuteMode) {
                mMuteMode = inMuteMode;
                invalidate();
            }
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);

            /*
             * Find the coordinates of the center point on the screen, and ignore the window
             * insets, so that, on round watches with a "chin", the watch face is centered on the
             * entire screen, not just the usable portion.
             */
            mCenterX = width / 2f;
            mCenterY = height / 2f;

            /*
             * Calculate lengths of different hands based on watch screen size.
             */


            /* Scale loaded background image (more efficient) if surface dimensions change. */
//            float scale = ((float) width) / (float) mBackgroundBitmap.getWidth();

//            mBackgroundBitmap = Bitmap.createScaledBitmap(mBackgroundBitmap,
//                    (int) (mBackgroundBitmap.getWidth() * scale),
//                    (int) (mBackgroundBitmap.getHeight() * scale), true);

            /*
             * Create a gray version of the image only if it will look nice on the device in
             * ambient mode. That means we don't want devices that support burn-in
             * protection (slight movements in pixels, not great for images going all the way to
             * edges) and low ambient mode (degrades image quality).
             *
             * Also, if your watch face will know about all images ahead of time (users aren't
             * selecting their own photos for the watch face), it will be more
             * efficient to create a black/white version (png, etc.) and load that when you need it.
             */
//            if (!mBurnInProtection && !mLowBitAmbient) {
//                initGrayBackgroundBitmap();
//            }
        }

//        private void initGrayBackgroundBitmap() {
//            mGrayBackgroundBitmap = Bitmap.createBitmap(
//                    mBackgroundBitmap.getWidth(),
//                    mBackgroundBitmap.getHeight(),
//                    Bitmap.Config.ARGB_8888);
//            Canvas canvas = new Canvas(mGrayBackgroundBitmap);
//            Paint grayPaint = new Paint();
//            ColorMatrix colorMatrix = new ColorMatrix();
//            colorMatrix.setSaturation(0);
//            ColorMatrixColorFilter filter = new ColorMatrixColorFilter(colorMatrix);
//            grayPaint.setColorFilter(filter);
//            canvas.drawBitmap(mBackgroundBitmap, 0, 0, grayPaint);
//        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {

            long now = System.currentTimeMillis();
            mCalendar.setTimeInMillis(now);

            final int canvasColor =
                    (mAmbient && (mLowBitAmbient || mBurnInProtection)) ? Color.BLACK : Color.WHITE;
            canvas.drawColor(canvasColor);

            final float minutesRotation = mCalendar.get(Calendar.MINUTE) * 6f;

            final float hourHandOffset = mCalendar.get(Calendar.MINUTE) / 2f;
            final float hoursRotation = (mCalendar.get(Calendar.HOUR) * 30) + hourHandOffset;

            /*
             * Save the canvas state before we can begin to rotate it.
             */
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

            /* Restore the canvas' original orientation. */
            canvas.restore();

            /* Draw rectangle behind peek card in ambient mode to improve readability. */
            if (mAmbient) {
                canvas.drawRect(mPeekCardBounds, mBackgroundPaint);
            }
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                Wearable.DataApi.addListener(mGoogleApiClient, this);
                mGoogleApiClient.connect();
                registerReceiver();
                /* Update time zone in case it changed while we weren't visible. */
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            } else {
                Wearable.DataApi.removeListener(mGoogleApiClient, this);
                mGoogleApiClient.disconnect();
                unregisterReceiver();
            }

            /* Check and trigger whether or not timer should be running (only in active mode). */
            updateTimer();
        }

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

        /**
         * Starts/stops the {@link #mUpdateTimeHandler} timer based on the state of the watch face.
         */
        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        /**
         * Returns whether the {@link #mUpdateTimeHandler} timer should be running. The timer
         * should only run in active mode.
         */
        private boolean shouldTimerBeRunning() {
            return isVisible() && !mAmbient;
        }

        /**
         * Handle updating the time periodically in interactive mode.
         */
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
                    DataModel model =
                            DataModel.buildDataModelFromItem(mGoogleApiClient, dataEvent.getDataItem());
                    Log.d(getClass().getSimpleName(), model.toString());
                }
            }

        }

        @Override
        public void onConnected(@Nullable Bundle bundle) {
            Log.d(getClass().getSimpleName(), "onConnected");
            Wearable.DataApi.addListener(mGoogleApiClient, this);
            Wearable.DataApi.getDataItems(mGoogleApiClient).setResultCallback(onConnectedResultCallback);
        }

        @Override
        public void onConnectionSuspended(int i) {}

        @Override
        public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {}

        private final ResultCallback<DataItemBuffer> onConnectedResultCallback =
                new ResultCallback<DataItemBuffer>() {
                    @Override
                    public void onResult(@NonNull DataItemBuffer dataItems) {
                        for (DataItem item : dataItems) {
                            DataModel model = DataModel.buildDataModelFromItem(mGoogleApiClient, item);
                            Log.d(getClass().getSimpleName(), model.toString());
                        }

                    }
                };
    }
}
