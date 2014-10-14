package com.votoxveto.app.helpers;


import android.os.Handler;
import android.os.Looper;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.votoxveto.app.ext.future.AndroidFutureCallbackWrapper;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AsyncHelper {

    public static final ExecutorService THREAD_POOL = Executors.newCachedThreadPool();

    private static Handler sUiHandler;

    public static <V> ListenableFuture<V> executeTask(final Callable<V> callable) {
        final SettableFuture<V> future = SettableFuture.create();
        THREAD_POOL.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    V ans = callable.call();
                    future.set(ans);
                } catch (Exception e) {
                    future.setException(e);
                }
            }
        });
        return future;
    }

    public static ListenableFuture<Void> executeTask(final Runnable runnable) {
        return executeTask(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                runnable.run();
                return null;
            }
        });
    }

    public static ListenableFuture<Void> timeOut(final long millis) {
        return executeTask(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                Thread.sleep(millis);
                return null;
            }
        });
    }

    public static <T> ListenableFuture<T> timeOut(final long millis, final T value) {
        return executeTask(new Callable<T>() {
            @Override
            public T call() throws Exception {
                Thread.sleep(millis);
                return value;
            }
        });
    }

    public static ListenableFuture<Void> executeWithDelay(final Runnable runnable, final long millis) {
        return executeTask(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                Thread.sleep(millis);
                runnable.run();
                return null;
            }
        });
    }

    public static <T> ListenableFuture<T> allWithFirstResult(ListenableFuture<T> future, ListenableFuture<?>... futures) {
        return Futures.transform(Futures.allAsList(Lists.asList(future, futures)), new Function<List<?>, T>() {
            @Override
            public T apply(List<?> list) {
                //noinspection unchecked
                return (T) list.get(0);
            }
        });
    }

    public static <T> void setWithDelay(final SettableFuture<T> future, final T value, final long millis) {
        THREAD_POOL.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                Thread.sleep(millis);
                future.set(value);
                return null;
            }
        });
    }

    public static void executeOnUiThread(Runnable task) {
        checkUiHandler();
        sUiHandler.post(task);
    }

    public static void executeOnUiThread(Runnable task, long millis) {
        checkUiHandler();
        sUiHandler.postDelayed(task, millis);
    }

    private static void checkUiHandler() {
        if (sUiHandler == null) {
            sUiHandler = new Handler(Looper.getMainLooper());
        }
    }

    public static <V> void addCallbackOnLooper(
            ListenableFuture<V> future,
            FutureCallback<? super V> callback,
            Looper looper) {
        Futures.addCallback(future, new AndroidFutureCallbackWrapper<>(callback, looper));
    }

    public static <V> void addCallbackOnUiThread(
            ListenableFuture<V> future,
            FutureCallback<? super V> callback) {
        addCallbackOnLooper(future, callback, Looper.getMainLooper());
    }

    // Prevents instantiation
    private AsyncHelper() {
        throw new AssertionError("Cannot instantiate object from " + this.getClass());
    }
}
