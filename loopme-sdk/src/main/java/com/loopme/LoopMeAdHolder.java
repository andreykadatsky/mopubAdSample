package com.loopme;

import android.content.Context;
import android.text.TextUtils;

import java.util.HashMap;
import java.util.Map;

public class LoopMeAdHolder {

    private static final Map<String, LoopMeInterstitial> mInterstitialMap =
            new HashMap<String, LoopMeInterstitial>();

    private static final Map<String, LoopMeBanner> mBannerMap =
            new HashMap<String, LoopMeBanner>();

    private LoopMeAdHolder() {
    }

    static void putAd(BaseAd ad) {
        String appKey = ad.getAppKey();

        if (ad.getAdFormat() == AdFormat.INTERSTITIAL) {
            mInterstitialMap.put(appKey, (LoopMeInterstitial) ad);
        } else {
            mBannerMap.put(appKey, (LoopMeBanner) ad);
        }
    }

    static LoopMeInterstitial getInterstitial(String appKey, Context context) {
        if (mInterstitialMap.containsKey(appKey)) {
            return mInterstitialMap.get(appKey);
        } else {
            if (context == null || TextUtils.isEmpty(appKey)) {
                return null;
            } else {
                LoopMeInterstitial interstitial = new LoopMeInterstitial(
                        context, appKey);
                mInterstitialMap.put(appKey, interstitial);
                return interstitial;
            }
        }
    }

    static LoopMeBanner getBanner(String appKey, Context context) {
        if (mBannerMap.containsKey(appKey)) {
            return mBannerMap.get(appKey);
        } else {
            if (context == null || TextUtils.isEmpty(appKey)) {
                return null;
            } else {
                LoopMeBanner banner = new LoopMeBanner(context, appKey);
                mBannerMap.put(appKey, banner);
                return banner;
            }
        }
    }

    static void removeInterstitial(String appKey) {
        mInterstitialMap.remove(appKey);
    }

    static void removeBanner(String appKey) {
        mBannerMap.remove(appKey);
    }
}
