package com.QuarkLabs.BTCeClient;

import android.support.annotation.StringDef;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static com.QuarkLabs.BTCeClient.WexLocale.CN;
import static com.QuarkLabs.BTCeClient.WexLocale.EN;
import static com.QuarkLabs.BTCeClient.WexLocale.RU;

@Target({ElementType.PARAMETER, ElementType.FIELD, ElementType.LOCAL_VARIABLE, ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
@StringDef(value = {EN, RU, CN})
public @interface WexLocale {
    String EN = "en";
    String CN = "cn";
    String RU = "ru";
}
