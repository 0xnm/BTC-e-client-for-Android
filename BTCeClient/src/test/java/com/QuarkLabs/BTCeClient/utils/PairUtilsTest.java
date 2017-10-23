package com.QuarkLabs.BTCeClient.utils;

import org.junit.Test;

import java.math.BigDecimal;
import java.util.HashMap;
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

}