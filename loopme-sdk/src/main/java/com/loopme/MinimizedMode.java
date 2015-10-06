package com.loopme;

import android.util.DisplayMetrics;
import android.view.ViewGroup;

import com.loopme.Logging.LogLevel;

public class MinimizedMode {

    private static final String LOG_TAG = MinimizedMode.class.getSimpleName();

    private int mWidth = 100;
    private int mHeight = 100;
    private int mMarginRight = 10;
    private int mMarginBottom = 10;
    private ViewGroup mRoot;

    public MinimizedMode(ViewGroup root) {
        if (root == null) {
            Logging.out(LOG_TAG, "Root view should be not null. Minimized mode will not work",
                    LogLevel.ERROR);
            return;
        }
        mRoot = root;

        DisplayMetrics dm = Utils.getDisplayMetrics(root.getContext());
        // portrait mode
        if (dm.heightPixels > dm.widthPixels) {
            mWidth = dm.widthPixels / 2;
        } else { //landscape mode
            mWidth = dm.widthPixels / 3;
        }
        mHeight = mWidth * 2 / 3;
        mWidth = mWidth - 6;
    }

    public void setViewSize(int width, int height) {
        mWidth = Utils.convertDpToPixel(width);
        mHeight = Utils.convertDpToPixel(height);
    }

    public void setMarginRight(int margin) {
        mMarginRight = Utils.convertDpToPixel(margin);
    }

    public void setMarginBottom(int margin) {
        mMarginBottom = Utils.convertDpToPixel(margin);
    }

    int getWidth() {
        return mWidth;
    }

    int getHeight() {
        return mHeight;
    }

    ViewGroup getRootView() {
        return mRoot;
    }

    int getMarginRight() {
        return mMarginRight;
    }

    int getMarginBottom() {
        return mMarginBottom;
    }
}
