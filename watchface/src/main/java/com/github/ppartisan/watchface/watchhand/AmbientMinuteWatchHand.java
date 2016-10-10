package com.github.ppartisan.watchface.watchhand;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

final class AmbientMinuteWatchHand extends AbsWatchHand {

    static final String TAG = AmbientMinuteWatchHand.class.getSimpleName();

    private final Paint mAmbientPaint;

    AmbientMinuteWatchHand() {
        mAmbientPaint = new Paint();
        mAmbientPaint.setColor(Color.WHITE);
        mAmbientPaint.setStyle(Paint.Style.STROKE);
        mAmbientPaint.setStrokeWidth(STROKE_WIDTH);
        mAmbientPaint.setAntiAlias(false);
    }

    @Override
    public void drawWatchHand(Canvas canvas, float cx, float cy) {
        canvas.drawCircle(cx, cy, WATCH_HAND_RADIUS, mAmbientPaint);
    }

}
