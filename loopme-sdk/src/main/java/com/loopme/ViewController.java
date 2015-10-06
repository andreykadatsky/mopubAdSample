package com.loopme;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.os.Build;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.webkit.WebView;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;

import com.loopme.Logging.LogLevel;

class ViewController implements TextureView.SurfaceTextureListener {

    private static final String LOG_TAG = ViewController.class.getSimpleName();

    private static final String EXTRA_URL = "url";

    private AdView mAdView;
    private volatile Bridge.Listener mBridgeListener;

    private VideoController mVideoController;

    private boolean mIsVideoPresented;

    private BaseAd mAd;

    private TextureView mTextureView;

    private int mDisplayMode = DisplayMode.NORMAL;
    private MinimizedMode mMinimizedMode;
    private LoopMeBannerView mMinimizedView;

    private boolean mHorizontalScrollOrientation;

    //we should ignore first command
    private boolean mIsFirstFullScreenCommand = true;

    public ViewController(BaseAd ad) {
        mAd = ad;
        mAdView = new AdView(mAd.getContext());
        mBridgeListener = initBridgeListener();
        mAdView.addBridgeListener(mBridgeListener);
        mAdView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return (event.getAction() == MotionEvent.ACTION_MOVE);
            }
        });
        mVideoController = new VideoController(mAd.getAppKey(), mAdView, mAd.getAdFormat());
    }

    void destroy(boolean interruptFile) {
        mBridgeListener = null;
        if (mVideoController != null) {
            mVideoController.destroy(interruptFile);
            mVideoController = null;
        }
        if (mAdView != null) {
            mAdView.stopLoading();
            mAdView.clearCache(true);
            mAdView = null;
            Logging.out(LOG_TAG, "AdView destroyed", LogLevel.DEBUG);
        }
        mMinimizedMode = null;
    }

    void setWebViewState(int state) {
        if (mAdView != null) {
            mAdView.setWebViewState(state);
        }
    }

    void onAdShake() {
        if (mAdView != null) {
            mAdView.shake();
        }
    }

    int getCurrentVideoState() {
        if (mAdView != null) {
            return mAdView.getCurrentVideoState();
        }
        return -1;
    }

    int getCurrentDisplayMode() {
        return mDisplayMode;
    }

    void buildStaticAdView(ViewGroup bannerView) {
        if (bannerView == null || mAdView == null) {
            return;
        }
        mAdView.setBackgroundColor(Color.BLACK);
        bannerView.addView(mAdView);
    }

    void buildVideoAdView(ViewGroup bannerView) {
        mTextureView = new TextureView(mAd.getContext());
        mTextureView.setBackgroundColor(Color.TRANSPARENT);
        mTextureView.setSurfaceTextureListener(this);

        mAdView.setBackgroundColor(Color.TRANSPARENT);
        mAdView.setLayerType(WebView.LAYER_TYPE_SOFTWARE, null);
        bannerView.setBackgroundColor(Color.BLACK);
        bannerView.addView(mTextureView, 0);
        if (mAdView.getParent() != null) {
            ((ViewGroup) mAdView.getParent()).removeView(mAdView);
        }
        bannerView.addView(mAdView, 1);
    }

    void rebuildView(ViewGroup bannerView) {
        if (bannerView == null || mAdView == null || mTextureView == null) {
            return;
        }
        bannerView.setBackgroundColor(Color.BLACK);
        if (mTextureView.getParent() != null) {
            ((ViewGroup) mTextureView.getParent()).removeView(mTextureView);
        }
        if (mAdView.getParent() != null) {
            ((ViewGroup) mAdView.getParent()).removeView(mAdView);
        }

        bannerView.addView(mTextureView, 0);
        bannerView.addView(mAdView, 1);
    }

    void setHorizontalScrollingOrientation() {
        mHorizontalScrollOrientation = true;
    }

    void ensureAdIsVisible(View view) {
        if (mAdView == null || view == null) {
            return;
        }

        Rect rect = new Rect();
        boolean b = view.getGlobalVisibleRect(rect);

        int halfOfView = mHorizontalScrollOrientation ? view.getWidth() / 2 : view.getHeight() / 2;
        int rectHeight = mHorizontalScrollOrientation ? rect.width() : rect.height();

        if (b) {
            if (rectHeight < halfOfView) {
                setWebViewState(WebviewState.HIDDEN);
            } else if (rectHeight >= halfOfView) {
                setWebViewState(WebviewState.VISIBLE);
            }
        } else {
            setWebViewState(WebviewState.HIDDEN);
        }
    }

    void switchToMinimizedMode() {
        if (mDisplayMode == DisplayMode.MINIMIZED) {
            if (getCurrentVideoState() == VideoState.PAUSED) {
                setWebViewState(WebviewState.VISIBLE);
            }
            return;
        }
        Logging.out(LOG_TAG, "switchToMinimizedMode", LogLevel.DEBUG);
        mDisplayMode = DisplayMode.MINIMIZED;

        int width = mMinimizedMode.getWidth();
        int height = mMinimizedMode.getHeight();
        mMinimizedView = new LoopMeBannerView(mAdView.getContext(), width, height);

        rebuildView(mMinimizedView);
        addBordersToView(mMinimizedView);

        if (mAdView.getCurrentWebViewState() == WebviewState.HIDDEN) {
            mMinimizedView.setAlpha(0);
        }

        mMinimizedMode.getRootView().addView(mMinimizedView);
        configMinimizedViewLayoutParams(mMinimizedView);

        setWebViewState(WebviewState.VISIBLE);

        mAdView.setOnTouchListener(new SwipeListener(width,
                new SwipeListener.Listener() {
                    @Override
                    public void onSwipe(boolean toRight) {
                        mAdView.setWebViewState(WebviewState.HIDDEN);

                        Animation anim = AnimationUtils.makeOutAnimation(mAd.getContext(),
                                toRight);
                        anim.setDuration(200);
                        mMinimizedView.startAnimation(anim);

                        switchToNormalMode();
                        mMinimizedMode = null;
                    }
                }));
    }

    private void configMinimizedViewLayoutParams(LoopMeBannerView bannerView) {
        LayoutParams lp = (LayoutParams) bannerView.getLayoutParams();
        lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        lp.bottomMargin = mMinimizedMode.getMarginBottom();
        lp.rightMargin = mMinimizedMode.getMarginRight();
        bannerView.setLayoutParams(lp);
    }

    @SuppressLint("NewApi")
    private void addBordersToView(LoopMeBannerView bannerView) {
        ShapeDrawable drawable = new ShapeDrawable(new RectShape());
        drawable.getPaint().setColor(Color.BLACK);
        drawable.getPaint().setStyle(Style.FILL_AND_STROKE);
        drawable.getPaint().setAntiAlias(true);

        bannerView.setPadding(2, 2, 2, 2);
        if (Build.VERSION.SDK_INT < 16) {
            bannerView.setBackgroundDrawable(drawable);
        } else {
            bannerView.setBackground(drawable);
        }
    }

    void switchToNormalMode() {
        if (mDisplayMode == DisplayMode.NORMAL) {
            return;
        }
        if (mDisplayMode == DisplayMode.FULLSCREEN) {
            handleFullscreenMode(false);
        }
        Logging.out(LOG_TAG, "switchToNormalMode", LogLevel.DEBUG);
        mDisplayMode = DisplayMode.NORMAL;

        LoopMeBannerView initialView = ((LoopMeBanner) mAd).getBannerView();
        initialView.setVisibility(View.VISIBLE);

        if (mMinimizedView != null && mMinimizedView.getParent() != null) {
            ((ViewGroup) mMinimizedView.getParent()).removeView(mMinimizedView);
            rebuildView(initialView);
            mMinimizedView.removeAllViews();
        }

        mAdView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return (event.getAction() == MotionEvent.ACTION_MOVE);
            }
        });

    }

    void setMinimizedMode(MinimizedMode mode) {
        mMinimizedMode = mode;
    }

    boolean isMinimizedModeEnable() {
        return mMinimizedMode != null && mMinimizedMode.getRootView() != null;
    }

    void destroyMinimizedView() {
        if (mMinimizedView != null) {
            if (mMinimizedView.getParent() != null) {
                ((ViewGroup) mMinimizedView.getParent()).removeView(mMinimizedView);
            }
            mMinimizedView.removeAllViews();
            mMinimizedView = null;
        }
    }

    void preloadHtml(String html) {
        if (mAdView != null) {
            Logging.out(LOG_TAG, "loadDataWithBaseURL", LogLevel.DEBUG);
            mAdView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
        } else {
            mAd.onAdLoadFail(new LoopMeError("Html loading error"));
        }
    }

    public boolean isVideoPresented() {
        return mIsVideoPresented;
    }

    private Bridge.Listener initBridgeListener() {
        return new Bridge.Listener() {

            @Override
            public void onJsVideoPlay(int time) {
                handleVideoPlay(time);
            }

            @Override
            public void onJsVideoPause(final int time) {
                handleVideoPause(time);
            }

            @Override
            public void onJsVideoMute(boolean mute) {
                handleVideoMute(mute);
            }

            @Override
            public void onJsVideoLoad(final String videoUrl) {
                handleVideoLoad(videoUrl);
            }

            @Override
            public void onJsLoadSuccess() {
                handleLoadSuccess();
            }

            @Override
            public void onJsClose() {
                handleClose();
            }

            @Override
            public void onJsLoadFail(String mess) {
                handleLoadFail(mess);
            }

            @Override
            public void onJsFullscreenMode(boolean b) {
                handleFullscreenMode(b);
            }

            @Override
            public void onNonLoopMe(String url) {
                handleNonLoopMe(url);
            }

            @Override
            public void onJsVideoStretch(boolean b) {
                handleVideoStretch(b);
            }
        };
    }

    VideoController getVideoController() {
        return mVideoController;
    }

    private void loadFail(BaseAd baseAd, LoopMeError error) {
        baseAd.onAdLoadFail(error);
    }

    private void handleLoadSuccess() {
        Logging.out(LOG_TAG, "JS command: load success", LogLevel.DEBUG);
        mAd.startExpirationTimer();
        mAd.onAdLoadSuccess();
    }

    private void handleLoadFail(String mess) {
        Logging.out(LOG_TAG, "JS command: load fail", LogLevel.DEBUG);
        loadFail(mAd, new LoopMeError("Failed to process ad"));
    }

    private void handleVideoLoad(String videoUrl) {
        Logging.out(LOG_TAG, "JS command: load video " + videoUrl, LogLevel.DEBUG);

        mIsVideoPresented = true;
        if (mVideoController != null) {
            mVideoController.loadVideoFile(videoUrl, mAd.getContext());
        }
    }

    private void handleVideoMute(boolean mute) {
        Logging.out(LOG_TAG, "JS command: video mute " + mute, LogLevel.DEBUG);

        if (mVideoController != null) {
            mVideoController.muteVideo(mute);
        }
    }

    private void handleVideoPlay(final int time) {
        Logging.out(LOG_TAG, "JS command: play video " + time, LogLevel.DEBUG);

        if (mVideoController != null) {
            mVideoController.playVideo(time);
        }

        if (mDisplayMode == DisplayMode.MINIMIZED) {
            Utils.animateAppear(mMinimizedView);
        }
    }

    private void handleVideoPause(int time) {
        Logging.out(LOG_TAG, "JS command: pause video " + time, LogLevel.DEBUG);
        if (mVideoController != null) {
            mVideoController.pauseVideo(time);
        }
    }

    private void handleClose() {
        Logging.out(LOG_TAG, "JS command: close", LogLevel.DEBUG);
        mAd.dismiss();
    }

    private void handleVideoStretch(boolean b) {
        Logging.out(LOG_TAG, "JS command: stretch video ", LogLevel.DEBUG);
        if (mVideoController != null) {
            if (b) {
                mVideoController.setStreachVideoParameter(VideoController.StretchOption.STRECH);
            } else {
                mVideoController.setStreachVideoParameter(VideoController.StretchOption.NO_STRETCH);
            }
        }
    }

    private void handleFullscreenMode(boolean b) {
        if (mIsFirstFullScreenCommand) {
            mIsFirstFullScreenCommand = false;
            mAdView.setFullscreenMode(false);
            return;
        }
        if (b) {
            switchToFullScreenMode();
        } else {
            broadcastDestroyIntent();
        }
        mAdView.setFullscreenMode(b);
    }

    private void broadcastDestroyIntent() {
        Intent intent = new Intent();
        intent.setAction(StaticParams.DESTROY_INTENT);
        mAd.getContext().sendBroadcast(intent);
    }

    private void switchToFullScreenMode() {
        if (mDisplayMode != DisplayMode.FULLSCREEN) {
            Logging.out(LOG_TAG, "switch to fullscreen mode", LogLevel.DEBUG);
            mDisplayMode = DisplayMode.FULLSCREEN;

            startAdActivity();
        }
    }

    private void startAdActivity() {
        LoopMeAdHolder.putAd(mAd);

        Context context = mAd.getContext();
        Intent intent = new Intent(context, AdActivity.class);
        intent.putExtra(StaticParams.APPKEY_TAG, mAd.getAppKey());
        intent.putExtra(StaticParams.FORMAT_TAG, mAd.getAdFormat());
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    private void handleNonLoopMe(String url) {
        Logging.out(LOG_TAG, "Non Js command", LogLevel.DEBUG);
        Context context = mAd.getContext();
        if (Utils.isOnline(context)) {
            Intent intent = new Intent(context, AdBrowserActivity.class);
            intent.putExtra(EXTRA_URL, url);
            intent.putExtra(StaticParams.APPKEY_TAG, mAd.getAppKey());
            intent.putExtra(StaticParams.FORMAT_TAG, mAd.getAdFormat());
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            mAd.onAdClicked();
            setWebViewState(WebviewState.HIDDEN);
            broadcastAdClickedIntent();

            context.startActivity(intent);
        } else {
            Logging.out(LOG_TAG, "No internet connection", LogLevel.DEBUG);
        }
    }

    private void broadcastAdClickedIntent() {
        Intent intent = new Intent();
        intent.setAction(StaticParams.CLICK_INTENT);
        mAd.getContext().sendBroadcast(intent);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width,
                                          int height) {

        Logging.out(LOG_TAG, "onSurfaceTextureAvailable", LogLevel.DEBUG);

        int viewWidth = 0;
        int viewHeight = 0;

        switch (mDisplayMode) {
            case DisplayMode.MINIMIZED:
                if (mMinimizedMode != null) {
                    viewWidth = mMinimizedMode.getWidth();
                    viewHeight = mMinimizedMode.getHeight();
                } else {
                    Logging.out(LOG_TAG, "WARNING: MinimizedMode is null", LogLevel.ERROR);
                }
                break;

            case DisplayMode.NORMAL:
                viewWidth = mAd.detectWidth();
                viewHeight = mAd.detectHeight();
                break;

            case DisplayMode.FULLSCREEN:
                viewWidth = Utils.getScreenWidth();
                viewHeight = Utils.getScreenHeight();
                break;

            default:
                Logging.out(LOG_TAG, "Unknown display mode", LogLevel.ERROR);
                break;
        }

        if (mVideoController != null) {
            mVideoController.setSurface(mTextureView);
            mVideoController.resizeVideo(mTextureView, viewWidth, viewHeight);
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width,
                                            int height) {
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        Logging.out(LOG_TAG, "onSurfaceTextureDestroyed", LogLevel.DEBUG);
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
    }
}
