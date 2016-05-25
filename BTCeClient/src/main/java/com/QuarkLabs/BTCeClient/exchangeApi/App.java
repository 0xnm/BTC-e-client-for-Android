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

package com.QuarkLabs.BTCeClient.exchangeApi;

import android.content.Context;
import com.QuarkLabs.BTCeClient.BtcEApplication;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class App {

    private AuthRequest mAuthRequest;

    public App(Context context) {
        if (mAuthRequest == null) {
            mAuthRequest = new AuthRequest(System.currentTimeMillis() / 1000L, context);
        }
    }

    /**
     * Gets info for provided pairs
     *
     * @param pairs Array of pairs to get info for
     * @return JSONObject with data, sample https://btc-e.com/api/3/ticker/btc_usd-btc_rur
     * @throws JSONException
     */
    public static JSONObject getPairInfo(String[] pairs) throws JSONException {
        String url = BtcEApplication.getHostUrl() + "/api/3/ticker/";
        for (String x : pairs) {
            url += x.replace("/", "_").toLowerCase(Locale.US) + "-";
        }
        SimpleRequest reqSim = new SimpleRequest();
        return reqSim.makeRequest(url.substring(0, url.length() - 1));
    }

    /**
     * Gets account info
     *
     * @return JSONObject with account info, https://btc-e.com/api/documentation
     * @throws JSONException
     */
    public JSONObject getAccountInfo() throws JSONException {

        JSONObject response;
        response = mAuthRequest.makeRequest("getInfo", null);
        return response;

    }

    /**
     * Gets history of transactions
     *
     * @param params Possible parameters and their values, https://btc-e.com/api/documentation
     * @return JSONObject with transactions, https://btc-e.com/api/documentation
     * @throws JSONException
     */
    public JSONObject getTransactionsHistory(Map<String, String> params) throws JSONException {
        return mAuthRequest.makeRequest("TransHistory", params);
    }

    /**
     * Gets history of trades
     *
     * @param params Possible parameters and their values, https://btc-e.com/api/documentation
     * @return JSONObject with trades, https://btc-e.com/api/documentation
     * @throws JSONException
     */
    public JSONObject getTradeHistory(Map<String, String> params) throws JSONException {
        return mAuthRequest.makeRequest("TradeHistory", params);
    }

    /**
     * Gets active orders
     *
     * @return JSONObject with active orders, https://btc-e.com/api/documentation
     * @throws JSONException
     */
    public JSONObject getActiveOrders() throws JSONException {
        return mAuthRequest.makeRequest("ActiveOrders", null);
    }

    /**
     * Makes trade request
     *
     * @param pair   Pair to trade
     * @param type   Sell of Buy
     * @param rate   Trade price
     * @param amount Trade volume
     * @return JSONObject with trade response, https://btc-e.com/api/documentation
     * @throws JSONException
     */
    public JSONObject trade(String pair, String type, String rate, String amount) throws JSONException {

        HashMap<String, String> temp = new HashMap<>(4);
        temp.put("pair", pair);
        temp.put("type", type);
        temp.put("rate", rate);
        temp.put("amount", amount);

        return mAuthRequest.makeRequest("Trade", temp);

    }

    /**
     * Cancels order
     *
     * @param orderId Id of the order to cancel
     * @return JSONObject with cancellation response, https://btc-e.com/api/documentation
     * @throws JSONException
     */
    public JSONObject cancelOrder(int orderId) throws JSONException {

        Map<String, String> temp = new HashMap<>(1);
        temp.put("order_id", String.valueOf(orderId));

        return mAuthRequest.makeRequest("CancelOrder", temp);

    }

}