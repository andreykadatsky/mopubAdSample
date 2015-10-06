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

    private static LoopMeBannerView mBannerView;
    private static LoopMeBanner mBanner;
    private static boolean mLoadFailed;

    private LoopMeEventNative.OnCompleteListener mListener;

    public LoopMeNativeAd(Context context, String appKey, LoopMeEventNative.OnCompleteListener listener) {
        mBanner = LoopMeBanner.getInstance(appKey, context);
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
            changeChildrenVisibility(viewGroup, View.GONE);

            if (mBannerView == null) {

                mBannerView = new LoopMeBannerView(view.getContext());
                viewGroup.addView(mBannerView);

                int w = Utils.convertDpToPixel(320);
                int h = Utils.convertDpToPixel(250);

                //mBannerView.setViewSize(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
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

    private void changeChildrenVisibility(ViewGroup viewGroup, int visibility) {
        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            View view = viewGroup.getChildAt(i);
            if (view instanceof LoopMeBannerView) {
                continue;
            }
            view.setVisibility(visibility);
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

