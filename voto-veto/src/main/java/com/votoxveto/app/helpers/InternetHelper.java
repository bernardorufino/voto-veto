package com.votoxveto.app.helpers;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import com.google.common.base.Function;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

public class InternetHelper {

    public static boolean hasInternetConnectivity(Context context) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    // Listener is called on UI Thread
    public static Future<Void> executeWhenConnectivityChange(final Context context, final Function<Boolean, Void> listener) {
        Future<Void> ans = AsyncHelper.executeTask(new Callable<Void>() {
            private boolean mState = hasInternetConnectivity(context);

            @Override
            public Void call() throws Exception {
                while (true) {
                    final boolean state = hasInternetConnectivity(context);
                    if (state != mState) {
                        mState = state;
                        AsyncHelper.executeOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                listener.apply(state);
                            }
                        });
                    }
                    Thread.sleep(300);
                }
            }
        });
        return ans;
    }

    // Prevents instantiation
    private InternetHelper() {
        throw new AssertionError("Cannot instantiate object from " + this.getClass());
    }
}
