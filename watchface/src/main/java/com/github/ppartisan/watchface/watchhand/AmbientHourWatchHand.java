package com.github.ppartisan.watchface.watchhand;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;


final class AmbientHourWatchHand extends AbsWatchHand {

    static final String TAG = AmbientHourWatchHand.class.getSimpleName();
    
    private final Paint mAmbientPaint;

    AmbientHourWatchHand() {
        mAmbientPaint = new Paint();
        mAmbientPaint.setColor(Color.WHITE);
        mAmbientPaint.setStyle(Paint.Style.STROKE);
        mAmbientPaint.setStrokeWidth(STROKE_WIDTH);
        mAmbientPaint.setAntiAlias(false);
    }

    @Override
    public void drawWatchHand(Canvas canvas, float cx, float cy) {
        canvas.drawCircle(cx, cy, radius, mAmbientPaint);
        canvas.drawCircle(cx, cy, radius/2, mAmbientPaint);
    }

}
