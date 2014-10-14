package com.votoxveto.app.ext.iterator;

import com.google.common.base.Throwables;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.votoxveto.app.helpers.AsyncHelper;
import com.votoxveto.app.helpers.CustomHelper;

import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public abstract class CachedAsyncIterator<T> implements AsyncIterator<T> {

    private final int mLowerBound;
    private final int mUpperBound;
    private volatile boolean mPendingRequest = false;
    private BlockingQueue<T> mQueue = new LinkedBlockingQueue<>();
    @SuppressWarnings("unchecked")
    private T mEndMarker = (T) new Object();

    protected CachedAsyncIterator(int lowerBound, int upperBound) {
        mLowerBound = lowerBound;
        mUpperBound = upperBound;
        triggerRequest(upperBound);
    }

    public int getCacheSize() {
        return mQueue.size();
    }

    /**
     * Retrieves a new object on the queue. Note that the result may be an immediate future, meaning the result could be
     * used right away, but it can also need to wait, hence we use a ListenableFuture. Moreover, you shouldn't make
     * subsequent next calls before one future returns.
     * @return a ListenableFuture representing the object, which can be null in case there are no more elements
     */
    @Override
    public ListenableFuture<T> next() {
        onNextRequest();
        final SettableFuture<T> ans = SettableFuture.create();
        AsyncHelper.THREAD_POOL.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    CustomHelper.log("queue(" + mQueue.size() + ") = " + mQueue);
                    T item = mQueue.take();
                    if (item == mEndMarker) {
                        mQueue.offer(item);
                        ans.set(null);
                    } else {
                        ans.set(item);
                    }
                } catch (InterruptedException e) {
                    Throwables.propagate(e);
                }
            }
        });
        if (mQueue.size() < mLowerBound && !mPendingRequest && !isProviderEmpty()) {
            triggerRequest(mUpperBound - mQueue.size());
        }
        return ans;
    }

    protected void onNextRequest() {
        /* Override */
    }

    private boolean isProviderEmpty() {
        return mQueue.peek() == mEndMarker;
    }

    private void triggerRequest(final int nitems) {
        mPendingRequest = true;
        AsyncHelper.THREAD_POOL.submit(new Runnable() {
            @Override
            public void run() {
                InternalYielder yielder = new InternalYielder();
                produce(nitems, mQueue, yielder);
                if (yielder.mCount < nitems) {
                    mQueue.add(mEndMarker);
                }
                mPendingRequest = false;
            }
        });
    }

    protected abstract void produce(int items, Queue<T> queue, Yielder<T> yielder);

    public static interface Yielder<T> {

        public void yield(T item);
    }

    private class InternalYielder implements Yielder<T> {

        private int mCount = 0;

        @Override
        public void yield(T item) {
            mQueue.offer(item);
            mCount++;
        }
    }
}
