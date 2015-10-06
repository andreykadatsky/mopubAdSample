package com.loopme;

import android.view.MotionEvent;
import android.view.View;

public class SwipeListener implements View.OnTouchListener {

    private float initialX;

    private int mViewWidth;

    private Listener mListener;

    public SwipeListener(int viewWidth, Listener listener) {
        mViewWidth = viewWidth;
        mListener = listener;
    }

    public interface Listener {
        void onSwipe(boolean toRight);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                initialX = event.getX();
                break;

            case MotionEvent.ACTION_MOVE:
                return true;

            case MotionEvent.ACTION_UP:
                float finalX = event.getX();

                if (initialX < finalX) {
                    float distanceX = finalX - initialX;
                    if (mListener != null && distanceX > mViewWidth / 2) {
                        mListener.onSwipe(true);
                    }
                }

                if (initialX > finalX) {
                    float distanceX = initialX - finalX;
                    if (mListener != null && distanceX > mViewWidth / 2) {
                        mListener.onSwipe(false);
                    }
                }
                break;
        }
        return false;
    }
}
