package com.mopub.nativeads;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.loopme.LoopMeAdapter;

import java.util.Map;

/**
 * Preconditions: the root element of ads item should be FrameLayout.
 */
public class LoopMeEventNative extends CustomEventNative {

    private static final String TAG = LoopMeEventNative.class.getSimpleName();

    private static final String APP_KEY = "app_key";

    private static boolean nativeAdAddedToAdapter = false;
    private static LoopMeNativeAd sLoopMeNativeAd;
    private static LoopMeAdapter mLoopMeAdapter;
    private static ListView mListView;
    private static int mBannerBgColor = Color.WHITE;

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

    public static void init(ListView listView) {
        mListView = listView;
        mLoopMeAdapter = new LoopMeAdapter() {
            @Override
            public boolean isAd(int i) {
                if (sLoopMeNativeAd == null) {
                    return false;
                }
                if (mListView.getAdapter().getItem(i) instanceof NativeAdData) {
                    NativeAdData adData = (NativeAdData) mListView.getAdapter().getItem(i);

                    // We put in Title field specific string for Loopme banner,
                    // and can detect is it Loopme ad, using it.
                    if (LoopMeNativeAd.TITLE.equals(adData.getAd().getTitle())) {
                        return true;
                    }
                }
                return false;
            }
        };
    }

    public static void setBannerBackgroundColor(int color) {
        LoopMeEventNative.mBannerBgColor = color;
    }

    public static void onScroll() {
        if (sLoopMeNativeAd != null) {
            sLoopMeNativeAd.onScroll(mLoopMeAdapter, mListView);
            removeBannerFromConvertView();
        }
    }

    public static void onPause() {
        if (sLoopMeNativeAd != null) {
            sLoopMeNativeAd.onPause();
            removeBannerFromConvertView();
        }
    }

    public static void onResume() {
        if (sLoopMeNativeAd != null) {
            sLoopMeNativeAd.onResume(mLoopMeAdapter, mListView);
            removeBannerFromConvertView();
        }
    }

    /**
     * Clear ListView item from Loopme banner, to other bridges can reuse it
     */
    private static void removeBannerFromConvertView() {
        if (mListView == null || mLoopMeAdapter == null) {
            return;
        }
        MoPubAdAdapter moPubAdAdapter = (MoPubAdAdapter) mListView.getAdapter();
        int first = mListView.getFirstVisiblePosition();
        int last = mListView.getLastVisiblePosition();
        for (int i = first; i <= last; i++) {
            if (moPubAdAdapter.isAd(i)) {
                final int childIndex = i - first;
                View itemView = mListView.getChildAt(childIndex);
                if (itemView instanceof ViewGroup) {
                    ViewGroup itemRoot = (ViewGroup) itemView;
                    View bannerHolder = itemRoot.getChildAt(itemRoot.getChildCount() - 1);
                    if (LoopMeNativeAd.BANNER_HOLDER_TAG.equals(bannerHolder.getTag())) {
                        if (!mLoopMeAdapter.isAd(i)) {
                            itemRoot.removeView(bannerHolder);
                        }
                    }
                }
            }
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

            if (TextUtils.isEmpty(appKey)) {
                customEventNativeListener.onNativeAdFailed(NativeErrorCode.UNSPECIFIED);
                return;
            }

            OnCompleteListener listener = new OnCompleteListener(customEventNativeListener);
            sLoopMeNativeAd = new LoopMeNativeAd(context, appKey, mBannerBgColor, listener);
        } else {
            // ad already present, we can have only one at once
            customEventNativeListener.onNativeAdFailed(NativeErrorCode.UNSPECIFIED);
        }

    }

}
