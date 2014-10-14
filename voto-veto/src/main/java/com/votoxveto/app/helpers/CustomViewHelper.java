package com.votoxveto.app.helpers;


import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.View;
import android.view.ViewManager;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import com.google.common.base.Function;

import static com.google.common.base.Preconditions.checkArgument;

public class CustomViewHelper {

    public static boolean hasAnimation() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
    }

    public static void removeOnGlobalLayoutListener(
            ViewTreeObserver observer,
            ViewTreeObserver.OnGlobalLayoutListener listener) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            observer.removeOnGlobalLayoutListener(listener);
        } else {
            observer.removeGlobalOnLayoutListener(listener);
        }
    }

    public static void tryRemoveOnGlobalLayoutListener(
            ViewTreeObserver observer,
            ViewTreeObserver.OnGlobalLayoutListener listener) {
        if (!observer.isAlive()) return;
        removeOnGlobalLayoutListener(observer, listener);
    }

    public static <T extends View> boolean executeOnNextLayout(final T view, final Function<? super T, Void> listener) {
        final ViewTreeObserver viewTreeObserver = view.getViewTreeObserver();
        if (viewTreeObserver.isAlive()) {
            viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    tryRemoveOnGlobalLayoutListener(viewTreeObserver, this);
                    tryRemoveOnGlobalLayoutListener(view.getViewTreeObserver(), this);
                    listener.apply(view);
                }
            });
            return true;
        }
        return false;
    }

    public static <T extends View> boolean executeOnLayout(final T view, final Function<? super T, Boolean> listener) {
        final ViewTreeObserver viewTreeObserver = view.getViewTreeObserver();
        if (viewTreeObserver.isAlive()) {
            viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    boolean remove = listener.apply(view);
                    if (remove) {
                        tryRemoveOnGlobalLayoutListener(viewTreeObserver, this);
                        tryRemoveOnGlobalLayoutListener(view.getViewTreeObserver(), this);
                    }
                }
            });
            return true;
        }
        return false;
    }

    public static void setBackground(View v, Drawable background) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            v.setBackground(background);
        } else {
            v.setBackgroundDrawable(background);
        }
    }

    public static void toggleVisibleGone(View view, boolean visibility) {
        view.setVisibility(visibility ? View.VISIBLE : View.GONE);
    }

    public static void toggleVisibleInvisible(View view, boolean visibility) {
        view.setVisibility(visibility ? View.VISIBLE : View.INVISIBLE);
    }


    public static boolean tryMakeOrphan(View view) {
        ViewParent parent = view.getParent();
        if (parent instanceof ViewManager) {
            ((ViewManager) parent).removeView(view);
            return true;
        }
        return false;
    }

    public static <T extends View> T makeOrphan(T view) {
        checkArgument(tryMakeOrphan(view), "Can't make view orphan.");
        return view;
    }

    // Prevents instantiation
    private CustomViewHelper() {
        throw new AssertionError("Cannot instantiate object from " + this.getClass());
    }
}
