package com.QuarkLabs.BTCeClient.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.preference.EditTextPreference;
import android.util.AttributeSet;

import com.QuarkLabs.BTCeClient.utils.SecurityManager;

public class EncryptedEditTextPreference extends EditTextPreference {
    public EncryptedEditTextPreference(Context context) {
        super(context);
    }

    public EncryptedEditTextPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public EncryptedEditTextPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public EncryptedEditTextPreference(Context context, AttributeSet attrs,
                                       int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected boolean persistString(String value) {
        return super.persistString(SecurityManager.getInstance(getContext())
                .encryptString(value));
    }

    @Override
    protected String getPersistedString(String defaultReturnValue) {
        String output = super.getPersistedString(defaultReturnValue);
        if (output == null || output.length() == 0) {
            return output;
        }
        return SecurityManager
                .getInstance(getContext())
                .decryptString(output);
    }
}
