package com.QuarkLabs.BTCeClient;

import android.content.Context;
import android.preference.EditTextPreference;
import android.util.AttributeSet;

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

    @Override
    protected boolean persistString(String value) {
        return super.persistString(SecurityManager.getInstance(getContext())
                .encryptString(value));
    }

    @Override
    protected String getPersistedString(String defaultReturnValue) {
        return SecurityManager
                .getInstance(getContext())
                .decryptString(super.getPersistedString(defaultReturnValue));
    }
}
