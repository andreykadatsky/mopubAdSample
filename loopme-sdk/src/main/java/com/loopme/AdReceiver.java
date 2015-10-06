package com.loopme;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

class AdReceiver extends BroadcastReceiver {

    private Listener mListener;

    public interface Listener {
        void onDestroyBroadcast();

        void onClickBroadcast();
    }

    public AdReceiver(Listener listener) {
        mListener = listener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (mListener == null) {
            return;
        }

        if (intent.getAction().equalsIgnoreCase(StaticParams.DESTROY_INTENT)) {
            mListener.onDestroyBroadcast();

        } else if (intent.getAction().equalsIgnoreCase(StaticParams.CLICK_INTENT)) {
            mListener.onClickBroadcast();
        }
    }
}
