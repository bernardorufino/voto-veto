package com.votoxveto.app.application;

import android.app.Application;
import com.parse.Parse;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CustomApplication extends Application {

    public static final String PREFERENCES_FILE = CustomApplication.class.getPackage().getName() + ".PREF";
    public static final ExecutorService THREAD_POOL = Executors.newCachedThreadPool();

    public void onCreate() {
        super.onCreate();
        Parse.initialize(this, "Va7QJffwIiQg0crCRvFQXXFJOEuoiy21M9euWSs0", "mBJ4GJzkWKIQSpxMyt1YRXD6BZRuPwO1VPrrsSOH");
    }
}
