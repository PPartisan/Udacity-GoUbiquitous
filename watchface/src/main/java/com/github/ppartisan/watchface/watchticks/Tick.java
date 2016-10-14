package com.github.ppartisan.watchface.watchticks;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;

final class Tick {

    private final int index;
    private PointF center;

    Tick(int index, PointF center) {
        this.index = index;
        this.center = center;
    }

    void setCenterPoint(float x, float y) {
        center.set(x, y);
    }

    void draw(Canvas canvas, Paint paint) {

        final float innerRadius = center.x - 10;
        final float outerRadius = center.x + 100;

        final float rotation = (float) (index * Math.PI * 2 / 12);

        float innerX = (float) Math.sin(rotation) * innerRadius;
        float innerY = (float) -Math.cos(rotation) * innerRadius;
        float outerX = (float) Math.sin(rotation) * outerRadius;
        float outerY = (float) -Math.cos(rotation) * outerRadius;

        canvas.drawLine(center.x + innerX, center.y + innerY,
                center.x + outerX, center.y + outerY, paint);

    }

}
