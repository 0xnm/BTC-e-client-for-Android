package com.QuarkLabs.BTCeClient.api;

import android.content.Context;
import android.support.annotation.Nullable;

import com.google.gson.JsonParser;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static com.QuarkLabs.BTCeClient.api.AuthApi.TradeMethod.ACTIVE_ORDERS;
import static com.QuarkLabs.BTCeClient.api.AuthApi.TradeMethod.CANCEL_ORDER;
import static com.QuarkLabs.BTCeClient.api.AuthApi.TradeMethod.GET_INFO;
import static com.QuarkLabs.BTCeClient.api.AuthApi.TradeMethod.TRADE;
import static com.QuarkLabs.BTCeClient.api.AuthApi.TradeMethod.TRADE_HISTORY;
import static com.QuarkLabs.BTCeClient.api.AuthApi.TradeMethod.TRANSACTIONS_HISTORY;
import static com.QuarkLabs.BTCeClient.api.AuthApi.TradeMethod.WITHDRAW;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

@SuppressWarnings("WrongConstant")
public class ApiTest {

    private static final JsonParser JSON_PARSER = new JsonParser();

    @Mock
    private GuestApi mockGuestApi;
    @Mock
    private AuthApi mockAuthApi;
    @Mock
    private Context mockContext;

    private Api testable;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mockContext.getString(anyInt())).thenReturn("");
        testable = new Api(mockContext, "", mockGuestApi, mockAuthApi);
    }

    @Test
    public void bigDecimal_behavior() {
        assertNotEquals(new BigDecimal("1E+6"), new BigDecimal("1000000"));
        assertEquals(new BigDecimal("1E+6").toPlainString(),
                new BigDecimal("1000000").toPlainString());
        assertEquals(new BigDecimal("1000000").stripTrailingZeros().toString(), "1E+6");

        assertEquals(new BigDecimal("1.00000").stripTrailingZeros().toPlainString(), "1");
    }

    @Test
    public void info_success() throws Exception {
        when(mockGuestApi.call(anyString()))
                .thenReturn(JSON_PARSER.parse("{\"server_time\":1505766539," +
                        "\"pairs\":{\"btc_usd\":{\"decimal_places\":3,\"min_price\":0.1," +
                        "\"max_price\":10000,\"min_amount\":0.001,\"hidden\":0,\"fee\":0.2}," +
                        "\"btc_rur\":{\"decimal_places\":5,\"min_price\":1,\"max_price\":1000000," +
                        "\"min_amount\":0.001,\"hidden\":0,\"fee\":0.2}}}").getAsJsonObject());

        CallResult<ExchangeInfo> callResult = testable.getExchangeInfo();
        assertTrue(callResult.isSuccess());
        assertNull(callResult.getError());

        ExchangeInfo exchangeInfo = callResult.getPayload();

        assertNotNull(exchangeInfo);
        assertEquals(1505766539L, exchangeInfo.getServerTime());
        assertNotNull(exchangeInfo.getPairs());
        assertEquals(2, exchangeInfo.getPairs().size());

        ExchangePairInfo pairInfo = findByKey(exchangeInfo.getPairs(),
                new Predicate<ExchangePairInfo>() {
                    @Override
                    public boolean check(ExchangePairInfo element) {
                        return "BTC/RUR".equals(element.getPair());
                    }
                });
        assertNotNull(pairInfo);
        assertEquals("BTC/RUR", pairInfo.getPair());
        assertEquals(5, pairInfo.getDecimalPlaces());
        assertEquals(new BigDecimal("1"), pairInfo.getMinPrice());
        // because of stripTrailingZeros
        assertEquals(new BigDecimal("1000000").toPlainString(),
                pairInfo.getMaxPrice().toPlainString());
        assertEquals(new BigDecimal("0.001"), pairInfo.getMinAmount());
        assertFalse(pairInfo.isHidden());
        assertEquals(new BigDecimal("0.2"), pairInfo.getFee());
    }

    @Test
    public void info_error() throws Exception {
        when(mockGuestApi.call(anyString()))
                .thenReturn(JSON_PARSER.parse("{\"success\":0," +
                        " \"error\":\"API is disabled\"}").getAsJsonObject());

        CallResult<ExchangeInfo> callResult
                = testable.getExchangeInfo();

        assertFalse(callResult.isSuccess());
        assertNotNull(callResult.getError());
        assertNull(callResult.getPayload());
    }

    @Test
    public void getPairInfo_success() throws Exception {
        when(mockGuestApi.call(anyString()))
                .thenReturn(JSON_PARSER.parse("{\"btc_usd\":{\"high\":2381.507,\"low\":2300," +
                        "\"avg\":2340.7535,\"vol\":10359347.87922,\"vol_cur\":4423.75448," +
                        "\"last\":2374,\"buy\":2374,\"sell\":2373.551,\"updated\":1496436981}," +
                        "\"ltc_usd\":{\"high\":27.612,\"low\":25.60265,\"avg\":26.607325," +
                        "\"vol\":4404490.34434,\"vol_cur\":164911.27789,\"last\":26.768," +
                        "\"buy\":26.769,\"sell\":26.65201,\"updated\":1496436981}}")
                        .getAsJsonObject());

        CallResult<List<Ticker>> callResult
                = testable.getPairInfo(new HashSet<>(Arrays.asList("BTC/USD", "LTC/USD")));
        assertTrue(callResult.isSuccess());
        assertNull(callResult.getError());

        List<Ticker> tickers = callResult.getPayload();

        assertNotNull(tickers);
        assertEquals(2, tickers.size());

        Ticker ticker = findByKey(tickers, new Predicate<Ticker>() {
            @Override
            public boolean check(Ticker element) {
                return "LTC/USD".equals(element.getPair());
            }
        });

        assertNotNull(ticker);
        assertEquals(new BigDecimal("27.612"), ticker.getHigh());
        assertEquals(new BigDecimal("25.60265"), ticker.getLow());
        assertEquals(new BigDecimal("26.607325"), ticker.getAvg());
        assertEquals(new BigDecimal("4404490.34434"), ticker.getVol());
        assertEquals(new BigDecimal("164911.27789"), ticker.getVolCur());
        assertEquals(new BigDecimal("26.768"), ticker.getLast());
        assertEquals(new BigDecimal("26.769"), ticker.getBuy());
        assertEquals(new BigDecimal("26.65201"), ticker.getSell());
        assertEquals(1496436981L, ticker.getUpdated());
    }

    @Test
    public void getPairInfo_error() throws Exception {
        when(mockGuestApi.call(anyString()))
                .thenReturn(JSON_PARSER.parse("{\"success\":0," +
                        " \"error\":\"Invalid pair name: ltc_us\"}").getAsJsonObject());

        CallResult<List<Ticker>> callResult
                = testable.getPairInfo(new HashSet<>(Arrays.asList("BTC/USD", "LTC/US")));

        assertFalse(callResult.isSuccess());
        assertNotNull(callResult.getError());
        assertNull(callResult.getPayload());
    }

    @Test
    public void depth_success() throws Exception {
        when(mockGuestApi.call(anyString()))
                .thenReturn(JSON_PARSER.parse("{\"btc_usd\":{\"asks\":[[2377,1],[2377.45,0.15]" +
                        ",[2377.7,0.2]],\"bids\":[[2369.801,0.1],[2369.551,0.15]]}}")
                        .getAsJsonObject());

        CallResult<Depth> depthCallResult = testable.depth("BTC/USD");
        assertTrue(depthCallResult.isSuccess());
        assertNull(depthCallResult.getError());

        Depth depth = depthCallResult.getPayload();

        assertNotNull(depth);
        assertEquals("BTC/USD", depth.getPair());
        assertEquals(3, depth.getAsks().size());
        assertEquals(2, depth.getBids().size());
    }

    @Test
    public void depth_error() throws Exception {
        when(mockGuestApi.call(anyString()))
                .thenReturn(JSON_PARSER.parse("{\"success\":0," +
                        " \"error\":\"Invalid pair name: btc_us\"}").getAsJsonObject());

        CallResult<Depth> depthCallResult = testable.depth("BTC/US");
        assertFalse(depthCallResult.isSuccess());
        assertNotNull(depthCallResult.getError());
        assertNull(depthCallResult.getPayload());
    }

    @Test
    public void getAccountInfo_success() throws Exception {
        when(mockAuthApi.makeRequest(eq(GET_INFO), anyMapOf(String.class, String.class)))
                .thenReturn(JSON_PARSER.parse("{\n" +
                        "\t\"success\":1,\n" +
                        "\t\t\"return\":{\n" +
                        "\t\t\"funds\":{\n" +
                        "\t\t\t\"usd\":325,\n" +
                        "\t\t\t\"btc\":23.998,\n" +
                        "\t\t\t\"sc\":121.998,\n" +
                        "\t\t\t\"ltc\":0,\n" +
                        "\t\t\t\"ruc\":0,\n" +
                        "\t\t\t\"nmc\":0\n" +
                        "\t\t},\n" +
                        "\t\t\"rights\":{\n" +
                        "\t\t\t\"info\":1,\n" +
                        "\t\t\t\"trade\":1\n" +
                        "\t\t},\n" +
                        "\t\t\"transaction_count\":80,\n" +
                        "\t\t\"open_orders\":1,\n" +
                        "\t\t\"server_time\":1342123547\n" +
                        "\t}\n" +
                        "}").getAsJsonObject());

        CallResult<AccountInfo> callResult = testable.getAccountInfo();

        assertTrue(callResult.isSuccess());
        assertNull(callResult.getError());

        AccountInfo accountInfo = callResult.getPayload();
        assertNotNull(accountInfo);

        assertEquals(80, accountInfo.getTransactionCount());
        assertEquals(1, accountInfo.getOpenOrdersCount());
        assertEquals(1342123547, accountInfo.getServerTime());

        assertEquals(new BigDecimal("23.998"), accountInfo.getFunds().get("BTC"));
    }

    @Test
    public void getAccountInfo_error() throws Exception {
        when(mockAuthApi.makeRequest(eq(GET_INFO),
                anyMapOf(String.class, String.class)))
                .thenReturn(JSON_PARSER.parse("{\"success\":0,\"error\":\"error\"}")
                        .getAsJsonObject());

        CallResult<AccountInfo> callResult = testable.getAccountInfo();

        assertFalse(callResult.isSuccess());
        assertNotNull(callResult.getError());
        assertNull(callResult.getPayload());
    }

    @Test
    public void getTransactionsHistory_success() throws Exception {
        when(mockAuthApi.makeRequest(eq(TRANSACTIONS_HISTORY),
                anyMapOf(String.class, String.class)))
                .thenReturn(JSON_PARSER.parse("{\n" +
                        "\t\"success\":1,\n" +
                        "\t\"return\":{\n" +
                        "\t\t\"1081672\":{\n" +
                        "\t\t\t\"type\":1,\n" +
                        "\t\t\t\"amount\":1.00000000,\n" +
                        "\t\t\t\"currency\":\"BTC\",\n" +
                        "\t\t\t\"desc\":\"BTC Payment\",\n" +
                        "\t\t\t\"status\":2,\n" +
                        "\t\t\t\"timestamp\":1342448420\n" +
                        "\t\t}\n" +
                        "\t}\n" +
                        "}").getAsJsonObject());

        CallResult<List<Transaction>> callResult = testable.getTransactionsHistory(
                Collections.<String, String>emptyMap());

        assertTrue(callResult.isSuccess());
        assertNull(callResult.getError());
        List<Transaction> transactions = callResult.getPayload();
        assertNotNull(transactions);

        assertEquals(1, transactions.size());

        Transaction transaction = transactions.get(0);
        assertEquals(1081672, transaction.getId());
        assertEquals(1, transaction.getType());
        assertEquals(1342448420, transaction.getTimestamp());
        assertEquals(2, transaction.getStatus());
        assertEquals("BTC", transaction.getCurrency());
        assertEquals("BTC Payment", transaction.getDescription());
        assertEquals(new BigDecimal("1"), transaction.getAmount());
    }

    @Test
    public void getTransactionsHistory_error() throws Exception {
        when(mockAuthApi.makeRequest(eq(TRANSACTIONS_HISTORY),
                anyMapOf(String.class, String.class)))
                .thenReturn(JSON_PARSER.parse("{\"success\":0,\"error\":\"error\"}")
                        .getAsJsonObject());


        CallResult<List<Transaction>> callResult =
                testable.getTransactionsHistory(Collections.<String, String>emptyMap());

        assertFalse(callResult.isSuccess());
        assertNotNull(callResult.getError());
        assertNull(callResult.getPayload());
    }

    @Test
    public void getTradeHistory_success() throws Exception {
        when(mockAuthApi.makeRequest(eq(TRADE_HISTORY), anyMapOf(String.class, String.class)))
                .thenReturn(JSON_PARSER.parse("{\n" +
                        "\t\"success\":1,\n" +
                        "\t\"return\":{\n" +
                        "\t\t\"166830\":{\n" +
                        "\t\t\t\"pair\":\"btc_usd\",\n" +
                        "\t\t\t\"type\":\"sell\",\n" +
                        "\t\t\t\"amount\":1,\n" +
                        "\t\t\t\"rate\":1,\n" +
                        "\t\t\t\"order_id\":343148,\n" +
                        "\t\t\t\"is_your_order\":1,\n" +
                        "\t\t\t\"timestamp\":1342445793\n" +
                        "\t\t}\n" +
                        "\t}\n" +
                        "}").getAsJsonObject());

        CallResult<List<TradeHistoryEntry>>
                callResult = testable.getTradeHistory(Collections.<String, String>emptyMap());

        assertTrue(callResult.isSuccess());
        assertNull(callResult.getError());

        List<TradeHistoryEntry> trades = callResult.getPayload();
        assertNotNull(trades);

        assertEquals(1, trades.size());

        TradeHistoryEntry trade = trades.get(0);
        assertEquals(166830, trade.getId());
        assertEquals(343148, trade.getOrderId());
        assertEquals(1342445793, trade.getTimestamp());
        assertTrue(trade.isYourOrder());
        assertEquals("BTC/USD", trade.getPair());
        assertEquals("sell", trade.getType());
        assertEquals(new BigDecimal("1"), trade.getRate());
        assertEquals(new BigDecimal("1"), trade.getAmount());
    }

    @Test
    public void getTradeHistory_error() throws Exception {
        when(mockAuthApi.makeRequest(eq(TRADE_HISTORY), anyMapOf(String.class, String.class)))
                .thenReturn(JSON_PARSER.parse("{\"success\":0,\"error\":\"error\"}")
                        .getAsJsonObject());


        CallResult<List<TradeHistoryEntry>> callResult =
                testable.getTradeHistory(Collections.<String, String>emptyMap());

        assertFalse(callResult.isSuccess());
        assertNotNull(callResult.getError());
        assertNull(callResult.getPayload());
    }

    @Test
    public void activeOrders_success() throws Exception {
        when(mockAuthApi.makeRequest(eq(ACTIVE_ORDERS), anyMapOf(String.class, String.class)))
                .thenReturn(JSON_PARSER.parse("{\n" +
                        "\t\"success\":1,\n" +
                        "\t\"return\":{\n" +
                        "\t\t\"343154\":{\n" +
                        "\t\t\t\"pair\":\"btc_usd\",\n" +
                        "\t\t\t\"type\":\"sell\",\n" +
                        "\t\t\t\"amount\":1.00000000,\n" +
                        "\t\t\t\"rate\":3.00000000,\n" +
                        "\t\t\t\"timestamp_created\":1342448420,\n" +
                        "\t\t\t\"status\":0\n" +
                        "\t\t}\n" +
                        "\t}\n" +
                        "}").getAsJsonObject());

        CallResult<List<ActiveOrder>>
                callResult = testable.getActiveOrders();

        assertTrue(callResult.isSuccess());
        assertNull(callResult.getError());
        List<ActiveOrder> activeOrders = callResult.getPayload();
        assertNotNull(activeOrders);

        assertEquals(1, activeOrders.size());
        ActiveOrder activeOrder = activeOrders.get(0);

        assertEquals(343154, activeOrder.getId());
        assertEquals("BTC/USD", activeOrder.getPair());
        assertEquals("sell", activeOrder.getType());
        assertEquals(new BigDecimal("1"), activeOrder.getAmount());
        assertEquals(new BigDecimal("3"), activeOrder.getRate());
        assertEquals(1342448420, activeOrder.getCreatedAt());
        assertEquals(0, activeOrder.getStatus());
    }

    @Test
    public void activeOrders_error() throws Exception {
        when(mockAuthApi.makeRequest(eq(ACTIVE_ORDERS), anyMapOf(String.class, String.class)))
                .thenReturn(JSON_PARSER.parse("{\"success\":0,\"error\":\"error\"}")
                        .getAsJsonObject());


        CallResult<List<TradeHistoryEntry>> callResult =
                testable.getTradeHistory(Collections.<String, String>emptyMap());

        assertFalse(callResult.isSuccess());
        assertNotNull(callResult.getError());
        assertNull(callResult.getPayload());
    }

    @Test
    public void trade_success() throws Exception {
        when(mockAuthApi.makeRequest(eq(TRADE), anyMapOf(String.class, String.class)))
                .thenReturn(JSON_PARSER.parse("{\n" +
                        "\t\"success\":1,\n" +
                        "\t\"return\":{\n" +
                        "\t\t\"received\":0.1,\n" +
                        "\t\t\"remains\":0.2,\n" +
                        "\t\t\"order_id\":123456,\n" +
                        "\t\t\"funds\":{\n" +
                        "\t\t\t\"usd\":325,\n" +
                        "\t\t\t\"btc\":2.498,\n" +
                        "\t\t\t\"sc\":121.998,\n" +
                        "\t\t\t\"ltc\":0,\n" +
                        "\t\t\t\"ruc\":0,\n" +
                        "\t\t\t\"nmc\":0\n" +
                        "\t\t}\n" +
                        "\t}\n" +
                        "}").getAsJsonObject());

        CallResult<TradeResponse>
                callResult = testable.trade("", "", "", "");

        assertTrue(callResult.isSuccess());
        assertNull(callResult.getError());

        TradeResponse tradeResponse = callResult.getPayload();
        assertNotNull(tradeResponse);

        assertEquals(new BigDecimal("0.1"), tradeResponse.getReceived());
        assertEquals(new BigDecimal("0.2"), tradeResponse.getRemains());
        assertEquals(123456, tradeResponse.getOrderId());

        assertEquals(new BigDecimal("2.498"), tradeResponse.getFunds().get("BTC"));

    }

    @Test
    public void trade_error() throws Exception {
        when(mockAuthApi.makeRequest(eq(TRADE), anyMapOf(String.class, String.class)))
                .thenReturn(JSON_PARSER.parse("{\"success\":0,\"error\":\"error\"}")
                        .getAsJsonObject());


        CallResult<List<TradeHistoryEntry>> callResult =
                testable.getTradeHistory(Collections.<String, String>emptyMap());

        assertFalse(callResult.isSuccess());
        assertNotNull(callResult.getError());
        assertNull(callResult.getPayload());
    }

    @Test
    public void cancelOrder_success() throws Exception {
        when(mockAuthApi.makeRequest(eq(CANCEL_ORDER), anyMapOf(String.class, String.class)))
                .thenReturn(JSON_PARSER.parse("{\n" +
                        "\t\"success\":1,\n" +
                        "\t\"return\":{\n" +
                        "\t\t\"order_id\":343154,\n" +
                        "\t\t\"funds\":{\n" +
                        "\t\t\t\"usd\":325,\n" +
                        "\t\t\t\"btc\":24.998,\n" +
                        "\t\t\t\"sc\":121.998,\n" +
                        "\t\t\t\"ltc\":0,\n" +
                        "\t\t\t\"ruc\":0,\n" +
                        "\t\t\t\"nmc\":0\n" +
                        "\t\t}\n" +
                        "\t}\n" +
                        "}").getAsJsonObject());

        CallResult<CancelOrderResponse>
                callResult = testable.cancelOrder(123456);

        assertTrue(callResult.isSuccess());
        assertNull(callResult.getError());
        CancelOrderResponse cancelOrderResponse = callResult.getPayload();

        assertNotNull(cancelOrderResponse);
        assertEquals(343154, cancelOrderResponse.getOrderId());
        assertEquals(new BigDecimal("24.998"),
                cancelOrderResponse.getFunds().get("BTC"));
    }

    @Test
    public void cancelOrder_error() throws Exception {
        when(mockAuthApi.makeRequest(eq(CANCEL_ORDER), anyMapOf(String.class, String.class)))
                .thenReturn(JSON_PARSER.parse("{\"success\":0,\"error\":\"error\"}")
                        .getAsJsonObject());


        CallResult<CancelOrderResponse> callResult =
                testable.cancelOrder(123456);

        assertFalse(callResult.isSuccess());
        assertNotNull(callResult.getError());
        assertNull(callResult.getPayload());
    }

    @Test
    public void withdraw_success() throws Exception {
        when(mockAuthApi.makeRequest(eq(WITHDRAW), anyMapOf(String.class, String.class)))
                .thenReturn(JSON_PARSER.parse("{\n" +
                        "\t\"success\":1,\n" +
                        "\t\"return\":{\n" +
                        "\t\t\"tId\":37832629,\n" +
                        "\t\t\"amountSent\":0.009,\n" +
                        "\t\t\"funds\":{\n" +
                        "\t\t\t\"usd\":325,\n" +
                        "\t\t\t\"btc\":24.998,\n" +
                        "\t\t\t\"ltc\":0\n" +
                        "\t\t}\n" +
                        "\t}\n" +
                        "}").getAsJsonObject());

        CallResult<WithdrawResponse>
                callResult = testable.withdraw("BTC", 5, "1Az");

        assertTrue(callResult.isSuccess());
        assertNull(callResult.getError());
        WithdrawResponse response = callResult.getPayload();

        assertNotNull(response);
        assertEquals(37832629, response.getTransactionId());
        assertEquals(new BigDecimal("0.009"), response.getAmountSent());
        assertEquals(new BigDecimal("24.998"),
                response.getFunds().get("BTC"));
    }

    @Test
    public void withdraw_error() throws Exception {
        when(mockAuthApi.makeRequest(eq(CANCEL_ORDER), anyMapOf(String.class, String.class)))
                .thenReturn(JSON_PARSER.parse("{\"success\":0,\"error\":\"error\"}")
                        .getAsJsonObject());


        CallResult<WithdrawResponse> callResult = testable.withdraw("BTC", 5, "1Az");

        assertFalse(callResult.isSuccess());
        assertNotNull(callResult.getError());
        assertNull(callResult.getPayload());
    }

    @Nullable
    private <T> T findByKey(List<T> collection, Predicate<T> predicate) {
        collection = collection == null ? Collections.<T>emptyList() : collection;
        for (T element : collection) {
            if (predicate.check(element)) {
                return element;
            }
        }
        return null;
    }

    interface Predicate<T> {
        boolean check(T element);
    }

}