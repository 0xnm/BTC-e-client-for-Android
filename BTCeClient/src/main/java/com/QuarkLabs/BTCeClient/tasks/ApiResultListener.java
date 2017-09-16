package com.QuarkLabs.BTCeClient.tasks;

import android.support.annotation.NonNull;

/**
 * Listener for any call to WEX API
 *
 * @param <T> Result type
 */
public interface ApiResultListener<T> {
    /**
     * Called on success response
     *
     * @param result Result
     */
    void onSuccess(@NonNull T result);

    /**
     * Called if got error from server
     *
     * @param error Error text
     */
    void onError(@NonNull String error);
}
