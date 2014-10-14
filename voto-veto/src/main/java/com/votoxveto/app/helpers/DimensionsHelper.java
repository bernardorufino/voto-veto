package com.votoxveto.app.helpers;

import android.content.Context;
import android.util.DisplayMetrics;

public class DimensionsHelper {

    public static int dpToPx(Context context, int dp) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        return Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
    }

    public static int pxToDp(Context context, int px) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        return Math.round(px / (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
    }

    // Prevents instantiation
    private DimensionsHelper() {
        throw new AssertionError("Cannot instantiate object from " + this.getClass());
    }
}
