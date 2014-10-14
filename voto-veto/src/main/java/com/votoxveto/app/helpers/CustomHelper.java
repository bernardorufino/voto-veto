package com.votoxveto.app.helpers;


import android.util.Log;

public class CustomHelper {

    public static final String INTENT_ACTION_MAIN = "android.intent.action.MAIN";
    public static final String INTENT_CATEGORY_LAUNCHER= "android.intent.category.LAUNCHER";
    private static final String LOG_TAG = "VOTOVETO";

//    public static boolean hasPlayServices(Context context) {
//        return GooglePlayServicesUtil.isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS;
//    }

    public static void log(String message) {
        Log.d(LOG_TAG, message);
    }

    public static void logException(Throwable e) {
        if (e != null) Log.e(LOG_TAG, e.toString(), e);
        else Log.d(LOG_TAG, "Logging null exception");
    }

    public static void logException(String msg, Throwable e) {
        if (e != null) Log.e(LOG_TAG, msg, e);
        else Log.d(LOG_TAG, "Logging null exception");
    }

    // Prevents instantiation
    private CustomHelper() {
        throw new AssertionError("Cannot instantiate object from " + this.getClass());
    }
}
