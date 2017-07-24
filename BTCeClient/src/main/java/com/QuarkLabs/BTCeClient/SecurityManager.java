package com.QuarkLabs.BTCeClient;

import android.content.Context;
import android.provider.Settings;
import android.util.Base64;
import android.util.Log;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

final class SecurityManager {

    private static final String TAG = SecurityManager.class.getSimpleName();

    private static SecurityManager sInstance;
    private SecretKey mKey;

    private SecurityManager(Context context) {
        String androidId = Settings.Secure
                .getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        try {
            DESKeySpec keySpec = new DESKeySpec(androidId.getBytes("UTF8"));
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
            mKey = keyFactory.generateSecret(keySpec);
        } catch (InvalidKeyException | UnsupportedEncodingException
                | NoSuchAlgorithmException | InvalidKeySpecException e) {
            logException(e);
        }
    }

    public static SecurityManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new SecurityManager(context);
        }
        return sInstance;
    }

    String encryptString(String stringToEncrypt) {
        String output = stringToEncrypt;
        try {
            byte[] clearText = stringToEncrypt.getBytes("UTF8");
            Cipher cipher = Cipher.getInstance("DES"); // cipher is not thread safe
            cipher.init(Cipher.ENCRYPT_MODE, mKey);
            output = new String(Base64.encode(cipher.doFinal(clearText), Base64.DEFAULT), "UTF8");
        } catch (UnsupportedEncodingException | NoSuchAlgorithmException | NoSuchPaddingException
                | InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
            logException(e);
        }
        return output;
    }

    String decryptString(String stringToDecrypt) {
        String output = stringToDecrypt;
        try {
            byte[] encryptedPwdBytes = Base64.decode(stringToDecrypt, Base64.DEFAULT);
            Cipher cipher = Cipher.getInstance("DES"); // cipher is not thread safe
            cipher.init(Cipher.DECRYPT_MODE, mKey);
            output = new String(cipher.doFinal(encryptedPwdBytes), "UTF8");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException
                | UnsupportedEncodingException | IllegalBlockSizeException
                | BadPaddingException e) {
            logException(e);
        }
        return output;
    }

    private void logException(Exception e) {
        Log.e(TAG, "Exception during encoding/decoding sensitive data", e);
    }
}
