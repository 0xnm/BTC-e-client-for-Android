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

package com.QuarkLabs.BTCeClient.loaders;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.os.Bundle;
import com.QuarkLabs.BTCeClient.ListTypes;
import com.QuarkLabs.BTCeClient.MyActivity;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

public class OrdersLoader extends AsyncTaskLoader<JSONObject> {

    private final Bundle mBundle;
    private final ListTypes mType;
    private JSONObject mData;

    public OrdersLoader(Context context, Bundle bundle, ListTypes type) {
        super(context);
        mBundle = bundle;
        mType = type;
    }

    @Override
    public JSONObject loadInBackground() {
        Map<String, String> hashMap = new HashMap<>(2);
        switch (mType) {
            case ActiveOrders:
                try {
                    try {
                        mData = MyActivity.app.getActiveOrders();
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
            case Transactions:
                hashMap.put("since", mBundle.getString("startDate"));
                hashMap.put("end", mBundle.getString("endDate"));
                try {
                    mData = MyActivity.app.getTransactionsHistory(hashMap);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
            case TradeOrders:
                //TODO should be fixed
                hashMap.put("since", "0");
                hashMap.put("end", mBundle.getString("endDate"));
                try {
                    mData = MyActivity.app.getTradeHistory(hashMap);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
            default:
                break;
        }
        return mData;

    }

    @Override
    protected void onStartLoading() {
        if (mData != null) {
            deliverResult(mData);
        }
        if (takeContentChanged() || mData == null) {
            forceLoad();
        }
    }
}
