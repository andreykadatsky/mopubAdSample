package com.loopme;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.Surface;
import android.view.TextureView;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;

import android.widget.Toast;
import com.loopme.Logging.LogLevel;
import com.loopme.tasks.VideoTask;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

class VideoController implements OnPreparedListener, OnErrorListener, OnCompletionListener,
        MediaPlayer.OnVideoSizeChangedListener, MediaPlayer.OnBufferingUpdateListener {

    private static final String LOG_TAG = VideoController.class.getSimpleName();

    private volatile MediaPlayer mPlayer;
    private AdView mAdView;

    private StretchOption mStretch = StretchOption.NONE;
    private Handler mHandler;

    private Runnable mRunnable;

    private String mAppKey;
    private int mVideoDuration;

    private VideoTask mVideoTask;

    private int mFormat;

    private boolean mWasError;
    private int mVideoWidth;

    private int mVideoHeight;

    private boolean mPostponeResize;
    private TextureView mTextureView;
    private int mResizeWidth;

    private int mResizeHeight;
    private boolean mPlayerReady;

    private boolean mEnoughBufferingLevel;
    private boolean mIsFirstBufferingUpdate100 = true;

    @Override
    public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
        mVideoWidth = width;
        mVideoHeight = height;
    }

    public enum StretchOption {
        NONE,
        STRECH,
        NO_STRETCH
    }

    public VideoController(String appKey, AdView adview, int format) {
        mAppKey = appKey;
        mAdView = adview;
        mFormat = format;
        mHandler = new Handler(Looper.getMainLooper());
        initRunnable();
    }

    public void loadVideoFile(String videoUrl, Context context) {
        mVideoTask = new VideoTask(videoUrl, context,
                new VideoTask.Listener() {

                    @Override
                    public void onComplete(String filePath, boolean cached) {
                        if (filePath == null) {
                            sendLoadFail(new LoopMeError("Error during video loading"));
                        }
                        if (cached) {
                            preparePlayerFromFile(filePath);
                        } else {
                            if (StaticParams.USE_PART_PRELOAD) {
                                preparePlayerFromUrl(filePath);
                            } else {
                                sendLoadFail(new LoopMeError("Error during video loading"));
                            }
                        }
                    }
                });
        mVideoTask.start();
    }

    private void sendLoadFail(LoopMeError error) {
        BaseAd ad;
        if (mFormat == AdFormat.INTERSTITIAL) {
            ad = LoopMeAdHolder.getInterstitial(mAppKey, null);
        } else {
            ad = LoopMeAdHolder.getBanner(mAppKey, null);
        }
        ad.onAdLoadFail(error);
    }

    private void setVideoState(int state) {
        if (mAdView != null) {
            mAdView.setVideoState(state);
        }
    }

    void pauseVideo(int time) {
        if (mPlayer != null && mAdView != null && !mWasError) {
            try {
                if (mPlayer.isPlaying()) {
                    Logging.out(LOG_TAG, "Pause video", LogLevel.DEBUG);
                    mHandler.removeCallbacks(mRunnable);
                    mPlayer.pause();
                    mAdView.setVideoState(VideoState.PAUSED);
                }
            } catch (IllegalStateException e) {
                e.printStackTrace();
                Logging.out(LOG_TAG, e.getMessage(), LogLevel.ERROR);
            }
        }
    }

    void playVideo(int time) {
        if (mPlayer != null && mAdView != null && !mWasError) {
            try {
                if (mPlayer.isPlaying()) {
                    return;
                }
                Logging.out(LOG_TAG, "Play video", LogLevel.DEBUG);
                if (time > 0) {
                    mPlayer.seekTo(time);
                }

                mPlayer.start();
                mAdView.setVideoState(VideoState.PLAYING);

                mHandler.postDelayed(mRunnable, 200);

            } catch (IllegalStateException e) {
                e.printStackTrace();
                Logging.out(LOG_TAG, e.getMessage(), LogLevel.ERROR);
            }
        }
    }

    private void initRunnable() {
        mRunnable = new Runnable() {

            @Override
            public void run() {
                if (mPlayer == null || mAdView == null) {
                    return;
                }
                int position = mPlayer.getCurrentPosition();
                mAdView.setVideoCurrentTime(position);
                if (position < mVideoDuration) {
                    mHandler.postDelayed(mRunnable, 200);
                }
            }
        };
    }

    void setStreachVideoParameter(StretchOption option) {
        mStretch = option;
    }

    void destroy(boolean interruptFile) {
        Logging.out(LOG_TAG, "Destroy VideoController", LogLevel.DEBUG);
        if (mHandler != null) {
            mHandler.removeCallbacks(mRunnable);
        }
        mRunnable = null;
        if (mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
        }
        if (mVideoTask != null) {
            mVideoTask.stop(interruptFile);
        }
    }

    private void preparePlayerFromFile(String filePath) {
        mPlayer = new MediaPlayer();
        initPlayerListeners();

        try {
            File file = new File(filePath);
            FileInputStream inputStream = new FileInputStream(file);
            mPlayer.setDataSource(inputStream.getFD());
            mEnoughBufferingLevel = true;

            mPlayer.prepareAsync();

        } catch (IllegalStateException e) {
            Logging.out(LOG_TAG, e.getMessage(), LogLevel.ERROR);
            setVideoState(VideoState.BROKEN);

        } catch (IOException e) {
            Logging.out(LOG_TAG, e.getMessage(), LogLevel.ERROR);
            setVideoState(VideoState.BROKEN);
        }
    }

    private void initPlayerListeners() {
        mPlayer.setLooping(false);
        mPlayer.setOnPreparedListener(this);
        mPlayer.setOnErrorListener(this);
        mPlayer.setOnCompletionListener(this);
        mPlayer.setOnVideoSizeChangedListener(this);

        mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
    }

    private void preparePlayerFromUrl(String videoUrl) {
        mPlayer = new MediaPlayer();
        initPlayerListeners();
        mPlayer.setOnBufferingUpdateListener(this);

        try {
            mPlayer.setDataSource(videoUrl);
            mPlayer.prepareAsync();

        } catch (IllegalStateException e) {
            setVideoState(VideoState.BROKEN);

        } catch (IOException e) {
            setVideoState(VideoState.BROKEN);

        } catch (IllegalArgumentException e) {
            setVideoState(VideoState.BROKEN);

        } catch (SecurityException e) {
            setVideoState(VideoState.BROKEN);
        }
    }

    void muteVideo(boolean mute) {
        if (mPlayer != null) {
            mAdView.setVideoMute(mute);
            if (mute) {
                mPlayer.setVolume(0f, 0f);
            } else {
                float systemVolume = Utils.getSystemVolume();
                mPlayer.setVolume(systemVolume, systemVolume);
            }
        }
    }

    boolean isMediaPlayerValid() {
        return mAdView != null && mAdView.getCurrentVideoState() == VideoState.READY;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        Logging.out(LOG_TAG, "onPrepared", LogLevel.DEBUG);
        if (mTextureView != null) {
            SurfaceTexture surfaceTexture = mTextureView.getSurfaceTexture();
            Surface surface = new Surface(surfaceTexture);
            mPlayer.setSurface(surface);
        }
        if (mPostponeResize) {
            updateLayoutParams();
        }

        mVideoDuration = mPlayer.getDuration();
        mAdView.setVideoDuration(mVideoDuration);

        float systemVolume = Utils.getSystemVolume();
        mPlayer.setVolume(systemVolume, systemVolume);

        mPlayerReady = true;

        if (mEnoughBufferingLevel) {
            setVideoState(VideoState.READY);
        }
        if (!mIsFirstBufferingUpdate100) {
            mp.start();
            mp.pause();
        }
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        Logging.out(LOG_TAG, "onBufferingUpdate " + percent, LogLevel.DEBUG);
        if (percent < 0 || percent > 100) {
            return;
        }
        if (mAdView.getCurrentVideoState() == VideoState.BROKEN
                && percent == 100 && mIsFirstBufferingUpdate100) {

            mIsFirstBufferingUpdate100 = false;
            if (mPlayerReady) {
                //when comes fake onBufferingUpdate 100%, buffering process
                // stops without start/pause player
                mp.start();
                mp.pause();
            }
            return;
        }
        if (percent >= StaticParams.BUFFERING_LEVEL) {
            mEnoughBufferingLevel = true;
            mIsFirstBufferingUpdate100 = false;

            if (mPlayerReady && mAdView.getCurrentVideoState() != VideoState.PLAYING
                    && mAdView.getCurrentVideoState() != VideoState.PAUSED) {
                setVideoState(VideoState.READY);
            }

            if (mVideoTask != null && percent == 100) {
                mVideoTask.downloadVideo(null);
                mp.setOnBufferingUpdateListener(null);
            }
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Logging.out(LOG_TAG, "onError: " + extra, LogLevel.ERROR);

        mHandler.removeCallbacks(mRunnable);

        if (mAdView.getCurrentVideoState() == VideoState.BROKEN) {
            sendLoadFail(new LoopMeError("Error during video loading"));
        } else {

            mAdView.setWebViewState(WebviewState.HIDDEN);
            mAdView.setVideoState(VideoState.PAUSED);

            if (mFormat == AdFormat.BANNER) {
                LoopMeBanner banner = LoopMeAdHolder.getBanner(mAppKey, null);
                banner.playbackFinishedWithError();
            }
            mp.setOnErrorListener(null);
            mp.setOnCompletionListener(null);

            mPlayer.reset();

            mWasError = true;
        }
        return true;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if (mAdView.getCurrentVideoState() != VideoState.COMPLETE) {
            mHandler.removeCallbacks(mRunnable);
            mAdView.setVideoCurrentTime(mVideoDuration);
            mAdView.setVideoState(VideoState.COMPLETE);
            sendVideoReachEndNotification();
        }
    }

    private void sendVideoReachEndNotification() {
        BaseAd base;
        if (mFormat == AdFormat.BANNER) {
            base = LoopMeAdHolder.getBanner(mAppKey, null);
        } else {
            base = LoopMeAdHolder.getInterstitial(mAppKey, null);
        }
        if (base != null) {
            base.onAdVideoDidReachEnd();
        }
    }

    void resizeVideo(final TextureView texture, int viewWidth, int viewHeight) {

        Logging.out(LOG_TAG, "resizeVideo", LogLevel.DEBUG);

        mTextureView = texture;
        mResizeWidth = viewWidth;
        mResizeHeight = viewHeight;

        if (mPlayer == null || mVideoHeight == 0 || mVideoWidth == 0) {
            Logging.out(LOG_TAG, "postpone resize", LogLevel.DEBUG);
            mPostponeResize = true;
            return;
        } else {
            updateLayoutParams();
        }
    }

    void updateLayoutParams() {
        Logging.out(LOG_TAG, "updateLayoutParams()", LogLevel.DEBUG);

        if (mTextureView == null || mResizeWidth == 0 || mResizeHeight == 0
                || mVideoWidth == 0 || mVideoHeight == 0) {
            return;
        }

        FrameLayout.LayoutParams lp = (LayoutParams) mTextureView.getLayoutParams();
        lp.gravity = Gravity.CENTER;

        int blackLines;
        float percent = 0;

        if (mVideoWidth > mVideoHeight) {
            lp.width = mResizeWidth;
            lp.height = (int) ((float) mVideoHeight / (float) mVideoWidth * (float) mResizeWidth);

            blackLines = mResizeHeight - lp.height;
            if (lp.height != 0) {
                percent = blackLines * 100 / lp.height;
            }
        } else {
            lp.height = mResizeHeight;
            lp.width = (int) ((float) mVideoWidth / (float) mVideoHeight * (float) mResizeHeight);

            blackLines = mResizeWidth - lp.width;
            if (lp.width != 0) {
                percent = blackLines * 100 / lp.width;
            }
        }

        switch (mStretch) {
            case NONE:
                if (percent < 11) {
                    lp.width = mResizeWidth;
                    lp.height = mResizeHeight;
                }
                break;

            case STRECH:
                lp.width = mResizeWidth;
                lp.height = mResizeHeight;
                break;

            case NO_STRETCH:
                //
                break;
        }
        mTextureView.setLayoutParams(lp);
    }

    void setSurface(final TextureView textureView) {
        ExecutorHelper.getExecutor().submit(new Runnable() {

            @Override
            public void run() {
                if (textureView != null && textureView.isAvailable()) {
                    mTextureView = textureView;
                    SurfaceTexture surfaceTexture = textureView.getSurfaceTexture();
                    Surface surface = new Surface(surfaceTexture);
                    Logging.out(LOG_TAG, "mPlayer.setSurface()", LogLevel.DEBUG);
                    mPlayer.setSurface(surface);

                } else {
                    Logging.out(LOG_TAG, "textureView not Available ", LogLevel.DEBUG);
                }
            }
        });
    }
}