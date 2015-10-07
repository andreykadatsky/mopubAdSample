package com.loopme;

import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ListView;
import android.widget.ScrollView;

import com.loopme.Logging.LogLevel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The `LoopMeBanner` class provides facilities to display a custom size ads
 * during natural transition points in your application.
 * <p>
 * It is recommended to implement `LoopMeBanner.Listener` to stay informed about ad state changes,
 * such as when an ad has been loaded or has failed to load its content, when video ad has been watched completely,
 * when an ad has been presented or dismissed from the screen, and when an ad has expired or received a tap.
 */
public class LoopMeBanner extends BaseAd {

    private static final String LOG_TAG = LoopMeBanner.class.getSimpleName();

    /**
     * AppKey for test purposes
     */
    public static final String TEST_MPU_BANNER = "test_mpu";

    private Listener mAdListener;

    private volatile LoopMeBannerView mBannerView;

    private boolean mIsVideoFinished;

    private AdVisibilityListener mAdVisibilityListener;

    public interface AdVisibilityListener {

        void displayed();

        void hidden();

    }
    public interface Listener {

        void onLoopMeBannerLoadSuccess(LoopMeBanner banner);

        void onLoopMeBannerLoadFail(LoopMeBanner banner, LoopMeError error);

        void onLoopMeBannerShow(LoopMeBanner banner);

        void onLoopMeBannerHide(LoopMeBanner banner);

        void onLoopMeBannerClicked(LoopMeBanner banner);

        void onLoopMeBannerLeaveApp(LoopMeBanner banner);

        void onLoopMeBannerVideoDidReachEnd(LoopMeBanner banner);

        void onLoopMeBannerExpired(LoopMeBanner banner);

    }
    /**
     * Creates new `LoopMeBanner` object with the given appKey
     *
     * @param context - application context
     * @param appKey  - your app key
     * @throws IllegalArgumentException if any of parameters is null
     */
    LoopMeBanner(Context context, String appKey) {
        super(context, appKey);
        Logging.out(LOG_TAG, "Start creating banner with app key: " + appKey, LogLevel.INFO);

        mViewController = new ViewController(this);

        Utils.init(context);
    }

    /**
     * Getting already initialized ad object or create new one with specified appKey
     * Note: Returns null if Android version under 4.0
     *
     * @param appKey  - your app key
     * @param context - Activity context
     */
    public static LoopMeBanner getInstance(String appKey, Context context) {
        if (Build.VERSION.SDK_INT >= 14) {
            return LoopMeAdHolder.getBanner(appKey, context);
        } else {
            Logging.out(LOG_TAG, "Not supported Android version. Expected Android 4.0+",
                    LogLevel.DEBUG);
            return null;
        }
    }

    private void ensureAdIsVisible() {
        if (mViewController != null) {
            mViewController.ensureAdIsVisible(mBannerView);
        }
    }

    /**
     * NOTE: should be in UI thread
     */
    @Override
    public void destroy() {
        mAdListener = null;
        if (mBannerView != null) {
            mBannerView.setVisibility(View.GONE);
            mBannerView.removeAllViews();
            mBannerView = null;
        }
        if (mViewController != null) {
            mViewController.destroyMinimizedView();
        }

        super.destroy();
    }

    /**
     * Links (@link LoopMeBannerView) view to banner.
     * If ad doesn't linked to @link LoopMeBannerView, it can't be display.
     *
     * @param viewGroup - @link LoopMeBannerView (container for ad) where ad will be displayed.
     */
    public void bindView(LoopMeBannerView viewGroup) {
        mBannerView = viewGroup;
    }

    public void setMinimizedMode(MinimizedMode mode) {
        if (mViewController != null && mode != null) {
            mViewController.setMinimizedMode(mode);
        }
    }

    LoopMeBannerView getBannerView() {
        return mBannerView;
    }

    /**
     * Checks whether any view already binded to ad or not.
     *
     * @return true - if binded,
     * false - otherwise.
     */
    public boolean isViewBinded() {
        return mBannerView != null;
    }

