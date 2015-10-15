package com.mopub.nativeads;

import android.content.Context;
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

/**
 * Preconditions: the root element of ads item should be FrameLayout.
 *
 * LoopMeBannerView added to ListView item on prepare()
 * and removed when ListView item disappears from the screen.
 *
 */
public class LoopMeNativeAd extends BaseForwardingNativeAd implements LoopMeBanner.Listener {

    static final String TITLE = "LoopmeBanner";
    static final String BANNER_HOLDER_TAG = "BannerViewHolder";

    public static final int BANNER_WIDTH = 320;
    public static final int BANNER_HEIGHT = 250;

    private LoopMeBannerView mBannerView;
    private FrameLayout mBannerViewHolder;
    private LoopMeBanner mBanner;
    private boolean mLoadFailed;

    private boolean itemViewClean = true;
    private ViewGroup currentItem;

    private LoopMeEventNative.OnCompleteListener mListener;

    private final int bannerBgColor;

    public LoopMeNativeAd(Context context, String appKey, int bannerBgColor, LoopMeEventNative.OnCompleteListener listener) {
        this.bannerBgColor = bannerBgColor;

        // used for detect LoopMe ad in LoopMeEventNative
        setTitle(TITLE);
        mBanner = LoopMeBanner.getInstance(appKey, context);
        mBanner.setAdVisibilityListener(new LoopMeBanner.AdVisibilityListener() {
            @Override
            public void displayed() {
                if (mBannerViewHolder.getParent() != null) {
                    ViewGroup parent = (ViewGroup) mBannerViewHolder.getParent();
                    ViewGroup.LayoutParams params = mBannerViewHolder.getLayoutParams();
                    params.width = parent.getWidth();
                    params.height = parent.getHeight();
                    mBannerViewHolder.setLayoutParams(params);
                }

                if (itemViewClean) {
                    if (mBannerViewHolder.getParent() == null) {
                        mBanner.bindView(mBannerView);
                        mBanner.show(null, null);
                        mBanner.rebuildView(mBannerView);

                        currentItem.addView(mBannerViewHolder);

                        itemViewClean = false;
                    }
                }

            }

            @Override
            public void hidden() {
                if (!itemViewClean) {
                    cleanListItemView();
                }

            }
        });

        mListener = listener;
        if (mBanner != null) {
            if (!mBanner.isReady() && !mBanner.isLoading() && !mLoadFailed) {
                mBanner.setListener(this);
                mBanner.load();
            }
        }
    }

    private void cleanListItemView() {
        if (mBannerView != null && mBannerViewHolder.getParent() != null) {
            ViewGroup listItem = (ViewGroup) mBannerViewHolder.getParent();
            listItem.removeView(mBannerViewHolder);
            itemViewClean = true;
        }
    }

    @Override
    public void prepare(View view) {

        if (view instanceof LinearLayout) {
            return;
        }

        if (view != null && view instanceof ViewGroup) {

            ViewGroup viewGroup = (ViewGroup) view;

            currentItem = viewGroup;

            if (mBannerView == null) {

                mBannerView = new LoopMeBannerView(view.getContext());

                mBannerViewHolder = new FrameLayout(view.getContext());
                mBannerViewHolder.setTag(BANNER_HOLDER_TAG);
                mBannerViewHolder.setBackgroundColor(bannerBgColor);
                mBannerViewHolder.addView(mBannerView);

                FrameLayout.LayoutParams frameParams = (FrameLayout.LayoutParams) mBannerView.getLayoutParams();
                frameParams.gravity = Gravity.CENTER;

                viewGroup.addView(mBannerViewHolder);

                mBannerViewHolder.bringToFront();
                viewGroup.invalidate();
                itemViewClean = false;

                int width = Utils.convertDpToPixel(BANNER_WIDTH);
                int height = Utils.convertDpToPixel(BANNER_HEIGHT);

                mBannerView.setViewSize(width, height);

                if (view instanceof FrameLayout) {
                    FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) mBannerViewHolder.getLayoutParams();
                    params.gravity = Gravity.CENTER;
                    mBannerViewHolder.setLayoutParams(params);
                } else if (view instanceof RelativeLayout) {
                    RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mBannerViewHolder.getLayoutParams();
                    params.addRule(RelativeLayout.CENTER_IN_PARENT);
                    mBannerViewHolder.setLayoutParams(params);
                }
                mBanner.bindView(mBannerView);
                mBanner.show(null, null);
            }
        }
    }

    @Override
    public void destroy() {
        if (mBanner != null) {
            cleanListItemView();
            mBanner.dismiss();
            mBanner.destroy();
            mBanner = null;
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
        mListener.loaded();
    }

    @Override
    public void onLoopMeBannerLoadFail(LoopMeBanner banner, LoopMeError error) {
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

