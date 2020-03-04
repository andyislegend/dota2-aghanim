package com.avenga.steamclient.util.retry;

import com.avenga.steamclient.exception.CallbackTimeoutException;

@FunctionalInterface
public interface SupplierCallbackHandler<T> {

    T get() throws CallbackTimeoutException;
}
