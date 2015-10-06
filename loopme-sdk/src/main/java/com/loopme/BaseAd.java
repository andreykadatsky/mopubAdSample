package com.loopme;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.loopme.Logging.LogLevel;
import com.loopme.tasks.AdFetcher;
import com.loopme.tasks.AdvIdFetcher;

import java.util.concurrent.Future;

public abstract class BaseAd implements AdTargeting {

    private static final String LOG_TAG = BaseAd.class.getSimpleName();

    private Context mContext;
    private String mAppKey;

    protected volatile ViewController mViewController;

    protected Future mFuture;
    protected String mRequestUrl;

    protected ExpirationTimer mExpirationTimer;
    protected ExpirationTimer.Listener mExpirationListener;

    protected AdFetcherTimer mFetcherTimer;
    protected AdFetcherTimer.Listener mFetcherTimerListener;

    protected volatile AdFetcher.Listener mAdFetcherListener;

    protected volatile int mAdState = AdState.NONE;

    protected volatile boolean mIsReady;

    private AdParams mAdParams;
    private AdTargetingData mAdTargetingData = new AdTargetingData();

    protected Handler mHandler = new Handler(Looper.getMainLooper());

    public BaseAd(Context context, String appKey) {
        if (context == null || TextUtils.isEmpty(appKey)) {
            throw new IllegalArgumentException("Wrong parameters");
        }
        mContext = context;
        mAppKey = appKey;
    }

    /**
     * Indicates whether ad content was loaded successfully and ready to be displayed.
     * After you initialized a `LoopMeInterstitial`/`LoopMeBanner` object and triggered the `load` method,
     * this property will be set to TRUE on it's successful completion.
     * It is set to FALSE when loaded ad content has expired or already was presented,
     * in this case it requires next `load` method triggering
     */
    public boolean isReady() {
        return mIsReady;
    }

    /**
     * Indicates whether `LoopMeInterstitial`/`LoopMeBanner` currently presented on screen.
     * Ad status will be set to `AdState.SHOWING` after trigger `show` method
     *
     * @return true - if ad presented on screen
     * false - if ad absent on scrren
     */
    public boolean isShowing() {
        return mAdState == AdState.SHOWING;
    }

    /**
     * Indicates whether `LoopMeInterstitial`/`LoopMeBanner` in "loading ad content" process.
     * Ad status will be set to `AdState.LOADING` after trigger `load` method
     *
     * @return true - if ad is loading now
     * false - if ad is not loading now
     */
    public boolean isLoading() {
        return mAdState == AdState.LOADING;
    }

    /**
     * Starts loading ad content process.
     * It is recommended triggering it in advance to have interstitial/banner ad ready and to be able to display instantly in your
     * application.
     * After its execution, the interstitial/banner notifies whether the loading of the ad content failed or succeded.
     */
    public void load() {
        if (mAdState == AdState.LOADING || mAdState == AdState.SHOWING) {
            return;
        }
        if (mViewController == null) {
            mViewController = new ViewController(this);
        }

        mAdState = AdState.LOADING;
        Logging.out(LOG_TAG, "Start loading ad", LogLevel.INFO);
        startFetcherTimer();

        if (isReady()) {
            Logging.out(LOG_TAG, "Ad already loaded", LogLevel.INFO);
            onAdLoadSuccess();
            return;
        }

        if (Build.VERSION.SDK_INT < 14) {
            onAdLoadFail(new LoopMeError("Not supported Android version. Expected Android 4.0+"));
            return;
        }

        if (Utils.isOnline(getContext())) {
            proceedLoad();
        } else {
            onAdLoadFail(new LoopMeError("No connection"));
        }
    }

