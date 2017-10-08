package com.QuarkLabs.BTCeClient;

import com.QuarkLabs.BTCeClient.utils.PairUtils;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class PairUtilsTest {

    @Test
    public void localToServer() throws Exception {
        assertEquals("btc_usd", PairUtils.localToServer("BTC/USD"));
    }

    @Test
    public void serverToLocal() throws Exception {
        assertEquals("BTC/USD", PairUtils.serverToLocal("btc_usd"));
    }

    @Test
    public void currencyComparator_pairs() {
        List<String> pairs = Arrays.asList("NMCEH/RUR", "BTC/USD", "BCH/BTC", "BCHEH/BTC");

        Collections.sort(pairs, PairUtils.CURRENCY_COMPARATOR);

        assertEquals("BCH/BTC", pairs.get(0));
        assertEquals("BTC/USD", pairs.get(1));
        assertEquals("BCHEH/BTC", pairs.get(2));
        assertEquals("NMCEH/RUR", pairs.get(3));
    }

    @Test
    public void currencyComparator_currencies() {
        List<String> pairs = Arrays.asList("NMCEH", "BTC", "BCH", "BCHEH");

        Collections.sort(pairs, PairUtils.CURRENCY_COMPARATOR);

        assertEquals("BCH", pairs.get(0));
        assertEquals("BTC", pairs.get(1));
        assertEquals("BCHEH", pairs.get(2));
        assertEquals("NMCEH", pairs.get(3));
    }


}