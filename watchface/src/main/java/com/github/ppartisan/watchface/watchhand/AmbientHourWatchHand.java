package com.github.ppartisan.watchface.watchhand;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;


class AmbientHourWatchHand implements WatchHand {

    static final String TAG = AmbientHourWatchHand.class.getSimpleName();

    private final int mInnerWatchHandRadius;
    private final Paint mAmbientPaint;

    AmbientHourWatchHand() {

        mInnerWatchHandRadius = WATCH_HAND_RADIUS/2;

        mAmbientPaint = new Paint();
        mAmbientPaint.setColor(Color.WHITE);
        mAmbientPaint.setStyle(Paint.Style.STROKE);
        mAmbientPaint.setStrokeWidth(STROKE_WIDTH);
        mAmbientPaint.setAntiAlias(false);

    }

    @Override
    public void drawWatchHand(Canvas canvas, float cx, float cy) {
        canvas.drawCircle(cx, cy, WATCH_HAND_RADIUS, mAmbientPaint);
        canvas.drawCircle(cx, cy, mInnerWatchHandRadius, mAmbientPaint);
    }

}
