package com.github.ppartisan.watchface.watchhand;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

final class MinuteWatchHand extends AbsWatchHand {

    static final String TAG = MinuteWatchHand.class.getSimpleName();

    private final Paint mPaint;

    MinuteWatchHand(int accentColor) {
        mPaint = new Paint();
        mPaint.setColor(accentColor);
        mPaint.setStrokeWidth(STROKE_WIDTH);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setAntiAlias(true);
        mPaint.setShadowLayer(SHADOW_RADIUS, 1, -1, Color.DKGRAY);
    }

    @Override
    public void drawWatchHand(Canvas canvas, float cx, float cy) {
        canvas.drawCircle(cx, cy, WATCH_HAND_RADIUS, mPaint);
    }

}
