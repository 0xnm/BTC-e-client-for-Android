/*
 * WEX client
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

package com.QuarkLabs.BTCeClient.ui.depth;

import android.content.AsyncTaskLoader;
import android.content.Context;

import com.QuarkLabs.BTCeClient.BtcEApplication;
import com.QuarkLabs.BTCeClient.api.Api;
import com.QuarkLabs.BTCeClient.api.CallResult;
import com.QuarkLabs.BTCeClient.api.Depth;

public class OrderBookLoader extends AsyncTaskLoader<CallResult<Depth>> {
    private final String pair;
    private CallResult<Depth> orders;
    private final Api api;

    public OrderBookLoader(Context context, String pair) {
        super(context);
        api = BtcEApplication.get(context).getApi();
        this.pair = pair;
    }

    @Override
    public CallResult<Depth> loadInBackground() {
        orders = api.depth(pair);
        return orders;
    }

    @Override
    protected void onStartLoading() {
        if (orders != null) {
            deliverResult(orders);
        }
        if (takeContentChanged() || orders == null) {
            forceLoad();
        }
    }
}
