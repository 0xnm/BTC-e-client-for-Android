package com.QuarkLabs.BTCeClient.utils;

import android.content.Context;
import android.support.annotation.NonNull;

import com.QuarkLabs.BTCeClient.BtcEApplication;
import com.QuarkLabs.BTCeClient.api.ExchangeInfo;
import com.QuarkLabs.BTCeClient.api.ExchangePairInfo;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class PairUtils {

    private static final String LOCAL_PAIR_DELIMITER = "/";
    private static final String SERVER_PAIR_DELIMITER = "_";

    /**
     * Sorts in the following order: first pairs/currencies with normal tickers in alphabetical
     * order, then pairs/currencies with tokens
     */
    public static final Comparator<String> CURRENCY_COMPARATOR = (lhs, rhs) -> {
        int result;
        if (lhs == null) {
            result = -1;
        } else if (rhs == null) {
            result = 1;
        } else if (lhs.length() == rhs.length()) {
            result = lhs.compareTo(rhs);
        } else {
            result = lhs.length() - rhs.length();
        }

        return result;
    };

    private PairUtils() {
    }

    /**
     * Converts pair from local format to server format
     *
     * @param pair Pair in local format, ex. "BTC/USD"
     * @return Pair in server format, ex. "btc_usd"
     */
    @NonNull
    public static String localToServer(@NonNull String pair) {
        return pair.replace(LOCAL_PAIR_DELIMITER, SERVER_PAIR_DELIMITER).toLowerCase(Locale.US);
    }

    /**
     * Converts pair from server format to local format
     *
     * @param pair Pair in server format, ex. "btc_usd"
     * @return Pair in local format, ex. "BTC/USD"
     */
    @NonNull
    public static String serverToLocal(@NonNull String pair) {
        return pair.replace(SERVER_PAIR_DELIMITER, LOCAL_PAIR_DELIMITER).toUpperCase(Locale.US);
    }

    /**
     * Shows is pair is currently supported by exchange
     *
     * @param context Context
     * @param pair    Pair in local format
     * @return {@code True} if pair is supported by the exchange, {@code false} otherwise.
     */
    public static boolean isSupportedPair(@NonNull Context context, @NonNull String pair) {
        return supportedPairs(context).contains(pair);
    }

    private static List<String> supportedPairs(@NonNull Context context) {
        return BtcEApplication.get(context).getAppPreferences().getExchangePairs();
    }

    /**
     * Gets pairs supported by exchange from server reply.
     *
     * @param exchangeInfo Server reply.
     * @return Collection of pairs in local format.
     */
    @NonNull
    public static Set<String> exchangePairs(@NonNull ExchangeInfo exchangeInfo) {
        Set<String> pairs = new HashSet<>();
        for (ExchangePairInfo pairInfo : exchangeInfo.getPairs()) {
            pairs.add(pairInfo.getPair());
        }
        return pairs;
    }

    /**
     * Filters out zero values in the input funds
     *
     * @param funds Funds to filter
     * @return Funds without zero values
     */
    @NonNull
    public static Map<String, BigDecimal> filterForNonZero(
            @NonNull Map<String, BigDecimal> funds) {
        Map<String, BigDecimal> output = new HashMap<>();
        for (Map.Entry<String, BigDecimal> entry : funds.entrySet()) {
            if (entry.getValue().doubleValue() != 0) {
                output.put(entry.getKey(), entry.getValue());
            }
        }
        return output;
    }
}
