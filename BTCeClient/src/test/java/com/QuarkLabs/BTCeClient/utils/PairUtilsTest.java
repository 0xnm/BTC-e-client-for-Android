package com.QuarkLabs.BTCeClient.utils;

import org.junit.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PairUtilsTest {
    @Test
    public void filterForNonZero() throws Exception {
        Map<String, BigDecimal> funds = new HashMap<>();

        funds.put("BTC", BigDecimal.ZERO);
        funds.put("LTC", new BigDecimal("1"));
        funds.put("ZEC", new BigDecimal("0.0"));
        funds.put("ETH", new BigDecimal("0"));
        funds.put("NVC", new BigDecimal("0.00"));
        funds.put("PPC", new BigDecimal("0.000000000001"));

        Map<String, BigDecimal> filtered = PairUtils.filterForNonZero(funds);

        assertEquals(2, filtered.size());
        assertEquals(1, filtered.get("LTC").intValue());
        assertTrue(filtered.containsKey("PPC"));
    }

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