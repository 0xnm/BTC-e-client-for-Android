package com.QuarkLabs.BTCeClient;


import android.support.annotation.NonNull;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

public final class DateTimeUtils {

    private DateTimeUtils() { }

    /**
     * Creates date/time format object for "EEE, MMM d, yyyy HH:mm:ss"
     *
     * @return {@link DateFormat} object
     */
    @NonNull
    public static DateFormat createLongDateTimeFormat() {
        return new SimpleDateFormat("EEE, MMM d, yyyy HH:mm:ss", Locale.US);
    }
}
