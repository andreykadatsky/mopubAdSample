package com.loopme.tasks;

import android.content.Context;

import com.loopme.AdvertisingIdClient;
import com.loopme.AdvertisingIdClient.AdInfo;
import com.loopme.Logging;
import com.loopme.Logging.LogLevel;

public class AdvIdFetcher implements Runnable {

    private static final String LOG_TAG = AdvIdFetcher.class.getSimpleName();

    private final Context mContext;
    private final Listener mListener;

    private String mAdvertisingId;

    public interface Listener {
        void onComplete(String advId);
    }

    public AdvIdFetcher(Context context, Listener listener) {
        mContext = context;
        mListener = listener;
    }

    @Override
    public void run() {
        mAdvertisingId = "";
        try {
            AdInfo adInfo = AdvertisingIdClient.getAdvertisingIdInfo(mContext);
            mAdvertisingId = adInfo.getId();
        } catch (Exception e) {
            Logging.out(LOG_TAG, "Exception: " + e.getMessage(), LogLevel.ERROR);
        }

        if (mListener != null) {
            mListener.onComplete(mAdvertisingId);
        }
    }
}
