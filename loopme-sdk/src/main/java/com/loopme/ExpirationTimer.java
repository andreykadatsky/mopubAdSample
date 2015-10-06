package com.loopme;

import android.os.CountDownTimer;

import com.loopme.Logging.LogLevel;

public class ExpirationTimer extends CountDownTimer {

    private static final String LOG_TAG = ExpirationTimer.class.getSimpleName();

    private Listener mListener;

    public interface Listener {
        void onExpired();
    }

    public ExpirationTimer(long millisInFuture, Listener listener) {
        super(millisInFuture, 1000 * 60);
        if (listener == null) {
            Logging.out(LOG_TAG, "Listener should not be null", LogLevel.DEBUG);
        }
        Logging.out(LOG_TAG, "Start schedule expiration", LogLevel.DEBUG);
        mListener = listener;
    }

    @Override
    public void onFinish() {
        if (mListener != null) {
            mListener.onExpired();
        }
    }

    @Override
    public void onTick(long millisUntilFinished) {
    }
}
