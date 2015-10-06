package com.mopub.nativeads;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.ListView;

import com.loopme.LoopMeAdapter;

import java.util.Map;

public class LoopMeEventNative extends CustomEventNative {

    private static final String TAG = LoopMeEventNative.class.getSimpleName();

    private static final String APP_KEY = "app_key";

    private static LoopMeNativeAd sLoopMeNativeAd;
    private static boolean nativeAdAddedToAdapter = false;

    static class OnCompleteListener {

        public OnCompleteListener(CustomEventNativeListener listener) {
            this.listener = listener;
        }

        private CustomEventNativeListener listener;

        void loaded() {
            if (!nativeAdAddedToAdapter) {
                listener.onNativeAdLoaded(sLoopMeNativeAd);
                nativeAdAddedToAdapter = true;
            }
        }

        void failed() {
            listener.onNativeAdFailed(NativeErrorCode.UNSPECIFIED);
        }

        void destroy() {
            nativeAdAddedToAdapter = false;
            sLoopMeNativeAd = null;
        }

    }

    public static void onScroll(LoopMeAdapter adapter, ListView listView) {
        if (sLoopMeNativeAd != null) {
            sLoopMeNativeAd.onScroll(adapter, listView);
        }
    }

    public static void onPause() {
        if (sLoopMeNativeAd != null) {
            sLoopMeNativeAd.onPause();
        }
    }

    public static void onResume(LoopMeAdapter adapter, ListView listView) {
        if (sLoopMeNativeAd != null) {
            sLoopMeNativeAd.onResume(adapter, listView);
        }
    }

    @Override
    protected void loadNativeAd(@NonNull Context context,
                                @NonNull CustomEventNativeListener customEventNativeListener,
                                @NonNull Map<String, Object> localExtras,
                                @NonNull Map<String, String> serverExtras) {
        if (sLoopMeNativeAd == null) {
            String appKey = serverExtras.get(APP_KEY);
            Log.d(TAG, "loadNativeAd, appKey = " + appKey);
            OnCompleteListener listener = new OnCompleteListener(customEventNativeListener);
            sLoopMeNativeAd = new LoopMeNativeAd(context, appKey, listener);
        } else {
            // ad already present, we can have only one at once
            customEventNativeListener.onNativeAdFailed(NativeErrorCode.UNSPECIFIED);
        }

    }

}
