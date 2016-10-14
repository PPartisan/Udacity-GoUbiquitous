package com.github.ppartisan.watchface.watchticks;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.support.annotation.IntDef;
import android.support.v4.content.ContextCompat;

import com.github.ppartisan.watchface.R;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

public final class WatchTicks {

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({AMBIENT,REGULAR})
    public @interface Mode {}
    public static final int AMBIENT = 1;
    public static final int REGULAR = 2;

    private List<Tick> mWatchTicks;
    private final Paint mRegularPaint, mAmbientPaint;

    public WatchTicks(Context context, PointF center) {

        if (center == null) center = new PointF(0,0);

        mWatchTicks = buildWatchTicks(center);

        mRegularPaint = new Paint();
        mRegularPaint.setColor(ContextCompat.getColor(context, R.color.accent));
        mRegularPaint.setStrokeCap(Paint.Cap.ROUND);
        mRegularPaint.setAntiAlias(true);
        mRegularPaint.setStrokeWidth(3);
        mRegularPaint.setStyle(Paint.Style.STROKE);

        mAmbientPaint = new Paint();
        mAmbientPaint.setColor(Color.WHITE);
        mRegularPaint.setStrokeWidth(3);
        mRegularPaint.setAntiAlias(false);
        mRegularPaint.setStyle(Paint.Style.STROKE);

    }

    public void setCenterPoint(float x, float y) {
        updateWatchTicksCenter(x, y);
    }

    public void drawTicks(Canvas canvas, @Mode int mode) {
        for (Tick tick : mWatchTicks) {
            tick.draw(canvas, getWatchTickPaint(mode));
        }
    }

    private Paint getWatchTickPaint(@Mode int mode) {

        Paint paint = null;

        switch (mode) {
            case AMBIENT:
                paint = mAmbientPaint;
                break;
            case REGULAR:
                paint = mRegularPaint;
                break;
        }

        return paint;
    }

    private List<Tick> buildWatchTicks(PointF center) {

        List<Tick> watchTicks = new ArrayList<>(12);
        for (int i = 0; i < 12; i++) {
            watchTicks.add(new Tick(i, center));
        }
        return watchTicks;

    }

    private void updateWatchTicksCenter(float x, float y) {

        if (mWatchTicks == null) {
            mWatchTicks = buildWatchTicks(new PointF(x, y));
            return;
        }

        for (Tick tick : mWatchTicks) {
            tick.setCenterPoint(x, y);
        }

    }





}
