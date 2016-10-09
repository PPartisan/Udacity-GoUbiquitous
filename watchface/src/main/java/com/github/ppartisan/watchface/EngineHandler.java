package com.github.ppartisan.watchface;

import android.os.Handler;
import android.os.Message;

import java.lang.ref.WeakReference;

class EngineHandler extends Handler {
    private final WeakReference<SunshineWatchFace.Engine> mWeakReference;

    EngineHandler(SunshineWatchFace.Engine reference) {
        mWeakReference = new WeakReference<>(reference);
    }

    @Override
    public void handleMessage(Message msg) {
        SunshineWatchFace.Engine engine = mWeakReference.get();
        if (engine != null) {
            switch (msg.what) {
                case SunshineWatchFace.MSG_UPDATE_TIME:
                    engine.handleUpdateTimeMessage();
                    break;
            }
        }
    }
}