    /**
     * Destroy to clean-up resources if Ad no longer needed
     * If the Ad is in "loading ad" phase, destroy causes it's interrupting and cleaning-up all related resources
     * If the Ad is in "displaying ad" phase, destroy causes "closing ad" and cleaning-up all related resources
     */
    public void destroy() {
        Logging.out(LOG_TAG, "Ad will be destroyed", LogLevel.DEBUG);

        mAdFetcherListener = null;
        mIsReady = false;
        stopExpirationTimer();
        stopFetcherTimer();
        mAdState = AdState.NONE;
        getAdTargetingData().clear();
        AdRequestParametersProvider.getInstance().reset();
        releaseViewController(false);

        if (getAdFormat() == AdFormat.INTERSTITIAL) {
            LoopMeAdHolder.removeInterstitial(mAppKey);
        } else {
            LoopMeAdHolder.removeBanner(mAppKey);
        }

        if (mFuture != null) {
            mFuture.cancel(true);
        }
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
        }
    }

    protected void cancelFetcher() {
        Logging.out(LOG_TAG, "Cancel ad fether", LogLevel.DEBUG);

        mAdFetcherListener = null;
        releaseViewController(true);

        if (mFuture != null) {
            mFuture.cancel(true);
        }
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
        }
    }

    public abstract int getAdFormat();

    public abstract void dismiss();

    abstract void onAdExpired();

    abstract void onAdLoadSuccess();

    abstract void onAdLoadFail(LoopMeError errorCode);

    abstract void onAdLeaveApp();

    abstract void onAdClicked();

    abstract void onAdVideoDidReachEnd();

    abstract int detectWidth();

    abstract int detectHeight();

    /**
     * The appKey uniquely identifies your app to the LoopMe ad network.
     * To get an appKey visit the LoopMe Dashboard.
     */
    public String getAppKey() {
        return mAppKey;
    }

    protected Context getContext() {
        return mContext;
    }

    protected AdTargetingData getAdTargetingData() {
        return mAdTargetingData;
    }

    protected AdParams getAdParams() {
        return mAdParams;
    }

    protected void setAdParams(AdParams params) {
        mAdParams = params;
    }

    protected void fetchAdComplete(AdParams params) {
        setAdParams(params);
        preloadHtmlInWebview(params.getHtml());
    }

    private void preloadHtmlInWebview(String html) {
        if (TextUtils.isEmpty(html)) {
            onAdLoadFail(new LoopMeError("Broken response"));
        } else {
            if (mViewController != null) {
                mViewController.preloadHtml(html);
            } else {
                onAdLoadFail(new LoopMeError("Html loading error"));
            }
        }
    }

    protected AdFetcher.Listener initAdFetcherListener() {
        return new AdFetcher.Listener() {

            @Override
            public void onComplete(final AdParams params,
                                   final LoopMeError error) {

                if (params != null && !params.getPackageIds().isEmpty()) {

                    boolean b = Utils.isPackageInstalled(params.getPackageIds());

                    if (b) {
                        mAdState = AdState.NONE;
                        completeRequest(null, new LoopMeError("No valid ads found"));
                        EventManager eventManager = new EventManager();
                        eventManager.trackSdkEvent(params.getToken());

                    } else {
                        completeRequest(params, error);
                    }
                } else {
                    completeRequest(params, error);
                }
            }
        };
    }

    private void completeRequest(final AdParams params, final LoopMeError error) {

        mHandler.post(new Runnable() {

            @Override
            public void run() {
                if (params == null) {
                    if (error != null) {
                        onAdLoadFail(error);
                    } else {
                        onAdLoadFail(new LoopMeError("Request timeout"));
                    }
                } else {
                    fetchAdComplete(params);
                }
            }
        });
    }

    private void proceedLoad() {
        if (AdRequestParametersProvider.getInstance().getGoogleAdvertisingId() == null) {
            Logging.out(LOG_TAG, "Start initialization google adv id", LogLevel.DEBUG);

            detectGoogleAdvertisingId();

        } else {
            fetchAd();
        }
    }

    private void detectGoogleAdvertisingId() {
        AdvIdFetcher advTask = new AdvIdFetcher(mContext, new AdvIdFetcher.Listener() {

            @Override
            public void onComplete(String advId) {
                AdRequestParametersProvider.getInstance().setGoogleAdvertisingId(advId);
                fetchAd();
            }
        });
        mFuture = ExecutorHelper.getExecutor().submit(advTask);
    }

    protected void releaseViewController(boolean interruptFile) {
        Logging.out(LOG_TAG, "Release ViewController", LogLevel.DEBUG);

        if (mViewController != null) {
            mViewController.destroy(interruptFile);
            mViewController = null;
        }
    }

    protected void startExpirationTimer() {
        if (mExpirationTimer != null || mAdParams == null ||
                mViewController == null || !mViewController.isVideoPresented()) {
            return;
        }
        int validTime = mAdParams.getExpiredTime();
        mExpirationListener = new ExpirationTimer.Listener() {

            @Override
            public void onExpired() {
                onAdExpired();
            }
        };
        mExpirationTimer = new ExpirationTimer(validTime, mExpirationListener);
        mExpirationTimer.start();
    }

    protected void stopExpirationTimer() {
        if (mExpirationTimer != null) {
            Logging.out(LOG_TAG, "Stop schedule expiration", LogLevel.DEBUG);
            mExpirationTimer.cancel();
            mExpirationTimer = null;
        }
        mExpirationListener = null;
    }

    protected void startFetcherTimer() {
        if (mFetcherTimer != null) {
            return;
        }
        mFetcherTimerListener = new AdFetcherTimer.Listener() {

            @Override
            public void onTimeout() {
                cancelFetcher();
                onAdLoadFail(new LoopMeError("Ad processing timeout"));
            }
        };
        mFetcherTimer = new AdFetcherTimer(StaticParams.FETCH_TIMEOUT,
                mFetcherTimerListener);
        mFetcherTimer.start();
    }

    protected void stopFetcherTimer() {
        Logging.out(LOG_TAG, "Stop fetcher timer", LogLevel.DEBUG);
        if (mFetcherTimer != null) {
            mFetcherTimer.cancel();
            mFetcherTimer = null;
        }
        mFetcherTimerListener = null;
    }

    protected void fetchAd() {
        LoopMeAdHolder.putAd(this);
        mRequestUrl = new AdRequestUrlBuilder(mContext).buildRequestUrl(mAppKey, mAdTargetingData);
        if (mRequestUrl == null) {
            onAdLoadFail(new LoopMeError("Error during building ad request url"));
            return;
        }

        mAdFetcherListener = initAdFetcherListener();
        AdFetcher fetcher = new AdFetcher(mRequestUrl, mAdFetcherListener, getAdFormat());
        mFuture = ExecutorHelper.getExecutor().submit(fetcher);
    }

    /**
     * Changes default value of time interval during which video file will be cached.
     * Default time interval is 32 hours.
     */
    public void setVideoCacheTimeInterval(long milliseconds) {
        if (milliseconds > 0) {
            StaticParams.CACHED_VIDEO_LIFE_TIME = milliseconds;
        }
    }

    /**
     * Defines, should use mobile network for caching video or not.
     * By default, video will not cache on mobile network (only on wi-fi)
     *
     * @param b - true if need to cache video on mobile network,
     *          false if need to cache video only on wi-fi network.
     */
    public void useMobileNetworkForCaching(boolean b) {
        StaticParams.USE_MOBILE_NETWORK_FOR_CACHING = b;
    }

    /**
     * Use it for figure out any problems during integration process.
     * We recommend to set it "false" after full integration and testing.
     * <p>
     * If true - all debug logs will be in Logcat.
     * If false - only main info logs will be in Logcat.
     */
    public void setDebugMode(boolean mode) {
        StaticParams.DEBUG_MODE = mode;
    }

    @Override
    public void setKeywords(String keywords) {
        mAdTargetingData.setKeywords(keywords);
    }

    @Override
    public void setGender(String gender) {
        mAdTargetingData.setGender(gender);
    }

    @Override
    public void setYearOfBirth(int year) {
        mAdTargetingData.setYob(year);
    }

    @Override
    public void addCustomParameter(String param, String paramValue) {
        mAdTargetingData.setCustomParameters(param, paramValue);
    }

    ViewController getViewController() {
        return mViewController;
    }
}
