package com.github.ppartisan.watchface.watchhand;

import android.graphics.Canvas;
import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashMap;
import java.util.Map;

public interface WatchHand {

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({AMBIENT_HOUR, AMBIENT_MINUTE, HOUR, MINUTE})
    @interface Mode {}
    int AMBIENT_HOUR = 400;
    int AMBIENT_MINUTE = 401;
    int HOUR = 500;
    int MINUTE = 501;

    int SHADOW_RADIUS = 2;
    int WATCH_HAND_RADIUS = 12;
    float STROKE_WIDTH = 2f;

    void drawWatchHand(Canvas canvas, float cx, float cy);
    void setWatchHandRadius(int radius);
    int getWatchHandRadius();

    class Factory {

        private final Map<String, WatchHand> mWatchHands = new HashMap<>(4);

        public Factory(int color) {
            mWatchHands.put(AmbientHourWatchHand.TAG, new AmbientHourWatchHand());
            mWatchHands.put(AmbientMinuteWatchHand.TAG, new AmbientMinuteWatchHand());
            mWatchHands.put(HourWatchHand.TAG, new HourWatchHand(color));
            mWatchHands.put(MinuteWatchHand.TAG, new MinuteWatchHand(color));
        }

        public WatchHand getWatchHand(@Mode int mode) {

            WatchHand watchHand = null;

            switch (mode) {
                case AMBIENT_HOUR:
                    watchHand = mWatchHands.get(AmbientHourWatchHand.TAG);
                    break;
                case AMBIENT_MINUTE:
                    watchHand = mWatchHands.get(AmbientMinuteWatchHand.TAG);
                    break;
                case HOUR:
                    watchHand = mWatchHands.get(HourWatchHand.TAG);
                    break;
                case MINUTE:
                    watchHand = mWatchHands.get(MinuteWatchHand.TAG);
                    break;
            }

            return watchHand;

        }

    }

}
