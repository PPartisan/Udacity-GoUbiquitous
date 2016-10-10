package com.github.ppartisan.watchface.watchhand;
abstract class AbsWatchHand implements WatchHand {

    int radius;

    AbsWatchHand() {
        radius = WATCH_HAND_RADIUS;
    }

    @Override
    public void setWatchHandRadius(int radius) {
        this.radius = radius;
    }

    @Override
    public int getWatchHandRadius() {
        return radius;
    }
}
