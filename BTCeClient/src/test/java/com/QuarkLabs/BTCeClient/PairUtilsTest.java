package com.QuarkLabs.BTCeClient;

import org.junit.Test;

import static org.junit.Assert.*;

public class PairUtilsTest {

    @Test
    public void localToServer() throws Exception {
        assertEquals("btc_usd", PairUtils.localToServer("BTC/USD"));
    }

    @Test
    public void serverToLocal() throws Exception {
        assertEquals("BTC/USD", PairUtils.serverToLocal("btc_usd"));
    }

}