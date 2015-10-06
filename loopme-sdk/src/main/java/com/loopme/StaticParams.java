package com.loopme;

public class StaticParams {

    private StaticParams() {
    }

    public static final String SDK_VERSION = "4.5.0";
    public static String BASE_URL = "loopme.me/api/loopme/ads/v3";

    static boolean DEBUG_MODE = true;

    /**
     * AdParams default values
     */
    static final int DEFAULT_EXPIRED_TIME = 1000 * 60 * 10;//10 minutes
    static final String ORIENTATION_PORT = "portrait";
    static final String ORIENTATION_LAND = "landscape";

    static final String DESTROY_INTENT = "com.loopme.DESTROY_INTENT";
    static final String CLICK_INTENT = "com.loopme.CLICK_INTENT";

    public static long CACHED_VIDEO_LIFE_TIME = 1000 * 60 * 60 * 32;//32 hours

    static final long FETCH_TIMEOUT = 1000 * 60 * 3;//3 minutes

    static final int SHRINK_MODE_KEEP_AFTER_FINISH_TIME = 1000;

    public static boolean USE_MOBILE_NETWORK_FOR_CACHING = false;

    public static final String BANNER_TAG = "banner";
    public static final String INTERSTITIAL_TAG = "interstitial";

    static final String APPKEY_TAG = "appkey";
    static final String FORMAT_TAG = "format";

    //do not change
    public static boolean USE_PART_PRELOAD = false;

    /**
     * Buffering level for play video, when used part preload.
     */
    public static int BUFFERING_LEVEL = 25;
}
