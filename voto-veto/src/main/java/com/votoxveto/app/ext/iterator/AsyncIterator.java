package com.votoxveto.app.ext.iterator;

import com.google.common.util.concurrent.ListenableFuture;

public interface AsyncIterator<T> {

    /**
     * @return a future for the next object or null when it has finished
     */
    public ListenableFuture<T> next();

}
