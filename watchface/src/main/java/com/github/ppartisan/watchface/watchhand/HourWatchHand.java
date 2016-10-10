package com.github.ppartisan.watchface.watchhand;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

final class HourWatchHand extends AbsWatchHand {

    static final String TAG = HourWatchHand.class.getSimpleName();
    private final Paint mPaint;

    HourWatchHand(int accentColor) {
        mPaint = new Paint();
        mPaint.setColor(accentColor);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setAntiAlias(true);
        mPaint.setShadowLayer(SHADOW_RADIUS, 1, -1, Color.DKGRAY);
    }

    @Override
    public void drawWatchHand(Canvas canvas, float cx, float cy) {
        canvas.drawCircle(cx, cy, WATCH_HAND_RADIUS, mPaint);
    }

}
