/*
 * BTC-e client
 *     Copyright (C) 2014  QuarkDev Solutions <quarkdev.solutions@gmail.com>
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.QuarkLabs.BTCeClient.api;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.annotation.WorkerThread;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Api {

    private static final String SUCCESS_KEY = "success";
    private static final String ERROR_KEY = "error";
    private static final String RETURN_KEY = "return";

    @NonNull
    private final String generalErrorText;
    @NonNull
    private final GuestApi guestApi;

    private AuthApi authApi;

    private String hostUrl;

    public Api(@NonNull String defaultError, @NonNull String hostUrl,
               @Nullable String apiKey, @Nullable String apiSecret) {
        this(defaultError, hostUrl,
                new GuestApi(),
                new AuthApi(System.currentTimeMillis() / 1000L,
                        apiKey == null ? "" : apiKey,
                        apiSecret == null ? "" : apiSecret,
                        hostUrl)
        );
    }

    @VisibleForTesting
    Api(@NonNull String defaultError, @NonNull String hostUrl,
        @NonNull GuestApi guestApi, @NonNull AuthApi authApi) {
        this.generalErrorText = defaultError;
        this.hostUrl = hostUrl;
        this.guestApi = guestApi;
        this.authApi = authApi;
    }

    public void setCredentials(@NonNull String apiKey, @NonNull String apiSecret) {
        authApi = new AuthApi(System.currentTimeMillis() / 1000L, apiKey,
                apiSecret, hostUrl);
    }

    public void setHostUrl(@NonNull String hostUrl) {
        this.hostUrl = hostUrl;
        authApi.setHostUrl(hostUrl);
    }

    /**
     * Gets info for provided pairs
     *
     * @param pairs Array of pairs to get info for
     * @return List of tickers, sample https://btc-e.com/api/3/ticker/btc_usd-btc_rur
     */
    @WorkerThread
    @NonNull
    public CallResult<List<Ticker>> getPairInfo(@NonNull List<String> pairs) {
        try {
            String url = hostUrl + "/api/3/ticker/";
            for (String x : pairs) {
                url += x.replace("/", "_").toLowerCase(Locale.US) + "-";
            }
            JSONObject response = guestApi.call(url.substring(0, url.length() - 1));
            CallResult<List<Ticker>> result = new CallResult<>();
            if (response == null || response.optInt(SUCCESS_KEY, 1) == 0) {
                result.isSuccess = false;
                result.error = response == null ?
                        generalErrorText : response.optString(ERROR_KEY, generalErrorText);
                return result;
            }

            result.isSuccess = true;
            Iterator<String> pairsIterator = response.keys();
            List<Ticker> tickers = new ArrayList<>();
            while (pairsIterator.hasNext()) {
                String pair = pairsIterator.next();
                tickers.add(Ticker.createFromServer(pair, response.getJSONObject(pair)));
            }
            result.payload = tickers;
            return result;
        } catch (JSONException e) {
            throw new JsonParseException(e);
        }
    }

    /**
     * Get depth for the given pair
     *
     * @param pair Pair (in app format, ex. "BTC/USD")
     * @return Depth call result
     */
    @NonNull
    @WorkerThread
    public CallResult<Depth> depth(@NonNull String pair) {
        try {
            pair = pair.toLowerCase(Locale.US).replace("/", "_");
            final String url = hostUrl + "/api/3/depth/" + pair;
            JSONObject response = guestApi.call(url);
            CallResult<Depth> result = new CallResult<>();
            if (response == null || response.optInt(SUCCESS_KEY, 1) == 0) {
                result.isSuccess = false;
                result.error = response == null ?
                        generalErrorText : response.optString(ERROR_KEY, generalErrorText);
                return result;
            }

            result.isSuccess = true;
            result.payload = Depth.create(pair, response.getJSONObject(pair));
            return result;
        } catch (JSONException e) {
            throw new JsonParseException(e);
        }
    }

    /**
     * Gets account info
     *
     * @return Account info, https://btc-e.com/api/documentation
     */
    @NonNull
    @WorkerThread
    public CallResult<AccountInfo> getAccountInfo() {

        try {
            JSONObject response = authApi.makeRequest(AuthApi.TradeMethod.GET_INFO, null);
            CallResult<AccountInfo> result = new CallResult<>();
            if (response == null || response.getInt(SUCCESS_KEY) == 0) {
                result.isSuccess = false;
                result.error = response == null ?
                        generalErrorText : response.optString(ERROR_KEY, generalErrorText);
                return result;
            }

            result.isSuccess = true;
            result.payload = AccountInfo.create(response.getJSONObject(RETURN_KEY));
            return result;

        } catch (JSONException e) {
            throw new JsonParseException(e);
        }
    }

    /**
     * Gets history of transactions.
     *
     * @param parameters Possible parameters and their values, https://btc-e.com/api/documentation
     * @return Result with transactions, https://btc-e.com/api/documentation
     */
    @NonNull
    @WorkerThread
    public CallResult<List<Transaction>> getTransactionsHistory(
            @NonNull Map<String, String> parameters) {
        try {
            JSONObject response = authApi.makeRequest(AuthApi.TradeMethod.TRANSACTIONS_HISTORY,
                    parameters);
            CallResult<List<Transaction>> result = new CallResult<>();
            if (response == null || response.getInt(SUCCESS_KEY) == 0) {
                result.isSuccess = false;
                result.error = response == null ?
                        generalErrorText : response.optString(ERROR_KEY, generalErrorText);
                return result;
            }

            result.isSuccess = true;
            List<Transaction> transactions = new ArrayList<>();
            Iterator<String> transactionIdsIterator = response.getJSONObject(RETURN_KEY).keys();
            while (transactionIdsIterator.hasNext()) {
                String key = transactionIdsIterator.next();
                long transactionId = Long.parseLong(key);
                transactions.add(Transaction.create(transactionId,
                        response.getJSONObject(RETURN_KEY).getJSONObject(key)));
            }
            result.payload = transactions;
            return result;
        } catch (JSONException e) {
            throw new JsonParseException(e);
        }
    }

    /**
     * Gets history of trades.
     *
     * @param parameters Possible parameters and their values, https://btc-e.com/api/documentation
     * @return Result with trades, https://btc-e.com/api/documentation
     */
    @NonNull
    @WorkerThread
    public CallResult<List<TradeHistoryEntry>> getTradeHistory(
            @NonNull Map<String, String> parameters) {
        try {
            JSONObject response = authApi.makeRequest(AuthApi.TradeMethod.TRADE_HISTORY,
                    parameters);
            CallResult<List<TradeHistoryEntry>> result = new CallResult<>();
            if (response == null || response.getInt(SUCCESS_KEY) == 0) {
                result.isSuccess = false;
                result.error = response == null ?
                        generalErrorText : response.optString(ERROR_KEY, generalErrorText);
                return result;
            }

            result.isSuccess = true;
            List<TradeHistoryEntry> trades = new ArrayList<>();
            Iterator<String> transactionIdsIterator = response.getJSONObject(RETURN_KEY).keys();
            while (transactionIdsIterator.hasNext()) {
                String key = transactionIdsIterator.next();
                long tradeId = Long.parseLong(key);
                trades.add(TradeHistoryEntry.create(tradeId,
                        response.getJSONObject(RETURN_KEY).getJSONObject(key)));
            }
            result.payload = trades;
            return result;
        } catch (JSONException e) {
            throw new JsonParseException(e);
        }
    }

    /**
     * Gets active orders
     *
     * @return Result with active orders, https://btc-e.com/api/documentation
     */
    @NonNull
    @WorkerThread
    public CallResult<List<ActiveOrder>> getActiveOrders() {
        try {
            JSONObject response = authApi.makeRequest(AuthApi.TradeMethod.ACTIVE_ORDERS, null);
            CallResult<List<ActiveOrder>> result = new CallResult<>();
            if (response == null || response.getInt(SUCCESS_KEY) == 0) {
                result.isSuccess = false;
                result.error = response == null ?
                        generalErrorText : response.optString(ERROR_KEY, generalErrorText);
                return result;
            }

            result.isSuccess = true;
            List<ActiveOrder> activeOrders = new ArrayList<>();
            Iterator<String> orderIdsIterator = response.getJSONObject(RETURN_KEY).keys();
            while (orderIdsIterator.hasNext()) {
                String key = orderIdsIterator.next();
                long orderId = Long.parseLong(key);
                activeOrders.add(ActiveOrder.create(orderId,
                        response.getJSONObject(RETURN_KEY).getJSONObject(key)));
            }
            result.payload = activeOrders;
            return result;
        } catch (JSONException e) {
            throw new JsonParseException(e);
        }
    }

    /**
     * Makes trade request
     *
     * @param pair   Pair to trade
     * @param type   Sell of Buy ("sell" or "buy")
     * @param rate   Trade price
     * @param amount Trade volume
     * @return Trade response, https://btc-e.com/api/documentation
     */
    @NonNull
    @WorkerThread
    public CallResult<TradeResponse> trade(@NonNull String pair, @NonNull String type,
                                           @NonNull String rate, @NonNull String amount) {

        try {
            HashMap<String, String> parameters = new HashMap<>();
            parameters.put("pair", pair);
            parameters.put("type", type);
            parameters.put("rate", rate);
            parameters.put("amount", amount);

            JSONObject response = authApi.makeRequest(AuthApi.TradeMethod.TRADE, parameters);

            CallResult<TradeResponse> result = new CallResult<>();
            if (response == null || response.getInt(SUCCESS_KEY) == 0) {
                result.isSuccess = false;
                result.error = response == null ?
                        generalErrorText : response.optString(ERROR_KEY, generalErrorText);
                return result;
            }

            result.isSuccess = true;
            result.payload = TradeResponse.create(response.getJSONObject(RETURN_KEY));
            return result;
        } catch (JSONException e) {
            throw new JsonParseException(e);
        }

    }

    /**
     * Cancels order
     *
     * @param orderId Id of the order to cancel
     * @return Cancellation response, https://btc-e.com/api/documentation
     */
    @NonNull
    @WorkerThread
    public CallResult<CancelOrderResponse> cancelOrder(int orderId) {

        try {
            Map<String, String> parameters = new HashMap<>();
            parameters.put("order_id", String.valueOf(orderId));

            JSONObject response = authApi.makeRequest(AuthApi.TradeMethod.CANCEL_ORDER, parameters);
            CallResult<CancelOrderResponse> result = new CallResult<>();
            if (response == null || response.getInt(SUCCESS_KEY) == 0) {
                result.isSuccess = false;
                result.error = response == null ?
                        generalErrorText : response.optString(ERROR_KEY, generalErrorText);
                return result;
            }

            result.isSuccess = true;
            result.payload = CancelOrderResponse.create(response.getJSONObject(RETURN_KEY));
            return result;
        } catch (JSONException e) {
            throw new JsonParseException(e);
        }
    }

}