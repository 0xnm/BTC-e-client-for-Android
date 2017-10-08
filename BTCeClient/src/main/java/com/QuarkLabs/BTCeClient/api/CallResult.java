package com.QuarkLabs.BTCeClient.api;

import android.support.annotation.Nullable;

public class CallResult<T> {
    boolean isSuccess;
    @Nullable
    String error;
    @Nullable
    T payload;

    /**
     * Creates new instance
     */
    CallResult() { }

    public boolean isSuccess() {
        return isSuccess;
    }

    @Nullable
    public String getError() {
        return error;
    }

    @Nullable
    public T getPayload() {
        return payload;
    }
}