    /**
     * Pauses video ad
     * Needs to be triggered on appropriate Activity life-cycle method "onPause()".
     */
    public void pause() {
        if (mViewController != null) {
            if (mViewController.getCurrentDisplayMode() == DisplayMode.FULLSCREEN) {
                return;
            }

            if (mViewController.getCurrentVideoState() == VideoState.PLAYING) {
                Logging.out(LOG_TAG, "pause Ad", LogLevel.DEBUG);
                mViewController.setWebViewState(WebviewState.HIDDEN);
            }
        }
    }

    /**
     * Sets listener in order to receive notifications during the loading/displaying ad processes
     */
    public void setListener(Listener listener) {
        mAdListener = listener;
    }

    /**
     * Removes listener.
     */
    public void removeListener() {
        mAdListener = null;
    }

    /**
     * Shows banner ad.
     * This method intended to be used for displaying ad not in scrollable content
     * Otherwise see "handleScrollViewVisible()" methods
     * <p>
     * As a result you'll receive onLoopMeBannerShow() callback
     */
    private void handleAdVisibility() {
        if (mAdState == AdState.SHOWING) {
            ensureAdIsVisible();
            return;
        }
    }

    private boolean handleFirstShow(final LoopMeAdapter adapter, final View view) {
        if (isReady() && mBannerView != null) {
            Logging.out(LOG_TAG, "Banner did start showing ad", LogLevel.INFO);
            mAdState = AdState.SHOWING;
            stopExpirationTimer();

            mViewController.buildVideoAdView(mBannerView);

            if (mBannerView.getVisibility() != View.VISIBLE) {
                mBannerView.setVisibility(View.VISIBLE);
            }

            final ViewTreeObserver observer = (view != null) ?
                    view.getViewTreeObserver()
                    : mBannerView.getViewTreeObserver();

            ViewTreeObserver.OnGlobalLayoutListener layoutListener =
                    new ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {
                            Logging.out(LOG_TAG, "onGlobalLayout", LogLevel.DEBUG);
                            if (mViewController != null &&
                                    mViewController.getCurrentDisplayMode() != DisplayMode.FULLSCREEN) {
                                handleVisibility(adapter, view);
                            }
                            if (observer.isAlive()) {
                                if (Build.VERSION.SDK_INT >= 16) {
                                    observer.removeOnGlobalLayoutListener(this);
                                } else {
                                    observer.removeGlobalOnLayoutListener(this);
                                }
                            }
                        }
                    };
            observer.addOnGlobalLayoutListener(layoutListener);

            onLoopMeBannerShow(this);
            ensureAdIsVisible();
            return true;
        }
        return false;
    }

    private void handleListViewVisible(LoopMeAdapter adapter, ListView listview) {
        if (adapter == null || listview == null) {
            return;
        }
        boolean isAmongVisibleElements = false;
        int first = listview.getFirstVisiblePosition();
        int last = listview.getLastVisiblePosition();
        for (int i = first; i <= last; i++) {
            if (adapter.isAd(i)) {
                switchToNormalMode();
                handleAdVisibility();
                isAmongVisibleElements = true;
            }
        }
        if (isAmongVisibleElements) {
            if (mAdVisibilityListener != null) {
                mAdVisibilityListener.displayed();
            }

        } else {
            switchToMinimizedMode();
            if (mAdVisibilityListener != null) {
                mAdVisibilityListener.hidden();
            }
        }
    }

    private void handleRecyclerViewVisible(LoopMeAdapter adapter, RecyclerView recyclerView) {
        if (adapter == null || recyclerView == null) {
            return;
        }

        boolean isAmongVisibleElements = false;
        int first = 0;
        int last = 0;

        RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
        int orientation = -1;

        if (layoutManager instanceof LinearLayoutManager) {
            first = ((LinearLayoutManager) layoutManager).findFirstVisibleItemPosition();
            last = ((LinearLayoutManager) layoutManager).findLastVisibleItemPosition();
            orientation = ((LinearLayoutManager) layoutManager).getOrientation();

        } else if (layoutManager instanceof GridLayoutManager) {
            first = ((GridLayoutManager) layoutManager).findFirstVisibleItemPosition();
            last = ((GridLayoutManager) layoutManager).findLastVisibleItemPosition();
            orientation = ((GridLayoutManager) layoutManager).getOrientation();

        } else if (layoutManager instanceof StaggeredGridLayoutManager) {
            int[] firsts = null;
            int[] lasts = null;
            try {
                firsts = ((StaggeredGridLayoutManager) layoutManager).findFirstVisibleItemPositions(null);
                lasts = ((StaggeredGridLayoutManager) layoutManager).findLastVisibleItemPositions(null);
            } catch (NullPointerException e) {
                return;
            }

            orientation = ((StaggeredGridLayoutManager) layoutManager).getOrientation();

            List<Integer> firstList = new ArrayList<Integer>(firsts.length);
            for (int i = 0; i < firsts.length; i++) {
                firstList.add(firsts[i]);
            }

            List<Integer> lastList = new ArrayList<Integer>(lasts.length);
            for (int i = 0; i < lasts.length; i++) {
                lastList.add(lasts[i]);
            }

            first = Collections.min(firstList);
            last = Collections.max(lastList);
        }

        if (orientation == OrientationHelper.HORIZONTAL && mViewController != null) {
            mViewController.setHorizontalScrollingOrientation();
        }

        for (int i = first; i <= last; i++) {
            if (adapter.isAd(i)) {
                switchToNormalMode();
                handleAdVisibility();
                isAmongVisibleElements = true;
            }
        }
        if (!isAmongVisibleElements) {
            switchToMinimizedMode();
        }
    }

    /**
     * Show video ad.
     * Calculates video ad visibility inside "ListView"/"RecyclerView" content to manage video playback
     * For better user experience video is paused if it's out of viewport and resumes when it's in viewport
     * "out of viewport" means less then 50% of ad is visible on scrollable content, othervise it's "in viewport"
     * Usage examples:
     * show(null, null) - show ad inside non-scrollable content
     * show(null, scrollView) - show ad inside `ScrollView`
     * show(adapter, listView) - show ad inside `ListView`
     * show(adapter, recyclerView) - show ad inside `RecyclerView`
     *
     * @param adapter - custom adapter which implements @link LoopMeAdapter interface.
     * @param view    - listview/recyclerview/scrollview in which native video ad is displayed.
     */
    public void show(LoopMeAdapter adapter, View view) {
        if (mViewController != null &&
                mViewController.getCurrentDisplayMode() == DisplayMode.FULLSCREEN) {
            return;
        }
        if (mAdState == AdState.SHOWING) {
            handleVisibility(adapter, view);
        } else {
            handleFirstShow(adapter, view);
        }
    }

    private void handleVisibility(LoopMeAdapter adapter, View view) {
        if (view == null) {
            handleAdVisibility();

        } else if (view instanceof ScrollView) {
            handleScrollViewVisible((ScrollView) view);

        } else if (view instanceof ListView) {
            handleListViewVisible(adapter, (ListView) view);

        } else if (view instanceof RecyclerView) {
            handleRecyclerViewVisible(adapter, (RecyclerView) view);
        }
    }

    private void switchToMinimizedMode() {
        if (mAdState == AdState.SHOWING && mViewController != null && !mIsVideoFinished) {
            if (mViewController.isMinimizedModeEnable()) {
                mViewController.switchToMinimizedMode();
            } else {
                pause();
            }
        }

    }

    void playbackFinishedWithError() {
        mIsVideoFinished = true;
    }

    private void switchToNormalMode() {
        if (mAdState == AdState.SHOWING && mViewController != null) {
            mViewController.switchToNormalMode();
        }
    }

    /**
     * Show video ad. This method intended to be used for displaying ad inside scrollable content.
     * Calculates video ad visibility inside "scrollView" content to manage video playback
     * For better user experience video is paused if it's out of viewport and resumes when it's in viewport
     * "out of viewport" means less then 50% of ad is visible on scrollable content, othervise it's "in viewport"
     *
     * @param scrollview - scrollview in which native video ad is displayed.
     */
    private void handleScrollViewVisible(ScrollView scrollview) {
        if (checkVisibilityOnScreen(scrollview)) {
            switchToNormalMode();
            handleAdVisibility();
        } else {
            switchToMinimizedMode();
        }
    }

    private boolean checkVisibilityOnScreen(ScrollView scrollview) {
        if (scrollview == null || mBannerView == null) {
            return false;
        }
        Rect scrollBounds = new Rect();
        scrollview.getHitRect(scrollBounds);
        if (mBannerView.getLocalVisibleRect(scrollBounds)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Dismisses an banner ad
     * This method dismisses an banner ad and only if it is currently presented.
     * NOTE: should be called from UI thread
     * <p>
     * After it banner ad requires "loading process" to be ready for displaying
     * <p>
     * As a result you'll receive onLoopMeBannerHide() notification
     * <p>
     * NOTE: should be triggered from UI thread
     */
    public void dismiss() {
        Logging.out(LOG_TAG, "Banner will be dismissed", LogLevel.DEBUG);
        if (mAdState == AdState.SHOWING) {
            if (mBannerView != null) {
                mBannerView.setVisibility(View.GONE);
                mBannerView.removeAllViews();
                mBannerView = null;
            }
            if (mViewController != null) {
                mViewController.destroyMinimizedView();
                mViewController.setWebViewState(WebviewState.CLOSED);
            }
            onLoopMeBannerHide(this);
        } else {
            Logging.out(LOG_TAG, "Can't dismiss ad, it's not displaying", LogLevel.DEBUG);
        }
    }

    @Override
    public int getAdFormat() {
        return AdFormat.BANNER;
    }

    /**
     * Triggered when banner ad failed to load ad content
     *
     * @param banner - banner object - the sender of message
     * @param error  - error of unsuccesful ad loading attempt
     */
    void onLoopMeBannerLoadFail(LoopMeBanner banner, final LoopMeError error) {
        Logging.out(LOG_TAG, "Ad fails to load: " + error.getMessage(), LogLevel.INFO);
        mAdState = AdState.NONE;
        mIsReady = false;
        stopFetcherTimer();
        if (mAdListener != null) {
            mAdListener.onLoopMeBannerLoadFail(this, error);
        }
    }

    /**
     * Triggered when the banner has successfully loaded the ad content
     *
     * @param banner - banner object the sender of message
     */
    void onLoopMeBannerSuccessLoad(LoopMeBanner banner) {
        Logging.out(LOG_TAG, "Ad successfully loaded ", LogLevel.INFO);
        mIsReady = true;
        mAdState = AdState.NONE;
        stopFetcherTimer();
        if (mAdListener != null) {
            mAdListener.onLoopMeBannerLoadSuccess(this);
        }
    }

    /**
     * Triggered when the banner ad appears on the screen
     *
     * @param banner - banner object the sender of message
     */
    void onLoopMeBannerShow(LoopMeBanner banner) {
        Logging.out(LOG_TAG, "Ad appeared on screen", LogLevel.INFO);
        mIsVideoFinished = false;
        if (mAdListener != null) {
            mAdListener.onLoopMeBannerShow(this);
        }
    }

    /**
     * Triggered when the banner ad disappears on the screen
     *
     * @param banner - banner object the sender of message
     */
    void onLoopMeBannerHide(LoopMeBanner banner) {
        Logging.out(LOG_TAG, "Ad disappeared from screen", LogLevel.INFO);
        mIsReady = false;
        mAdState = AdState.NONE;
        releaseViewController(false);
        if (mAdListener != null) {
            mAdListener.onLoopMeBannerHide(this);
        }
    }

    /**
     * Triggered when the user taps the banner ad and the banner is about to perform extra actions
     * Those actions may lead to displaying a modal browser or leaving your application.
     *
     * @param banner - banner object the sender of message
     */
    void onLoopMeBannerClicked(LoopMeBanner banner) {
        Logging.out(LOG_TAG, "Ad received click event", LogLevel.INFO);
        if (mAdListener != null) {
            mAdListener.onLoopMeBannerClicked(this);
        }
    }

    /**
     * Triggered when your application is about to go to the background, initiated by the SDK.
     * This may happen in various ways, f.e if user wants open the SDK's browser web page in native browser or clicks
     * on `mailto:` links...
     *
     * @param banner - banner object the sender of message
     */
    void onLoopMeBannerLeaveApp(LoopMeBanner banner) {
        Logging.out(LOG_TAG, "Leaving application", LogLevel.INFO);
        if (mAdListener != null) {
            mAdListener.onLoopMeBannerLeaveApp(LoopMeBanner.this);
        }
    }

    /**
     * Triggered only when banner's video was played until the end.
     * It won't be sent if the video was skipped or the banner was dissmissed during the displaying process
     *
     * @param banner - banner object - the sender of message
     */
    void onLoopMeBannerVideoDidReachEnd(LoopMeBanner banner) {
        Logging.out(LOG_TAG, "Video did reach end", LogLevel.INFO);
        mIsVideoFinished = true;
        Runnable runnable = new Runnable() {

            @Override
            public void run() {
                if (mViewController != null) {
                    mViewController.switchToNormalMode();
                }
            }
        };
        Handler handler = new Handler(Looper.getMainLooper());
        if (mViewController.getCurrentDisplayMode() == DisplayMode.MINIMIZED) {
            handler.postDelayed(runnable, StaticParams.SHRINK_MODE_KEEP_AFTER_FINISH_TIME);

        } else if (mViewController.getCurrentDisplayMode() == DisplayMode.FULLSCREEN) {
            handler.post(runnable);
        }

        if (mAdListener != null) {
            mAdListener.onLoopMeBannerVideoDidReachEnd(this);
        }
    }

    /**
     * Triggered when the banner's loaded ad content is expired.
     * Expiration happens when loaded ad content wasn't displayed during some period of time, approximately one hour.
     * Once the banner is presented on the screen, the expiration is no longer tracked and banner won't
     * receive this message
     *
     * @param banner - banner object the sender of message
     */
    void onLoopMeBannerExpired(LoopMeBanner banner) {
        Logging.out(LOG_TAG, "Ad content is expired", LogLevel.INFO);
        mExpirationTimer = null;
        mIsReady = false;
        mAdState = AdState.NONE;
        releaseViewController(false);
        if (mAdListener != null) {
            mAdListener.onLoopMeBannerExpired(this);
        }
    }

    @Override
    void onAdExpired() {
        onLoopMeBannerExpired(this);
    }

    @Override
    void onAdLoadSuccess() {
        onLoopMeBannerSuccessLoad(this);
    }

    @Override
    void onAdLoadFail(final LoopMeError error) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                onLoopMeBannerLoadFail(LoopMeBanner.this, error);
            }
        });
    }

    @Override
    void onAdLeaveApp() {
        onLoopMeBannerLeaveApp(this);
    }

    @Override
    void onAdClicked() {
        onLoopMeBannerClicked(this);
    }

    @Override
    void onAdVideoDidReachEnd() {
        onLoopMeBannerVideoDidReachEnd(this);
    }

    @Override
    int detectWidth() {
        if (mBannerView == null) {
            return 0;
        }
        android.view.ViewGroup.LayoutParams params = mBannerView.getLayoutParams();
        return params.width;
    }

    @Override
    int detectHeight() {
        if (mBannerView == null) {
            return 0;
        }
        android.view.ViewGroup.LayoutParams params = mBannerView.getLayoutParams();
        return params.height;
    }

    public void rebuildView(LoopMeBannerView bannerView) {
        mViewController.rebuildView(bannerView);
    }

    public void setAdVisibilityListener(AdVisibilityListener mAdVisibilityListener) {
        this.mAdVisibilityListener = mAdVisibilityListener;
    }

}
