package com.loopme;

import android.app.Activity;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.loopme.Logging.LogLevel;

public final class AdActivity extends Activity implements AdReceiver.Listener {

    private static final String LOG_TAG = AdActivity.class.getSimpleName();

    private BaseAd mBaseAd;
    private ViewController mViewController;

    private FrameLayout mLayout;

    /**
     * If true - standard activity behaviour when it become inactive.
     * Used to keep activity alive when ad clicked and started AdBrowserActivity.
     * If false - finish activity at any time when it become inactive (incoming call or etc.)
     */
    private boolean mKeepAlive = true;
    private AdReceiver mReceiver;

    private SensorManager mSensorManager;

    private float mAccel;
    private float mAccelCurrent;
    private float mAccelLast;

    private int mInitialOrientation;

    private final SensorEventListener mSensorListener = new SensorEventListener() {

        public void onSensorChanged(SensorEvent se) {
            float x = se.values[0];
            float y = se.values[1];
            float z = se.values[2];
            mAccelLast = mAccelCurrent;
            mAccelCurrent = (float) Math.sqrt((double) (x * x + y * y + z * z));
            float delta = mAccelCurrent - mAccelLast;
            mAccel = mAccel * 0.9f + delta;

            if (delta > 5) {
                if (mViewController != null) {
                    mViewController.onAdShake();
                }
            }
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    @Override
    public final void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mInitialOrientation = Utils.getScreenOrientation(this);

        String appKey = getIntent().getStringExtra(StaticParams.APPKEY_TAG);
        if (TextUtils.isEmpty(appKey)) {
            Logging.out(LOG_TAG, "Empty app key", LogLevel.ERROR);
        }
        int format = getIntent().getIntExtra(StaticParams.FORMAT_TAG, 0);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);

        Logging.out(LOG_TAG, "onCreate", LogLevel.DEBUG);

        if (format == AdFormat.INTERSTITIAL) {
            mBaseAd = LoopMeAdHolder.getInterstitial(appKey, null);
        } else if (format == AdFormat.BANNER) {
            mBaseAd = LoopMeAdHolder.getBanner(appKey, null);
        }

        if (mBaseAd == null || mBaseAd.getViewController() == null) {
            Logging.out(LOG_TAG, "No ads with app key " + appKey, LogLevel.ERROR);
            finish();
        } else {
            mViewController = mBaseAd.getViewController();

            if (mBaseAd.getAdFormat() == AdFormat.INTERSTITIAL) {
                applyOrientationFromAdParams();
            } else {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
            }

            mLayout = buildLayout();
            setContentView(mLayout);

            initSensor();
            initDestroyReceiver();

            if (mBaseAd.getAdFormat() == AdFormat.INTERSTITIAL) {
                ((LoopMeInterstitial) mBaseAd).onLoopMeInterstitialShow((LoopMeInterstitial) mBaseAd);
            }
        }
    }

    private void initSensor() {
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccel = 0.00f;
        mAccelCurrent = SensorManager.GRAVITY_EARTH;
        mAccelLast = SensorManager.GRAVITY_EARTH;
    }

    private FrameLayout buildLayout() {
        mLayout = new FrameLayout(this);

        if (mBaseAd.getAdFormat() == AdFormat.INTERSTITIAL) {
            if (mViewController != null) {
                if (isVideoPresented()) {
                    mViewController.buildVideoAdView(mLayout);
                } else {
                    mViewController.buildStaticAdView(mLayout);
                }
            }
        } else {
            LoopMeBannerView banner = new LoopMeBannerView(this);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT);
            mViewController.rebuildView(banner);
            mLayout.addView(banner, params);
        }

        return mLayout;
    }

    private boolean isVideoPresented() {
        VideoController videoController = mViewController.getVideoController();
        return videoController != null
                && videoController.isMediaPlayerValid();
    }

    private void initDestroyReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(StaticParams.DESTROY_INTENT);
        filter.addAction(StaticParams.CLICK_INTENT);
        mReceiver = new AdReceiver(this);
        registerReceiver(mReceiver, filter);
    }

    /**
     * Apply orientation from AdParams.
     * Do nothing if orientation parameter absent in AdParams.
     */
    private void applyOrientationFromAdParams() {
        String or = mBaseAd.getAdParams().getAdOrientation();
        if (or == null) {
            return;
        }

        if (or.equalsIgnoreCase(StaticParams.ORIENTATION_PORT)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
        } else if (or.equalsIgnoreCase(StaticParams.ORIENTATION_LAND)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        }
    }

    @Override
    protected void onDestroy() {
        Logging.out(LOG_TAG, "onDestroy", LogLevel.DEBUG);
        unregisterReceiver(mReceiver);
        if (mLayout != null) {
            mLayout.removeAllViews();
        }
        if (mBaseAd != null && mBaseAd.getAdFormat() == AdFormat.INTERSTITIAL) {
            ((LoopMeInterstitial) mBaseAd).onLoopMeInterstitialHide((LoopMeInterstitial) mBaseAd);
        }
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Logging.out(LOG_TAG, "onPause", LogLevel.DEBUG);
        if (mSensorManager != null) {
            mSensorManager.unregisterListener(mSensorListener);
        }
        if (mBaseAd.getAdFormat() == AdFormat.BANNER) {
            ((LoopMeBanner) mBaseAd).pause();
        } else if (!mKeepAlive && mBaseAd.getAdFormat() == AdFormat.INTERSTITIAL) {
            if (mViewController != null) {
                mViewController.setWebViewState(WebviewState.CLOSED);
            }
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Logging.out(LOG_TAG, "onResume", LogLevel.DEBUG);
        mKeepAlive = false;
        if (mViewController != null) {
            mViewController.setWebViewState(WebviewState.VISIBLE);
        }

        if (mSensorManager != null) {
            mSensorManager.registerListener(mSensorListener, mSensorManager.getDefaultSensor(
                    Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    public void onBackPressed() {
        if (mBaseAd.getAdFormat() == AdFormat.BANNER) {
            setRequestedOrientation(mInitialOrientation);
            mViewController.rebuildView(((LoopMeBanner) mBaseAd).getBannerView());
            super.onBackPressed();
        }
    }

    @Override
    public void onDestroyBroadcast() {
        Logging.out(LOG_TAG, "onDestroyBroadcast", LogLevel.DEBUG);
        if (mBaseAd.getAdFormat() == AdFormat.BANNER) {
            setRequestedOrientation(mInitialOrientation);
            mViewController.rebuildView(((LoopMeBanner) mBaseAd).getBannerView());
            mViewController.switchToNormalMode();
        }
        finish();
    }

    @Override
    public void onClickBroadcast() {
        mKeepAlive = true;
    }
}