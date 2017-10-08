package com.QuarkLabs.BTCeClient.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.preference.EditTextPreference;
import android.util.AttributeSet;

public class NoSpaceEditTextPreference extends EditTextPreference {
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public NoSpaceEditTextPreference(Context context, AttributeSet attrs,
                                     int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public NoSpaceEditTextPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public NoSpaceEditTextPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public NoSpaceEditTextPreference(Context context) {
        super(context);
    }

    @Override
    protected boolean persistString(String value) {
        return super.persistString(value.trim());
    }
}
