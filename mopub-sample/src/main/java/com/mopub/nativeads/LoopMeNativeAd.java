package com.mopub.nativeads;

import android.content.Context;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;

import com.loopme.LoopMeAdapter;
import com.loopme.LoopMeBanner;
import com.loopme.LoopMeBannerView;
import com.loopme.LoopMeError;
import com.loopme.Utils;

public class LoopMeNativeAd extends BaseForwardingNativeAd implements LoopMeBanner.Listener {

    static final String TITLE = "LoopmeBanner";

    private LoopMeBannerView mBannerView;
    private LoopMeBanner mBanner;
    private boolean mLoadFailed;

    private boolean itemViewClean = true;
    private ViewGroup currentItem;

    private LoopMeEventNative.OnCompleteListener mListener;

    public LoopMeNativeAd(Context context, String appKey, LoopMeEventNative.OnCompleteListener listener) {
        setTitle(TITLE);
        mBanner = LoopMeBanner.getInstance(appKey, context);
        mBanner.setAdVisibilityListener(new LoopMeBanner.AdVisibilityListener() {
            @Override
            public void displayed() {
                Log.d("debug2", "displayed");

                if (itemViewClean) {
                    if (mBannerView.getParent() == null) {
                        Log.d("debug2", "rebuildView");
                        mBanner.bindView(mBannerView);
                        mBanner.show(null, null);
                        mBanner.rebuildView(mBannerView);
                        currentItem.addView(mBannerView);
                        itemViewClean = false;
                    }
                }

            }

            @Override
            public void hidden() {
                Log.d("debug2", "hidden");
                if (!itemViewClean) {
                    if (mBannerView != null && mBannerView.getParent() != null) {
                        ViewGroup listItem = (ViewGroup) mBannerView.getParent();
                        listItem.removeView(mBannerView);
                        itemViewClean = true;
                        Log.d("debug2", "restore View state");
                    }
                }

            }
        });

        mListener = listener;
        if (mBanner != null) {
            if (!mBanner.isReady() && !mBanner.isLoading() && !mLoadFailed) {
                Log.d("debug2", "load()");
                mBanner.setListener(this);
                mBanner.load();
            }
        }
        Log.d("debug2", "LoopMeNativeAd item created");
    }

    @Override
    public void prepare(View view) {
        Log.d("debug2", "prepare");
        if (view != null && view instanceof ViewGroup) {

            ViewGroup viewGroup = (ViewGroup) view;

            currentItem = viewGroup;

            if (mBannerView == null) {

                mBannerView = new LoopMeBannerView(view.getContext());
                viewGroup.addView(mBannerView);
                itemViewClean = false;

                int w = Utils.convertDpToPixel(320);
                int h = Utils.convertDpToPixel(250);

                mBannerView.setViewSize(w, h);

                if (view instanceof FrameLayout) {
                    FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) mBannerView.getLayoutParams();
                    params.gravity = Gravity.CENTER;
                    mBannerView.setLayoutParams(params);
                } else if (view instanceof RelativeLayout) {
                    RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mBannerView.getLayoutParams();
                    params.addRule(RelativeLayout.CENTER_IN_PARENT);
                    mBannerView.setLayoutParams(params);
                } else if (view instanceof LinearLayout) {
                    LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) mBannerView.getLayoutParams();
                    params.gravity = Gravity.CENTER;
                    mBannerView.setLayoutParams(params);
                }
                mBanner.bindView(mBannerView);
                mBanner.show(null, null);
            }
        }
    }

    @Override
    public void destroy() {
        Log.d("debug2", "destroy");
        if (mBanner != null) {
            mBanner.dismiss();
            mBanner.destroy();
            mListener.destroy();
        }
    }

    void onScroll(LoopMeAdapter adapter, ListView listView) {
        if (mBanner != null && mBanner.isShowing()) {
            mBanner.show(adapter, listView);
        }
    }

    void onPause() {
        if (mBanner != null) {
            mBanner.pause();
        }
    }

    void onResume(LoopMeAdapter adapter, ListView listView) {
        if (mBanner != null) {
            mBanner.show(adapter, listView);
        }
    }

    @Override
    public void onLoopMeBannerLoadSuccess(LoopMeBanner loopMeBanner) {
        Log.d("debug2", "onLoopMeBannerLoadSuccess");
        mListener.loaded();
    }

    @Override
    public void onLoopMeBannerLoadFail(LoopMeBanner banner, LoopMeError error) {
        Log.d("debug2", "onLoopMeBannerLoadFail");
        mLoadFailed = true;
        mListener.failed();
    }

    @Override
    public void onLoopMeBannerShow(LoopMeBanner loopMeBanner) {

    }

    @Override
    public void onLoopMeBannerHide(LoopMeBanner loopMeBanner) {

    }

    @Override
    public void onLoopMeBannerClicked(LoopMeBanner loopMeBanner) {

    }

    @Override
    public void onLoopMeBannerLeaveApp(LoopMeBanner loopMeBanner) {

    }

    @Override
    public void onLoopMeBannerVideoDidReachEnd(LoopMeBanner loopMeBanner) {

    }

    @Override
    public void onLoopMeBannerExpired(LoopMeBanner loopMeBanner) {

    }
}

