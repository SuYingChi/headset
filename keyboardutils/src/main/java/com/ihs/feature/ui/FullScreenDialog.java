package com.ihs.feature.ui;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.PixelFormat;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import com.ihs.keyboardutils.BuildConfig;
import com.ihs.keyboardutils.utils.CommonUtils;

public abstract class FullScreenDialog extends FloatWindowDialog {

    @SuppressWarnings("PointlessBooleanExpression")
    private static final boolean DEBUG_SEMI_TRANSPARENT_CONTENT = false && BuildConfig.DEBUG;

    protected ViewGroup mContentView;

    public FullScreenDialog(Context context) {
        this(context, null);
    }

    public FullScreenDialog(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FullScreenDialog(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mContentView = (ViewGroup) View.inflate(getContext(), getResId(), this);
        if (IsInitStatusBarPadding()) {
            mContentView.getChildAt(0).setPadding(mContentView.getPaddingLeft(),
                    mContentView.getPaddingTop() + CommonUtils.getStatusBarHeight(getContext()),
                    mContentView.getPaddingRight(),
                    mContentView.getPaddingBottom());
        }
        if (DEBUG_SEMI_TRANSPARENT_CONTENT) {
            mContentView.setAlpha(0.7f);
        }
    }

    protected abstract int getResId();

    /**
     * Control content view padding top whether use status bar height or not. Default is true.
     */
    protected boolean IsInitStatusBarPadding() {
        return true;
    }

    @Override
    public WindowManager.LayoutParams getLayoutParams() {
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.type = WindowManager.LayoutParams.TYPE_PHONE;
        lp.format = PixelFormat.RGBA_8888;
        lp.flags |= WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED;
        lp.flags |= WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_FULLSCREEN |
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
        lp.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
        this.setLayoutParams(lp);
        return lp;
    }

    @Override
    public void dismiss() {
        onDismiss();
        FloatWindowManager.getInstance().removeDialog(getType());
    }

    protected void onDismiss() {
    }

    @Override
    public boolean shouldDismissOnLauncherStop() {
        return false;
    }
}

