package com.launcher;

import android.content.Context;
import android.os.Bundle;
import android.view.ViewGroup;

/**
 * Abstraction for "locker screen"s.
 *
 * Terminology: lock screen = locker & charging screen.
 *
 * BE CONSISTENT with class and method names on this. Don't use non-standard terms like
 * "locker screen" or "charge screen". Don't use the term "locker" when referring to both
 * locker" and "charging screen". Use "lock screen" please.
 */
public abstract class LockScreen {

    protected ViewGroup mRootView;

    /**
     * Initialization.
     */
    public void setup(ViewGroup root, Bundle extra) {
        mRootView = root;
    }

    public ViewGroup getRootView() {
        return mRootView;
    }

    protected Context getContext() {
        return mRootView.getContext();
    }

    /**
     * @param dismissKeyguard Whether to remove system keyguard.
     */
    public void dismiss(Context context, boolean dismissKeyguard) {
        int hideType = (dismissKeyguard ? 0 : FloatWindowController.HIDE_LOCK_WINDOW_NO_ANIMATION);
        FloatWindowController.getInstance().hideLockScreen(hideType);
    }
}
